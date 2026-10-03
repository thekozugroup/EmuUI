package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.ScreenRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.shared.settings.HapticFeedbackMode
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.touchinput.controller.R
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import com.swordfish.touchinput.radial.ui.GlassSurface
import com.swordfish.touchinput.radial.ui.LemuroidButtonPressFeedback
import gg.padkit.PadKit
import gg.padkit.config.HapticFeedbackType
import gg.padkit.inputstate.InputState
import kotlin.math.roundToInt

@Composable
fun MobileGameScreen(viewModel: BaseGameScreenViewModel) {
    val posture = rememberFoldPosture()
    val accessibleControls = rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val rootPosition = remember { mutableStateOf(IntOffset.Zero) }
    val safeInsets = WindowInsets.safeDrawing
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().background(Color.Black).onGloballyPositioned {
                val position = it.positionInWindow()
                rootPosition.value = IntOffset(position.x.roundToInt(), position.y.roundToInt())
            },
    ) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val isLandscape = width > height
        val rawLayout =
            FoldGeometry.resolve(
                width,
                height,
                posture.fold?.relativeTo(rootPosition.value.x, rootPosition.value.y),
                with(density) { 8.dp.roundToPx() },
            )
        val insetLeft = safeInsets.getLeft(density, LayoutDirection.Ltr)
        val insetRight = safeInsets.getRight(density, LayoutDirection.Ltr)
        val foldLayout =
            rawLayout.copy(
                upper =
                    rawLayout.upper.copy(
                        left = insetLeft,
                        right = (width - insetRight).coerceAtLeast(insetLeft),
                        top = safeInsets.getTop(density).coerceAtMost(rawLayout.upper.bottom),
                    ),
                lower =
                    rawLayout.lower.copy(
                        left = insetLeft,
                        right = (width - insetRight).coerceAtLeast(insetLeft),
                        bottom = (height - safeInsets.getBottom(density)).coerceAtLeast(rawLayout.lower.top),
                    ),
            ).let { layout ->
                val minimumDisplayHeight = with(density) { 96.dp.roundToPx() }
                val minimumControlsHeight = with(density) { 144.dp.roundToPx() }
                if (layout.guidance == null &&
                    (layout.upper.height < minimumDisplayHeight || layout.lower.height < minimumControlsHeight)
                ) {
                    layout.copy(guidance = FoldGuidance.WINDOW_TOO_SMALL)
                } else {
                    layout
                }
            }
        LaunchedEffect(isLandscape) {
            viewModel.onScreenOrientationChanged(
                if (isLandscape) {
                    TouchControllerSettingsManager.Orientation.LANDSCAPE
                } else {
                    TouchControllerSettingsManager.Orientation.PORTRAIT
                },
            )
        }
        val currentControllerConfig = viewModel.getTouchControllerConfig().collectAsState(null).value
        val touchControlsVisible = viewModel.isTouchControllerVisible().collectAsState(false).value
        val touchControllerSettings =
            viewModel.getTouchControlsSettings(
                density,
                WindowInsets.displayCutout,
            ).collectAsState(null).value
        val isDualScreen =
            currentControllerConfig?.touchControllerID in
                setOf(
                    TouchControllerID.MELONDS,
                    TouchControllerID.DESMUME,
                )
        val sideWidth = minOf(with(density) { 216.dp.roundToPx() }, foldLayout.lower.width / 3)
        val dualScreen =
            if (isDualScreen) {
                FoldGeometry.dualScreen(
                    foldLayout,
                    sideWidth,
                    maxGap = if (currentControllerConfig?.touchControllerID == TouchControllerID.DESMUME) 100 else 126,
                )
            } else {
                null
            }
        val viewport =
            dualScreen?.viewport ?: foldLayout.upper.let {
                ScreenRect(it.left.toFloat(), it.top.toFloat(), it.right.toFloat(), it.bottom.toFloat())
            }
        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }
        val hapticFeedbackMode = viewModel.getTouchHapticFeedbackMode().collectAsState(HapticFeedbackMode.NONE)
        val padHapticFeedback =
            when (hapticFeedbackMode.value) {
                HapticFeedbackMode.NONE -> HapticFeedbackType.NONE
                HapticFeedbackMode.PRESS -> HapticFeedbackType.PRESS
                HapticFeedbackMode.PRESS_RELEASE -> HapticFeedbackType.PRESS_RELEASE
            }
        val touchGate = remember { FoldTouchGate() }
        LaunchedEffect(foldLayout, dualScreen) {
            viewModel.releaseVirtualControls()
            if (width == 0 || height == 0 || viewport.width <= 0 || viewport.height <= 0) return@LaunchedEffect
            val gameView = viewModel.retroGameView.retroGameViewFlow()
            // A replayed frame is also suitable. Core variables do not exist before load.
            viewModel.retroGameView.waitGLEvent<GLRetroView.GLRetroEvents.FrameRendered>()
            if (isDualScreen) viewModel.retroGameView.setConsoleScreenGap(dualScreen?.nativeGap ?: 0)
            touchGate.release(gameView)
            gameView.viewport =
                RectF(
                    viewport.left / width,
                    viewport.top / height,
                    viewport.right / width,
                    viewport.bottom / height,
                )
        }
        PadKit(
            modifier = Modifier.fillMaxSize(),
            onInputEvents = { viewModel.handleVirtualInputEvent(it) },
            hapticFeedbackType = padHapticFeedback,
            simulatedState = tiltSimulatedStates,
            simulatedControlIds = tiltSimulatedControls,
        ) {
            val context = LocalContext.current
            val lifecycle = LocalLifecycleOwner.current
            // This is intentionally a single stable AndroidView call site. A posture or
            // orientation change only updates its viewport, never creates another core.
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewModel.createRetroView(context, lifecycle) },
                update = { view ->
                    if (isDualScreen || foldLayout.guidance != null || !viewModel.retroGameView.acceptsTouchInput) {
                        touchGate.update(view, if (foldLayout.guidance == null) dualScreen?.lowerScreen else null)
                        view.setOnTouchListener(touchGate)
                    } else {
                        view.setOnTouchListener(null)
                    }
                },
            )
            val settings = touchControllerSettings
            val config = currentControllerConfig
            if (foldLayout.guidance == null && settings != null && config != null) {
                CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                    val pads = config.getTouchControllerConfig()
                    // Clip and constrain complete radial pads to the lower pane. User
                    // scale/margins remain saved but cannot push controls into the hinge.
                    val leftEnd = dualScreen?.lowerScreen?.left?.roundToInt() ?: (width / 2)
                    val rightStart = dualScreen?.lowerScreen?.right?.roundToInt() ?: (width / 2)
                    if (touchControlsVisible || accessibleControls.value) {
                        ConsoleRegion(foldLayout.lower.copy(right = leftEnd)) {
                            PadContainer(Modifier.fillMaxSize())
                            if (accessibleControls.value && supportsAccessibleConsole(config.touchControllerID)) {
                                key(foldLayout) { AccessibleConsoleControls(true, config.touchControllerID, viewModel) }
                            } else {
                                pads.leftComposable(this@PadKit, Modifier.fillMaxSize(), settings)
                            }
                        }
                        ConsoleRegion(foldLayout.lower.copy(left = rightStart)) {
                            PadContainer(Modifier.fillMaxSize())
                            if (accessibleControls.value && supportsAccessibleConsole(config.touchControllerID)) {
                                key(
                                    foldLayout,
                                ) { AccessibleConsoleControls(false, config.touchControllerID, viewModel) }
                            } else {
                                pads.rightComposable(this@PadKit, Modifier.fillMaxSize(), settings)
                            }
                        }
                    }
                    // Explicit 48dp action supplements the existing radial hold-menu
                    // gesture and remains available when a hardware controller is paired.
                    ConsoleRegion(foldLayout.upper) {
                        TextButton(
                            onClick = { viewModel.openGameMenu() },
                            modifier =
                                Modifier.align(Alignment.TopStart).sizeIn(minWidth = 64.dp, minHeight = 48.dp)
                                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(16.dp)),
                        ) { Text("Menu", color = Color.White) }
                        if (supportsAccessibleConsole(config.touchControllerID)) {
                            TextButton(
                                onClick = {
                                    viewModel.releaseVirtualControls()
                                    accessibleControls.value = !accessibleControls.value
                                },
                                modifier =
                                    Modifier.align(Alignment.TopEnd).sizeIn(minHeight = 48.dp)
                                        .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(16.dp)),
                            ) {
                                Text(
                                    if (accessibleControls.value) "Radial controls" else "Accessible controls",
                                    color = Color.White,
                                )
                            }
                        }
                        GameScreenRunningCentralMenu(
                            Modifier.align(Alignment.Center),
                            viewModel,
                            settings,
                            config,
                        )
                    }
                }
            }
            ConsoleRegion(rawLayout.hinge) {
                Box(Modifier.fillMaxSize().background(Color(0xFF080A0E)))
            }
            if (foldLayout.guidance != null) {
                FoldGameGuidance(foldLayout.guidance, viewModel::openGameMenu)
            } else if (isDualScreen && dualScreen == null) {
                ConsoleRegion(foldLayout.lower) {
                    Column(Modifier.align(Alignment.Center).background(Color.Black).padding(12.dp)) {
                        Text("Open the window wider for two screens", color = Color.White)
                        TextButton(onClick = viewModel::openGameMenu) { Text("Game menu") }
                    }
                }
            }
        }
        if (viewModel.loadingState.collectAsState(true).value) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

