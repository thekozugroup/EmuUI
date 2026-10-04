package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.feature.home.ConsoleHomeScreen
import com.swordfish.lemuroid.app.mobile.feature.home.HomeViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ThemePreferences
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/** Fresh screenshots of real production composables; no database/permission/device changes. */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LauncherLayoutCaptureTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun realImportedLauncherHasFunctionalWingsAndRetainsSelection() {
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        capture("actual-00-no-fold-gate", "Normal MainActivity, no fold metadata", emptyMap())
        publish(FoldingFeature.State.HALF_OPENED)
        waitForTag("launcher_preview")
        val games = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        check(games.size >= 2) { "At least two previously imported lawful fixtures are required" }
        compose.onNodeWithTag("launcher_game_${games.first().id}").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Select game to the right").performClick()
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        assertGeometry()
        capture(
            "actual-01-populated-half-open",
            "Actual imported library with injected horizontal HALF_OPENED metadata",
            geometry(),
        )
        publish(FoldingFeature.State.FLAT)
        waitForTag("launcher_preview")
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        assertGeometry()
        capture(
            "actual-02-populated-flat",
            "Actual imported library with injected horizontal FLAT metadata",
            geometry(),
        )
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        publish(FoldingFeature.State.HALF_OPENED)
        waitForTag("launcher_preview")
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        capture(
            "actual-03-selection-after-gate",
            "Actual selection retained across loss and return of injected fold metadata",
            geometry(),
        )
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed()
        assertRouteBounds()
        capture(
            "actual-04-settings-in-center",
            "Actual Settings route inside lower center with preview and wings retained",
            geometry(),
        )
        compose.onNodeWithText("Advanced settings").performScrollTo().assertIsDisplayed()
        capture(
            "actual-05-settings-scrolled",
            "Actual Settings content scrolled to final Advanced settings action",
            geometry(),
        )
        compose.onNodeWithText("Advanced settings").performClick()
        compose.onNodeWithText("Cache size limit").performScrollTo().performClick()
        waitForTag("console_settings_dialog")
        assertSettingsDialogBounds()
        capture(
            "actual-05a-settings-list-dialog",
            "Actual Cache size limit popup contained in lower center; no setting selected",
            geometry() + ("console_settings_dialog" to bounds("console_settings_dialog")),
        )
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        assertTrue(
            "Settings popup must disappear without open-fold metadata",
            compose.onAllNodes(hasTestTag("console_settings_dialog")).fetchSemanticsNodes().isEmpty(),
        )
        capture(
            "actual-05b-settings-popup-gated",
            "Actual settings popup suppressed by strict posture gate",
            emptyMap(),
        )
        publish(FoldingFeature.State.HALF_OPENED)
        waitForTag("console_settings_dialog")
        assertSettingsDialogBounds()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        compose.onNodeWithContentDescription("Help and supported formats").performClick()
        compose.onNodeWithText("Back to console").assertIsDisplayed()
        assertRouteBounds()
        capture(
            "actual-06-help-in-center",
            "Actual Help route inside lower center with preview and wings retained",
            geometry(),
        )
        compose.onNodeWithText("Back to console").performClick()
        compose.onNodeWithTag("launcher_game_${games[1].id}").assertIsSelected()
        capture(
            "actual-07-console-after-help-close",
            "Actual library and selection restored after Help close",
            geometry(),
        )
    }

    /** Actual external wm resizes; intentionally does not change requestedOrientation or device settings. */
    @Test
    fun actualWindowResizeShowsGuidanceAndRetainsSelection() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val run = requireNotNull(InstrumentationRegistry.getArguments().getString("qaCaptureRun"))
        require(run != "default" && run.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,95}"))) {
            "Supply a new, path-safe qaCaptureRun for the resize coordinator"
        }
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir("qa-launcher-layout")), run)
        check(directory.mkdir()) { "qaCaptureRun already exists or cannot be created: $run" }
        val windows = JSONArray()
        val report =
            JSONObject().put("run", run).put("evidenceType", "Actual wm resize; injected horizontal fold metadata")
                .put("requestedSizes", JSONArray(listOf("1440x1200", "1200x1440", "1440x1200")))
                .put("orientationRequestsChanged", false).put("windows", windows)
        var stage = "starting"

        fun checkpoint(
            name: String,
            nextSize: String? = null,
        ) {
            stage = name
            val window = actualWindowEvidence().put("stage", name).put("recordedAtEpochMs", System.currentTimeMillis())
            windows.put(window)
            if (nextSize != null) {
                writeResizeJson(
                    directory,
                    "$name.json",
                    JSONObject().put("run", run).put("stage", name).put("nextSize", nextSize).put("window", window),
                )
            }
        }
        try {
            waitForActualWindow(Configuration.ORIENTATION_LANDSCAPE)
            publishFreshResizeFeature()
            waitForTag("launcher_preview")
            val games = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
            check(games.size >= 2) { "At least two previously imported lawful fixtures are required" }
            val selectedId = games[1].id
            report.put("selectedGameId", selectedId).put("selectedFilter", "Favorites")
            compose.onNodeWithText("All games").performClick().assertIsSelected()
            compose.onNodeWithTag("launcher_game_$selectedId").performClick().assertIsSelected()
            compose.onNodeWithText("Favorites").performClick().assertIsSelected()
            assertGeometry()
            capture(
                "resize-00-landscape-favorites",
                "Actual landscape MainActivity; Favorites and a non-default imported game selected before resize",
                geometry(),
            )
            // Remove old-window metadata before the coordinator changes the physical display size.
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag("fold_guidance")
            checkpoint("ready-for-portrait", "1200x1440")
            stage = "waiting-for-actual-portrait"
            waitForActualWindow(Configuration.ORIENTATION_PORTRAIT)
            checkpoint("portrait-window-observed")
            publishFreshResizeFeature()
            compose.waitUntil(60_000) {
                runCatching { compose.onNodeWithText("Turn to your happy place").assertIsDisplayed() }.isSuccess
            }
            compose.onNodeWithText(
                "Use EmuUI in landscape, with the top screen above the library.",
            ).assertIsDisplayed()
            listOf(
                "launcher_preview",
                "launcher_library_center",
                "launcher_left_controls",
                "launcher_right_controls",
            ).forEach { compose.onNodeWithTag(it).assertIsNotDisplayed() }
            for (game in games) compose.onNodeWithTag("launcher_game_${game.id}").assertIsNotDisplayed()
            assertEquals(
                "Portrait guidance must expose no hidden clickable controls",
                0,
                compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
                    .fetchSemanticsNodes().size,
            )
            capture(
                "resize-01-portrait-guidance",
                "Actual PORTRAIT configuration and decor; current-window horizontal HALF_OPENED metadata",
                mapOf("fold_guidance" to bounds("fold_guidance")),
                verifyGuidanceContrast = true,
            )
            report.put("portraitGuidancePassed", true).put("hiddenClickableControls", 0)
            windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
            waitForTag("fold_guidance")
            checkpoint("ready-for-landscape", "1440x1200")
            stage = "waiting-for-actual-landscape-return"
            waitForActualWindow(Configuration.ORIENTATION_LANDSCAPE)
            checkpoint("landscape-window-restored")
            publishFreshResizeFeature()
            waitForTag("launcher_preview")
            compose.onNodeWithText("Favorites").assertIsSelected()
            assertGeometry()
            capture(
                "resize-02-landscape-favorites-restored",
                "Actual LANDSCAPE return; Favorites retained without another Favorites click",
                geometry(),
            )
            report.put("favoritesRetained", true)
            // Returning to All games reveals the previously selected card without selecting it again.
            compose.onNodeWithText("All games").performClick().assertIsSelected()
            compose.onNodeWithTag("launcher_game_$selectedId").assertIsSelected()
            assertGeometry()
            capture(
                "resize-03-landscape-game-selection-restored",
                "Actual LANDSCAPE return; non-default imported game selection retained across both resizes",
                geometry(),
            )
            checkpoint("completed")
            report.put("gameSelectionRetained", true).put("passed", true).put("stage", stage)
            writeResizeJson(directory, "resize-result.json", report)
        } catch (failure: Throwable) {
            report.put("passed", false).put("stage", stage).put("failure", failure.toString())
                .put("lastWindow", runCatching { actualWindowEvidence() }.getOrNull() ?: JSONObject.NULL)
            runCatching { writeResizeJson(directory, "resize-result.json", report) }
            throw failure
        }
    }

    private fun actualWindowEvidence(): JSONObject {
        var evidence = JSONObject()
        compose.activityRule.scenario.onActivity { activity ->
            val configuration = activity.resources.configuration
            val decor = activity.window.decorView
            evidence =
                JSONObject().put("orientation", configuration.orientation)
                    .put("decorWidth", decor.width).put("decorHeight", decor.height)
                    .put("screenWidthDp", configuration.screenWidthDp)
                    .put("screenHeightDp", configuration.screenHeightDp)
                    .put("densityDpi", configuration.densityDpi).put("fontScale", configuration.fontScale)
                    .put("requestedOrientation", activity.requestedOrientation)
                    .put("activityIdentity", System.identityHashCode(activity))
        }
        return evidence
    }

    private fun waitForActualWindow(expectedOrientation: Int) {
        compose.waitUntil(60_000) {
            runCatching {
                val window = actualWindowEvidence()
                val width = window.getInt("decorWidth")
                val height = window.getInt("decorHeight")
                window.getInt("orientation") == expectedOrientation && width > 0 && height > 0 &&
                    if (expectedOrientation == Configuration.ORIENTATION_PORTRAIT) width < height else width > height
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun publishFreshResizeFeature() {
        // The resized activity/composition must subscribe before receiving its new current-window feature.
        compose.waitForIdle()
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        compose.onNodeWithText("Open your foldable").assertIsDisplayed()
        publish(FoldingFeature.State.HALF_OPENED)
    }

    private fun writeResizeJson(
        directory: File,
        name: String,
        value: JSONObject,
    ) {
        // Atomic rename prevents the host from observing a partially written readiness marker.
        val pending = File(directory, "$name.tmp")
        pending.writeText(value.toString(2))
        check(pending.renameTo(File(directory, name))) { "Could not publish resize evidence: $name" }
    }

    @Test
    fun themeChoicesPersistAcrossActivityRecreation() {
        val preferences = SharedPreferencesHelper.getSharedPreferences(compose.activity.applicationContext)
        val requested = InstrumentationRegistry.getArguments().getString("qaThemeMode")
        val choices =
            listOf("Light" to "light", "Dark" to "dark", "System" to "system")
                .filter { requested == null || it.second == requested }
        check(choices.isNotEmpty()) { "qaThemeMode must be light, dark or system" }
        waitForTag("fold_guidance")
        publish(FoldingFeature.State.HALF_OPENED)
        waitForTag("launcher_preview")
        for ((label, value) in choices) {
            compose.onNodeWithContentDescription("Settings").performClick()
            compose.onNodeWithText("Theme").performScrollTo().performClick()
            waitForTag("console_settings_dialog")
            compose.onNode(
                hasAnyAncestor(hasTestTag("console_settings_dialog")) and hasText(label),
            ).performClick()
            compose.waitUntil(30_000) {
                preferences.getString(ThemePreferences.THEME_MODE_KEY, null) == value
            }
            compose.onNodeWithContentDescription("Back").performClick()
            waitForTag("launcher_preview")
            capture(
                "theme-$value-selected",
                "Actual app Theme setting $label; ${themeEvidenceLabel()}",
                geometry(),
            )
            compose.activityRule.scenario.recreate()
            waitForTag("fold_guidance")
            publish(FoldingFeature.State.HALF_OPENED)
            waitForTag("launcher_preview")
            assertEquals(
                "Theme choice must survive Activity recreation",
                value,
                SharedPreferencesHelper.getSharedPreferences(compose.activity.applicationContext)
                    .getString(ThemePreferences.THEME_MODE_KEY, null),
            )
            assertGeometry()
            capture(
                "theme-$value-after-recreate",
                "Actual persisted $label theme after MainActivity recreation; ${themeEvidenceLabel()}",
                geometry(),
            )
        }
    }

    @Test
    fun presentationStatesAtNormalAndLargeText() {
        val games = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        val state = mutableStateOf(HomeViewModel.UIState(allGames = games, isLoading = false, indexInProgress = false))
        val scale = mutableStateOf(1f)
        val selectedId = mutableStateOf(games.firstOrNull()?.id)
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val physicalDensity = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(physicalDensity, scale.value)) {
                    AppTheme {
                        ConsoleHomeScreen(
                            modifier = Modifier,
                            state = state.value,
                            selectedGameId = selectedId.value,
                            onGameSelected = { selectedId.value = it.id },
                            onPlay = {},
                            onGameOptions = {},
                            onImport = {},
                            onRetry = {},
                            onOpenSettings = {},
                            onOpenSystems = {},
                            onOpenSearch = {},
                            onOpenHelp = {},
                            onOpenCoreSelection = {},
                            onSyncSaves = null,
                            onEnableNotifications = {},
                            onEnableMicrophone = {},
                        )
                    }
                }
            }
        }
        // The replacement composition must subscribe before publishing its window feature.
        compose.waitForIdle()
        waitForTag("fold_guidance")
        publish(FoldingFeature.State.HALF_OPENED)
        val scenarios =
            listOf(
                "populated" to HomeViewModel.UIState(allGames = games, isLoading = false, indexInProgress = false),
                "empty" to HomeViewModel.UIState(isLoading = false, indexInProgress = false, showNoGamesCard = true),
                "error" to
                    HomeViewModel.UIState(
                        isLoading = false,
                        indexInProgress = false,
                        errorMessage = "Folder access was denied. Choose a folder you can open.",
                        scanFailed = true,
                    ),
                "onboarding" to
                    HomeViewModel.UIState(
                        isLoading = false,
                        indexInProgress = false,
                        showNoGamesCard = true,
                        showNoMicrophonePermissionCard = true,
                        showNoNotificationPermissionCard = true,
                    ),
            )
        for (fontScale in listOf(1f, 1.5f, 2f)) {
            for ((name, value) in scenarios) {
                compose.runOnIdle {
                    scale.value = fontScale
                    state.value = value
                }
                waitForTag("launcher_preview")
                val label = "presentation-$name-font-${(fontScale * 100).toInt()}"
                capture(
                    label,
                    "Presentation-only production composable; test UIState and LocalDensity fontScale=$fontScale; " +
                        "injected horizontal fold metadata; no import/error integration claim",
                    geometry(),
                )
                assertGeometry()
                if (name == "onboarding") {
                    compose.onNodeWithContentDescription("Finish optional setup").performClick()
                    waitForTag("launcher_setup")
                    capture(
                        "$label-dialog",
                        "Presentation-only setup dialog at fontScale=$fontScale; no permission requested or granted",
                        emptyMap(),
                    )
                    compose.onNodeWithText("Done").performScrollTo().assertIsDisplayed()
                    capture(
                        "$label-actions",
                        "Presentation-only setup actions scrolled into view at fontScale=$fontScale",
                        emptyMap(),
                    )
                    compose.onNodeWithText("Done").performClick()
                }
            }
        }
    }

    @Test
    fun ordinaryNoFoldShowsReadableGuidance() {
        // Uses the actual persisted theme selected through the public Settings UI.
        waitForTag("fold_guidance")
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        compose.onNodeWithText("Open your foldable").assertIsDisplayed()
        capture(
            "ordinary-no-fold-guidance",
            "Actual MainActivity, no fold metadata; ${themeEvidenceLabel()}",
            mapOf("fold_guidance" to bounds("fold_guidance")),
            verifyGuidanceContrast = true,
        )
    }

    @Test
    fun constrainedWindowShowsReadableGuidance() {
        val arguments = InstrumentationRegistry.getArguments()
        val expected = requireNotNull(arguments.getString("qaExpectedGuidance"))
        val scope = requireNotNull(arguments.getString("qaGuidanceScope"))
        require(scope in setOf("window", "library"))
        waitForTag("fold_guidance")
        publish(FoldingFeature.State.HALF_OPENED)
        compose.waitUntil(60_000) {
            runCatching { compose.onNodeWithText(expected).assertIsDisplayed() }.isSuccess
        }
        val games = runBlocking { compose.activity.retrogradeDb.gameDao().observeLibrary().first() }
        for (game in games) compose.onNodeWithTag("launcher_game_${game.id}").assertIsNotDisplayed()
        val regions =
            if (scope == "window") {
                compose.onNodeWithTag("launcher_preview").assertIsNotDisplayed()
                compose.onNodeWithTag("launcher_left_controls").assertIsNotDisplayed()
                compose.onNodeWithTag("launcher_right_controls").assertIsNotDisplayed()
                mapOf("fold_guidance" to bounds("fold_guidance"))
            } else {
                assertGeometry()
                val filters = bounds("launcher_filters")
                val dock = bounds("launcher_dock")
                val center = bounds("launcher_library_center")
                assertTrue("Filter and dock bands must not overlap", filters.bottom <= dock.top)
                assertTrue(
                    "Filter/dock must remain inside the center",
                    filters.top >= center.top && dock.bottom <= center.bottom,
                )
                geometry() + mapOf("launcher_filters" to filters, "launcher_dock" to dock)
            }
        capture(
            "constrained-$scope-guidance",
            "Actual constrained MainActivity; injected horizontal fold metadata; expected guidance: $expected",
            regions,
            verifyGuidanceContrast = scope == "window",
        )
    }

    private fun themeEvidenceLabel(): String =
        if (Build.VERSION.SDK_INT < 31) {
            "API${Build.VERSION.SDK_INT} neutral fallback; no Monet runtime claim"
        } else {
            "API${Build.VERSION.SDK_INT}; Wallpaper colors preference=" +
                SharedPreferencesHelper.getSharedPreferences(compose.activity.applicationContext)
                    .getBoolean(ThemePreferences.DYNAMIC_COLOR_KEY, true) +
                "; rendered palette requires screenshot verification"
        }

    private fun assertGeometry() {
        val preview = bounds("launcher_preview")
        val center = bounds("launcher_library_center")
        val left = bounds("launcher_left_controls")
        val right = bounds("launcher_right_controls")
        assertTrue("Preview must occupy full upper span", preview.left <= left.left && preview.right >= right.right)
        assertTrue("Library must stay below preview", center.top >= preview.bottom)
        assertTrue("Left controls must bracket central library", left.right <= center.left + 1f)
        assertTrue("Right controls must bracket central library", right.left >= center.right - 1f)
        assertTrue("Controls must stay below preview", left.top >= preview.bottom && right.top >= preview.bottom)
        compose.onNodeWithTag("launcher_left_controls").assertIsDisplayed()
        compose.onNodeWithTag("launcher_right_controls").assertIsDisplayed()
    }

    private fun geometry() =
        listOf(
            "launcher_preview",
            "launcher_library_center",
            "launcher_left_controls",
            "launcher_right_controls",
            "launcher_title_plaque",
        )
            .associateWith(::bounds)

    private fun assertRouteBounds() {
        compose.waitForIdle()
        assertGeometry()
        val center = bounds("launcher_library_center")
        val route = bounds("console_route_content")
        assertTrue(
            "Route must remain inside central lower pane",
            route.left >= center.left && route.top >= center.top &&
                route.right <= center.right && route.bottom <= center.bottom,
        )
    }

    private fun assertSettingsDialogBounds() {
        val center = bounds("launcher_library_center")
        val dialog = bounds("console_settings_dialog")
        assertTrue(
            "Settings dialog must remain in lower center",
            dialog.left >= center.left && dialog.top >= center.top &&
                dialog.right <= center.right && dialog.bottom <= center.bottom,
        )
    }

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(
            tag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInWindow

    private fun waitForTag(tag: String) {
        compose.waitUntil(60_000) {
            runCatching {
                compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
                val rect = bounds(tag)
                rect.width > 0f && rect.height > 0f
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun publish(state: FoldingFeature.State) {
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        size = 24,
                        state = state,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
    }

    /** Measures rendered glyph contrast, independent of API level, theme mode or wallpaper palette. */
    private fun guidanceTextContrastReport(bitmap: Bitmap): JSONObject {
        val errors = mutableListOf<String>()
        val measurements = JSONArray()
        val report =
            JSONObject().put("minimumContrast", 4.5).put("minimumPixelCount", 64)
                .put("minimumPixelFraction", 0.02).put("minimumBackgroundFraction", 0.60)
                .put("textRegions", measurements)
        try {
            val offset = IntArray(2)
            compose.runOnIdle {
                val window = IntArray(2)
                compose.activity.window.decorView.getLocationOnScreen(offset)
                compose.activity.window.decorView.getLocationInWindow(window)
                offset[0] -= window[0]
                offset[1] -= window[1]
            }
            report.put("windowOriginOnScreenX", offset[0]).put("windowOriginOnScreenY", offset[1])
            val matcher =
                hasAnyAncestor(hasTestTag("fold_guidance")) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)
            val nodes =
                compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
                    .sortedBy { it.boundsInWindow.top }
            if (nodes.size != 2) errors += "Expected exactly the title and body Text nodes; found ${nodes.size}"
            nodes.forEachIndexed { index, node ->
                val role = if (index == 0) "title" else "body"
                val text = node.config[SemanticsProperties.Text].joinToString(" ") { it.text }
                val rect = node.boundsInWindow
                val left = floor(rect.left + offset[0]).toInt()
                val top = floor(rect.top + offset[1]).toInt()
                val right = ceil(rect.right + offset[0]).toInt()
                val bottom = ceil(rect.bottom + offset[1]).toInt()
                val measurement =
                    JSONObject().put("role", role).put("text", text)
                        .put("left", left).put("top", top).put("right", right).put("bottom", bottom)
                measurements.put(measurement)
                if (left < 0 || top < 0 || right > bitmap.width || bottom > bitmap.height ||
                    right <= left || bottom <= top
                ) {
                    errors += "$role Text bounds must be nonempty and entirely within the screenshot"
                    return@forEachIndexed
                }
                val histogram = mutableMapOf<Int, Int>()
                for (y in top until bottom) {
                    for (x in left until right) {
                        val rgb = bitmap.getPixel(x, y) or (0xFF shl 24)
                        histogram[rgb] = (histogram[rgb] ?: 0) + 1
                    }
                }
                val background = checkNotNull(histogram.maxByOrNull { it.value })
                val total = (right - left) * (bottom - top)
                val backgroundFraction = background.value.toDouble() / total
                val qualifying =
                    histogram.entries.sumOf { (rgb, count) ->
                        if (ColorUtils.calculateContrast(rgb, background.key) >= 4.5) count else 0
                    }
                val fraction = qualifying.toDouble() / total
                val palette = JSONArray()
                histogram.entries.sortedByDescending { it.value }.take(8).forEach { (rgb, count) ->
                    palette.put(
                        JSONObject().put("rgb", String.format("#%08X", rgb)).put("pixels", count)
                            .put("contrast", ColorUtils.calculateContrast(rgb, background.key)),
                    )
                }
                val passed = backgroundFraction >= 0.60 && qualifying >= 64 && fraction >= 0.02
                measurement.put("dominantBackground", String.format("#%08X", background.key))
                    .put("backgroundFraction", backgroundFraction).put("samplePixels", total)
                    .put("contrastPixels", qualifying).put("contrastPixelFraction", fraction)
                    .put("mostCommonColors", palette).put("passed", passed)
                if (!passed) {
                    errors += "$role: background fraction=$backgroundFraction, " +
                        ">=4.5:1 pixels=$qualifying/$total ($fraction)"
                }
            }
        } catch (failure: Throwable) {
            errors += "Contrast measurement failed: $failure"
        }
        val preferences = SharedPreferencesHelper.getSharedPreferences(compose.activity.applicationContext)
        val actualMode = preferences.getString(ThemePreferences.THEME_MODE_KEY, "system")
        val requested = InstrumentationRegistry.getArguments().getString("qaThemeMode")
        if (requested != null && requested != actualMode) {
            errors += "Select $requested through Settings before this capture; actual theme is $actualMode"
        }
        return report.put("passed", errors.isEmpty()).put("errors", JSONArray(errors))
            .put("themePreference", actualMode).put("requestedThemeMode", requested ?: JSONObject.NULL)
            .put("wallpaperColorsPreference", preferences.getBoolean(ThemePreferences.DYNAMIC_COLOR_KEY, true))
    }

    private fun capture(
        name: String,
        label: String,
        regions: Map<String, Rect>,
        verifyGuidanceContrast: Boolean = false,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val run = InstrumentationRegistry.getArguments().getString("qaCaptureRun") ?: "default"
        val directory =
            File(
                checkNotNull(instrumentation.targetContext.getExternalFilesDir("qa-launcher-layout")),
                run,
            ).apply {
                mkdirs()
            }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(
                directory,
                "$name.png",
            ).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val json =
                JSONObject().put("evidenceType", label).put("capturedAtEpochMs", System.currentTimeMillis())
                    .put("screenWidth", bitmap.width).put("screenHeight", bitmap.height)
            regions.forEach { (tag, rect) ->
                json.put(
                    tag,
                    JSONObject().put(
                        "left",
                        rect.left,
                    ).put("top", rect.top).put("right", rect.right).put("bottom", rect.bottom),
                )
            }
            val contrast = if (verifyGuidanceContrast) guidanceTextContrastReport(bitmap) else null
            if (contrast != null) json.put("guidanceTextContrast", contrast)
            File(directory, "$name.json").writeText(json.toString(2))
            // Preserve the exact screenshot and report before failing a rendered-pixel assertion.
            if (contrast != null) {
                assertTrue("Guidance text contrast: ${contrast.getJSONArray("errors")}", contrast.getBoolean("passed"))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
