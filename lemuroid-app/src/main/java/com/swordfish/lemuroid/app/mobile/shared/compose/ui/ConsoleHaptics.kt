package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.SharedPreferences
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.app.utils.settings.safeGetString
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import gg.padkit.inputevents.InputEvent
import kotlin.math.abs

/** View feedback retains Android's system setting and hardware fallback behavior. */
class ConsoleHaptics(
    val mode: HapticFeedbackMode,
    private val emit: (Int) -> Unit,
) {
    fun press() {
        if (mode != HapticFeedbackMode.NONE) emit(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    fun release() {
        if (mode == HapticFeedbackMode.PRESS_RELEASE) {
            emit(if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.VIRTUAL_KEY_RELEASE else HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun selection() {
        if (mode != HapticFeedbackMode.NONE) {
            emit(if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.CLOCK_TICK else HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }
}

val LocalConsoleHaptics = staticCompositionLocalOf { ConsoleHaptics(HapticFeedbackMode.NONE) {} }

/** Emit one tick for a batch of real input edges, never once per held gameplay frame. */
internal class GameControlHaptics(private val haptics: ConsoleHaptics) {
    private val heldKeys = mutableSetOf<Int>()
    private val directions = mutableMapOf<Int, Offset>()

    fun reset() {
        heldKeys.clear()
        directions.clear()
    }

    fun onInputEvents(events: List<InputEvent>) {
        var pressed = false
        var released = false
        events.forEach { event ->
            when (event) {
                is InputEvent.Button -> {
                    if (event.pressed) pressed = heldKeys.add(event.id) || pressed
                    else released = heldKeys.remove(event.id) || released
                }
                is InputEvent.DiscreteDirection, is InputEvent.ContinuousDirection -> {
                    val (id, raw) = when (event) {
                        is InputEvent.DiscreteDirection -> event.id to event.direction
                        is InputEvent.ContinuousDirection -> event.id to event.direction
                        else -> error("Not a direction")
                    }
                    // Analog motion only ticks when crossing a direction sector, not on every move.
                    fun sector(value: Float): Float = if (abs(value) < .5f) 0f else if (value > 0f) 1f else -1f
                    val next = Offset(sector(raw.x), sector(raw.y))
                    val previous = directions[id] ?: Offset.Zero
                    if (next != previous) {
                        if (next == Offset.Zero) released = true else pressed = true
                        directions[id] = next
                    }
                }
            }
        }
        if (pressed) haptics.press() else if (released) haptics.release()
    }
}

@Composable
internal fun rememberConsoleHaptics(): ConsoleHaptics {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences = remember(context) { SharedPreferencesHelper.getSharedPreferences(context) }
    val key = context.getString(R.string.pref_key_haptic_feedback_mode)
    fun readMode() = HapticFeedbackMode.parse(preferences.safeGetString(key, "press") ?: "press")
    var mode by remember(preferences) { mutableStateOf(readMode()) }
    DisposableEffect(preferences, lifecycle, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changed ->
            if (changed == null || changed == key) mode = readMode()
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) mode = readMode()
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        lifecycle.addObserver(observer)
        mode = readMode()
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            lifecycle.removeObserver(observer)
        }
    }
    return remember(view, mode) { ConsoleHaptics(mode) { view.performHapticFeedback(it) } }
}

/** Feedback comes from real press/release events; cancellation never produces a release tick. */
@Composable
fun rememberConsoleControlInteractions(enabled: Boolean = true): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    val haptics = LocalConsoleHaptics.current
    LaunchedEffect(source, haptics, enabled) {
        source.interactions.collect { interaction ->
            if (enabled) {
                when (interaction) {
                    is PressInteraction.Press -> haptics.press()
                    is PressInteraction.Release -> haptics.release()
                    else -> Unit
                }
            }
        }
    }
    return source
}
