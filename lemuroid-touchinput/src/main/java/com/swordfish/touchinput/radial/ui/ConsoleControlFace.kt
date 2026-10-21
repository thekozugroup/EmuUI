package com.swordfish.touchinput.radial.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shared painting only: callers retain their input, sizing, accessibility and haptics. */
object ConsoleControlStyle {
    fun shape(systemKey: Boolean) = RoundedCornerShape(if (systemKey) 18.dp else 24.dp)

    @Composable
    fun border() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

    @Composable
    fun containerColor(
        pressed: Boolean,
        prominent: Boolean = false,
    ): Color =
        when {
            pressed -> MaterialTheme.colorScheme.surfaceContainerHighest
            prominent -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

    @Composable
    fun contentColor(prominent: Boolean = false): Color =
        if (prominent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    fun labelSize(label: String): TextUnit = if (label.length > 1) 10.sp else 17.sp

    fun scale(pressed: Boolean): Float = if (pressed) .94f else 1f
}

/** The pressed treatment changes paint only, leaving input regions and accessibility bounds intact. */
fun Modifier.consoleControlPressed(pressed: Boolean): Modifier =
    drawWithContent {
        scale(ConsoleControlStyle.scale(pressed)) { this@drawWithContent.drawContent() }
    }

@Composable
fun ConsoleControlFace(
    pressed: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    systemKey: Boolean = false,
    prominent: Boolean = false,
    containerColor: Color = ConsoleControlStyle.containerColor(false, prominent),
    contentColor: Color = ConsoleControlStyle.contentColor(prominent),
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    val shape = ConsoleControlStyle.shape(systemKey)
    val foreground = if (pressed) ConsoleControlStyle.contentColor() else contentColor
    CompositionLocalProvider(LocalContentColor provides if (enabled) foreground else foreground.copy(alpha = .38f)) {
        BoxWithConstraints(
            modifier.consoleControlPressed(pressed)
                .background(if (pressed) ConsoleControlStyle.containerColor(true) else containerColor, shape)
                .border(ConsoleControlStyle.border(), shape),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun ConsoleControlLabel(
    label: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = ConsoleControlStyle.labelSize(label),
) {
    Text(
        label,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = LocalContentColor.current,
        maxLines = 1,
    )
}

/** Keep the analog, D-pad and simultaneous-button regions visible without a separate glass skin. */
@Composable
fun ConsoleControlGroupSurface(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
) {
    Box(
        modifier.background(
            if (pressed) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            CircleShape,
        ).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f), CircleShape),
    )
}
