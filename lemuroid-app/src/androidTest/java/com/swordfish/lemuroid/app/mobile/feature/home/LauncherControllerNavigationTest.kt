package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Production launcher composition and native side-button clicks with test-only UIState.
 * Callback recording avoids launching games, writing Room, importing files or granting permissions.
 * Synthetic WindowManager metadata verifies presentation, not a physical foldable or gamepad.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LauncherControllerNavigationTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val state = mutableStateOf(readyState())
    private val selected = mutableStateOf<Int?>(null)
    private val selectionRequest = mutableStateOf(0)
    private val libraryActive = mutableStateOf(true)
    private val actions = mutableListOf<String>()

    @Test
    fun sideDpadScrollsLongLibraryAndAUsesTheVisibleSelection() {
        val games = (1..36).map(::game)
        install(readyState(games))
        val columns =
            compose.onNodeWithTag("launcher_grid").fetchSemanticsNode()
                .config[SemanticsProperties.CollectionInfo].columnCount
        assertTrue("The fixture needs multiple rows", games.size > columns * 3)
        repeat(3) {
            compose.onNodeWithContentDescription("Select game below").performClick()
            val expected = games[(it + 1) * columns]
            waitForGame(expected.id)
            compose.onNodeWithTag("launcher_game_${expected.id}").assertIsDisplayed().assertIsSelected()
        }
        val expected = games[columns * 3]
        clickKey("A")
        assertEquals(listOf("play:${expected.id}"), actions)
        compose.onNodeWithContentDescription("Select game to the right").performClick()
        val next = games[columns * 3 + 1]
        waitForGame(next.id)
        compose.onNodeWithTag("launcher_game_${next.id}").assertIsSelected()
        clickKey("START")
        assertEquals(listOf("play:${expected.id}", "play:${next.id}"), actions)
    }

    @Test
    fun emptyFavoritesCannotLaunchTheHiddenAllGamesSelection() {
        val games = listOf(game(1), game(2))
        install(readyState(games))
        compose.onNodeWithTag("launcher_game_2").performClick().assertIsSelected()
        repeat(2) { clickKey("R") }
        compose.onNodeWithText("Favorites").assertIsSelected()
        compose.onNodeWithText("Keep your favorites close").assertIsDisplayed()
        clickKey("A")
        compose.onNodeWithText("All games").assertIsSelected()
        assertEquals("A confirms See all games; it must never launch a hidden game", emptyList<String>(), actions)
        repeat(2) { clickKey("R") }
        clickKey("B")
        compose.onNodeWithText("All games").assertIsSelected()
        assertEquals("B clears the filter before navigating away", emptyList<String>(), actions)
    }

    @Test
    fun errorEmptyAndLoadingActionsNeverFallThroughToHiddenGame() {
        install(readyState(listOf(game(1))))
        compose.runOnIdle { state.value = readyState(listOf(game(1))).copy(errorMessage = "Original QA error") }
        compose.onNodeWithText("Let's try that again").assertIsDisplayed()
        clickKey("A")
        assertEquals(listOf("retry"), actions)
        compose.runOnIdle { state.value = readyState().copy(showNoGamesCard = true) }
        clickKey("A")
        assertEquals(listOf("retry", "import"), actions)
        compose.runOnIdle { state.value = HomeViewModel.UIState(isLoading = true) }
        key("A").assertIsNotEnabled()
        key("START").assertIsNotEnabled()
        compose.runOnIdle { state.value = readyState().copy(indexInProgress = true) }
        key("A").assertIsNotEnabled()
        assertEquals(listOf("retry", "import"), actions)
    }

    @Test
    fun selectAndDpadReachBottomShortcutsWhileShouldersChangeFilterAndBCancelsFocus() {
        install(readyState(listOf(game(1), game(2).copy(isFavorite = true))))
        repeat(2) { clickKey("R") }
        compose.onNodeWithText("Favorites").assertIsSelected()
        clickKey("SELECT")
        assertReady("launcher_shortcut_0")
        repeat(3) { compose.onNodeWithContentDescription("Move focus right").performClick() }
        assertReady("launcher_shortcut_3")
        clickKey("A")
        assertEquals(listOf("settings"), actions)
        clickKey("B")
        compose.onNodeWithText("Favorites").assertIsSelected()
        clickKey("B")
        compose.onNodeWithText("All games").assertIsSelected()
        assertEquals(listOf("settings"), actions)
    }

    private fun assertReady(tag: String) {
        compose.onNodeWithTag(tag).assertIsDisplayed().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ready to confirm with A"),
        )
    }

    @Test
    fun sameIdExplicitResultRevealsGameButOrdinaryBackKeepsTheEmptyFilter() {
        install(readyState(listOf(game(1), game(2))))
        compose.onNodeWithTag("launcher_game_1").assertIsSelected()
        repeat(2) { clickKey("R") }
        compose.onNodeWithText("Keep your favorites close").assertIsDisplayed()
        // Model a route roundtrip with no explicit result selection. B alone must retain the filter.
        compose.runOnIdle { libraryActive.value = false }
        clickKey("B")
        compose.onNodeWithText("Favorites").assertIsSelected()
        compose.onNodeWithText("Keep your favorites close").assertIsDisplayed()
        assertEquals(listOf("back"), actions)
        // A real Search/Systems click supplies a new token even when its game ID is unchanged.
        compose.runOnIdle { selectionRequest.value += 1 }
        compose.onNodeWithText("All games").assertIsSelected()
        compose.onNodeWithTag("launcher_game_1").assertIsDisplayed().assertIsSelected()
        assertEquals("Revealing a search result must not start its core", listOf("back"), actions)
        compose.runOnIdle { libraryActive.value = true }
        repeat(2) { clickKey("R") }
        compose.runOnIdle { state.value = state.value.copy(showNoMicrophonePermissionCard = true) }
        compose.onNodeWithText("Favorites").assertIsSelected()
        compose.onNodeWithText("Keep your favorites close").assertIsDisplayed()
    }

    @Test
    fun sideNavigationStateSurvivesStrictPostureGateWithoutExposingActions() {
        install(readyState(listOf(game(1), game(2))))
        compose.onNodeWithContentDescription("Select game to the right").performClick()
        compose.onNodeWithTag("launcher_game_2").assertIsSelected()
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        waitForTag("fold_guidance")
        assertTrue(
            "Closed posture must not expose hidden clickable descendants",
            compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().isEmpty(),
        )
        publish()
        waitForTag("launcher_preview")
        compose.onNodeWithTag("launcher_game_2").assertIsDisplayed().assertIsSelected()
        clickKey("A")
        assertEquals(listOf("play:2"), actions)
    }

    private fun install(initial: HomeViewModel.UIState) {
        state.value = initial
        selected.value = initial.allGames.firstOrNull()?.id
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    ConsoleHomeScreen(
                        modifier = Modifier,
                        state = state.value,
                        selectedGameId = selected.value,
                        selectionRequest = selectionRequest.value,
                        libraryActive = libraryActive.value,
                        onGameSelected = { selected.value = it.id },
                        onPlay = { actions += "play:${it.id}" },
                        onGameOptions = { actions += "options:${it.id}" },
                        onImport = { actions += "import" },
                        onRetry = { actions += "retry" },
                        onOpenSettings = { actions += "settings" },
                        onOpenSystems = { actions += "systems" },
                        onOpenSearch = { actions += "search" },
                        onOpenHelp = { actions += "help" },
                        onOpenCoreSelection = { actions += "cores" },
                        onSyncSaves = null,
                        onEnableNotifications = { actions += "notifications" },
                        onEnableMicrophone = { actions += "microphone" },
                        onBack = {
                            actions += "back"
                            libraryActive.value = true
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
        publish()
        waitForTag("launcher_preview")
    }

    private fun key(label: String) = compose.onNode(hasText(label) and hasClickAction())

    private fun clickKey(label: String) {
        key(label).performClick()
        compose.waitForIdle()
    }

    private fun waitForGame(id: Int) = waitForTag("launcher_game_$id")

    private fun waitForTag(tag: String) {
        compose.waitUntil(30_000) {
            runCatching {
                compose.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
    }

    private fun publish() {
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
    }

    private companion object {
        fun readyState(games: List<Game> = emptyList()) =
            HomeViewModel.UIState(allGames = games, isLoading = false, indexInProgress = false)

        fun game(index: Int) =
            Game(
                id = index,
                fileName = "OriginalNavigationFixture$index.nes",
                fileUri = "content://emuui.invalid/navigation/$index",
                title = "Original navigation fixture $index",
                systemId = "nes",
                developer = null,
                coverFrontUrl = null,
                lastIndexedAt = 0,
            )
    }
}
