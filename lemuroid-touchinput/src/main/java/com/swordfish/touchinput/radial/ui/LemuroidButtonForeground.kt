package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme

@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    label: (@Composable BoxWithConstraintsScope.() -> Unit),
    icon: (@Composable BoxWithConstraintsScope.() -> Unit),
    systemKey: Boolean = false,
    prominent: Boolean = false,
) {
    val theme = LocalLemuroidPadTheme.current

    ConsoleControlFace(
        modifier = modifier.fillMaxSize().padding(theme.foregroundPadding),
        pressed = pressed.value,
        systemKey = systemKey,
        prominent = prominent,
        content = {
            icon()
            label()
        },
    )
}

@Composable
fun LemuroidButtonForeground(
    modifier: Modifier = Modifier,
    pressed: State<Boolean>,
    label: String? = null,
    icon: Int? = null,
    iconScale: Float = 0.5f,
    labelScale: Float = 1.0f,
) {
    LemuroidButtonForeground(
        modifier = modifier,
        pressed = pressed,
        label = { LemuroidButtonForegroundLabel(label, labelScale) },
        icon = { LemuroidButtonForegroundIcon(icon, iconScale) },
        systemKey = (label?.length ?: 0) > 1,
        prominent = label == "A",
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundIcon(
    icon: Int?,
    scale: Float,
) {
    if (icon == null) return

    Icon(
        modifier = Modifier.size(maxWidth * scale, maxHeight * scale),
        painter = painterResource(icon),
        contentDescription = "",
        tint = LocalContentColor.current,
    )
}

@Composable
private fun BoxWithConstraintsScope.LemuroidButtonForegroundLabel(
    label: String?,
    scale: Float,
) {
    if (label.isNullOrEmpty()) return
    val fontSize =
        with(LocalDensity.current) {
            minOf(
                ConsoleControlStyle.labelSize(label).value * scale,
                (maxHeight * 0.5f * scale).toSp().value,
                (maxWidth / label.length * scale).toSp().value,
            ).sp
        }
    ConsoleControlLabel(
        label = label,
        modifier = Modifier.wrapContentSize(),
        fontSize = fontSize,
    )
}