@Composable
private fun ConsoleRegion(
    rect: FoldRect,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier.offset { IntOffset(rect.left, rect.top) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clipToBounds(),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
private fun FoldGameGuidance(
    guidance: FoldGuidance,
    onMenu: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF080A0E)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.RotateLeft, contentDescription = null, tint = Color(0xFFCBF77B))
        Text(
            when (guidance) {
                FoldGuidance.ROTATE_LANDSCAPE -> "Rotate to landscape"
                FoldGuidance.ROTATE_HINGE -> "Rotate until the crease runs left to right"
                FoldGuidance.WINDOW_TOO_SMALL -> "Open a larger window"
            },
            color = Color.White,
        )
        Text("Display above the crease. Controls below.", color = Color(0xFFACB4C0))
        TextButton(onClick = onMenu, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text("Game menu") }
    }
}

@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier,
        cornerRadius = theme.level0CornerRadius,
        fillColor = theme.level0Fill,
        shadowColor = theme.level0Shadow,
        shadowWidth = theme.level0ShadowWidth,
    )
}

@Composable
private fun GameScreenRunningCentralMenu(
    modifier: Modifier = Modifier,
    viewModel: BaseGameScreenViewModel,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
    controllerConfig: ControllerConfig,
) {
    val menuPressed = viewModel.isMenuPressed().collectAsState(false)
    Box(
        modifier = modifier.wrapContentSize(),
        contentAlignment = Alignment.Center,
    ) {
        LemuroidButtonPressFeedback(
            pressed = menuPressed.value,
            animationDurationMillis = MENU_LOADING_ANIMATION_MILLIS,
            icon = R.drawable.button_menu,
        )
        MenuEditTouchControls(viewModel, controllerConfig, touchControllerSettings)
    }
}

