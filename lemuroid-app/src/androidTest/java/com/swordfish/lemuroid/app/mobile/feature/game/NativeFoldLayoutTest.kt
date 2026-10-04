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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Opt-in real core/layout capture on an ordinary emulator using injected WindowManager
 * fold metadata. No production posture bypass, fabricated database, ROM or core install.
 * Run in :game with -e gameTitle <existing fixture> -e coreName MELONDS or FCEUMM.
 * Screenshots and JSON are candidate evidence, not physical-foldable certification.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class NativeFoldLayoutTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var prefix: String
    private val pauseObservations = JSONObject()
    private var observedCoreAspectRatio = 0f
    private val eglResumeObservation = JSONObject()

    @Test
    fun nativeScreensInputsAndFoldTransitions() {
        assumeTrue("Select GameProcessTestRunner explicitly", instrumentation is GameProcessTestRunner)
        val processName =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .runningAppProcesses.single { it.pid == Process.myPid() }.processName
        assertEquals("${context.packageName}:game", processName)
        val args = InstrumentationRegistry.getArguments()
        val title = args.getString("gameTitle") ?: "EmuUI_DS_Legacy_QA"
        val coreName = args.getString("coreName") ?: "MELONDS"
        val dual = coreName in setOf("MELONDS", "DESMUME")
        prefix = if (dual) "ds" else "single"
        val game = readImportedGame(title)
        val core = GameSystem.findById(game.systemId).systemCoreConfigs.single { it.coreID.name == coreName }
        val intent =
            Intent(context, GameActivity::class.java).apply {
                putExtra("GAME", game)
                putExtra("LOAD_SAVE", false)
                putExtra("LEANBACK", false)
                putExtra("EXTRA_SYSTEM_CORE_CONFIG", core)
            }
        val scenario = ActivityScenario.launch<GameActivity>(intent)
        try {
            val activity = AtomicReference<GameActivity>()
            val model = AtomicReference<BaseGameScreenViewModel>()
            scenario.onActivity {
                activity.set(it)
                model.set(ViewModelProvider(it)[BaseGameScreenViewModel::class.java])
            }
            // Loading guidance has its own tracker. Wait for the real game surface
            // before publishing because WindowLayoutInfoPublisherRule has no replay.
            waitForTag(SURFACE)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag(GUIDANCE)
            capture("00-unknown-gate", emptyMap())
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            val native = awaitUsableCore(model.get())
            waitForTag(LEFT)
            assertGeometry(dual)
            capture("01-half-open-native", geometry(dual))

            // Actual accessible controls send native controller events; the fixture's
            // displayed values make their effect externally reviewable in screenshots.
            compose.onNodeWithText("Accessible controls").performClick()
            compose.onNodeWithContentDescription("A button").assertIsDisplayed()
            var frozenDsTuple: JSONObject? = null
            if (dual) {
                compose.onNodeWithContentDescription("Start button").performClick()
                awaitFrames(native)
                SystemClock.sleep(250)
                compose.onNodeWithContentDescription("B button").performClick()
                awaitFrames(native)
                SystemClock.sleep(250)
                compose.onNodeWithContentDescription("A button").performClick()
                awaitFrames(native)
                SystemClock.sleep(250)
                val lower = bounds(LOWER)
                touch(lower.left + lower.width * 0.25f, lower.top + lower.height * 0.75f, native)
                val touched =
                    capture("02-native-buttons-and-lower-touch", geometry(true))
                        .getJSONObject("decodedFromPixels")
                assertEquals("A and B must reach the original fixture", 28L, touched.getLong("state"))
                assertTrue("Lower-screen stylus must register", touched.getLong("touches") > 0)
                assertEquals("Lower stylus X mapping", 64.0, touched.getDouble("x"), 2.0)
                assertEquals("Lower stylus Y mapping", 144.0, touched.getDouble("y"), 2.0)
                val upper = bounds(UPPER)
                touch(upper.center.x, upper.center.y, native)
                val hinge = bounds(HINGE)
                touch(hinge.center.x, hinge.center.y, native)
                val rejected =
                    capture("03-upper-and-hinge-touch-rejected", geometry(true))
                        .getJSONObject("decodedFromPixels")
                for (key in listOf("state", "frame", "x", "y", "touches")) {
                    assertEquals("Upper/hinge touch must not change $key", touched.getLong(key), rejected.getLong(key))
                }
                frozenDsTuple = rejected
            } else {
                compose.onNodeWithContentDescription("Right button").performClick()
                awaitFrames(native)
                compose.onNodeWithContentDescription("A button").performClick()
                awaitFrames(native)
                capture("02-native-single-controls", geometry(false))
            }

            val initialUpperHeight = bounds(UPPER).height
            val surface = bounds(SURFACE)
            val centerHinge = (surface.top + surface.height * 0.5f).roundToInt()
            val offCenterHinge = (surface.top + surface.height * 0.6f).roundToInt()
            publish(activity.get(), FoldingFeature.State.HALF_OPENED, center = offCenterHinge)
            waitForHingeCenter(offCenterHinge)
            awaitFrames(native)
            assertSame(
                "Moving the fold must retain the native view",
                native,
                model.get().retroGameView.existingRetroView(),
            )
            assertGeometry(dual)
            assertTrue(
                "A taller upper pane must enlarge its native image",
                bounds(UPPER).height > initialUpperHeight + 2f,
            )
            val resized = capture("03a-off-center-independent-fit", geometry(dual))
            if (dual) {
                assertTupleEquals(checkNotNull(frozenDsTuple), resized.getJSONObject("decodedFromPixels"))
                assertTrue("Upper/lower scale must now differ", bounds(UPPER).height > bounds(LOWER).height + 2f)
                val lower = bounds(LOWER)
                touch(lower.left + lower.width * 0.75f, lower.top + lower.height * 0.25f, native)
                val retouched = capture("03b-off-center-lower-touch", geometry(true)).getJSONObject("decodedFromPixels")
                val before = checkNotNull(frozenDsTuple)
                assertEquals("Resize must preserve frozen state", before.getLong("state"), retouched.getLong("state"))
                assertEquals("Resize must preserve frozen frame", before.getLong("frame"), retouched.getLong("frame"))
                assertEquals("Resized lower stylus X mapping", 192.0, retouched.getDouble("x"), 2.0)
                assertEquals("Resized lower stylus Y mapping", 48.0, retouched.getDouble("y"), 2.0)
                assertEquals(
                    "Resized lower stylus contact",
                    before.getLong("touches") + 1,
                    retouched.getLong("touches"),
                )
                frozenDsTuple = retouched
            }

            for ((name, state) in listOf(
                "flat" to FoldingFeature.State.FLAT,
                "half" to FoldingFeature.State.HALF_OPENED,
            )) {
                publish(activity.get(), state)
                waitForHingeCenter(centerHinge)
                waitForTag(LEFT)
                compose.waitForIdle()
                assertSame(
                    "Posture must not replace the live native view",
                    native,
                    model.get().retroGameView.existingRetroView(),
                )
                assertGeometry(dual)
                awaitFrames(native)
                val restored = capture("04-$name-retained-native", geometry(dual))
                if (dual) assertTupleEquals(checkNotNull(frozenDsTuple), restored.getJSONObject("decodedFromPixels"))
            }
            publish(activity.get(), FoldingFeature.State.HALF_OPENED, FoldingFeature.Orientation.VERTICAL)
            waitForTag(GUIDANCE)
            assertSame(native, model.get().retroGameView.existingRetroView())
            assertTrue(
                "Gate must remove playable controls",
                compose.onAllNodes(hasTestTag(LEFT)).fetchSemanticsNodes().isEmpty(),
            )
            assertNativePaused(model.get(), native, "vertical")
            capture("05-vertical-gate", emptyMap())
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag(GUIDANCE)
            assertSame(native, model.get().retroGameView.existingRetroView())
            assertNativePaused(model.get(), native, "unknown")
            capture("06-unknown-gate-retained-native", emptyMap())
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            waitForTag(LEFT)
            awaitFrames(native)
            assertSame(native, model.get().retroGameView.existingRetroView())
            capture("07-restored-native", geometry(dual))
            assertTrue("Real core remains serializable after fold transitions", native.serializeState().isNotEmpty())
            compose.onNodeWithText("Radial controls").performClick()
            capture("08-restored-radial-controls", geometry(dual))
            compose.onNodeWithText("Menu").performClick()
            waitForTag(GUIDANCE)
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            waitForTag("emuui_menu_center")
            assertMenuGeometry()
            capture("09-game-menu", menuGeometry())
            compose.onNodeWithContentDescription("Y, core options").performClick()
            compose.waitForIdle()
            assertMenuGeometry()
            capture("10-game-core-options", menuGeometry())
            compose.onNodeWithContentDescription("X, game menu home").performClick()
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag(GUIDANCE)
            assertTrue(
                "Invalid posture must remove game-menu controls",
                compose.onAllNodes(hasTestTag("emuui_menu_center")).fetchSemanticsNodes().isEmpty(),
            )
            capture("11-game-menu-gated", emptyMap())
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            waitForTag("emuui_menu_center")
            assertMenuGeometry()
            capture("12-game-menu-restored", menuGeometry())
            compose.onNodeWithContentDescription("Start, resume game").performClick()
            compose.waitUntil(60_000) { scenario.state == androidx.lifecycle.Lifecycle.State.RESUMED }
            compose.waitForIdle()
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            waitForTag(LEFT)
            awaitFrames(native)
            assertSame(
                "Menu roundtrip must retain the native view",
                native,
                model.get().retroGameView.existingRetroView(),
            )
            capture("13-native-after-menu-roundtrip", geometry(dual))
            if (dual) verifyEglResume(scenario, activity, model.get(), native)
        } finally {
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
    }

    private fun verifyEglResume(
        scenario: ActivityScenario<GameActivity>,
        activity: AtomicReference<GameActivity>,
        model: BaseGameScreenViewModel,
        native: GLRetroView,
    ) {
        val originalPreservation = AtomicReference<Boolean>()
        instrumentation.runOnMainSync { originalPreservation.set(native.preserveEGLContextOnPause) }
        val baseline = capture("14-before-egl-context-loss", geometry(true), hashNativeScreens = true)
        val surfaceCount = AtomicInteger()
        val eventTimes = Collections.synchronizedList(mutableListOf<Long>())
        val collectorScope = CoroutineScope(Dispatchers.Default)
        val collector =
            collectorScope.launch(start = CoroutineStart.UNDISPATCHED) {
                native.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.SurfaceCreated>().collect {
                    eventTimes.add(SystemClock.uptimeMillis())
                    surfaceCount.incrementAndGet()
                }
            }
        try {
            // Drain any already queued/replayed event before deliberately releasing this context.
            SystemClock.sleep(500)
            val previousSurfaces = surfaceCount.get()
            instrumentation.runOnMainSync { native.preserveEGLContextOnPause = false }
            assertFalse(native.preserveEGLContextOnPause)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            assertFalse("Host background must pause native rendering", model.retroGameView.isNativeViewResumed)
            assertSame("EGL release must keep the native view", native, model.retroGameView.existingRetroView())
            val resumeAt = SystemClock.uptimeMillis()
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            scenario.onActivity { activity.set(it) }
            waitForTag(SURFACE)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag(GUIDANCE)
            publish(activity.get(), FoldingFeature.State.HALF_OPENED)
            waitForTag(LEFT)
            val resumed = awaitUsableCore(model)
            compose.waitUntil(90_000) {
                surfaceCount.get() > previousSurfaces && synchronized(eventTimes) { eventTimes.any { it >= resumeAt } }
            }
            assertSame("EGL rebuild must not replace the native view", native, resumed)
            assertGeometry(true)
            eglResumeObservation.put("preserveContextBefore", originalPreservation.get())
                .put("preserveContextDuringPause", false).put("surfaceEventsBeforePause", previousSurfaces)
                .put("surfaceEventsAfterResume", surfaceCount.get()).put("resumeRequestedAtUptimeMs", resumeAt)
                .put("surfaceEventUptimeMs", org.json.JSONArray(synchronized(eventTimes) { eventTimes.toList() }))
                .put(
                    "scope",
                    "Actual GLSurfaceView context release/resume with software MELONDS; no hardware-core claim",
                )
            val after = capture("15-after-egl-context-rebuild", geometry(true), hashNativeScreens = true)
            assertTupleEquals(baseline.getJSONObject("decodedFromPixels"), after.getJSONObject("decodedFromPixels"))
            for (key in listOf("upperNativePixelSha256", "lowerNativePixelSha256")) {
                assertEquals(
                    "EGL rebuild must preserve every sampled native pixel",
                    baseline.getString(key),
                    after.getString(key),
                )
            }
            val lower = bounds(LOWER)
            touch(lower.left + lower.width * 0.375f, lower.top + lower.height * 0.5f, native)
            val touched =
                capture("16-after-egl-rebuild-lower-touch", geometry(true))
                    .getJSONObject("decodedFromPixels")
            val before = after.getJSONObject("decodedFromPixels")
            assertEquals("Restored-context touch X", 96.0, touched.getDouble("x"), 2.0)
            assertEquals("Restored-context touch Y", 96.0, touched.getDouble("y"), 2.0)
            assertEquals("Restored-context touch contact", before.getLong("touches") + 1, touched.getLong("touches"))
            assertEquals("Restored-context touch keeps state", before.getLong("state"), touched.getLong("state"))
            assertEquals(
                "Restored-context touch keeps frozen frame",
                before.getLong("frame"),
                touched.getLong("frame"),
            )
        } finally {
            instrumentation.runOnMainSync { native.preserveEGLContextOnPause = originalPreservation.get() }
            runBlocking { collector.cancelAndJoin() }
            collectorScope.cancel()
        }
    }

    private fun assertGeometry(dual: Boolean) {
        val upper = bounds(UPPER)
        val left = bounds(LEFT)
        val right = bounds(RIGHT)
        val hinge = bounds(HINGE)
        val surface = bounds(SURFACE)
        val upperPanel = bounds(UPPER_PANEL)
        val lowerPanel = bounds(LOWER_PANEL)
        assertTrue("Upper panel must clear hinge", upperPanel.bottom <= hinge.top)
        assertTrue("Lower panel must clear hinge", lowerPanel.top >= hinge.bottom)
        assertTrue(
            "Lower panel must fit between controls",
            lowerPanel.left > left.right && lowerPanel.right < right.left,
        )
        assertStrictlyInside("Upper screen must fit inside rounded panel", upper, upperPanel)
        val upperAspect = if (dual) 4f / 3f else observedCoreAspectRatio
        assertEquals(
            "Upper screen must preserve the actual native aspect",
            upperAspect,
            upper.width / upper.height,
            max(0.003f, (1f + upperAspect) / upper.height),
        )
        assertCenteredMaximum("Upper", upper, upperPanel)
        assertEquals("Lower panel must center on the lower safe half", left.center.y, lowerPanel.center.y, 1f)
        assertEquals("Both controls must share the lower safe half", left.center.y, right.center.y, 1f)
        assertTrue("Upper display must end above hinge", upper.bottom <= hinge.top + 1f)
        for (rail in listOf(left, right)) {
            assertTrue("Controls must be below hinge", rail.top >= hinge.bottom - 1f)
            assertTrue(
                "Controls must remain inside surface",
                rail.left >= surface.left && rail.right <= surface.right && rail.bottom <= surface.bottom,
            )
        }
        if (dual) {
            val lower = bounds(LOWER)
            assertStrictlyInside("Lower screen must fit inside rounded panel", lower, lowerPanel)
            assertCenteredMaximum("Lower", lower, lowerPanel)
            assertEquals(
                "Lower native screen must be vertically centered in the safe half",
                left.center.y,
                lower.center.y,
                1f,
            )
            assertTrue("Lower DS display must be below hinge", lower.top >= hinge.bottom - 1f)
            assertTrue(
                "Lower DS display must be between controls",
                lower.left >= left.right - 1f && lower.right <= right.left + 1f,
            )
            assertTrue("Lower DS display must remain within surface", lower.bottom <= surface.bottom)
            assertEquals("Native DS upper ratio", 256f / 192f, upper.width / upper.height, 0.015f)
            assertEquals("Native DS lower ratio", 256f / 192f, lower.width / lower.height, 0.015f)
        } else {
            assertTrue(
                "Single-screen core must not expose a lower native display",
                compose.onAllNodes(hasTestTag(LOWER)).fetchSemanticsNodes().isEmpty(),
            )
        }
    }

    private fun assertCenteredMaximum(
        label: String,
        screen: Rect,
        panel: Rect,
    ) {
        assertEquals("$label horizontal centering", panel.center.x, screen.center.x, 1f)
        assertEquals("$label vertical centering", panel.center.y, screen.center.y, 1f)
        // Raster semantics round float edges. Four extra pixels on the shorter axis must
        // not fit, while tolerating the original rectangle's subpixel edge rounding.
        val expansion = 1f + 4f / min(screen.width, screen.height)
        val halfWidth = screen.width * expansion / 2f
        val halfHeight = screen.height * expansion / 2f
        val enlarged =
            Rect(
                panel.center.x - halfWidth,
                panel.center.y - halfHeight,
                panel.center.x + halfWidth,
                panel.center.y + halfHeight,
            )
        assertFalse("$label must independently maximize its own native-aspect panel", fitsRoundedPanel(enlarged, panel))
    }

    private fun fitsRoundedPanel(
        screen: Rect,
        panel: Rect,
    ): Boolean {
        val guard = 1f
        val safe = Rect(panel.left + guard, panel.top + guard, panel.right - guard, panel.bottom - guard)
        if (screen.left < safe.left || screen.top < safe.top ||
            screen.right > safe.right || screen.bottom > safe.bottom
        ) {
            return false
        }
        val radius = min(24f * context.resources.displayMetrics.density, min(panel.width, panel.height) / 2f)
        for (x in listOf(screen.left, screen.right)) for (y in listOf(screen.top, screen.bottom)) {
            val dx = max(0f, max(safe.left + radius - x, x - (safe.right - radius)))
            val dy = max(0f, max(safe.top + radius - y, y - (safe.bottom - radius)))
            if (dx * dx + dy * dy > radius * radius) return false
        }
        return true
    }

    private fun geometry(dual: Boolean): Map<String, Rect> =
        (
            listOf(UPPER, LEFT, RIGHT, HINGE, SURFACE, UPPER_PANEL, LOWER_PANEL) +
                if (dual) listOf(LOWER) else emptyList()
        )
            .associateWith(::bounds)

    private fun assertStrictlyInside(
        message: String,
        screen: Rect,
        panel: Rect,
    ) {
        assertTrue(
            message,
            screen.left > panel.left && screen.top > panel.top &&
                screen.right < panel.right && screen.bottom < panel.bottom,
        )
    }

    private fun menuGeometry(): Map<String, Rect> =
        listOf(
            "emuui_menu_upper",
            "emuui_menu_left_controls",
            "emuui_menu_right_controls",
            "emuui_menu_center",
            "emuui_menu_hinge",
        ).associateWith(::bounds)

    private fun assertMenuGeometry() {
        val center = bounds("emuui_menu_center")
        val left = bounds("emuui_menu_left_controls")
        val right = bounds("emuui_menu_right_controls")
        val hinge = bounds("emuui_menu_hinge")
        assertTrue("Game menu must remain below hinge", center.top >= hinge.bottom - 1f)
        assertTrue(
            "Game menu must stay between wings",
            center.left >= left.right - 1f && center.right <= right.left + 1f,
        )
        compose.onNodeWithTag("emuui_menu_left_controls").assertIsDisplayed()
        compose.onNodeWithTag("emuui_menu_right_controls").assertIsDisplayed()
    }

    private fun bounds(tag: String) =
        compose.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInWindow

    private fun waitForTag(tag: String) {
        compose.waitUntil(60_000) {
            runCatching {
                compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun publish(
        activity: GameActivity,
        state: FoldingFeature.State,
        orientation: FoldingFeature.Orientation = FoldingFeature.Orientation.HORIZONTAL,
        center: Int = -1,
    ) {
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = activity,
                        center = center,
                        size = 24,
                        state = state,
                        orientation = orientation,
                    ),
                ),
            ),
        )
    }

    private fun waitForHingeCenter(expected: Int) {
        compose.waitUntil(60_000) {
            runCatching { kotlin.math.abs(bounds(HINGE).center.y - expected) <= 1f }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun assertTupleEquals(
        expected: JSONObject,
        actual: JSONObject,
    ) {
        for (key in listOf("state", "frame", "x", "y", "touches")) {
            assertEquals("Native resize must preserve $key", expected.getLong(key), actual.getLong(key))
        }
    }

    private fun touch(
        x: Float,
        y: Float,
        view: GLRetroView,
    ) {
        val down = SystemClock.uptimeMillis()
        val press = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0)
        try {
            instrumentation.sendPointerSync(press)
            // Hold a real contact across frames; short Compose taps are not stylus sampling proof.
            SystemClock.sleep(500)
            awaitFrames(view)
            val release = MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, x, y, 0)
            try {
                instrumentation.sendPointerSync(release)
            } finally {
                release.recycle()
            }
            awaitFrames(view)
        } finally {
            press.recycle()
        }
    }

    private fun capture(
        name: String,
        regions: Map<String, Rect>,
        hashNativeScreens: Boolean = false,
    ): JSONObject {
        instrumentation.waitForIdleSync()
        val run = InstrumentationRegistry.getArguments().getString("qaCaptureRun") ?: "default"
        val directory = File(checkNotNull(context.getExternalFilesDir("qa-fold-layout")), run).apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(
                directory,
                "$prefix-$name.png",
            ).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val json =
                JSONObject().put(
                    "evidenceType",
                    "Actual API29 native runtime with injected WindowLayoutInfoPublisherRule fold metadata",
                )
                    .put("process", Process.myPid()).put("capturedAtEpochMs", System.currentTimeMillis())
                    .put("screenWidth", bitmap.width).put("screenHeight", bitmap.height)
                    .put("nativePauseObservations", pauseObservations)
                    .put("actualCoreAspectRatio", observedCoreAspectRatio)
                    .put("eglResumeObservation", eglResumeObservation)
            regions.forEach { (tag, rect) ->
                json.put(
                    tag,
                    JSONObject().put(
                        "left",
                        rect.left,
                    ).put("top", rect.top).put("right", rect.right).put("bottom", rect.bottom),
                )
            }
            if (prefix == "ds" && UPPER in regions && LOWER in regions) {
                val upper = regions.getValue(UPPER)
                val lower = regions.getValue(LOWER)
                json.put(
                    "decodedFromPixels",
                    JSONObject().put("state", number(bitmap, upper, 80, 48, 2, 6))
                        .put("frame", number(bitmap, upper, 44, 75, 1, 10))
                        .put("x", number(bitmap, lower, 32, 31, 2, 3))
                        .put("y", number(bitmap, lower, 104, 31, 2, 3))
                        .put("touches", number(bitmap, lower, 56, 52, 1, 8)),
                )
            }
            if (hashNativeScreens) {
                json.put("upperNativePixelSha256", nativePixelsHash(bitmap, regions.getValue(UPPER)))
                    .put("lowerNativePixelSha256", nativePixelsHash(bitmap, regions.getValue(LOWER)))
            }
            File(directory, "$prefix-$name.json").writeText(json.toString(2))
            return json
        } finally {
            bitmap.recycle()
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
        return digest.digest().joinToString("") { "%02x".format(it) }
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

    private fun readImportedGame(title: String): Game =
        runBlocking {
            withContext(Dispatchers.IO) {
                check(
                    context.getDatabasePath(RetrogradeDatabase.DB_NAME).isFile,
                ) { "Import the lawful fixture before running" }
                val db =
                    Room.databaseBuilder(
                        context,
                        RetrogradeDatabase::class.java,
                        RetrogradeDatabase.DB_NAME,
                    ).build()
                try {
                    db.gameDao().observeLibrary().first().filter { it.title == title || it.fileName == title }.single()
                } finally {
                    db.close()
                }
            }
        }

    private fun awaitUsableCore(model: BaseGameScreenViewModel): GLRetroView {
        compose.waitUntil(180_000) {
            model.retroGameView.existingRetroView() != null &&
                model.retroGameView.canCaptureState && model.retroGameView.isNativeViewResumed
        }
        val view = checkNotNull(model.retroGameView.existingRetroView())
        compose.waitUntil(60_000) { view.coreAspectRatio.isFinite() && view.coreAspectRatio > 0f }
        observedCoreAspectRatio = view.coreAspectRatio
        compose.waitForIdle()
        awaitFrames(view)
        assertTrue("A real native core must serialize", view.serializeState().isNotEmpty())
        return view
    }

    private fun awaitFrames(view: GLRetroView) =
        runBlocking {
            withTimeout(90_000) {
                view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().take(3).toList()
            }
        }

    private fun assertNativePaused(
        model: BaseGameScreenViewModel,
        view: GLRetroView,
        label: String,
    ) {
        assertFalse("Invalid posture must pause the native lifecycle", model.retroGameView.isNativeViewResumed)
        runBlocking {
            val count = AtomicInteger()
            val collector =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>()
                        .collect { count.incrementAndGet() }
                }
            try {
                // Discard the replayed last frame and allow a queued frame to settle.
                delay(1_000)
                val settledCount = count.get()
                delay(1_500)
                val observedCount = count.get()
                pauseObservations.put(
                    label,
                    JSONObject().put("drainMillis", 1_000).put("observationMillis", 1_500)
                        .put("drainedEvents", settledCount).put("newEvents", observedCount - settledCount)
                        .put("nativeLifecycleResumed", model.retroGameView.isNativeViewResumed),
                )
                assertEquals("No fresh frames may render behind the posture gate", settledCount, observedCount)
                assertFalse(model.retroGameView.isNativeViewResumed)
            } finally {
                collector.cancelAndJoin()
            }
        }
    }

    private companion object {
        val DIGITS =
            arrayOf(
                intArrayOf(14, 17, 19, 21, 25, 17, 14), intArrayOf(4, 12, 4, 4, 4, 4, 14),
                intArrayOf(14, 17, 1, 2, 4, 8, 31), intArrayOf(30, 1, 1, 14, 1, 1, 30),
                intArrayOf(2, 6, 10, 18, 31, 2, 2), intArrayOf(31, 16, 16, 30, 1, 1, 30),
                intArrayOf(14, 16, 16, 30, 17, 17, 14), intArrayOf(31, 1, 2, 4, 8, 8, 8),
                intArrayOf(14, 17, 17, 14, 17, 17, 14), intArrayOf(14, 17, 17, 15, 1, 1, 14),
            )

        const val UPPER = "emuui_game_upper_screen"
        const val LOWER = "emuui_game_lower_screen"
        const val LEFT = "emuui_game_left_controls"
        const val RIGHT = "emuui_game_right_controls"
        const val GUIDANCE = "emuui_game_posture_guidance"
        const val HINGE = "emuui_game_hinge"
        const val SURFACE = "emuui_game_surface"
        const val UPPER_PANEL = "emuui_game_upper_panel"
        const val LOWER_PANEL = "emuui_game_lower_panel"
    }
}
