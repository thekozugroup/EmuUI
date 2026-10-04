package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
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
        publishOpenInnerDisplay()
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
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasText("Open your foldable")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText("Open your foldable").assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasContentDescription("Import games folder")).fetchSemanticsNodes().isEmpty())
        publishOpenInnerDisplay()
        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    @Test
    fun verticalHingeShowsGuidanceAndUnfoldRestoresSelection() {
        publishOpenInnerDisplay()
        waitForLibrary()
        compose.onNodeWithText("Favorites").performClick()
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        size = 24,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.VERTICAL,
                    ),
                ),
            ),
        )
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasText("Turn to your happy place")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText(
            "Rotate your device so the crease runs left to right. Preview goes above; your library goes below.",
        ).assertIsDisplayed()
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasText("Open your foldable")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText("Open your foldable").assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasContentDescription("Import games folder")).fetchSemanticsNodes().isEmpty())
        publishOpenInnerDisplay()
        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    @Test
    fun ordinaryLandscapePhoneHasNoInteractiveConsoleUntilHardwareFeatureArrives() {
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasText("Open your foldable")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.onNodeWithText("Open your foldable").assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasContentDescription("Import games folder")).fetchSemanticsNodes().isEmpty())
        publishOpenInnerDisplay()
        waitForLibrary()
    }

    private fun publishOpenInnerDisplay() {
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        state = FoldingFeature.State.FLAT,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
    }

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
