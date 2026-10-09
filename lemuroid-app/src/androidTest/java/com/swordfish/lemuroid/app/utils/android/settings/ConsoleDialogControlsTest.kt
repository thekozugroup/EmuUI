package com.swordfish.lemuroid.app.utils.android.settings

import android.os.Build
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToLog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.layout.WindowMetricsCalculator
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Real native modal + production controls with isolated choice callbacks; no user setting is changed. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConsoleDialogControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun accessibleModalWingsMoveConfirmAndDismissWithoutActivatingTheLauncher() {
        verifyModalControls(realTouch = false)
    }

    @Test
    fun touchscreenModalWingsRetainFocusAcrossRepeatedTapsAndConfirmExactlyOnce() {
        verifyModalControls(realTouch = true)
    }

    private fun verifyModalControls(realTouch: Boolean) {
        val shown = mutableStateOf(true)
        val modalWindow = AtomicReference<Window>()
        val actions = mutableListOf<String>()
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(compose.activity).bounds
        val width = metrics.width()
        val height = metrics.height()
        val side = width / 5
        val lowerTop = height / 2 + 24
        val region =
            ConsoleDialogRegion(
                FoldRect(side + 12, lowerTop, width - side - 12, height - 24),
                FoldRect(0, lowerTop, side, height - 24),
                FoldRect(width - side, lowerTop, width, height - 24),
            )
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    Box(Modifier.fillMaxSize()) {
                        Button(onClick = { actions += "behind-modal-launch" }) { Text("Underlying launch action") }
                        CompositionLocalProvider(LocalConsoleDialogRegion provides region) {
                            if (shown.value) {
                                ConsoleSettingsDialog(
                                    onDismissRequest = { shown.value = false },
                                    title = { Text("Original QA choices") },
                                    text = {
                                        val view = LocalView.current
                                        SideEffect { modalWindow.set((view.parent as DialogWindowProvider).window) }
                                        Column {
                                            listOf("One", "Two", "Three").forEach { choice ->
                                                Button(
                                                    onClick = { actions += "choice:$choice" },
                                                    modifier = Modifier.testTag("qa_choice_$choice"),
                                                ) { Text(choice) }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(10000) {
            val insets = modalWindow.get()?.decorView?.let { androidx.core.view.ViewCompat.getRootWindowInsets(it) }
            insets != null && !insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars()) &&
                !insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
        }
        compose.onNodeWithTag("console_dialog_left_controls").assertIsDisplayed()
        compose.onNodeWithTag("console_dialog_right_controls").assertIsDisplayed()
        val center = compose.onNodeWithTag("console_settings_dialog").fetchSemanticsNode().boundsInWindow
        val left = compose.onNodeWithTag("console_dialog_left_controls").fetchSemanticsNode().boundsInWindow
        val right = compose.onNodeWithTag("console_dialog_right_controls").fetchSemanticsNode().boundsInWindow
        if (Build.VERSION.SDK_INT >= 28) {
            val expected =
                if (Build.VERSION.SDK_INT >= 30) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            assertEquals(
                "A modal must explicitly opt into the same cutout policy as its host",
                expected,
                checkNotNull(modalWindow.get()).attributes.layoutInDisplayCutoutMode,
            )
        }
        // Supplied bounds are host-window coordinates; the separate fullscreen Dialog must
        // preserve that coordinate origin. This is policy/alignment proof, not a physical notch.
        compose.runOnIdle {
            val host = IntArray(2)
            val modal = IntArray(2)
            compose.activity.window.decorView.getLocationOnScreen(host)
            checkNotNull(modalWindow.get()).decorView.getLocationOnScreen(modal)
            assertEquals("Host and modal window X origin", host[0], modal[0])
            assertEquals("Host and modal window Y origin", host[1], modal[1])
        }
        assertEquals("Modal surface uses host-relative X", region.windowBounds!!.left.toFloat(), center.left, 1f)
        assertEquals("Modal surface uses host-relative Y", lowerTop.toFloat(), center.top, 1f)
        assertTrue("Dialog stays below the crease", center.top >= lowerTop)
        assertTrue(
            "Modal controls must not overlap central choices",
            left.right <= center.left && center.right <= right.left,
        )
        clickWing("Select, move focus forward", realTouch)
        compose.onNodeWithTag("console_settings_dialog").printToLog("ModalFocusAfterFirstSelect", maxDepth = 6)
        val validTargets =
            listOf(
                compose.onNodeWithContentDescription("Close"),
                compose.onNodeWithTag("qa_choice_One"),
                compose.onNodeWithTag("qa_choice_Two"),
                compose.onNodeWithTag("qa_choice_Three"),
            )
        assertTrue(
            "Initial side control must acquire a real modal target; entry order is platform-defined",
            validTargets.any { target ->
                try {
                    target.assertIsFocused()
                    true
                } catch (_: AssertionError) {
                    false
                }
            },
        )
        // Require actual traversal even if the platform's initial entry target happens to be Two.
        if (secondChoiceFocused()) clickWing("Y, move focus backward", realTouch)
        for (direction in listOf("Select, move focus forward", "Y, move focus backward")) {
            repeat(5) {
                if (!secondChoiceFocused()) {
                    clickWing(direction, realTouch)
                    compose.onNodeWithTag("console_settings_dialog").printToLog("ModalFocusAfterMove", maxDepth = 6)
                }
            }
        }
        compose.onNodeWithTag("qa_choice_Two").assertIsFocused()
        assertEquals("Focus movement must not activate a choice", emptyList<String>(), actions)
        clickWing("A, activate focused item", realTouch)
        assertEquals("A belongs to the modal, not the app behind it", listOf("choice:Two"), actions)
        clickWing("B, close dialog", realTouch)
        compose.onNodeWithTag("console_settings_dialog").assertDoesNotExist()
        compose.onNodeWithTag("console_dialog_left_controls").assertDoesNotExist()
        compose.runOnIdle { shown.value = true }
        compose.onNodeWithTag("console_settings_dialog").assertIsDisplayed()
        clickWing("B, close dialog", realTouch)
        compose.onNodeWithTag("console_settings_dialog").assertDoesNotExist()
        assertEquals("Repeated open/cancel must not confirm or launch anything", listOf("choice:Two"), actions)
    }

    private fun secondChoiceFocused(): Boolean =
        try {
            compose.onNodeWithTag("qa_choice_Two").assertIsFocused()
            true
        } catch (_: AssertionError) {
            false
        }

    private fun clickWing(
        description: String,
        realTouch: Boolean,
    ) {
        val target = compose.onNodeWithContentDescription(description)
        if (realTouch) {
            // A real Android pointer DOWN switches input mode; production must restore the
            // remembered center target before the subsequent UP click confirms or advances it.
            target.performTouchInput { down(center) }
            SystemClock.sleep(80)
            target.performTouchInput { up() }
        } else {
            target.performClick()
        }
        compose.waitForIdle()
    }
}
