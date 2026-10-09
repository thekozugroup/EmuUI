package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.utils.settings.safeGetBoolean
import com.swordfish.lemuroid.app.utils.settings.safeGetString
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

data class ThemePreferences(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val frame: FrameTheme = FrameTheme.OLED,
) {
    companion object {
        const val THEME_MODE_KEY = "emuui_theme_mode"
        const val FRAME_KEY = "emuui_frame_theme"
        const val DYNAMIC_COLOR_KEY = "emuui_dynamic_color"

        internal fun read(preferences: SharedPreferences): ThemePreferences =
            ThemePreferences(
                mode = ThemeMode.fromPreference(preferences.safeGetString(THEME_MODE_KEY, null)),
                dynamicColor = preferences.safeGetBoolean(DYNAMIC_COLOR_KEY, true),
                frame = FrameTheme.entries.firstOrNull { it.name == preferences.safeGetString(FRAME_KEY, null) } ?: FrameTheme.OLED,
            )
    }
}

internal val LocalThemePreferences = staticCompositionLocalOf { ThemePreferences() }

internal data class ThemeSnapshot(
    val preferences: ThemePreferences,
    val resumeGeneration: Int = 0,
)

/**
 * Use the app's existing Harmony store, whose file observer delivers changes across
 * the launcher and :game processes. Android's regular SharedPreferences cache and
 * MODE_MULTI_PROCESS cannot provide that contract. Retain the listener until disposal.
 * Resume also re-reads settings and invalidates wallpaper-derived color resources.
 */
@Composable
internal fun rememberThemePreferences(): State<ThemeSnapshot> {
    val context = LocalContext.current.applicationContext
    val preferences = remember(context) { SharedPreferencesHelper.getSharedPreferences(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember(preferences) { mutableStateOf(ThemeSnapshot(ThemePreferences.read(preferences))) }
    DisposableEffect(preferences, lifecycle) {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == null || key == ThemePreferences.THEME_MODE_KEY ||
                    key == ThemePreferences.FRAME_KEY || key == ThemePreferences.DYNAMIC_COLOR_KEY
                ) {
                    state.value = state.value.copy(preferences = ThemePreferences.read(preferences))
                }
            }
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    state.value =
                        ThemeSnapshot(
                            preferences = ThemePreferences.read(preferences),
                            resumeGeneration = state.value.resumeGeneration + 1,
                        )
                }
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        // Subscribe before refreshing so an edit during composition cannot be lost.
        state.value = state.value.copy(preferences = ThemePreferences.read(preferences))
        lifecycle.addObserver(observer)
        onDispose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
            lifecycle.removeObserver(observer)
        }
    }
    return state
}
