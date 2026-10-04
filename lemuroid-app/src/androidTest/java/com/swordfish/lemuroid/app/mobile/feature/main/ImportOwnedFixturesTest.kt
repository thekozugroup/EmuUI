package com.swordfish.lemuroid.app.mobile.feature.main

import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Opt-in setup only: the coordinator stages original lawful fixtures and operates the real SAF picker.
 * This opens the app's normal Import action and verifies its real scanner output. No fabricated rows,
 * direct preference writes, synthetic grants, core start or network acquisition are used.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ImportOwnedFixturesTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun realPickerImportsOnlyTheStagedOriginalQaFolder() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(InstrumentationRegistry.getArguments().getString("qaImportFolder") == "EmuUI-QA") {
            "Explicit opt-in requires staging original fixtures in EmuUI-QA first"
        }
        val expected =
            InstrumentationRegistry.getArguments().getString("expectedFiles")?.split(',')?.toSet()
                ?: setOf("EmuUI_QA.nes", "EmuUI_QA_Second.nes", "EmuUI_DS_Legacy_Long_Title_QA.nds")
        val before = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        check(before.isEmpty()) { "This setup test only runs against the dedicated empty QA library" }
        compose.waitForIdle()
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        size = 24,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
        compose.waitUntil(60_000) {
            try {
                compose.onNodeWithContentDescription("Import games folder").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithContentDescription("Import games folder").performClick()
        instrumentation.sendStatus(
            0,
            Bundle().apply { putString("qaImportStage", "choose-staged-EmuUI-QA-in-real-picker") },
        )
        compose.waitUntil(300_000) {
            SystemClock.sleep(500)
            val imported = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
            imported.map { it.fileName }.toSet() == expected
        }
        val imported = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        assertTrue(
            "All imported rows must originate from the staged QA folder",
            imported.all { "EmuUI-QA" in it.fileUri },
        )
        val grants = instrumentation.targetContext.contentResolver.persistedUriPermissions
        assertTrue(
            "The real picker must supply the QA folder read grant",
            grants.any {
                it.isReadPermission && "EmuUI-QA" in it.uri.toString()
            },
        )
    }
}