@Composable
private fun MenuEditTouchControls(
    viewModel: BaseGameScreenViewModel,
    controllerConfig: ControllerConfig,
    touchControllerSettings: TouchControllerSettingsManager.Settings,
) {
    val showEditControls = viewModel.isEditControlShown().collectAsState(false)
    if (!showEditControls.value) return

    Dialog(onDismissRequest = { viewModel.showEditControls(false) }) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MenuEditTouchControlRow(Icons.Default.OpenInFull, "Scale", 0f) {
                    Slider(
                        value = touchControllerSettings.scale,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(scale = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Horizontal Margin", 90f) {
                    Slider(
                        value = touchControllerSettings.marginX,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginX = it),
                            )
                        },
                    )
                }
                MenuEditTouchControlRow(Icons.Default.Height, "Vertical Margin", 0f) {
                    Slider(
                        value = touchControllerSettings.marginY,
                        onValueChange = {
                            viewModel.updateTouchControllerSettings(
                                touchControllerSettings.copy(marginY = it),
                            )
                        },
                    )
                }
                if (controllerConfig.allowTouchRotation) {
                    MenuEditTouchControlRow(Icons.Default.RotateLeft, "Rotate", 0f) {
                        Slider(
                            value = touchControllerSettings.rotation,
                            onValueChange = {
                                viewModel.updateTouchControllerSettings(
                                    touchControllerSettings.copy(rotation = it),
                                )
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { viewModel.resetTouchControls() },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_reset))
                    }
                    TextButton(
                        onClick = { viewModel.showEditControls(false) },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text(text = stringResource(R.string.touch_customize_button_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuEditTouchControlRow(
    icon: ImageVector,
    label: String,
    rotation: Float,
    slider: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.rotate(rotation),
            imageVector = icon,
            contentDescription = label,
        )
        slider()
    }
}
