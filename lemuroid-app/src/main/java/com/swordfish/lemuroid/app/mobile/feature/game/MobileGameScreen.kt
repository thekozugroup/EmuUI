package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.consoleFrameColor
import android.graphics.RectF
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.swordfish.lemuroid.app.mobile.feature.emuui.CornerOverlayGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.DisplayPanelLayout
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldInsets
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.ScreenRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.WindowCutoutSnapshot
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelTouchControls.Companion.MENU_LOADING_ANIMATION_MILLIS
import com.swordfish.lemuroid.app.utils.android.settings.ConsoleDialogRegion
import com.swordfish.lemuroid.app.utils.android.settings.ConsoleSettingsDialog
import com.swordfish.lemuroid.app.utils.android.settings.LocalConsoleDialogRegion
import com.swordfish.lemuroid.lib.controller.ControllerConfig
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.RenderRegion
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
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalConsoleHaptics
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.GameControlHaptics
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

@Composable
fun MobileGameScreen(
    viewModel: BaseGameScreenViewModel,
    displayCutout: WindowCutoutSnapshot = WindowCutoutSnapshot(),
) {
    val posture = rememberFoldPosture()
    val hostLifecycle = LocalLifecycleOwner.current
    val nativeLifecycle = remember(hostLifecycle) { FoldGameLifecycleOwner(hostLifecycle) }
    DisposableEffect(nativeLifecycle) {
        onDispose { nativeLifecycle.dispose() }
    }
    val accessibleControls = rememberSaveable { mutableStateOf(false) }
    val nativeView = remember(viewModel) { mutableStateOf<GLRetroView?>(null) }
    val presentation = remember(viewModel) { NativeGamePresentation() }
    // A new view starts at zero rather than inheriting the previous core's ratio.
    // The native StateFlow is cached and updated on GL; collecting it never blocks UI.
    val coreAspectRatio =
        key(nativeView.value) {
            nativeView.value?.getCoreAspectRatioUpdates()?.collectAsState()?.value ?: 0f
        }
    val density = LocalDensity.current
    val rootPosition = remember { mutableStateOf(IntOffset.Zero) }
    val safeInsets = WindowInsets.safeDrawing
    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize().background(consoleFrameColor()).onGloballyPositioned {
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
        val safeBounds =
            FoldGeometry.safeBounds(
                width,
                height,
                FoldInsets(
                    safeInsets.getLeft(density, LayoutDirection.Ltr),
                    safeInsets.getTop(density),
                    safeInsets.getRight(density, LayoutDirection.Ltr),
                    maxOf(safeInsets.getBottom(density), WindowInsets.mandatorySystemGestures.getBottom(density)),
                ),
            )
        val statusBarTop = (WindowInsets.statusBars.getTop(density) - rootPosition.value.y).coerceAtLeast(0)
        val upperBounds = rawLayout.upper.copy(top = statusBarTop.coerceAtMost(rawLayout.upper.bottom))
        val navigation = WindowInsets.navigationBars
        val upperControls =
            upperBounds.intersection(
                FoldGeometry.safeBounds(
                    width,
                    height,
                    FoldInsets(
                        navigation.getLeft(density, LayoutDirection.Ltr),
                        statusBarTop,
                        navigation.getRight(density, LayoutDirection.Ltr),
                        navigation.getBottom(density),
                    ),
                ),
            )
        val cutoutOcclusions =
            remember(displayCutout, width, height, rootPosition.value) {
                displayCutout.localOcclusions(width, height, rootPosition.value.x, rootPosition.value.y)
            }
        val foldLayout =
            rawLayout.copy(
                // Reserve the visible status bar once. Precise camera bounds protect native
                // pixels and top actions; the lower touchscreen keeps its existing safe bounds.
                upper = upperBounds,
                lower = rawLayout.lower.intersection(safeBounds),
            ).let { layout ->
                val minimumDisplayHeight = with(density) { 96.dp.roundToPx() }
                val minimumControlsHeight = with(density) { 144.dp.roundToPx() }
                if (layout.guidance == null &&
                    (upperControls.height < minimumDisplayHeight || layout.lower.height < minimumControlsHeight)
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
        val unsupportedDualScreen = currentControllerConfig?.touchControllerID == TouchControllerID.NINTENDO_3DS
        val sideWidth = minOf(with(density) { 176.dp.roundToPx() }, foldLayout.lower.width / 3)
        val console = FoldGeometry.lowerConsole(foldLayout.lower, sideWidth)
        val cornerRadius = with(density) { 24.dp.roundToPx() }
        // Fit against the rounded backing itself. A full rectangular inset would
        // throw away usable letterbox space, particularly on the upper display.
        val upperPanel = FoldGeometry.displayPanel(foldLayout.upper, 0, cornerRadius)
        val upperControlsPanel = FoldGeometry.displayPanel(upperControls, 0, cornerRadius)
        val lowerHorizontalInset = minOf(with(density) { 6.dp.roundToPx() }, console.center.width / 2)
        val lowerPanel =
            FoldGeometry.displayPanel(
                console.center.copy(
                    left = console.center.left + lowerHorizontalInset,
                    right = console.center.right - lowerHorizontalInset,
                ),
                0,
                cornerRadius,
            )
        val dualScreen =
            if (isDualScreen) {
                FoldGeometry.balancedDualScreen(upperPanel, lowerPanel, cutoutOcclusions)
            } else {
                null
            }
        val singleScreen =
            if (!isDualScreen) {
                FoldGeometry.fitInsidePanel(upperPanel, coreAspectRatio, occlusions = cutoutOcclusions)
            } else {
                null
            }
        val nativeWindows =
            dualScreen?.let { listOf(it.upperScreen, it.lowerScreen) } ?: listOfNotNull(singleScreen)
        val unavailableScreen =
            if (isDualScreen) dualScreen == null else coreAspectRatio > 0f && singleScreen == null
        val consoleAvailable =
            currentControllerConfig != null && foldLayout.guidance == null &&
                !unsupportedDualScreen && !unavailableScreen
        SideEffect {
            // Let the native view load and report its actual aspect even before a
            // single-screen rectangle is known. Hidden games never accept input.
            viewModel.setConsoleInputEnabled(consoleAvailable && nativeWindows.isNotEmpty())
            nativeLifecycle.setConsoleAvailable(consoleAvailable)
        }
        val renderRegions =
            if (dualScreen != null && width > 0 && height > 0) {
                listOf(
                    RenderRegion(RectF(0f, 0f, 1f, 0.5f), dualScreen.upperScreen.normalized(width, height)),
                    RenderRegion(RectF(0f, 0.5f, 1f, 1f), dualScreen.lowerScreen.normalized(width, height)),
                )
            } else {
                emptyList()
            }
        val viewport =
            when {
                singleScreen != null && width > 0 && height > 0 -> singleScreen.normalized(width, height)
                width > 0 && height > 0 && upperPanel.content.width > 0 && upperPanel.content.height > 0 -> {
                    // Aspect=0 remains masked, but keep the first native frame safely
                    // in the upper panel while its actual-aspect viewport is queued.
                    upperPanel.content.let {
                        RectF(
                            it.left.toFloat() / width,
                            it.top.toFloat() / height,
                            it.right.toFloat() / width,
                            it.bottom.toFloat() / height,
                        )
                    }
                }
                else -> RectF(0f, 0f, 1f, 1f)
            }
        val tiltConfiguration = viewModel.getTiltConfiguration().collectAsState(TiltConfiguration.Disabled)
        val tiltSimulatedStates = viewModel.getSimulatedTiltEvents().collectAsState(InputState())
        val tiltSimulatedControls = remember { derivedStateOf { tiltConfiguration.value.controlIds() } }
        val consoleHaptics = LocalConsoleHaptics.current
        val gameHaptics = remember(consoleHaptics) { GameControlHaptics(consoleHaptics) }
        LaunchedEffect(consoleAvailable) { gameHaptics.reset() }
        val touchGate = remember { FoldTouchGate() }
        LaunchedEffect(foldLayout, nativeWindows, consoleAvailable) {
            viewModel.releaseVirtualControls()
            nativeView.value?.let(touchGate::release)
        }
        DisposableEffect(nativeView.value) {
            val view = nativeView.value
            onDispose { view?.let(touchGate::release) }
        }
        val dialogBounds =
            lowerPanel.bounds.copy(
                left = lowerPanel.bounds.left + rootPosition.value.x,
                top = lowerPanel.bounds.top + rootPosition.value.y,
                right = lowerPanel.bounds.right + rootPosition.value.x,
                bottom = lowerPanel.bounds.bottom + rootPosition.value.y,
            )
        CompositionLocalProvider(
            LocalConsoleDialogRegion provides
                ConsoleDialogRegion(
                    if (consoleAvailable) dialogBounds else null,
                    if (consoleAvailable) {
                        console.leftControls.translated(rootPosition.value.x, rootPosition.value.y)
                    } else {
                        null
                    },
                    if (consoleAvailable) {
                        console.rightControls.translated(rootPosition.value.x, rootPosition.value.y)
                    } else {
                        null
                    },
                ),
        ) {
            PadKit(
                modifier = Modifier.fillMaxSize(),
                onInputEvents = { events ->
                    viewModel.handleVirtualInputEvent(events)
                    if (consoleAvailable) {
                        // Tilt simulates controls without a touch; it should remain silent.
                        val touchEvents = events.filterNot { event ->
                            when (event) {
                                is gg.padkit.inputevents.InputEvent.DiscreteDirection -> gg.padkit.ids.Id.DiscreteDirection(event.id) in tiltSimulatedControls.value
                                is gg.padkit.inputevents.InputEvent.ContinuousDirection -> gg.padkit.ids.Id.ContinuousDirection(event.id) in tiltSimulatedControls.value
                                is gg.padkit.inputevents.InputEvent.Button -> false
                            }
                        }
                        gameHaptics.onInputEvents(touchEvents)
                    } else gameHaptics.reset()
                },
                // One feedback path: platform View feedback honors Android's setting.
                hapticFeedbackType = HapticFeedbackType.NONE,
                simulatedState = tiltSimulatedStates,
                simulatedControlIds = tiltSimulatedControls,
            ) {
                val context = LocalContext.current
                // This is intentionally a single stable AndroidView call site. A posture or
                // orientation change only updates its viewport, never creates another core.
                AndroidView(
                    modifier = Modifier.fillMaxSize().testTag("emuui_game_surface"),
                    factory = {
                        // Configure the stacked source before native create/load so the
                        // first DS frame is gapless, regardless of saved layout options.
                        viewModel.retroGameView.setConsoleScreenGap(0)
                        viewModel.createRetroView(context, nativeLifecycle).also { nativeView.value = it }
                    },
                    update = { view ->
                        nativeView.value = view
                        touchGate.update(
                            view,
                            if (consoleAvailable && viewModel.retroGameView.acceptsTouchInput) {
                                dualScreen?.lowerScreen ?: singleScreen
                            } else {
                                null
                            },
                        )
                        view.setOnTouchListener(touchGate)
                        presentation.update(view, renderRegions, viewport)
                    },
                )
                // Keep the surface covered until its real geometry is known, including
                // initial aspect=0 and unavailable postures. No guessed-ratio flash.
                GameDisplayPanels(upperPanel, lowerPanel, if (consoleAvailable) nativeWindows else emptyList())
                if (consoleAvailable) {
                    ConsoleRegion(upperPanel.bounds, "emuui_game_upper_panel") { }
                    ConsoleRegion(lowerPanel.bounds, "emuui_game_lower_panel") { }
                    (dualScreen?.upperScreen ?: singleScreen)?.let { screen ->
                        ConsoleRegion(screen.rounded(), "emuui_game_upper_screen") { }
                    }
                    dualScreen?.let { screens ->
                        ConsoleRegion(screens.lowerScreen.rounded(), "emuui_game_lower_screen") { }
                    }
                }
                val settings = touchControllerSettings
                val config = currentControllerConfig
                if (consoleAvailable && settings != null && config != null) {
                    CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme(darkSurface = consoleFrameColor().luminance() < .5f)) {
                        val pads = config.getTouchControllerConfig()
                        // Clip and constrain complete radial pads to the lower pane. User
                        // scale/margins remain saved but cannot push controls into the hinge.
                        run {
                            ConsoleRegion(console.leftControls, "emuui_game_left_controls") {
                                PadContainer(Modifier.fillMaxSize())
                                if (accessibleControls.value && supportsAccessibleConsole(config.touchControllerID)) {
                                    key(
                                        foldLayout,
                                    ) { AccessibleConsoleControls(true, config.touchControllerID, viewModel) }
                                } else {
                                    pads.leftComposable(this@PadKit, Modifier.fillMaxSize(), settings)
                                }
                            }
                            ConsoleRegion(console.rightControls, "emuui_game_right_controls") {
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
                        ConsoleRegion(upperControlsPanel.content, "emuui_game_upper_controls") {
                            GameTopCornerControls(
                                Modifier.fillMaxSize(),
                                cutoutOcclusions.map {
                                    it.translated(
                                        -upperControlsPanel.content.left,
                                        -upperControlsPanel.content.top,
                                    )
                                },
                                supportsAccessibleConsole(config.touchControllerID),
                                accessibleControls.value,
                                onMenu = viewModel::openGameMenu,
                                onToggle = {
                                    viewModel.releaseVirtualControls()
                                    accessibleControls.value = !accessibleControls.value
                                },
                            )
                            GameScreenRunningCentralMenu(
                                Modifier.align(Alignment.Center),
                                viewModel,
                                settings,
                                config,
                            )
                        }
                    }
                }
                ConsoleRegion(rawLayout.hinge, "emuui_game_hinge") {
                    Box(Modifier.fillMaxSize().background(consoleFrameColor()))
                }
                if (foldLayout.guidance != null) {
                    FoldGameGuidance(foldLayout.guidance, viewModel::requestFinish, "Return to library")
                } else if (unsupportedDualScreen) {
                    Column(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            "3DS split-screen layout is not supported yet",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "3DS screen extraction and input mapping are not enabled in this preview.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::requestFinish) { Text("Return to library") }
                    }
                } else if (unavailableScreen) {
                    ConsoleRegion(foldLayout.lower) {
                        Column(
                            Modifier.align(
                                Alignment.Center,
                            ).background(MaterialTheme.colorScheme.surface).padding(12.dp),
                        ) {
                            Text("Open a larger unobstructed window", color = MaterialTheme.colorScheme.onSurface)
                            TextButton(onClick = viewModel::openGameMenu) { Text("Game menu") }
                        }
                    }
                }
            }
        }
        if (viewModel.loadingState.collectAsState(true).value && foldLayout.guidance == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }
}

/** Deduplicate recompositions and queue viewport work without waiting on the UI thread. */
private class NativeGamePresentation {
    private var appliedView: GLRetroView? = null
    private var appliedRegions: List<RenderRegion>? = null
    private var appliedViewport: RectF? = null

    fun update(
        view: GLRetroView,
        regions: List<RenderRegion>,
        viewport: RectF,
    ) {
        val changedView = appliedView !== view
        if (changedView || appliedRegions != regions) {
            view.setRenderRegions(regions)
            appliedRegions = regions
        }
        if (changedView || appliedViewport != viewport) {
            // GLRetroView's legacy property waits when called off GL. Setting it
            // inside its queue also preserves core rotation for single-screen games.
            val snapshot = RectF(viewport)
            view.queueEvent { view.viewport = snapshot }
            appliedViewport = snapshot
        }
        appliedView = view
    }
}

private fun ScreenRect.normalized(
    width: Int,
    height: Int,
) = RectF(left / width, top / height, right / width, bottom / height)

/** Camera-aware menu actions retain their touch targets and one shared top edge. */
@Composable
private fun GameTopCornerControls(
    modifier: Modifier,
    cutouts: List<FoldRect>,
    hasToggle: Boolean,
    accessible: Boolean,
    onMenu: () -> Unit,
    onToggle: () -> Unit,
) {
    val menu: @Composable (Boolean, Boolean) -> Unit = { compact, measuring ->
        TextButton(
            onClick = onMenu,
            modifier =
                Modifier.sizeIn(minWidth = if (compact) 48.dp else 64.dp, minHeight = 48.dp)
                    .then(if (measuring) Modifier.clearAndSetSemantics { } else Modifier.testTag("emuui_game_menu"))
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
        ) {
            if (compact) {
                Icon(
                    Icons.Default.Menu,
                    if (measuring) null else "Game menu",
                )
            } else {
                Text("Menu", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    SubcomposeLayout(modifier) { constraints ->
        val full = subcompose("menu-measure") { menu(false, true) }.single()
        val toggle =
            if (hasToggle) {
                subcompose("toggle") {
                    TextButton(
                        onClick = onToggle,
                        modifier =
                            Modifier.sizeIn(minHeight = 48.dp).testTag("emuui_game_controls_toggle")
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp)),
                    ) {
                        Text(
                            if (accessible) "Radial controls" else "Accessible controls",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }.single()
            } else {
                null
            }
        val widthLimit = constraints.maxWidth / 2
        val height =
            maxOf(48.dp.roundToPx(), full.maxIntrinsicHeight(widthLimit), toggle?.maxIntrinsicHeight(widthLimit) ?: 0)
        val placement =
            CornerOverlayGeometry.resolve(
                FoldRect(0, 0, constraints.maxWidth, constraints.maxHeight),
                full.maxIntrinsicWidth(height),
                48.dp.roundToPx(),
                toggle?.maxIntrinsicWidth(height)?.coerceAtMost(widthLimit) ?: 0,
                height,
                0,
                8.dp.roundToPx(),
                cutouts,
            )
        val chosen = subcompose("menu") { menu(placement?.compactBrand == true, false) }.single()
        val measureConstraints = Constraints(maxWidth = widthLimit, minHeight = height, maxHeight = height)
        val menuPlaceable = chosen.measure(measureConstraints)
        val togglePlaceable = toggle?.measure(measureConstraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (placement != null) {
                menuPlaceable.place(placement.left.left, placement.left.top)
                togglePlaceable?.place(placement.right.left, placement.right.top)
            }
        }
    }
}

@Composable
private fun GameDisplayPanels(
    upper: DisplayPanelLayout,
    lower: DisplayPanelLayout,
    nativeWindows: List<ScreenRect>,
) {
    val shell = consoleFrameColor()
    val upperBacking = shell
    val lowerBacking = shell
    Canvas(Modifier.fillMaxSize()) {
        val nativePixels =
            Path().apply {
                nativeWindows.forEach { window ->
                    // Protect complete edge pixels from float-to-integer viewport rounding.
                    addRect(
                        Rect(
                            floor(window.left),
                            floor(window.top),
                            ceil(window.right),
                            ceil(window.bottom),
                        ),
                    )
                }
            }
        clipPath(nativePixels, clipOp = ClipOp.Difference) {
            drawRect(shell)
            for ((panel, backing) in listOf(upper to upperBacking, lower to lowerBacking)) {
                drawRoundRect(
                    color = backing,
                    topLeft = Offset(panel.bounds.left.toFloat(), panel.bounds.top.toFloat()),
                    size = Size(panel.bounds.width.toFloat(), panel.bounds.height.toFloat()),
                    cornerRadius = CornerRadius(panel.cornerRadius.toFloat()),
                )
            }
        }
    }
}

private fun ScreenRect.rounded() =
    FoldRect(
        left.roundToInt(),
        top.roundToInt(),
        right.roundToInt(),
        bottom.roundToInt(),
    )

@Composable
internal fun ConsoleRegion(
    rect: FoldRect,
    testTag: String = "",
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    Box(
        modifier =
            Modifier.offset { IntOffset(rect.left, rect.top) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clipToBounds().testTag(testTag),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
internal fun FoldGameGuidance(
    guidance: FoldGuidance,
    onMenu: () -> Unit,
    actionLabel: String = "Back",
) {
    Column(
        Modifier.fillMaxSize().background(
            MaterialTheme.colorScheme.background,
        ).padding(24.dp).testTag("emuui_game_posture_guidance"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.RotateLeft, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            when (guidance) {
                FoldGuidance.OPEN_FOLDABLE -> "Open your foldable"
                FoldGuidance.ROTATE_LANDSCAPE -> "Rotate to landscape"
                FoldGuidance.ROTATE_HINGE -> "Rotate until the crease runs left to right"
                FoldGuidance.WINDOW_TOO_SMALL -> "Open a larger window"
            },
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "Use the inner display in full-screen landscape, with the crease running left to right. " +
                "Open flat or half-folded. Your game stays paused here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onMenu, modifier = Modifier.sizeIn(minHeight = 48.dp)) { Text(actionLabel) }
    }
}

@Composable
private fun PadContainer(modifier: Modifier = Modifier) {
    val theme = LocalLemuroidPadTheme.current
    GlassSurface(
        modifier = modifier,
        cornerRadius = theme.level0CornerRadius,
        fillColor = consoleFrameColor(),
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

    ConsoleSettingsDialog(
        onDismissRequest = { viewModel.showEditControls(false) },
        title = { Text("Touch controls") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(8.dp),
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
            }
        },
        dismissButton = {
            TextButton(
                onClick = { viewModel.resetTouchControls() },
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text(text = stringResource(R.string.touch_customize_button_reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.showEditControls(false) },
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Text(text = stringResource(R.string.touch_customize_button_done))
            }
        },
    )
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
