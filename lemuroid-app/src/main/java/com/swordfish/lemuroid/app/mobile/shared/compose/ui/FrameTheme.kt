package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color

enum class FrameTheme(val label: String) { OLED("OLED black"), GRAPHITE("Graphite"), THEME("Match app theme") }

@Composable
internal fun consoleFrameColor(): Color = when (LocalThemePreferences.current.frame) {
    FrameTheme.OLED -> Color.Black
    FrameTheme.GRAPHITE -> Color(0xFF20242A)
    FrameTheme.THEME -> MaterialTheme.colorScheme.surface
}
