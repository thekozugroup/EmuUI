package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ThemePreferences
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/** Synthetic fold metadata on the real launcher; never a hardware or native DS claim. */
class ConsolePolishTest {
    @get:Rule(order = 0) val windowInfo = WindowLayoutInfoPublisherRule()
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun openFold() {
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(TestFoldingFeature(
            activity = compose.activity, size = 24,
            state = FoldingFeature.State.HALF_OPENED,
            orientation = FoldingFeature.Orientation.HORIZONTAL,
        ))))
        compose.waitUntil(60000) { runCatching { compose.onNodeWithTag("launcher_preview").fetchSemanticsNode() }.isSuccess }
    }

    private fun capture(name: String) {
        val output = File(compose.activity.getExternalFilesDir(null), "polish-qa").apply { mkdirs() }
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(output, "$name.png")))
    }

    @Test fun repeatedSelectionHasNoHiddenFilterTargetsAndReturnsFromBackgroundImmersive() {
        val db = compose.activity.retrogradeDb.gameDao()
        val fixtures = (1..2).map { n -> Game(fileName = "polish-$n.nes", fileUri = "file:///emuui-owned-polish-$n.nes", title = "Original Homebrew $n", systemId = "nes", developer = null, coverFrontUrl = null, lastIndexedAt = System.currentTimeMillis()) }
        val ids = runBlocking(kotlinx.coroutines.Dispatchers.IO) { db.insert(fixtures) }
        try {
            openFold()
            compose.onNodeWithTag("launcher_filters").assertDoesNotExist()
            repeat(12) { n ->
                val id = ids[n % 2]
                compose.onNodeWithTag("launcher_game_$id").performScrollTo().performClick()
                compose.onNodeWithTag("launcher_game_$id").assertIsSelected()
            }
            capture("launcher-selection-oled")
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            openFold()
            compose.waitUntil(10000) {
                val insets = ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                insets != null && !insets.isVisible(WindowInsetsCompat.Type.statusBars()) && !insets.isVisible(WindowInsetsCompat.Type.navigationBars())
            }
            compose.onNodeWithTag("launcher_game_${ids.last()}").assertIsSelected()
            capture("launcher-resumed-immersive")
        } finally {
            runBlocking(kotlinx.coroutines.Dispatchers.IO) { db.delete(fixtures.mapIndexed { i, game -> game.copy(id = ids[i].toInt()) }) }
        }
    }

    @Test fun frameSettingPersistsAcrossActivityRecreation() {
        val preferences = SharedPreferencesHelper.getSharedPreferences(compose.activity)
        val before = preferences.getString(ThemePreferences.FRAME_KEY, null)
        try {
            openFold()
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Console frame").performScrollTo().performClick()
            compose.onNode(hasAnyAncestor(hasTestTag("console_settings_dialog")) and hasText("Graphite")).performClick()
            compose.waitUntil(10000) { preferences.getString(ThemePreferences.FRAME_KEY, null) == "GRAPHITE" }
            capture("frame-setting-graphite")
            compose.activityRule.scenario.recreate()
            openFold()
            assertEquals("GRAPHITE", SharedPreferencesHelper.getSharedPreferences(compose.activity).getString(ThemePreferences.FRAME_KEY, null))
            // The settings route is restored by rememberSaveable on recreation.
            compose.onNodeWithText("Console frame").performScrollTo().performClick()
            compose.onNode(hasAnyAncestor(hasTestTag("console_settings_dialog")) and hasText("OLED black")).performClick()
            compose.waitUntil(10000) { preferences.getString(ThemePreferences.FRAME_KEY, null) == "OLED" }
            capture("frame-setting-oled")
        } finally {
            preferences.edit().apply { if (before == null) remove(ThemePreferences.FRAME_KEY) else putString(ThemePreferences.FRAME_KEY, before) }.commit()
        }
    }
    @Test fun calibratedPositionSurvivesRecreationAndCanBeReset() {
        val prefs = SharedPreferencesHelper.getSharedPreferences(compose.activity)
        val key = com.swordfish.lemuroid.app.mobile.feature.emuui.ConsoleOrientation.KEY
        val deviceKey = com.swordfish.lemuroid.app.mobile.feature.emuui.ConsoleOrientation.DEVICE_KEY
        val old = prefs.getInt(key, -1)
        val oldDevice = prefs.getString(deviceKey, null)
        try {
            openFold()
            compose.runOnIdle { com.swordfish.lemuroid.app.mobile.feature.emuui.ConsoleOrientation.calibrate(compose.activity) }
            val saved = prefs.getInt(key, -1)
            assertTrue(saved == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || saved == android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
            compose.activityRule.scenario.recreate()
            openFold()
            assertEquals(saved, compose.activity.requestedOrientation)
            compose.runOnIdle { com.swordfish.lemuroid.app.mobile.feature.emuui.ConsoleOrientation.clear(compose.activity) }
            assertEquals(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, compose.activity.requestedOrientation)
        } finally {
            prefs.edit().apply {
                if (old == -1) remove(key) else putInt(key, old)
                if (oldDevice == null) remove(deviceKey) else putString(deviceKey, oldDevice)
            }.commit()
        }
    }

}
