package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Process
import android.os.SystemClock
import android.view.MotionEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Two separately instrumented :game processes, selected with -e stage create|restore.
 * Uses an already imported, original legacy DS fixture and cached MELONDS core.
 * The coordinator must back up baseline saves before running and choose a fresh qaCaptureRun.
 * Only production background lifecycle writes the measured autosave. No save seeding or direct save calls.
 * Injected WindowManager metadata is emulator evidence, not physical-foldable certification.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class ActiveBackgroundSaveTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var evidence: File

    @Test
    fun activeBackgroundSaveAndFreshProcessRestore() {
        assumeTrue("Select GameProcessTestRunner explicitly", instrumentation is GameProcessTestRunner)
        val processName =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .runningAppProcesses.single { it.pid == Process.myPid() }.processName
        assertEquals("${context.packageName}:game", processName)
        val args = InstrumentationRegistry.getArguments()
        val stage = checkNotNull(args.getString("stage")) { "Supply -e stage create or restore" }
        require(stage == "create" || stage == "restore")
        val run = checkNotNull(args.getString("qaCaptureRun")) { "Supply the same fresh qaCaptureRun for both stages" }
        require(run.matches(Regex("[A-Za-z0-9_-]+")))
        val title = args.getString("gameTitle") ?: "EmuUI_DS_Legacy_Long_Title_QA"
        val game = readImportedGame(title)
        val core = GameSystem.findById(game.systemId).systemCoreConfigs.single { it.coreID.name == "MELONDS" }
        val root = checkNotNull(context.getExternalFilesDir(null))
        evidence = File(root, "qa-active-save/$run").apply { check(isDirectory || mkdirs()) }
        val createFile = File(evidence, "create.json")
        val expected = if (stage == "restore") JSONObject(createFile.readText()) else null
        if (stage == "create") {
            check(!createFile.exists()) { "Use a fresh qaCaptureRun; do not replace successful create evidence" }
        } else {
            assertEquals("Create stage must have completed", true, expected!!.getBoolean("passed"))
            assertEquals(game.id.toString(), expected.getString("gameId"))
            assertEquals(game.fileName, expected.getString("gameFileName"))
            assertNotEquals("Restore must use a genuinely new :game process", expected.getInt("pid"), Process.myPid())
        }
        val saves =
            linkedMapOf(
                "autosave" to File(root, "states/${core.coreID.coreName}/${game.fileName}.state"),
                "metadata" to File(root, "states/${core.coreID.coreName}/${game.fileName}.state.metadata"),
                "sram" to File(root, "saves/${game.fileName.substringBeforeLast(".")}.srm"),
            )
        assertTrue(
            "An existing fixture autosave and coordinator backup are prerequisites",
            saves.getValue("autosave").isFile,
        )
        val before = saves.mapValues { fingerprint(it.value) }
        // melonDS DS v1.2.0 config.cpp:94,423-469 owns this writable DLDI SD image.
        // Exclude this exact file only; its .idx and all other core files remain protected.
        val coreAuxiliaryFile = File(root, "saves/melonDS DS/dldi_sd_card.bin")
        val coreAuxiliaryBefore = fingerprint(coreAuxiliaryFile)
        val permittedMutableFiles = saves.values.toSet() + coreAuxiliaryFile
        val protectedBefore = protectedSaves(root, permittedMutableFiles)
        if (expected != null) {
            assertEquals(
                "Restore must read the exact new background autosave",
                expected.getJSONObject("afterBackground").getString("autosave"),
                before.getValue("autosave"),
            )
            assertEquals(
                "Autosave metadata must remain paired",
                expected.getJSONObject("afterBackground").getString("metadata"),
                before.getValue("metadata"),
            )
        }
        val result =
            JSONObject()
                .put("stage", stage).put("pid", Process.myPid()).put("processName", processName)
                .put("gameId", game.id.toString()).put("gameFileName", game.fileName)
                .put("core", core.coreID.name).put("loadSave", stage == "restore")
                .put("startedAtEpochMs", System.currentTimeMillis()).put("before", JSONObject(before))
                .put(
                    "evidenceType",
                    "Actual native DS frames; injected WindowLayoutInfoPublisherRule; real Activity onStop save",
                )
        val activity = AtomicReference<GameActivity>()
        val model = AtomicReference<BaseGameScreenViewModel>()
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        val scenario =
            ActivityScenario.launch<GameActivity>(
                Intent(context, GameActivity::class.java).apply {
                    putExtra("GAME", game)
                    putExtra("LOAD_SAVE", stage == "restore")
                    putExtra("LEANBACK", false)
                    putExtra("EXTRA_SYSTEM_CORE_CONFIG", core)
                },
            )
        var after = before
        try {
            scenario.onActivity {
                activity.set(it)
                model.set(ViewModelProvider(it)[BaseGameScreenViewModel::class.java])
            }
            // Pump Compose through Loaded into the real MobileGameScreen. The loading
            // guidance has a different subscriber, and the publisher has replay=0.
            waitForTag(SURFACE, 180_000)
            waitForTag(GUIDANCE)
            publish(activity.get())
            waitForTag(LEFT)
            val native = awaitUsableCore(model.get())
            compose.onNodeWithText("Accessible controls").performClick()
            compose.onNodeWithContentDescription("A button").assertIsDisplayed()
            if (stage == "create") {
                val initial = capture("00-new-session")
                val initialTuple = initial.getJSONObject("decodedFromPixels")
                assertEquals("LOAD_SAVE=false must start the original program", 17, initialTuple.getInt("state"))
                assertEquals(0, initialTuple.getInt("touches"))
                assertEquals(128, initialTuple.getInt("x"))
                assertEquals(112, initialTuple.getInt("y"))
                for (button in listOf("Start", "B", "A", "A")) {
                    compose.onNodeWithContentDescription("$button button").performClick()
                    // Accessible taps release after 120 ms; separate the two A presses.
                    runBlocking { delay(250) }
                    awaitFrames(native)
                }
                val lower = bounds(LOWER)
                touch(lower.left + lower.width * 80f / 256f, lower.top + lower.height * 128f / 192f, native)
                val frozen = capture("01-frozen-before-background")
                val tuple = frozen.getJSONObject("decodedFromPixels")
                assertEquals("Two real A presses and B must change 17 to 29", 29, tuple.getInt("state"))
                assertTrue("Actual touch X must be near 80", tuple.getInt("x") in 78..82)
                assertTrue("Actual touch Y must be near 128", tuple.getInt("y") in 126..130)
                assertEquals("One real lower-screen contact", 1, tuple.getInt("touches"))
                awaitFrames(native)
                assertScreensEqual(
                    "Start must freeze actual frame/state pixels",
                    frozen,
                    capture("02-frozen-confirmed"),
                )
                scenario.onActivity {
                    assertTrue(
                        "Fully initialized active core must be save eligible",
                        model.get().retroGameView.canCaptureState,
                    )
                    assertTrue(
                        "The core must actually be running before background",
                        model.get().retroGameView.isNativeViewResumed,
                    )
                }
                result.put("beforeBackground", frozen).put("canCaptureStateBeforeBackground", true)
                scenario.moveToState(Lifecycle.State.CREATED)
                assertEquals(Lifecycle.State.CREATED, scenario.state)
                val drained = CompletableDeferred<Unit>()
                GameService.schedule { drained.complete(Unit) }
                runBlocking { withTimeout(45_000) { drained.await() } }
                instrumentation.runOnMainSync {
                    assertFalse("Background must pause native rendering", model.get().retroGameView.isNativeViewResumed)
                    assertTrue(
                        "Paused initialized core must still be capturable",
                        model.get().retroGameView.canCaptureState,
                    )
                }
                after = saves.mapValues { fingerprint(it.value) }
                assertTrue("Background must persist nonempty autosave bytes", saves.getValue("autosave").length() > 0)
                assertTrue("Background must persist metadata bytes", saves.getValue("metadata").length() > 0)
                assertNotEquals(
                    "The real background save must change actual autosave bytes",
                    before.getValue("autosave").substringAfterLast(':'),
                    after.getValue("autosave").substringAfterLast(':'),
                )
                result.put("afterBackground", JSONObject(after)).put("gameServiceBarrierCompleted", true)
                scenario.moveToState(Lifecycle.State.RESUMED)
                scenario.onActivity { activity.set(it) }
                waitForTag(SURFACE)
                windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
                waitForTag(GUIDANCE)
                publish(activity.get())
                waitForTag(LEFT)
                awaitUsableCore(model.get())
                assertSame(
                    "Background/resume must retain the real native view",
                    native,
                    model.get().retroGameView.existingRetroView(),
                )
                val resumed = capture("03-native-after-resume")
                assertScreensEqual("Background/resume must preserve both native displays and tuple", frozen, resumed)
                result.put("afterResume", resumed)
            } else {
                val restored = capture("04-fresh-process-restored")
                assertScreensEqual(
                    "LOAD_SAVE=true must restore actual state in a new process",
                    expected!!.getJSONObject("beforeBackground"),
                    restored,
                )
                awaitFrames(native)
                assertScreensEqual(
                    "Restored native tuple and pixels must remain frozen",
                    restored,
                    capture("05-restored-confirmed"),
                )
                after = saves.mapValues { fingerprint(it.value) }
                assertEquals("Restore itself must not rewrite saves", before, after)
                result.put("createdInPid", expected.getInt("pid")).put("restored", restored)
                    .put("canCaptureStateAfterRestore", true).put("afterRestore", JSONObject(after))
            }
        } finally {
            // Stop the consumer before Activity teardown requests exitProcess, so JUnit survives.
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
        result.put(
            "coreOwnedAuxiliary",
            JSONObject()
                .put("path", coreAuxiliaryFile.relativeTo(root).path)
                .put("before", coreAuxiliaryBefore)
                .put("after", fingerprint(coreAuxiliaryFile))
                .put("reason", "Current MELONDS core writable DLDI SD image; not an app autosave, manual slot or SRAM"),
        )
        assertEquals(
            "Unrelated autosaves and all manual slots must be unchanged",
            protectedBefore,
            protectedSaves(root, permittedMutableFiles),
        )
        assertEquals(
            "Final cleanup must not rewrite the measured saves",
            after,
            saves.mapValues { fingerprint(it.value) },
        )
        result.put("protectedSaveFiles", JSONObject(protectedBefore))
            .put("sramPresent", saves.getValue("sram").isFile)
            .put("sramBytes", saves.getValue("sram").length())
            .put(
                "sramLimitation",
                if (saves.getValue("sram").isFile) {
                    "Only actual existing SRAM bytes observed"
                } else {
                    "This fixture produced no SRAM file; SRAM persistence is not demonstrated"
                },
            )
            .put("completedAtEpochMs", System.currentTimeMillis()).put("passed", true)
        File(evidence, "$stage.json").writeText(result.toString(2))
    }

    private fun publish(activity: GameActivity) {
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = activity,
                        size = 24,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
    }

    private fun awaitUsableCore(model: BaseGameScreenViewModel): GLRetroView {
        // A blocking Ready/first-frame wait can starve the Compose test clock. Pump it
        // until the real view has rendered AND autosave restoration has completed.
        compose.waitUntil(180_000) {
            var ready = false
            instrumentation.runOnMainSync {
                ready = model.retroGameView.existingRetroView() != null &&
                    model.retroGameView.canCaptureState && model.retroGameView.isNativeViewResumed
            }
            ready
        }
        val current = AtomicReference<GLRetroView>()
        instrumentation.runOnMainSync { current.set(checkNotNull(model.retroGameView.existingRetroView())) }
        return current.get().also { awaitFrames(it) }
    }

    private fun awaitFrames(view: GLRetroView) =
        runBlocking {
            withTimeout(90_000) {
                view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().take(3).toList()
            }
        }

    private fun waitForTag(
        tag: String,
        timeoutMs: Long = 60_000,
    ) {
        compose.waitUntil(timeoutMs) {
            runCatching {
                compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInWindow

    private fun touch(
        x: Float,
        y: Float,
        view: GLRetroView,
    ) {
        val down = SystemClock.uptimeMillis()
        val press = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)
        try {
            instrumentation.sendPointerSync(press)
            // Match the sustained stylus contact used by NativeFoldLayoutTest.
            // Main-thread FrameRendered notifications may include queued older frames.
            SystemClock.sleep(500)
            awaitFrames(view)
        } finally {
            press.recycle()
            val release = MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
            try {
                instrumentation.sendPointerSync(release)
            } finally {
                release.recycle()
            }
        }
        awaitFrames(view)
    }

    private fun capture(name: String): JSONObject {
        compose.waitForIdle()
        instrumentation.waitForIdleSync()
        val regions = listOf(UPPER, LOWER, LEFT, RIGHT, HINGE, SURFACE).associateWith(::bounds)
        val upper = regions.getValue(UPPER)
        val lower = regions.getValue(LOWER)
        val hinge = regions.getValue(HINGE)
        assertTrue(upper.bottom <= hinge.top + 1f)
        assertTrue(lower.top >= hinge.bottom - 1f)
        assertTrue(lower.left >= regions.getValue(LEFT).right - 1f)
        assertTrue(lower.right <= regions.getValue(RIGHT).left + 1f)
        assertEquals(256f / 192f, upper.width / upper.height, 0.015f)
        assertEquals(256f / 192f, lower.width / lower.height, 0.015f)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(
                evidence,
                "$name.png",
            ).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val tuple =
                JSONObject()
                    .put("state", number(bitmap, upper, 80, 48, 2, 6))
                    .put("frame", number(bitmap, upper, 44, 75, 1, 10))
                    .put("x", number(bitmap, lower, 32, 31, 2, 3))
                    .put("y", number(bitmap, lower, 104, 31, 2, 3))
                    .put("touches", number(bitmap, lower, 56, 52, 1, 8))
            val json =
                JSONObject().put("pid", Process.myPid()).put("capturedAtEpochMs", System.currentTimeMillis())
                    .put("screenWidth", bitmap.width).put("screenHeight", bitmap.height)
                    .put(
                        "evidenceType",
                        "Native DS screenshot pixels, sampled at all 256x192 native pixel centers per screen",
                    )
                    .put("decodedFromPixels", tuple)
                    .put("upperNativePixelSha256", nativePixelsHash(bitmap, upper))
                    .put("lowerNativePixelSha256", nativePixelsHash(bitmap, lower))
            regions.forEach {
                    (tag, r) ->
                json.put(
                    tag,
                    JSONObject().put("left", r.left).put("top", r.top).put("right", r.right).put("bottom", r.bottom),
                )
            }
            File(evidence, "$name.json").writeText(json.toString(2))
            return json
        } finally {
            bitmap.recycle()
        }
    }

    private fun assertScreensEqual(
        message: String,
        expected: JSONObject,
        actual: JSONObject,
    ) {
        for (key in listOf("state", "frame", "x", "y", "touches")) {
            assertEquals(
                "$message ($key)",
                expected.getJSONObject("decodedFromPixels").getLong(key),
                actual.getJSONObject("decodedFromPixels").getLong(key),
            )
        }
        for (key in listOf("upperNativePixelSha256", "lowerNativePixelSha256")) {
            assertEquals("$message ($key)", expected.getString(key), actual.getString(key))
        }
    }

    private fun nativePixel(
        bitmap: Bitmap,
        rect: Rect,
        x: Float,
        y: Float,
    ): Int = bitmap.getPixel((rect.left + x * rect.width / 256f).toInt(), (rect.top + y * rect.height / 192f).toInt())

    private fun nativePixelsHash(
        bitmap: Bitmap,
        rect: Rect,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (y in 0 until 192) for (x in 0 until 256) {
            val pixel = nativePixel(bitmap, rect, x + 0.5f, y + 0.5f)
            digest.update(Color.red(pixel).toByte())
            digest.update(Color.green(pixel).toByte())
            digest.update(Color.blue(pixel).toByte())
        }
        return hex(digest.digest())
    }

    // Decode the original fixture's hand-authored 5x7 glyphs from observed pixels.
    // Reject absent/ambiguous glyphs; never substitute an expected value.
    private fun number(
        bitmap: Bitmap,
        rect: Rect,
        x: Int,
        y: Int,
        scale: Int,
        count: Int,
    ): Long {
        val result = StringBuilder()
        for (index in 0 until count) {
            val observed =
                BooleanArray(35) { cell ->
                    val pixel =
                        nativePixel(
                            bitmap,
                            rect,
                            x + index * 6f * scale + (cell % 5 + 0.5f) * scale,
                            y + (cell / 5 + 0.5f) * scale,
                        )
                    maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) > 155
                }
            if (!observed.any { it }) break
            val errors =
                DIGITS.mapIndexed { digit, rows ->
                    digit to
                        observed.indices.count {
                                cell ->
                            observed[cell] != (rows[cell / 5] and (1 shl (4 - cell % 5)) != 0)
                        }
                }.sortedBy { it.second }
            check(errors[0].second <= 7 && errors[1].second - errors[0].second >= 2) {
                "Ambiguous screenshot digit at $x,$y index $index: $errors"
            }
            result.append(errors[0].first)
        }
        check(result.isNotEmpty()) { "No screenshot digits at $x,$y" }
        return result.toString().toLong()
    }

    private fun fingerprint(file: File): String {
        if (!file.isFile) return "ABSENT"
        val hash = hex(MessageDigest.getInstance("SHA-256").digest(file.readBytes()))
        return "${file.length()}:${file.lastModified()}:$hash"
    }

    private fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    private fun protectedSaves(
        root: File,
        mutableFiles: Set<File>,
    ): Map<String, String> =
        listOf(File(root, "states"), File(root, "saves")).flatMap { directory ->
            if (directory.isDirectory) {
                directory.walkTopDown().filter { it.isFile && it !in mutableFiles }.toList()
            } else {
                emptyList()
            }
        }.associate { it.relativeTo(root).path to fingerprint(it) }

    private fun readImportedGame(title: String): Game =
        runBlocking {
            withTimeout(30_000) {
                withContext(Dispatchers.IO) {
                    check(context.getDatabasePath(RetrogradeDatabase.DB_NAME).isFile) {
                        "Import the original fixture through the app first"
                    }
                    val db =
                        Room.databaseBuilder(
                            context,
                            RetrogradeDatabase::class.java,
                            RetrogradeDatabase.DB_NAME,
                        ).build()
                    try {
                        db.gameDao().observeLibrary().first()
                            .filter { it.title == title || it.fileName == title }.single()
                    } finally {
                        db.close()
                    }
                }
            }
        }

    private companion object {
        const val GUIDANCE = "emuui_game_posture_guidance"
        const val UPPER = "emuui_game_upper_screen"
        const val LOWER = "emuui_game_lower_screen"
        const val LEFT = "emuui_game_left_controls"
        const val RIGHT = "emuui_game_right_controls"
        const val HINGE = "emuui_game_hinge"
        const val SURFACE = "emuui_game_surface"
        val DIGITS =
            arrayOf(
                intArrayOf(14, 17, 19, 21, 25, 17, 14), intArrayOf(4, 12, 4, 4, 4, 4, 14),
                intArrayOf(14, 17, 1, 2, 4, 8, 31), intArrayOf(30, 1, 1, 14, 1, 1, 30),
                intArrayOf(2, 6, 10, 18, 31, 2, 2), intArrayOf(31, 16, 16, 30, 1, 1, 30),
                intArrayOf(14, 16, 16, 30, 17, 17, 14), intArrayOf(31, 1, 2, 4, 8, 8, 8),
                intArrayOf(14, 17, 17, 14, 17, 17, 14), intArrayOf(14, 17, 17, 15, 1, 1, 14),
            )
    }
}
