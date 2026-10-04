package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Real native controls, with non-overlapping 48dp targets and accessible action names. */
@Composable
internal fun LauncherControlWing(
    left: Boolean,
    libraryActive: Boolean,
    canNavigate: Boolean,
    canPlay: Boolean,
    hasGame: Boolean,
    onNavigate: (LauncherDirection) -> Unit,
    onPlay: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onSearch: () -> Unit,
    onCycleFilter: (Boolean) -> Unit,
    onStart: () -> Unit = onPlay,
    startDescription: String? = null,
    searchDescription: String = "X, search library",
    menuDescription: String? = null,
    onSelectSection: (() -> Unit)? = null,
    selectDescription: String? = null,
    playDescription: String? = null,
    backDescription: String = "B, back",
    navigationIsSelection: Boolean = libraryActive,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 268.dp
        val shoulder: @Composable () -> Unit = {
            WingKey(
                label = if (left) "L" else "R",
                description =
                    if (!libraryActive) {
                        if (left) "L, move focus backward" else "R, move focus forward"
                    } else if (left) {
                        "L, previous library filter"
                    } else {
                        "R, next library filter"
                    },
                modifier = Modifier.width(if (compact) 48.dp else 104.dp).height(48.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = { onCycleFilter(!left) },
            )
        }
        val systemKey: @Composable () -> Unit = {
            WingKey(
                label = if (left) "SELECT" else "START",
                description =
                    if (left && selectDescription != null) {
                        selectDescription
                    } else if (!left && startDescription != null) {
                        startDescription
                    } else if (!libraryActive) {
                        if (left) "Select, move focus forward" else "Start, activate focused item"
                    } else if (left) {
                        "Select, next library filter"
                    } else if (hasGame) {
                        "Start, play selected game"
                    } else {
                        "Start, add your games"
                    },
                modifier = Modifier.width(if (compact) 88.dp else 104.dp).height(48.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = left || canPlay,
                onClick = { if (left) onSelectSection?.invoke() ?: onCycleFilter(true) else onStart() },
            )
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (compact) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    shoulder()
                    systemKey()
                }
            } else {
                shoulder()
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(144.dp)) {
                // Four independent 48dp targets leave the center open, like the reference controls.
                if (left) {
                    WingKey(
                        "",
                        if (navigationIsSelection) "Select game above" else "Move focus up",
                        Modifier.align(Alignment.TopCenter),
                        Icons.Filled.KeyboardArrowUp,
                        enabled = canNavigate,
                        onClick = { onNavigate(LauncherDirection.UP) },
                    )
                    WingKey(
                        "",
                        if (navigationIsSelection) "Select game to the left" else "Move focus left",
                        Modifier.align(Alignment.CenterStart),
                        Icons.Filled.KeyboardArrowLeft,
                        enabled = canNavigate,
                        onClick = { onNavigate(LauncherDirection.LEFT) },
                    )
                    WingKey(
                        "",
                        if (navigationIsSelection) "Select game to the right" else "Move focus right",
                        Modifier.align(Alignment.CenterEnd),
                        Icons.Filled.KeyboardArrowRight,
                        enabled = canNavigate,
                        onClick = { onNavigate(LauncherDirection.RIGHT) },
                    )
                    WingKey(
                        "",
                        if (navigationIsSelection) "Select game below" else "Move focus down",
                        Modifier.align(Alignment.BottomCenter),
                        Icons.Filled.KeyboardArrowDown,
                        enabled = canNavigate,
                        onClick = { onNavigate(LauncherDirection.DOWN) },
                    )
                } else {
                    WingKey(
                        "X",
                        searchDescription,
                        Modifier.align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = onSearch,
                    )
                    WingKey(
                        "Y",
                        menuDescription ?: if (hasGame) "Y, selected game options" else "Y, settings",
                        Modifier.align(Alignment.CenterStart),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = onMenu,
                    )
                    WingKey(
                        "A",
                        playDescription ?: if (!libraryActive) {
                            "A, activate focused item"
                        } else if (hasGame) {
                            "A, play selected game"
                        } else {
                            "A, add your games"
                        },
                        Modifier.align(Alignment.CenterEnd),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        enabled = canPlay,
                        onClick = onPlay,
                    )
                    WingKey(
                        "B",
                        backDescription,
                        Modifier.align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        onClick = onBack,
                    )
                }
            }
            if (!compact) {
                Spacer(Modifier.height(6.dp))
                systemKey()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WingKey(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        // Virtual controls must not become their own DPAD_CENTER target in menu mode.
        // TalkBack/Switch Access still see their Button semantics and click actions.
        modifier =
            modifier.size(
                48.dp,
            ).focusProperties { canFocus = false }.semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = contentColor,
            ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(28.dp))
        } else {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontSize = if (label.length > 1) 10.sp else 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
