package com.swordfish.lemuroid.app.utils.android.settings

import android.os.Build
import android.view.ViewTreeObserver
import com.swordfish.lemuroid.app.mobile.feature.emuui.enterImmersiveConsole
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.consoleFrameColor
import androidx.compose.foundation.focusGroup
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.allowDisplayCutouts
import com.swordfish.lemuroid.app.mobile.feature.home.LauncherControlWing
import com.swordfish.lemuroid.app.mobile.feature.home.LauncherDirection
import com.swordfish.lemuroid.app.mobile.feature.home.activateLauncherMenuFocus
import com.swordfish.lemuroid.app.mobile.feature.home.moveLauncherMenuFocus

/** A null rectangle explicitly blocks dialogs while a retained console route is unplaced. */
data class ConsoleDialogRegion(
    val windowBounds: FoldRect?,
    val leftControls: FoldRect? = null,
    val rightControls: FoldRect? = null,
)

/** No provider means an ordinary settings surface, such as the TV interface. */
val LocalConsoleDialogRegion = staticCompositionLocalOf<ConsoleDialogRegion?> { null }

/**
 * Console-owned settings content stays within the safe lower-center region. The native modal
 * window retains standard Back/focus behavior; only its bounded surface is drawn. Bounds are
 * physical pixels in the host window, already excluding the hinge, controls and safe insets.
 */
@OptIn(ExperimentalComposeUiApi::class)
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
        // These controls belong to the modal window, so no press can reach the launcher behind it.
        val focusManager = LocalFocusManager.current
        val inputModeManager = LocalInputModeManager.current
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        DisposableEffect(dialogWindow) {
            // Compose 1.8 does not propagate the host window's cutout policy to full-screen dialogs.
            val previousMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dialogWindow?.attributes?.layoutInDisplayCutoutMode
                } else {
                    null
                }
            dialogWindow?.allowDisplayCutouts()
            dialogWindow?.enterImmersiveConsole()
            val observer = dialogWindow?.decorView?.viewTreeObserver
            val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { focused ->
                if (focused) dialogWindow?.enterImmersiveConsole()
            }
            observer?.addOnWindowFocusChangeListener(focusListener)
            onDispose {
                if (observer?.isAlive == true) observer.removeOnWindowFocusChangeListener(focusListener)
                if (dialogWindow != null && previousMode != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    dialogWindow.attributes = dialogWindow.attributes.apply { layoutInDisplayCutoutMode = previousMode }
                }
            }
        }
        val centerFocus = remember { FocusRequester() }
        var centerHasFocus by remember { mutableStateOf(false) }
        val moveFocus: (FocusDirection) -> Unit = { direction ->
            moveLauncherMenuFocus(inputModeManager, focusManager, centerFocus, { centerHasFocus }, direction)
        }
        val activate = {
            activateLauncherMenuFocus(inputModeManager, centerFocus, { centerHasFocus }, view)
        }
        val wings = listOfNotNull(region.leftControls?.let { true to it }, region.rightControls?.let { false to it })
        Layout(
            modifier =
                Modifier.fillMaxSize().clipToBounds().pointerInput(bounds, onDismissRequest) {
                    // The transparent full-window host is inside the native Dialog window.
                    detectTapGestures { position ->
                        if (!bounds.contains(position.x, position.y) &&
                            wings.none { it.second.contains(position.x, position.y) }
                        ) {
                            onDismissRequest()
                        }
                    }
                },
            content = {
                Surface(
                    modifier =
                        Modifier.fillMaxSize().clipToBounds().testTag("console_settings_dialog")
                            .focusRequester(centerFocus).onFocusChanged { centerHasFocus = it.hasFocus }
                            .focusProperties { exit = { FocusRequester.Cancel } }.focusGroup(),
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
                wings.forEach { (left, _) ->
                    Surface(
                        modifier =
                            Modifier.fillMaxSize().testTag(
                                if (left) "console_dialog_left_controls" else "console_dialog_right_controls",
                            ),
                        color = consoleFrameColor(),
                    ) {
                        LauncherControlWing(
                            left = left,
                            libraryActive = false,
                            canNavigate = true,
                            canPlay = true,
                            hasGame = false,
                            onNavigate = {
                                moveFocus(
                                    when (it) {
                                        LauncherDirection.UP -> FocusDirection.Up
                                        LauncherDirection.DOWN -> FocusDirection.Down
                                        LauncherDirection.LEFT -> FocusDirection.Left
                                        LauncherDirection.RIGHT -> FocusDirection.Right
                                    },
                                )
                            },
                            onPlay = activate,
                            onBack = onDismissRequest,
                            onMenu = { moveFocus(FocusDirection.Previous) },
                            onSearch = { moveFocus(FocusDirection.Next) },
                            onCycleFilter = { moveFocus(if (it) FocusDirection.Next else FocusDirection.Previous) },
                            searchDescription = "X, move focus forward",
                            menuDescription = "Y, move focus backward",
                            backDescription = "B, close dialog",
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            // Every region is in window pixels and independently clipped during a resize.
            val regions = listOf(bounds) + wings.map { it.second }
            val clipped =
                regions.map {
                    val left = it.left.coerceIn(0, constraints.maxWidth)
                    val top = it.top.coerceIn(0, constraints.maxHeight)
                    FoldRect(
                        left,
                        top,
                        it.right.coerceIn(left, constraints.maxWidth),
                        it.bottom.coerceIn(top, constraints.maxHeight),
                    )
                }
            val children =
                measurables.mapIndexed { index, measurable ->
                    measurable.measure(Constraints.fixed(clipped[index].width, clipped[index].height))
                }
            layout(constraints.maxWidth, constraints.maxHeight) {
                children.forEachIndexed { index, child ->
                    val rect = clipped[index]
                    if (rect.width > 0 && rect.height > 0) child.place(rect.left, rect.top)
                }
            }
        }
    }
}
