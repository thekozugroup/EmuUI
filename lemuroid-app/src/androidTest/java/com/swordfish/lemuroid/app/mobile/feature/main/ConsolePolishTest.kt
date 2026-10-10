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
import kotlinx.coroutines.flow.first
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
        compose.waitForIdle()
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).waitForIdle(1000)
        val output = File(compose.activity.getExternalFilesDir(null), "polish-qa").apply { mkdirs() }
        assertTrue(UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(File(output, "$name.png")))
    }

    @Test fun refinedConsoleRoutesAndHapticPreferenceRemainReachable() {
        openFold()
        compose.onNodeWithTag("launcher_library_heading").assertIsDisplayed()
        capture("refined-library")
        compose.onNodeWithTag("launcher_shortcut_1").performClick()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        capture("refined-search")
        compose.onNodeWithContentDescription("B, back").performClick()
        compose.onNodeWithTag("launcher_shortcut_2").performClick()
        compose.onNodeWithText("Systems").assertIsDisplayed()
        capture("refined-systems")
        compose.onNodeWithContentDescription("B, back").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Console frame").performScrollTo().assertIsDisplayed()
        capture("refined-settings")
        compose.onNodeWithText("Control haptics").performScrollTo().performClick()
        compose.onNodeWithTag("console_settings_dialog").assertIsDisplayed()
        capture("refined-haptics-dialog")
        compose.onNodeWithContentDescription("B, close dialog").performClick()
        compose.onNodeWithContentDescription("B, back").performClick()
        compose.onNodeWithContentDescription("Help and supported formats").performClick()
        compose.onNodeWithText("EmuUI · Help & credits").assertIsDisplayed()
        capture("refined-help")
        compose.onNodeWithContentDescription("B, back").performClick()
        compose.onNodeWithTag("launcher_grid").assertIsDisplayed()
    }

    @Test fun searchTypingKeepsReadableInputAndRestoresConsoleControls() {
        openFold()
        compose.onNodeWithTag("launcher_shortcut_1").performClick()
        val field = compose.onNode(hasSetTextAction())
        field.performClick().performTextInput("EmuUI")
        compose.waitForIdle()
        val height = field.fetchSemanticsNode().boundsInWindow.height
        assertTrue("Keyboard must not compress the search field", height >= 48f * compose.activity.resources.displayMetrics.density - 1f)
        capture("refined-search-typing")
        field.performImeAction()
        compose.waitUntil(10000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime()) == false
        }
        compose.onNodeWithContentDescription("B, back").performClick()
        compose.onNodeWithTag("launcher_grid").assertIsDisplayed()
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

    @Test fun enlargedLibraryAndDialogKeepComfortableNonOverlappingControls() {
        openFold()
        fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInWindow
        val center = bounds("launcher_library_center")
        val left = bounds("launcher_left_controls")
        val right = bounds("launcher_right_controls")
        val preview = bounds("launcher_preview")
        val density = compose.activity.resources.displayMetrics.density
        assertTrue(left.right <= center.left && center.right <= right.left)
        assertEquals(preview.center.x, center.center.x, 1f)
        assertTrue("Wings retain at least 156dp", left.width >= 156f * density - 1f)
        assertTrue("Wings stay at most 176dp", left.width <= 176f * density + 1f)
        for (description in listOf("Select game to the right", "Select game to the left", "Select game above", "Select game below")) {
            val target = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInWindow
            assertTrue("48dp target: $description", target.width >= 48f * density - 1f && target.height >= 48f * density - 1f)
            assertTrue("Target inside wing: $description", target.left >= left.left && target.right <= left.right && target.top >= left.top && target.bottom <= left.bottom)
        }
        val games = runBlocking(kotlinx.coroutines.Dispatchers.IO) { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        check(games.size >= 2) { "Two existing original homebrew fixtures are required" }
        compose.onNodeWithTag("launcher_game_${games.first().id}").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Select game to the right").performTouchInput { click() }
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        capture("enlarged-library")
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Console frame").performScrollTo().performClick()
        val dialog = bounds("console_settings_dialog")
        val dialogLeft = bounds("console_dialog_left_controls")
        val dialogRight = bounds("console_dialog_right_controls")
        assertEquals(center.left, dialog.left, 2f)
        assertEquals(center.right, dialog.right, 2f)
        assertEquals(left.width, dialogLeft.width, 2f)
        assertEquals(right.width, dialogRight.width, 2f)
        assertTrue(dialogLeft.right <= dialog.left && dialog.right <= dialogRight.left)
        capture("enlarged-frame-dialog")
        compose.onNode(hasAnyAncestor(hasTestTag("console_settings_dialog")) and hasText("Match app theme"))
            .performScrollTo().assertIsDisplayed()
        capture("enlarged-frame-dialog-scrolled")
        val report = org.json.JSONObject().put("width", preview.width).put("density", density)
        for ((name, rect) in listOf("library" to center, "leftWing" to left, "rightWing" to right, "dialog" to dialog)) {
            report.put(name, org.json.JSONArray(listOf(rect.left, rect.top, rect.right, rect.bottom)))
        }
        File(compose.activity.getExternalFilesDir(null), "polish-qa/enlarged-layout.json").writeText(report.toString(2))
    }

}
