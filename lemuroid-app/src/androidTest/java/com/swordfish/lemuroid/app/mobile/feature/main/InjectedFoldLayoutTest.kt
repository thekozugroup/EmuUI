package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowMetricsCalculator
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Synthetic WindowManager events on an ordinary emulator. These test layout and
 * state reactions, not a physical device's hinge sensor, OEM extension, or game core.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InjectedFoldLayoutTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun offCentreHorizontalHingeKeepsPreviewAboveAndLibraryBelow() {
        waitForLibrary()
        compose.onNodeWithText("Favorites").performClick()
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(compose.activity).bounds
        val center = (bounds.height() * 0.45f).toInt()
        val size = 24

        for (state in listOf(FoldingFeature.State.HALF_OPENED, FoldingFeature.State.FLAT)) {
            windowInfo.overrideWindowLayoutInfo(
                TestWindowLayoutInfo(
                    listOf(
                        TestFoldingFeature(
                            activity = compose.activity,
                            center = center,
                            size = size,
                            state = state,
                            orientation = FoldingFeature.Orientation.HORIZONTAL,
                        ),
                    ),
                ),
            )
            compose.waitUntil(TIMEOUT) {
                runCatching {
                    compose.onNodeWithContentDescription("Import games folder")
                        .fetchSemanticsNode().boundsInWindow.top >=
                        center + size / 2
                }.getOrDefault(false)
            }
            val preview = compose.onNodeWithContentDescription("EmuUI home").fetchSemanticsNode().boundsInWindow
            val library = compose.onNodeWithText("Favorites").fetchSemanticsNode().boundsInWindow
            assertTrue("Preview header must stay above the injected hinge", preview.bottom <= center - size / 2)
            assertTrue("Library controls must stay below the injected hinge", library.top >= center + size / 2)
            compose.onNodeWithText("Favorites").assertIsSelected()
        }

        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    @Test
    fun clamshellCreaseTurnsToPortraitAndUnfoldRestoresSelection() {
        waitForLibrary()
        compose.onNodeWithText("Favorites").performClick()
        val landscapeActivity = compose.activity
        // A clamshell in a landscape window reports a vertical crease: ask for portrait.
        windowInfo.overrideWindowLayoutInfo(crease(landscapeActivity, FoldingFeature.Orientation.VERTICAL))
        // The rotation relaunches MainActivity. Wait for the new portrait instance before
        // publishing, because the test publisher does not replay to late collectors.
        compose.waitUntil(TIMEOUT) {
            val activity = compose.activity
            activity !== landscapeActivity &&
                activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
        compose.waitForIdle()
        val portraitActivity = compose.activity
        windowInfo.overrideWindowLayoutInfo(crease(portraitActivity, FoldingFeature.Orientation.HORIZONTAL))
        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
        val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(portraitActivity).bounds
        assertTrue("Portrait window expected", bounds.height() > bounds.width())
        val preview = compose.onNodeWithContentDescription("EmuUI home").fetchSemanticsNode().boundsInWindow
        val library = compose.onNodeWithText("Favorites").fetchSemanticsNode().boundsInWindow
        assertTrue("Preview stays above the crease", preview.bottom <= bounds.height() / 2f)
        assertTrue("Library stays below the crease", library.top >= bounds.height() / 2f)
        // Closing the device removes the crease and hands back the landscape default.
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        compose.waitUntil(TIMEOUT) {
            compose.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun crease(
        activity: MainActivity,
        orientation: FoldingFeature.Orientation,
    ) = TestWindowLayoutInfo(
        listOf(
            TestFoldingFeature(
                activity = activity,
                size = 24,
                state = FoldingFeature.State.HALF_OPENED,
                orientation = orientation,
            ),
        ),
    )

    private fun waitForLibrary() {
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasContentDescription("EmuUI home")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TIMEOUT = 30_000L
    }
}
