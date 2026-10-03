package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in, post-import regression. Import BOTH locally generated QA cartridges through
 * the real folder picker first. Pass -e qaGameTitle <the SECOND title> to am instrument.
 * Keeping this separate prevents a default-first fallback from falsely passing the test.
 * This is library selection coverage, not native gameplay/recreation coverage.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SeededLibraryStateTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun nonDefaultImportedSelectionSurvivesRecreation() {
        val title =
            requireNotNull(InstrumentationRegistry.getArguments().getString("qaGameTitle")) {
                "Import two original QA cartridges first and pass qaGameTitle for the non-default one"
            }
        val cartridge = hasText(title) and hasClickAction()
        compose.waitUntil(60_000) {
            runCatching { compose.onAllNodes(cartridge).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.onNode(cartridge).performClick().assertIsSelected()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(60_000) {
            runCatching { compose.onAllNodes(cartridge).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
        compose.onNode(cartridge).assertIsSelected()
    }
}
