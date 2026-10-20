package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.game.FoldGameGuidance
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production save-slot UI with isolated callbacks; never changes or claims to load a real save. */
@RunWith(AndroidJUnit4::class)
class SaveSlotsAccessibilityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun readingSlotsShowsProgressBeforeAnySlotAction() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme { SaveSlotsContent(GameMenuStatesViewModel.State(), {}, {}) }
            }
        }
        compose.onNodeWithText("Reading save slots…").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertDoesNotExist()
    }

    @Test
    fun failedSlotReadOffersAnExplicitRetry() {
        var retries = 0
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    SaveSlotsContent(
                        GameMenuStatesViewModel.State(isLoading = false, hasError = true),
                        { retries++ },
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Save slots could not be opened").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test
    fun missingLoadSlotStaysDisabledAndExistingSlotKeepsItsIndex() {
        var selectedSlot = -1
        val state =
            GameMenuStatesViewModel.State(
                entries =
                    listOf(
                        GameMenuStatesViewModel.StateEntry("State 1", "Empty slot", false, null),
                        GameMenuStatesViewModel.StateEntry("State 2", "Original QA save", true, null),
                    ),
                isLoading = false,
            )
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme { SaveSlotsContent(state, {}, { selectedSlot = it }) }
            }
        }
        compose.onNodeWithText("State 1").assertIsNotEnabled()
        compose.onNodeWithText("Empty slot").assertIsDisplayed()
        compose.onNodeWithText("State 2").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, selectedSlot) }
    }

    @Test
    fun postureGuidanceReturnRemainsReachableWithLargeTextInAShortWindow() {
        var returned = false
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                        Box(Modifier.requiredSize(320.dp, 160.dp)) {
                            FoldGameGuidance(FoldGuidance.WINDOW_TOO_SMALL, { returned = true }, "Return to library")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Return to library").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(true, returned) }
    }
}
