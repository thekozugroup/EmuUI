package com.swordfish.lemuroid.app.mobile.feature.main

import android.view.HapticFeedbackConstants
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ConsoleHaptics
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalConsoleHaptics
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberConsoleControlInteractions
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real pointer input and the production interaction collector, with a recording feedback sink. */
@RunWith(AndroidJUnit4::class)
class ConsoleHapticsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun gameplayButtonEdgesTickOnceAndHeldFramesRemainSilent() {
        val effects = mutableListOf<Int>()
        val haptics = ConsoleHaptics(HapticFeedbackMode.PRESS_RELEASE) { effects += it }
        val generator = com.swordfish.lemuroid.app.mobile.shared.compose.ui.GameControlHaptics(haptics)
        val pressed = listOf(gg.padkit.inputevents.InputEvent.Button(android.view.KeyEvent.KEYCODE_BUTTON_A, true))
        val released = listOf(gg.padkit.inputevents.InputEvent.Button(android.view.KeyEvent.KEYCODE_BUTTON_A, false))
        generator.onInputEvents(pressed)
        repeat(60) { generator.onInputEvents(pressed) }
        generator.onInputEvents(released)
        assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.VIRTUAL_KEY_RELEASE), effects)
        effects.clear()
        val direction = listOf(gg.padkit.inputevents.InputEvent.DiscreteDirection(0, Offset(1f, 0f)))
        generator.onInputEvents(direction)
        repeat(60) { generator.onInputEvents(direction) }
        generator.onInputEvents(listOf(gg.padkit.inputevents.InputEvent.DiscreteDirection(0, Offset.Zero)))
        assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.VIRTUAL_KEY_RELEASE), effects)
        effects.clear()
        val silent = com.swordfish.lemuroid.app.mobile.shared.compose.ui.GameControlHaptics(ConsoleHaptics(HapticFeedbackMode.NONE) { effects += it })
        silent.onInputEvents(pressed)
        silent.onInputEvents(released)
        assertEquals(0, effects.size)
    }

    @Test fun pressReleaseTicksOnceAndCancelledTouchHasNoReleaseTick() {
        val effects = mutableListOf<Int>()
        var clicks = 0
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    CompositionLocalProvider(LocalConsoleHaptics provides ConsoleHaptics(HapticFeedbackMode.PRESS_RELEASE) { effects += it }) {
                        Button(onClick = { clicks++ }, interactionSource = rememberConsoleControlInteractions(), modifier = Modifier.size(120.dp).testTag("control")) { Text("A") }
                    }
                }
            }
        }
        compose.onNodeWithTag("control").performTouchInput { down(center) }
        compose.runOnIdle { assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY), effects) }
        compose.onNodeWithTag("control").performTouchInput { up() }
        compose.runOnIdle {
            assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.VIRTUAL_KEY_RELEASE), effects)
            assertEquals(1, clicks)
            effects.clear()
        }
        compose.onNodeWithTag("control").performTouchInput { down(center); cancel() }
        compose.runOnIdle {
            assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY), effects)
            assertEquals(1, clicks)
        }
    }

    @Test fun offAndDisabledControlsAreSilentAndPressOnlyDoesNotTickOnRelease() {
        val effects = mutableListOf<Int>()
        val mode = mutableStateOf(HapticFeedbackMode.NONE)
        val enabled = mutableStateOf(true)
        var clicks = 0
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    CompositionLocalProvider(LocalConsoleHaptics provides ConsoleHaptics(mode.value) { effects += it }) {
                        Button(onClick = { clicks++ }, enabled = enabled.value, interactionSource = rememberConsoleControlInteractions(enabled.value), modifier = Modifier.size(120.dp).testTag("control")) { Text("A") }
                    }
                }
            }
        }
        compose.onNodeWithTag("control").performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, effects.size); assertEquals(1, clicks); mode.value = HapticFeedbackMode.PRESS }
        compose.onNodeWithTag("control").performTouchInput { click() }
        compose.runOnIdle { assertEquals(listOf(HapticFeedbackConstants.VIRTUAL_KEY), effects); effects.clear(); enabled.value = false }
        compose.onNodeWithTag("control").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, effects.size); assertEquals(2, clicks) }
    }
}
