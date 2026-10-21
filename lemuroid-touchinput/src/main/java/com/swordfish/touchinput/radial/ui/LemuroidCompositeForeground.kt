package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier

@Composable
fun LemuroidCompositeForeground(pressed: State<Boolean>) {
    ConsoleControlGroupSurface(
        modifier = Modifier.fillMaxSize(),
        pressed = pressed.value,
    )
}
