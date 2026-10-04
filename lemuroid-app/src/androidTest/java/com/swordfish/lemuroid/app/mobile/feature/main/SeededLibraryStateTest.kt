package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Opt-in, post-import regression. Import BOTH locally generated QA cartridges through
 * the real folder picker first. Pass -e qaGameTitle <the SECOND title> to am instrument.
 * Keeping this separate prevents a default-first fallback from falsely passing the test.
 * This is library selection coverage, not native gameplay/recreation coverage.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SeededLibraryStateTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun nonDefaultImportedSelectionSurvivesRecreation() {
        val title =
            requireNotNull(InstrumentationRegistry.getArguments().getString("qaGameTitle")) {
                "Import two original QA cartridges first and pass qaGameTitle for the non-default one"
            }
        val game =
            runBlocking {
                compose.activity.retrogradeDb.gameDao().observeLibrary().first()
                    .filter { it.title == title || it.fileName == title }.single()
            }
        publishOpenInnerDisplay()
        val cartridge = hasTestTag("launcher_game_${game.id}") and hasClickAction()
        compose.waitUntil(60_000) {
            runCatching { compose.onAllNodes(cartridge).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.onNode(cartridge).assert(
            hasText(game.title) or hasContentDescription(game.title, substring = true),
        ).performClick().assertIsSelected()
        compose.activityRule.scenario.recreate()
        publishOpenInnerDisplay()
        compose.waitUntil(60_000) {
            runCatching { compose.onAllNodes(cartridge).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.onNode(cartridge).assertIsSelected()
    }

    private fun publishOpenInnerDisplay() {
        // Subscribe the current composition before the non-replaying publisher emits.
        compose.waitForIdle()
        // Test-process-only WindowManager event. Production has no posture override.
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
}
