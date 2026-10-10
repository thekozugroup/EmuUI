package com.swordfish.lemuroid.app.utils.android.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.alorma.compose.settings.storage.base.SettingValueState
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalConsoleHaptics
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberConsoleControlInteractions
import kotlin.math.roundToInt

@Composable
fun LemuroidSettingsPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
fun LemuroidSettingsSwitch(
    enabled: Boolean = true,
    state: SettingValueState<Boolean>,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    val haptics = LocalConsoleHaptics.current
    ListItem(
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .toggleable(value = state.value, enabled = enabled, role = Role.Switch) {
                haptics.selection()
                state.value = it
                onCheckedChange(it)
            },
        headlineContent = { ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() } },
        supportingContent = subtitle,
        leadingContent = icon,
        trailingContent = { Switch(checked = state.value, onCheckedChange = null, enabled = enabled) },
        colors = lemuroidSettingsColor(enabled),
    )
}

@Composable
fun LemuroidSettingsMenuLink(
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        interactionSource = rememberConsoleControlInteractions(enabled),
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            headlineContent = { ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() } },
            supportingContent = subtitle,
            leadingContent = icon,
            trailingContent = action ?: { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            colors = lemuroidSettingsColor(enabled),
        )
    }
}

@Composable
fun LemuroidSettingsGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = LemuroidCardSettingsGroup(modifier, title, content)

@Composable
fun LemuroidCardSettingsGroup(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Box(Modifier.padding(horizontal = 8.dp).semantics { heading() }) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) { title() }
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)),
        ) { Column(Modifier.fillMaxWidth(), content = content) }
    }
}

@Composable
fun LemuroidSettingsSlider(
    modifier: Modifier = Modifier,
    state: SettingValueState<Int>,
    steps: Int,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    title: @Composable () -> Unit,
    subtitle: @Composable () -> Unit = {},
) {
    val haptics = LocalConsoleHaptics.current
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) { subtitle() }
        Slider(
            value = state.value.toFloat(),
            onValueChange = {
                val next = it.roundToInt()
                if (next != state.value) {
                    haptics.selection()
                    state.value = next
                }
            },
            enabled = enabled,
            steps = steps,
            valueRange = valueRange,
        )
    }
}

@Composable
private fun lemuroidSettingsColor(enabled: Boolean): ListItemColors = ListItemDefaults.colors(
    containerColor = Color.Transparent,
    headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f),
    leadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f),
    trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f),
    supportingColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f),
)
