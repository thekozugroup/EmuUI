package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Process
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Opt-in ordinary radial B regression using the already imported original DS fixture.
 * No accessible action, direct core input, seed, download, reset or explicit save.
 * No host background/recreation transition is requested.
 * B is edge-triggered in this fixture: this proves hold/release/re-press, not Dpad drift
 * or gate-forced release. Injected fold metadata does not certify a physical foldable.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class NativeRadialHoldTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var activity: GameActivity
    private lateinit var directory: File
    private val pointerEvents = JSONArray()

    @Test
    fun ordinaryRadialBHoldsReleasesAndRepresses() {
        assumeTrue("Select GameProcessTestRunner explicitly", instrumentation is GameProcessTestRunner)
        val processName =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .runningAppProcesses.single { it.pid == Process.myPid() }.processName
        assertEquals("${context.packageName}:game", processName)
        val args = InstrumentationRegistry.getArguments()
        assertEquals(FIXTURE_TITLE, args.getString("gameTitle") ?: FIXTURE_TITLE)
        assertEquals("MELONDS", args.getString("coreName") ?: "MELONDS")
        val run = args.getString("qaCaptureRun") ?: error("Supply a fresh qaCaptureRun evidence label")
        check(run.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}"))) { "Invalid qaCaptureRun label" }
        directory = File(checkNotNull(context.getExternalFilesDir("qa-radial-hold")), run)
        check(!directory.exists()) { "Use a new qaCaptureRun; preserve previous evidence" }
        check(directory.mkdirs())
        val game = readImportedGame()
        val core = GameSystem.findById(game.systemId).systemCoreConfigs.single { it.coreID.name == "MELONDS" }
        val intent =
            Intent(context, GameActivity::class.java).apply {
                putExtra("GAME", game)
                putExtra("LOAD_SAVE", false)
                putExtra("LEANBACK", false)
                putExtra("EXTRA_SYSTEM_CORE_CONFIG", core)
            }
        val scenario = ActivityScenario.launch<GameActivity>(intent)
        val results = JSONArray()
        try {
            val model = AtomicReference<BaseGameScreenViewModel>()
            scenario.onActivity {
                activity = it
                model.set(ViewModelProvider(it)[BaseGameScreenViewModel::class.java])
            }
            // The publisher has no replay: first wait for the actual native surface's tracker.
            waitForTag(SURFACE)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag(GUIDANCE)
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
            val native = awaitUsableCore(model.get())
            waitForTag(RIGHT)
            compose.onNodeWithText("Accessible controls").assertIsDisplayed()
            var previous = capture("00-before-pointer", radialBTarget())
            assertFixture(previous, expectedState = 17L)
            results.put(previous)

            for (cycle in 1..2) {
                val target = radialBTarget()
                val before = previous
                val expectedState = 17L + cycle * 10L
                val downTime = SystemClock.uptimeMillis()
                try {
                    // Real Android touchscreen dispatch into PadKit; no semantic click/pulse.
                    pointer(MotionEvent.ACTION_DOWN, downTime, target.center)
                    awaitFreshFrames(native)
                    val held = capture("0${cycle * 3 - 2}-b-down-$cycle", target)
                    assertFixture(held, expectedState)
                    assertFramesAdvance(before, held)
                    results.put(held)
                    awaitFreshFrames(native)
                    val stillHeld = capture("0${cycle * 3 - 1}-b-still-held-$cycle", target)
                    assertFixture(stillHeld, expectedState)
                    assertFramesAdvance(held, stillHeld)
                    results.put(stillHeld)
                    previous = stillHeld
                } finally {
                    // Always terminate the same gesture, including assertion/decoder failure.
                    pointer(MotionEvent.ACTION_UP, downTime, target.center)
                }
                awaitFreshFrames(native)
                val released = capture("0${cycle * 3}-b-up-$cycle", target)
                assertFixture(released, expectedState)
                assertFramesAdvance(previous, released)
                results.put(released)
                previous = released
            }
        } finally {
            // Stop the service before closing, matching NativeFoldLayoutTest cleanup.
            // No CREATED/background transition is requested; no autosave API is invoked.
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
        File(directory, "result.json").writeText(
            JSONObject().put("passed", true).put("fixture", FIXTURE_TITLE).put("core", "MELONDS")
                .put("process", Process.myPid()).put("pointerEvents", pointerEvents).put("captures", results)
                .put("scope", "Ordinary radial B touchscreen hold, release and fresh re-press; injected fold metadata")
                .put("notCovered", "Continuous Dpad, gate-forced release, background autosave, physical foldable")
                .toString(2),
        )
    }

    private fun radialBTarget(): Rect {
        compose.onNodeWithText("Accessible controls").assertIsDisplayed()
        val label =
            compose.onNode(
                hasText("B", substring = false) and hasAnyAncestor(hasTestTag(RIGHT)),
                useUnmergedTree = true,
            ).assertIsDisplayed().fetchSemanticsNode().boundsInWindow
        val rail = bounds(RIGHT)
        val surface = bounds(SURFACE)
        val hinge = bounds(HINGE)
        assertTrue("B label must have a real measured target", label.width > 0f && label.height > 0f)
        assertInside("B label must be strictly inside the right radial rail", label, rail)
        assertTrue("Radial rail must clear the hinge", rail.top >= hinge.bottom - 1f)
        assertTrue("Radial rail must stay on the surface", rail.right <= surface.right && rail.bottom <= surface.bottom)
        assertTrue("Radial B must not hit the native lower screen", label.left > bounds(LOWER).right)
        // MelonDSRight binds Text("B") to KEYCODE_BUTTON_B. LemuroidButtonForeground's
        // GlassSurface centers this label in its actual primary-anchor hit region. Thus
        // its measured center follows saved control scale/margins without guessed coords.
        // sendPointerSync and screenshots use screen coordinates, unlike boundsInWindow.
        return label.translate(windowScreenOffset())
    }

    private fun windowScreenOffset(): Offset {
        val result = AtomicReference<Offset>()
        instrumentation.runOnMainSync {
            val onScreen = IntArray(2)
            val inWindow = IntArray(2)
            activity.window.decorView.getLocationOnScreen(onScreen)
            activity.window.decorView.getLocationInWindow(inWindow)
            result.set(Offset((onScreen[0] - inWindow[0]).toFloat(), (onScreen[1] - inWindow[1]).toFloat()))
        }
        return result.get()
    }

    private fun pointer(
        action: Int,
        downTime: Long,
        point: Offset,
    ) {
        val eventTime = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(downTime, eventTime, action, point.x, point.y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        try {
            instrumentation.sendPointerSync(event)
            pointerEvents.put(
                JSONObject().put("action", action).put("downTime", downTime).put("eventTime", eventTime)
                    .put("x", point.x).put("y", point.y).put("source", InputDevice.SOURCE_TOUCHSCREEN),
            )
        } finally {
            event.recycle()
            File(directory, "pointer-events.json").writeText(pointerEvents.toString(2))
        }
    }

    private fun assertFixture(
        capture: JSONObject,
        expectedState: Long,
    ) {
        val tuple = capture.getJSONObject("decodedFromPixels")
        assertEquals("Physical radial B must add exactly 10 per fresh press", expectedState, tuple.getLong("state"))
        assertEquals("B must not move marker X", 128L, tuple.getLong("x"))
        assertEquals("B must not move marker Y", 112L, tuple.getLong("y"))
        assertEquals("B must not leak into stylus input", 0L, tuple.getLong("touches"))
    }

    private fun assertFramesAdvance(
        before: JSONObject,
        after: JSONObject,
    ) {
        assertTrue(
            "Original fixture FRAME must advance while physical input is held/released",
            after.getJSONObject("decodedFromPixels").getLong("frame") >
                before.getJSONObject("decodedFromPixels").getLong("frame"),
        )
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow

    private fun assertInside(
        message: String,
        inner: Rect,
        outer: Rect,
    ) {
        assertTrue(
            message,
            inner.left > outer.left && inner.top > outer.top &&
                inner.right < outer.right && inner.bottom < outer.bottom,
        )
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(60_000) {
            compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.waitForIdle()
    }

    private fun awaitUsableCore(model: BaseGameScreenViewModel): GLRetroView {
        compose.waitUntil(180_000) {
            model.retroGameView.existingRetroView() != null &&
                model.retroGameView.canCaptureState && model.retroGameView.isNativeViewResumed
        }
        val native = checkNotNull(model.retroGameView.existingRetroView())
        compose.waitUntil(60_000) { native.coreAspectRatio.isFinite() && native.coreAspectRatio > 0f }
        compose.waitForIdle()
        awaitFreshFrames(native)
        assertTrue("Real native core must be serializable", native.serializeState().isNotEmpty())
        return native
    }

    private fun awaitFreshFrames(view: GLRetroView) =
        runBlocking {
            withTimeout(90_000) {
                // GLRetroView replays one event. Conservatively discard the first matching
                // frame, then require three further events; a cached event cannot satisfy it.
                view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>()
                    .drop(1).take(3).toList()
            }
        }

    private fun capture(
        name: String,
        target: Rect,
    ): JSONObject {
        instrumentation.waitForIdleSync()
        val offset = windowScreenOffset()
        val regions = listOf(UPPER, LOWER, RIGHT, HINGE, SURFACE).associateWith { bounds(it).translate(offset) }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            assertInside(
                "Pointer target must fit the captured screen",
                target,
                Rect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat()),
            )
            File(
                directory,
                "$name.png",
            ).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val upper = regions.getValue(UPPER)
            val lower = regions.getValue(LOWER)
            val json =
                JSONObject().put("name", name).put("capturedAtEpochMs", System.currentTimeMillis())
                    .put("screenWidth", bitmap.width).put("screenHeight", bitmap.height)
                    .put(
                        "sdk",
                        android.os.Build.VERSION.SDK_INT,
                    ).put("densityDpi", context.resources.displayMetrics.densityDpi)
                    .put(
                        "pointerTargetScreenBounds",
                        rectJson(target),
                    ).put("pointerEvents", JSONArray(pointerEvents.toString()))
                    .put("windowToScreenX", offset.x).put("windowToScreenY", offset.y)
                    .put(
                        "decodedFromPixels",
                        JSONObject().put("state", number(bitmap, upper, 80, 48, 2, 6))
                            .put("frame", number(bitmap, upper, 44, 75, 1, 10))
                            .put("x", number(bitmap, lower, 32, 31, 2, 3))
                            .put("y", number(bitmap, lower, 104, 31, 2, 3))
                            .put("touches", number(bitmap, lower, 56, 52, 1, 8)),
                    )
            regions.forEach { (tag, rect) -> json.put(tag, rectJson(rect)) }
            File(directory, "$name.json").writeText(json.toString(2))
            return json
        } finally {
            bitmap.recycle()
        }
    }

    private fun rectJson(rect: Rect): JSONObject =
        JSONObject().put("left", rect.left).put("top", rect.top).put("right", rect.right).put("bottom", rect.bottom)

    // Original NativeFoldLayoutTest pixel decoder; never substitute expected fixture values.
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
                    val nativeX = x + index * 6f * scale + (cell % 5 + 0.5f) * scale
                    val nativeY = y + (cell / 5 + 0.5f) * scale
                    val pixel =
                        bitmap.getPixel(
                            (rect.left + nativeX * rect.width / 256f).toInt(),
                            (rect.top + nativeY * rect.height / 192f).toInt(),
                        )
                    maxOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) > 155
                }
            if (!observed.any { it }) break
            val errors =
                DIGITS.mapIndexed { digit, rows ->
                    digit to
                        observed.indices.count { cell ->
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

    private fun readImportedGame(): Game =
        runBlocking {
            withContext(Dispatchers.IO) {
                check(context.getDatabasePath(RetrogradeDatabase.DB_NAME).isFile) { "Import the lawful fixture first" }
                val db =
                    Room.databaseBuilder(
                        context,
                        RetrogradeDatabase::class.java,
                        RetrogradeDatabase.DB_NAME,
                    ).build()
                try {
                    db.gameDao().observeLibrary().first()
                        .filter { it.title == FIXTURE_TITLE || it.fileName == FIXTURE_TITLE }.single()
                } finally {
                    db.close()
                }
            }
        }

    private companion object {
        const val FIXTURE_TITLE = "EmuUI_DS_Legacy_Long_Title_QA"
        const val UPPER = "emuui_game_upper_screen"
        const val LOWER = "emuui_game_lower_screen"
        const val RIGHT = "emuui_game_right_controls"
        const val GUIDANCE = "emuui_game_posture_guidance"
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
