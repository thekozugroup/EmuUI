package com.swordfish.lemuroid.app.utils.android.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect

/** A null rectangle explicitly blocks dialogs while a retained console route is unplaced. */
data class ConsoleDialogRegion(val windowBounds: FoldRect?)

/** No provider means an ordinary settings surface, such as the TV interface. */
val LocalConsoleDialogRegion = staticCompositionLocalOf<ConsoleDialogRegion?> { null }

/**
 * Console-owned settings content stays within the safe lower-center region. The native modal
 * window retains standard Back/focus behavior; only its bounded surface is drawn. Bounds are
 * physical pixels in the host window, already excluding the hinge, controls and safe insets.
 */
@Composable
fun ConsoleSettingsDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    val region = LocalConsoleDialogRegion.current
    if (region == null) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = title,
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) { text() }
            },
            confirmButton = { confirmButton?.invoke() },
            dismissButton = dismissButton,
        )
        return
    }

    // An unplaced NavHost is still composed. Never create an independent window over its gate.
    val bounds = region.windowBounds ?: return
    if (bounds.width <= 0 || bounds.height <= 0) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Layout(
            modifier =
                Modifier.fillMaxSize().clipToBounds().pointerInput(bounds, onDismissRequest) {
                    // The transparent full-window host is inside the native Dialog window.
                    detectTapGestures { position ->
                        if (!bounds.contains(position.x, position.y)) onDismissRequest()
                    }
                },
            content = {
                Surface(
                    modifier = Modifier.fillMaxSize().clipToBounds().testTag("console_settings_dialog"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(Modifier.fillMaxSize().padding(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            // Always reachable, even when large text makes the body scroll.
                            IconButton(onClick = onDismissRequest, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                            }
                        }
                        Column(
                            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                Box(Modifier.semantics { heading() }) { title() }
                            }
                            Spacer(Modifier.height(12.dp))
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                            if (confirmButton != null || dismissButton != null) {
                                Spacer(Modifier.height(12.dp))
                                // Stack actions so translated labels and large fonts can wrap.
                                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                                    dismissButton?.invoke()
                                    confirmButton?.invoke()
                                }
                            }
                        }
                    }
                }
            },
        ) { measurables, constraints ->
            // Intersect defensively during a resize; never shift a panel across a control wing.
            val left = bounds.left.coerceIn(0, constraints.maxWidth)
            val top = bounds.top.coerceIn(0, constraints.maxHeight)
            val right = bounds.right.coerceIn(left, constraints.maxWidth)
            val bottom = bounds.bottom.coerceIn(top, constraints.maxHeight)
            val child = measurables.single().measure(Constraints.fixed(right - left, bottom - top))
            layout(constraints.maxWidth, constraints.maxHeight) {
                if (right > left && bottom > top) child.place(left, top)
            }
        }
    }
}
