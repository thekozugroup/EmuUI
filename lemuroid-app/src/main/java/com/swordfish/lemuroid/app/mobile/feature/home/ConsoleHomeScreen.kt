package com.swordfish.lemuroid.app.mobile.feature.home

import com.swordfish.lemuroid.app.mobile.shared.compose.ui.consoleFrameColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.emuui.CornerOverlayGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
import com.swordfish.lemuroid.app.mobile.feature.emuui.LocalWindowCutout
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.utils.android.settings.ConsoleDialogRegion
import com.swordfish.lemuroid.app.utils.android.settings.LocalConsoleDialogRegion
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlin.math.roundToInt

/** The two halves are measured against the physical window crease, never side by side. */
@Composable
internal fun ConsoleHomeScreen(
    modifier: Modifier,
    state: HomeViewModel.UIState,
    selectedGameId: Int?,
    onGameSelected: (Game) -> Unit,
    onPlay: (Game) -> Unit,
    onGameOptions: (Game) -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSystems: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenCoreSelection: () -> Unit,
    onSyncSaves: (() -> Unit)?,
    onEnableNotifications: () -> Unit,
    onEnableMicrophone: () -> Unit,
    selectionRequest: Int = 0,
    libraryActive: Boolean = true,
    onBack: (() -> Unit)? = null,
    centerContent: (@Composable (@Composable () -> Unit) -> Unit)? = null,
) {
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    val filteredGames =
        remember(state.allGames, filter) {
            filterLibrary(state.allGames, filter, Game::isFavorite, Game::lastPlayedAt)
        }
    var handledSelectionRequest by rememberSaveable { mutableStateOf(selectionRequest) }
    val revealSelection =
        revealLibrarySelectionRequest(
            selectionRequest,
            handledSelectionRequest,
            selectedGameId,
            filteredGames.map { it.id },
        )
    // A result click is explicit even when its ID matches the game hidden by an empty filter.
    // Resolve it immediately so the old filter cannot overwrite it with its first game.
    val games = if (revealSelection) state.allGames else filteredGames
    LaunchedEffect(selectionRequest) {
        if (revealSelection) filter = LibraryFilter.ALL
        handledSelectionRequest = selectionRequest
    }
    val selectedGame = resolveLibrarySelection(games, selectedGameId) { it.id }
    val primaryAction =
        launcherPrimaryAction(
            state.errorMessage != null,
            state.isLoading,
            state.indexInProgress,
            state.allGames.size,
            games.size,
            selectedGame?.systemId != "3ds",
        )
    val activateLibrary = {
        when (primaryAction) {
            LauncherPrimaryAction.PLAY -> selectedGame?.let(onPlay)
            LauncherPrimaryAction.IMPORT -> onImport()
            LauncherPrimaryAction.RETRY -> onRetry()
            LauncherPrimaryAction.SHOW_ALL -> filter = LibraryFilter.ALL
            LauncherPrimaryAction.WAIT, LauncherPrimaryAction.UNSUPPORTED -> Unit
        }
        Unit
    }
    LaunchedEffect(selectedGame?.id) {
        if (selectedGame != null && selectedGame.id != selectedGameId) onGameSelected(selectedGame)
    }
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val posture = rememberFoldPosture()
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val creasePadding = with(density) { 4.dp.roundToPx() }
    val hasSetupNotices =
        state.showNoMicrophonePermissionCard || state.showNoNotificationPermissionCard ||
            state.showDesmumeDeprecatedCard

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().background(consoleFrameColor())
                .onGloballyPositioned { rootPosition = it.positionInWindow() },
    ) {
        val geometry =
            FoldGeometry.resolve(
                constraints.maxWidth,
                constraints.maxHeight,
                posture.fold?.relativeTo(rootPosition.x.roundToInt(), rootPosition.y.roundToInt()),
                creasePadding,
            )
        val safeInsets = WindowInsets.safeDrawing
        val lowerWindowBounds =
            FoldRect(
                rootPosition.x.roundToInt() + geometry.lower.left + safeInsets.getLeft(density, LayoutDirection.Ltr),
                rootPosition.y.roundToInt() + geometry.lower.top,
                rootPosition.x.roundToInt() + geometry.lower.right - safeInsets.getRight(density, LayoutDirection.Ltr),
                rootPosition.y.roundToInt() + geometry.lower.bottom - safeInsets.getBottom(density),
            )
        val guidance =
            geometry.guidance ?: if (
                !launcherWindowFits(
                    widthDp = with(density) { lowerWindowBounds.width.toDp().value },
                    previewHeightDp =
                        with(density) {
                            (geometry.upper.height - safeInsets.getTop(density)).toDp().value - 12f
                        },
                    libraryHeightDp =
                        with(density) {
                            lowerWindowBounds.height.toDp().value - LAUNCHER_PANEL_INSET_DP * 2
                        },
                    fontScale = density.fontScale,
                )
            ) {
                FoldGuidance.WINDOW_TOO_SMALL
            } else {
                null
            }
        val dialogWingWidth =
            with(density) {
                launcherControlWingWidthDp(lowerWindowBounds.width.toDp().value).dp.roundToPx()
            }
        val dialogConsole =
            if (guidance == null) {
                FoldGeometry.lowerConsole(
                    lowerWindowBounds,
                    dialogWingWidth,
                    with(density) { 6.dp.roundToPx() },
                )
            } else {
                null
            }
        val dialogBounds =
            dialogConsole?.center?.let { center ->
                val inset = with(density) { LAUNCHER_PANEL_INSET_DP.dp.roundToPx() }
                FoldRect(center.left + inset, center.top + inset, center.right - inset, center.bottom - inset)
            }
        CompositionLocalProvider(
            LocalConsoleDialogRegion provides
                ConsoleDialogRegion(
                    dialogBounds, dialogConsole?.leftControls, dialogConsole?.rightControls,
                ),
        ) {
            // Keep navigation and remembered library state composed through folding changes.
            // Unplaced content has no visible, touchable or accessible descendants.
            Box(
                Modifier.fillMaxSize().then(
                    if (guidance != null) Modifier.clearAndSetSemantics {} else Modifier,
                ),
            ) {
                Layout(
                    content = {
                        PreviewPane(
                            state = state,
                            game = selectedGame,
                            primaryAction = primaryAction,
                            onPrimaryAction = activateLibrary,
                            onOptions = onGameOptions,
                            onBrandClick = {
                                showSetup = false
                                onBack?.invoke()
                            },
                            onOpenSettings = onOpenSettings,
                            onOpenHelp = onOpenHelp,
                            onSyncSaves = onSyncSaves,
                        )
                        ConsoleHinge()
                        LibraryPane(
                            state = state,
                            games = games,
                            selectedGameId = selectedGame?.id,
                            filter = filter,
                            onFilter = { filter = it },
                            onSelect = onGameSelected,
                            onOptions = onGameOptions,
                            onImport = onImport,
                            onRetry = onRetry,
                            onOpenSearch = onOpenSearch,
                            onOpenSystems = onOpenSystems,
                            onPlay = activateLibrary,
                            onMenu = { if (selectedGame != null) onGameOptions(selectedGame) else onOpenSettings() },
                            onBack = {
                                if (showSetup) {
                                    showSetup = false
                                } else if (libraryActive && filter != LibraryFilter.ALL) {
                                    filter = LibraryFilter.ALL
                                } else if (onBack != null) {
                                    onBack()
                                } else {
                                    backDispatcher?.onBackPressed()
                                }
                            },
                            onOpenSettings = onOpenSettings,
                            onOpenHelp = onOpenHelp,
                            primaryAction = primaryAction,
                            libraryActive = libraryActive && !showSetup,
                            centerContent = centerContent,
                            setupContent =
                                if (showSetup) {
                                    {
                                        SetupPane(
                                            state,
                                            onEnableNotifications,
                                            onEnableMicrophone,
                                            onChooseCore = {
                                                showSetup = false
                                                onOpenCoreSelection()
                                            },
                                            onDone = { showSetup = false },
                                        )
                                    }
                                } else {
                                    null
                                },
                            onSetup =
                                if (hasSetupNotices) {
                                    { showSetup = true }
                                } else {
                                    null
                                },
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                ) { measurables, constraints ->
                    val bounds = listOf(geometry.upper, geometry.hinge, geometry.lower)
                    val placeables =
                        measurables.mapIndexed { index, measurable ->
                            measurable.measure(Constraints.fixed(bounds[index].width, bounds[index].height))
                        }
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        if (guidance == null) {
                            placeables.forEachIndexed {
                                    index,
                                    placeable,
                                ->
                                placeable.place(bounds[index].x, bounds[index].y)
                            }
                        }
                    }
                }
            }
            if (guidance != null) OrientationGuidance(guidance)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreviewPane(
    state: HomeViewModel.UIState,
    game: Game?,
    primaryAction: LauncherPrimaryAction,
    onPrimaryAction: () -> Unit,
    onOptions: (Game) -> Unit,
    onBrandClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onSyncSaves: (() -> Unit)?,
) {
    var previewPosition by remember { mutableStateOf(Offset.Zero) }
    val cutout = LocalWindowCutout.current
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.getTop(density)
    BoxWithConstraints(
        Modifier.fillMaxSize().testTag("launcher_preview")
            .onGloballyPositioned { previewPosition = it.positionInWindow() },
    ) {
        val previewConstraints = constraints
        val largeText = LocalDensity.current.fontScale >= 1.3f
        var hasSavedPreview by remember(game?.id, game?.fileUri) { mutableStateOf(false) }
        Surface(
            Modifier.fillMaxSize().testTag("launcher_preview_surface"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                // A full-width artwork canvas is the upper screen, never a split text/cover card.
                SavedGamePreview(game, Modifier.fillMaxSize(), onPreviewAvailable = { hasSavedPreview = it }) {
                    GameArtwork(game, Modifier.fillMaxSize(), preview = true)
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = .08f),
                                MaterialTheme.colorScheme.surface.copy(alpha = .78f),
                            ),
                        ),
                    ),
                )
                PreviewOverlayLayout(
                    modifier = Modifier.fillMaxSize(),
                    titleFraction = if (largeText) .52f else .44f,
                    statusBarTop = (statusBarTop - previewPosition.y.roundToInt()).coerceAtLeast(0),
                    cutouts =
                        cutout.localOcclusions(
                            // Insets can precede the first window measurement. Keep unknown
                            // camera shapes conservative using this already measured preview.
                            previewConstraints.maxWidth,
                            maxOf(cutout.windowHeight, previewConstraints.maxHeight),
                            previewPosition.x.roundToInt(),
                            previewPosition.y.roundToInt(),
                        ),
                    brand = { compact, measuring ->
                        Surface(
                            onClick = onBrandClick,
                            modifier =
                                if (measuring) {
                                    Modifier.clearAndSetSemantics { }
                                } else {
                                    Modifier.testTag("launcher_corner_brand")
                                        .semantics { contentDescription = "EmuUI home" }
                                },
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
                            shape = RoundedCornerShape(18.dp),
                            tonalElevation = 2.dp,
                        ) {
                            Row(
                                Modifier.heightIn(
                                    min = 48.dp,
                                ).padding(horizontal = if (compact) 12.dp else 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(painterResource(R.drawable.emuui_mark), null, Modifier.size(24.dp))
                                if (!compact) {
                                    Spacer(Modifier.width(9.dp))
                                    Text(
                                        "emuui",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 23.sp,
                                        letterSpacing = (-1).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier =
                                            if (measuring) {
                                                Modifier
                                            } else {
                                                Modifier.testTag(
                                                    "launcher_brand_label",
                                                )
                                            },
                                    )
                                }
                                if (!largeText && !compact) {
                                    Spacer(Modifier.width(14.dp))
                                    Text(
                                        game?.systemId?.uppercase() ?: "PLAY SOMETHING GOOD",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Surface(
                        modifier = Modifier.testTag("launcher_corner_tools"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onSyncSaves != null) {
                                IconButton(
                                    onClick = onSyncSaves,
                                ) { Icon(Icons.Outlined.CloudSync, "Sync saved games") }
                            }
                            IconButton(
                                onClick = onOpenHelp,
                            ) { Icon(Icons.Outlined.HelpOutline, "Help and supported formats") }
                            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                        }
                    }
                    Surface(
                        modifier = Modifier.testTag("launcher_corner_count"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Row(
                            Modifier.heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                if (game?.isFavorite == true) Icons.Outlined.Star else Icons.Outlined.GridView,
                                null,
                                Modifier.size(15.dp),
                            )
                            Text(
                                if (state.indexInProgress) "IMPORTING" else "${state.allGames.size} GAMES",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = onPrimaryAction,
                        enabled = primaryAction.isEnabled(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier =
                            Modifier.heightIn(min = 48.dp).testTag("launcher_corner_play")
                                .semantics { contentDescription = primaryAction.label() },
                    ) {
                        Icon(
                            if (game == null) Icons.Outlined.FolderOpen else Icons.Filled.PlayArrow,
                            null,
                            Modifier.size(20.dp),
                        )
                        if (!largeText) {
                            Spacer(Modifier.width(6.dp))
                            Text(primaryAction.label())
                        }
                    }
                    // Sits against the divider, wholly above the physical hinge exclusion zone.
                    Surface(
                        modifier =
                            Modifier.testTag("launcher_title_plaque"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape =
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = 7.dp,
                                bottomEnd = 7.dp,
                            ),
                        shadowElevation = 3.dp,
                    ) {
                        Column(
                            Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                game?.title ?: "Your next little escape",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = if (largeText) 2 else 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(
                                if (hasSavedPreview) {
                                    "SAVED GAMEPLAY · ${game?.systemId?.uppercase().orEmpty()}"
                                } else {
                                    game?.developer?.takeIf { it.isNotBlank() }
                                        ?: game?.systemId?.uppercase()
                                        ?: "YOUR COLLECTION · YOUR CONSOLE"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(
                        Modifier.fillMaxSize().testTag("launcher_preview_size_guidance")
                            .verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("A little more room to play", style = MaterialTheme.typography.titleLarge)
                        Text("Expand this window to see the full preview and controls.")
                    }
                }
                if (game?.systemId == "3ds") {
                    Surface(
                        Modifier.align(Alignment.Center).fillMaxWidth(.72f),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .97f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            "Nintendo 3DS needs a different screen layout and is not supported in this console. " +
                                "Choose another system to play.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState()),
                        )
                    }
                }
                if (game != null) {
                    IconButton(
                        onClick = { onOptions(game) },
                        modifier =
                            Modifier.align(Alignment.CenterEnd)
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                                .padding(10.dp).background(
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .9f),
                                    CircleShape,
                                ),
                    ) {
                        Icon(Icons.Outlined.MoreHoriz, "Options for ${game.title}")
                    }
                }
            }
        }
    }
}

/** One status-bar inset, one horizontal margin, and precise camera avoidance for the top row. */
@Composable
private fun PreviewOverlayLayout(
    modifier: Modifier,
    titleFraction: Float,
    statusBarTop: Int,
    cutouts: List<FoldRect>,
    brand: @Composable (Boolean, Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout(modifier = modifier) { constraints ->
        val inset = 12.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val cornerWidth = ((constraints.maxWidth - inset * 2 - gap) / 2).coerceAtLeast(0)
        val fullBrand = subcompose("brand_measure") { brand(false, true) }.single()
        val measurables = subcompose("controls", content)
        val cornerHeight =
            maxOf(
                48.dp.roundToPx(),
                fullBrand.maxIntrinsicHeight(cornerWidth),
                measurables.take(3).maxOf { it.maxIntrinsicHeight(cornerWidth) },
            )
        val cornerConstraints = Constraints(maxWidth = cornerWidth, minHeight = cornerHeight, maxHeight = cornerHeight)
        val fullBrandWidth = fullBrand.maxIntrinsicWidth(cornerHeight).coerceAtMost(cornerWidth)
        val toolsWidth = measurables[0].maxIntrinsicWidth(cornerHeight).coerceAtMost(cornerWidth)
        val placement =
            CornerOverlayGeometry.resolve(
                FoldRect(0, statusBarTop, constraints.maxWidth, constraints.maxHeight),
                fullBrandWidth,
                48.dp.roundToPx(),
                toolsWidth,
                cornerHeight,
                inset,
                gap,
                cutouts,
            )
        val brandMeasurable = subcompose("brand") { brand(placement?.compactBrand == true, false) }.single()
        val brandPlaceable = brandMeasurable.measure(cornerConstraints)
        val corners = measurables.take(3).map { it.measure(cornerConstraints) }
        val titleWidth =
            launcherPreviewTitleWidth(
                constraints.maxWidth,
                titleFraction,
                corners[1].width,
                corners[2].width,
                inset,
                gap,
            )
        val titleHeight = measurables[3].maxIntrinsicHeight(titleWidth)
        val title = measurables[3].measure(Constraints.fixed(titleWidth, titleHeight))
        val fits =
            placement != null && titleWidth > 0 &&
                launcherPreviewFits(constraints.maxHeight - placement.left.top, cornerHeight, titleHeight, inset, gap)
        val guidance =
            if (!fits) {
                measurables[4].measure(
                    Constraints.fixed(constraints.maxWidth, constraints.maxHeight),
                )
            } else {
                null
            }
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (fits && placement != null) {
                brandPlaceable.place(placement.left.left, placement.left.top)
                corners[0].place(placement.right.left, placement.right.top)
                corners[1].placeRelative(inset, constraints.maxHeight - inset - cornerHeight)
                corners[2].placeRelative(
                    constraints.maxWidth - inset - corners[2].width,
                    constraints.maxHeight - inset - cornerHeight,
                )
                title.placeRelative((constraints.maxWidth - titleWidth) / 2, constraints.maxHeight - titleHeight)
            } else {
                guidance?.placeRelative(0, 0)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LibraryPane(
    state: HomeViewModel.UIState,
    games: List<Game>,
    selectedGameId: Int?,
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    onSelect: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSystems: () -> Unit,
    onSetup: (() -> Unit)?,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    primaryAction: LauncherPrimaryAction,
    libraryActive: Boolean,
    centerContent: (@Composable (@Composable () -> Unit) -> Unit)?,
    setupContent: (@Composable () -> Unit)?,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val inputModeManager = LocalInputModeManager.current
    val centerFocusRequester = remember { FocusRequester() }
    var centerHasFocus by remember { mutableStateOf(false) }
    val view = LocalView.current
    BoxWithConstraints(
        Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        ),
    ) {
        val wingWidth = with(density) { launcherControlWingWidthDp(maxWidth.value).dp.roundToPx() }
        val panes =
            FoldGeometry.lowerConsole(
                FoldRect(0, 0, constraints.maxWidth, constraints.maxHeight),
                wingWidth,
                with(density) { 6.dp.roundToPx() },
            )
        val columns =
            launcherGridColumns(
                with(density) { panes.center.width.toDp().value } - LAUNCHER_PANEL_INSET_DP * 2,
                density.fontScale,
                games.size,
            )
        val selectedIndex = games.indexOfFirst { it.id == selectedGameId }
        var section by rememberSaveable { mutableStateOf(LauncherSection.GAMES) }
        var sectionIndex by rememberSaveable { mutableStateOf(0) }
        val navigation = LauncherNavigationTarget(section, sectionIndex)
        val shortcuts =
            buildList {
                add(LauncherShortcut("Import games folder", Icons.Outlined.Add, !state.indexInProgress, onImport))
                add(LauncherShortcut("Search library", Icons.Outlined.Search, true, onOpenSearch))
                add(LauncherShortcut("Browse by system", Icons.Outlined.GridView, true, onOpenSystems))
                add(LauncherShortcut("Library settings", Icons.Outlined.Tune, true, onOpenSettings))
                add(LauncherShortcut("Console controls and help", Icons.Outlined.HelpOutline, true, onOpenHelp))
                if (onSetup != null) {
                    add(
                        LauncherShortcut("Finish optional setup", Icons.Outlined.MoreHoriz, true, onSetup),
                    )
                }
            }
        val setNavigation: (LauncherNavigationTarget) -> Unit = { target ->
            section = target.section
            sectionIndex = target.index
            if (target.section == LauncherSection.GAMES && state.errorMessage == null && !state.isLoading) {
                games.getOrNull(target.index)?.let(onSelect)
            }
        }
        val changeFilter: (LibraryFilter) -> Unit = { next ->
            section = LauncherSection.GAMES
            sectionIndex = 0
            onFilter(next)
        }
        val selectGame: (Game) -> Unit = { game ->
            section = LauncherSection.GAMES
            onSelect(game)
        }
        LaunchedEffect(shortcuts.size) {
            if (section == LauncherSection.SHORTCUTS) sectionIndex = sectionIndex.coerceIn(shortcuts.indices)
        }
        LaunchedEffect(libraryActive) {
            // A previous route must not retain a focus target behind the visible menu.
            if (!libraryActive) focusManager.clearFocus(force = true)
        }
        val onNavigate: (LauncherDirection) -> Unit = { direction ->
            if (libraryActive) {
                setNavigation(
                    launcherNavigationTarget(
                        navigation,
                        selectedIndex,
                        if (state.errorMessage == null && !state.isLoading) games.size else 0,
                        columns,
                        filter.ordinal,
                        shortcuts.size,
                        direction,
                    ),
                )
            } else {
                moveLauncherMenuFocus(
                    inputModeManager,
                    focusManager,
                    centerFocusRequester,
                    { centerHasFocus },
                    when (direction) {
                        LauncherDirection.UP -> FocusDirection.Up
                        LauncherDirection.DOWN -> FocusDirection.Down
                        LauncherDirection.LEFT -> FocusDirection.Left
                        LauncherDirection.RIGHT -> FocusDirection.Right
                    },
                )
            }
        }
        val activate = {
            if (libraryActive) {
                when (section) {
                    LauncherSection.GAMES -> onPlay()
                    LauncherSection.FILTERS -> changeFilter(LibraryFilter.values()[sectionIndex])
                    LauncherSection.SHORTCUTS ->
                        shortcuts.getOrNull(sectionIndex)?.let {
                            if (it.enabled) it.activate()
                        }
                }
            } else {
                activateLauncherMenuFocus(inputModeManager, centerFocusRequester, { centerHasFocus }, view)
            }
            Unit
        }
        val cycleFilterOrFocus: (Boolean) -> Unit = { forward ->
            if (libraryActive) {
                changeFilter(cycleLibraryFilter(filter, forward))
            } else {
                moveLauncherMenuFocus(
                    inputModeManager,
                    focusManager,
                    centerFocusRequester,
                    { centerHasFocus },
                    if (forward) FocusDirection.Next else FocusDirection.Previous,
                )
            }
        }
        val selectSection = {
            if (libraryActive) {
                setNavigation(launcherNextSection(section, selectedIndex, filter.ordinal))
            } else {
                cycleFilterOrFocus(true)
            }
        }
        val goBack = {
            if (libraryActive && section != LauncherSection.GAMES) {
                section = LauncherSection.GAMES
            } else {
                onBack()
            }
        }
        val actionLabel =
            when {
                !libraryActive -> "activate focused item"
                section == LauncherSection.FILTERS -> "choose ${LibraryFilter.values()[sectionIndex].label}"
                section == LauncherSection.SHORTCUTS -> shortcuts.getOrNull(sectionIndex)?.label ?: "activate shortcut"
                else ->
                    if (primaryAction == LauncherPrimaryAction.PLAY) {
                        "play selected game"
                    } else {
                        primaryAction.label().replaceFirstChar {
                            it.lowercase()
                        }
                    }
            }
        val actionEnabled =
            when {
                !libraryActive -> true
                section == LauncherSection.GAMES -> primaryAction.isEnabled()
                section == LauncherSection.SHORTCUTS -> shortcuts.getOrNull(sectionIndex)?.enabled == true
                else -> true
            }
        val wing: @Composable (Boolean) -> Unit = { left ->
            LauncherControlWing(
                left = left,
                libraryActive = libraryActive,
                canNavigate = true,
                canPlay = actionEnabled,
                hasGame = selectedGameId != null,
                onNavigate = onNavigate,
                onPlay = activate,
                onBack = goBack,
                onMenu = onMenu,
                onSearch = onOpenSearch,
                onCycleFilter = cycleFilterOrFocus,
                onSelectSection = selectSection,
                selectDescription = if (libraryActive) "Select, next library section" else null,
                playDescription = "A, $actionLabel",
                startDescription = "Start, $actionLabel",
                backDescription =
                    if (libraryActive && section != LauncherSection.GAMES) "B, return to games" else "B, back",
                navigationIsSelection = libraryActive && section == LauncherSection.GAMES,
            )
        }
        Layout(content = {
            Box(Modifier.fillMaxSize().testTag("launcher_left_controls")) {
                wing(true)
            }
            Surface(
                Modifier.fillMaxSize().padding(LAUNCHER_PANEL_INSET_DP.dp).testTag("launcher_library_center")
                    .focusRequester(centerFocusRequester)
                    .onFocusChanged { centerHasFocus = it.hasFocus }
                    .focusProperties { exit = { FocusRequester.Cancel } }.focusGroup(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                val library: @Composable () -> Unit = {
                    if (setupContent != null) {
                        setupContent()
                    } else {
                        LibraryCenter(
                            state, games, selectedGameId, filter, columns, changeFilter, selectGame, onOptions,
                            onImport, onRetry, shortcuts, navigation,
                            onFocusFilter = {
                                sectionIndex = it
                                section = LauncherSection.FILTERS
                            },
                            onFocusShortcut = {
                                sectionIndex = it
                                section = LauncherSection.SHORTCUTS
                            },
                        )
                    }
                }
                if (centerContent != null) centerContent(library) else library()
            }
            Box(Modifier.fillMaxSize().testTag("launcher_right_controls")) {
                wing(false)
            }
        }, modifier = Modifier.fillMaxSize()) { measurables, parent ->
            val bounds = listOf(panes.leftControls, panes.center, panes.rightControls)
            val children =
                measurables.mapIndexed { index, measurable ->
                    measurable.measure(Constraints.fixed(bounds[index].width, bounds[index].height))
                }
            layout(parent.maxWidth, parent.maxHeight) {
                children.forEachIndexed { index, child -> child.place(bounds[index].x, bounds[index].y) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryCenter(
    state: HomeViewModel.UIState,
    games: List<Game>,
    selectedGameId: Int?,
    filter: LibraryFilter,
    columns: Int,
    onFilter: (LibraryFilter) -> Unit,
    onSelect: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onImport: () -> Unit,
    onRetry: () -> Unit,
    shortcuts: List<LauncherShortcut>,
    navigation: LauncherNavigationTarget,
    onFocusFilter: (Int) -> Unit,
    onFocusShortcut: (Int) -> Unit,
) {
    BalancedLibraryLayout {
        Box(Modifier.fillMaxSize()) {
            when {
                state.errorMessage != null ->
                    LibraryMessage(
                        Icons.Outlined.Refresh,
                        "Let's try that again",
                        state.errorMessage,
                        "Retry",
                        onRetry,
                    )
                state.isLoading ->
                    Column(
                        Modifier.align(Alignment.Center).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                        )
                        Text("Opening your collection…", style = MaterialTheme.typography.bodySmall)
                    }
                state.allGames.isEmpty() && state.indexInProgress ->
                    LibraryMessage(
                        Icons.Outlined.FolderOpen,
                        "Making room for good times",
                        "Scanning your folder. Games will appear here as they're found.",
                    )
                state.allGames.isEmpty() -> WelcomeSteps(compact = true, onImport = onImport)
                games.isEmpty() ->
                    LibraryMessage(
                        Icons.Outlined.Star,
                        if (filter == LibraryFilter.FAVORITES) {
                            "Keep your favorites close"
                        } else {
                            "Your next game starts here"
                        },
                        if (filter == LibraryFilter.FAVORITES) {
                            "Open a game's options to add it to Favorites."
                        } else {
                            "Play something from All games and it will appear here."
                        },
                        "See all games",
                        { onFilter(LibraryFilter.ALL) },
                    )
                else -> GameShelf(games, selectedGameId, columns, onSelect, onOptions)
            }
            if (state.indexInProgress && !state.isLoading) {
                LinearWavyProgressIndicator(
                    Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
        // Compact, scrollable shortcut dock stays reachable at large font sizes.
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp))
                .testTag("launcher_dock"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shortcuts.forEachIndexed { index, shortcut ->
                IconButton(
                    onClick = shortcut.activate,
                    enabled = shortcut.enabled,
                    modifier =
                        Modifier.testTag("launcher_shortcut_$index")
                            .launcherNavigationFocus(
                                navigation.section == LauncherSection.SHORTCUTS && navigation.index == index,
                            ).onFocusChanged { if (it.isFocused) onFocusShortcut(index) },
                ) { Icon(shortcut.icon, shortcut.label) }
            }
        }
    }
}

/** The shelf receives the space previously occupied by the filter pills. */
@Composable
private fun BalancedLibraryLayout(content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val dockHeight = minOf(56.dp.roundToPx(), constraints.maxHeight)
        val dock = measurables[1].measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = dockHeight))
        val shelf = measurables[0].measure(Constraints.fixed(constraints.maxWidth, (constraints.maxHeight - dock.height).coerceAtLeast(0)))
        layout(constraints.maxWidth, constraints.maxHeight) {
            shelf.placeRelative(0, 0)
            dock.placeRelative((constraints.maxWidth - dock.width) / 2, constraints.maxHeight - dock.height)
        }
    }
}

@Composable
private fun GameShelf(
    games: List<Game>,
    selectedGameId: Int?,
    columns: Int,
    onSelect: (Game) -> Unit,
    onOptions: (Game) -> Unit,
) {
    val scrollState = rememberLazyListState()
    val selectedIndex = games.indexOfFirst { it.id == selectedGameId }
    val rows = (games.size + columns - 1) / columns
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tileSizeDp = launcherGridTileSizeForViewport(maxWidth.value, maxHeight.value, columns)
        if (tileSizeDp == null) {
            LibraryMessage(
                Icons.Outlined.GridView,
                "More room for your games",
                "Make this window taller to show game cards. Your selection is kept.",
            )
            return@BoxWithConstraints
        }
        LaunchedEffect(selectedIndex, columns, maxWidth, maxHeight) {
            val selectedRow = selectedIndex / columns
            val viewport = scrollState.layoutInfo
            val item = viewport.visibleItemsInfo.firstOrNull { it.index == selectedRow }
            val fullyVisible =
                item != null && item.offset >= viewport.viewportStartOffset &&
                    item.offset + item.size <= viewport.viewportEndOffset
            if (selectedIndex >= 0 && !fullyVisible) scrollState.animateScrollToItem(selectedRow)
        }
        val tileSize = tileSizeDp.dp
        val gridHeight = tileSize * rows + LAUNCHER_GRID_SPACING_DP.dp * (rows - 1)
        val verticalInset = ((maxHeight - gridHeight) / 2).coerceAtLeast(LAUNCHER_GRID_INSET_DP.dp)
        // Rows keep incomplete collections centered too. Long collections remain lazy and scrollable.
        LazyColumn(
            state = scrollState,
            modifier =
                Modifier.fillMaxSize().testTag("launcher_grid")
                    .semantics { collectionInfo = CollectionInfo(rows, columns) },
            contentPadding = PaddingValues(horizontal = LAUNCHER_GRID_INSET_DP.dp, vertical = verticalInset),
            verticalArrangement = Arrangement.spacedBy(LAUNCHER_GRID_SPACING_DP.dp),
        ) {
            items(rows, key = { row -> games[row * columns].id }) { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            LAUNCHER_GRID_SPACING_DP.dp,
                            Alignment.CenterHorizontally,
                        ),
                ) {
                    val rowStart = row * columns
                    val rowEnd = minOf(rowStart + columns, games.size)
                    for (index in rowStart until rowEnd) {
                        val game = games[index]
                        GameIconCard(
                            game = game,
                            isSelected = game.id == selectedGameId,
                            modifier =
                                Modifier.size(tileSize).semantics {
                                    collectionItemInfo = CollectionItemInfo(row, 1, index - rowStart, 1)
                                },
                            onSelect = { onSelect(game) },
                            onOptions = { onOptions(game) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameIconCard(
    game: Game,
    isSelected: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
    onOptions: () -> Unit,
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "Cartridge selection",
    )
    val selectedScale by animateFloatAsState(
        if (isSelected) 1.04f else 0.96f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "Cartridge growth",
    )
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.testTag("launcher_game_${game.id}")
            .graphicsLayer { scaleX = selectedScale; scaleY = selectedScale }.clip(shape)
            .background(containerColor)
            .border(
                if (isSelected) 3.dp else 1.dp,
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .onFocusChanged { if (it.isFocused) onSelect() }
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Preview ${game.title}",
                onLongClickLabel = "Game options",
                onClick = {
                    if (!isSelected) {
                        haptic.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove,
                        )
                    }
                    onSelect()
                },
                onLongClick = onOptions,
            )
            .semantics(mergeDescendants = true) {
                // The upper title plaque supplies the visible label; retain full titles for assistive tech.
                text = AnnotatedString(game.title)
                selected = isSelected
                customActions =
                    listOf(
                        CustomAccessibilityAction("Game options") {
                            onOptions()
                            true
                        },
                    )
            },
    ) {
        // The artwork reaches the card edge; the parent draws its outline above the image.
        GameArtwork(game, Modifier.fillMaxSize().testTag("launcher_game_art_${game.id}"))
        if (game.isFavorite) {
            Icon(
                Icons.Outlined.Star,
                "Favorite",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier.align(Alignment.TopEnd).padding(4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            CircleShape,
                        ).padding(4.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun SetupPane(
    state: HomeViewModel.UIState,
    onEnableNotifications: () -> Unit,
    onEnableMicrophone: () -> Unit,
    onChooseCore: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().testTag("launcher_setup").verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Make yourself at home",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        if (state.showNoNotificationPermissionCard) {
            Text("Allow notifications to see import progress while EmuUI is in the background.")
            OutlinedButton(onClick = onEnableNotifications, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Enable notifications")
            }
        }
        if (state.showNoMicrophonePermissionCard) {
            Text("Some Nintendo DS games use the microphone. You can enable it when you need it.")
            OutlinedButton(
                onClick = onEnableMicrophone,
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text("Enable microphone") }
        }
        if (state.showDesmumeDeprecatedCard) {
            Text("Your selected DS core is deprecated. Choose a supported core in settings.")
            OutlinedButton(onClick = onChooseCore, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose core") }
        }
        TextButton(onClick = onDone, modifier = Modifier.heightIn(min = 48.dp)) { Text("Done") }
    }
}

@Composable
private fun WelcomeSteps(
    compact: Boolean,
    onImport: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stacked = LocalDensity.current.fontScale >= 1.3f || maxHeight < 160.dp || maxWidth < 420.dp
        val tileModifier =
            if (stacked) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.width(if (compact) 236.dp else 280.dp).fillMaxHeight()
            }
        val tiles: @Composable () -> Unit = {
            WelcomeTile(
                "01",
                "Bring your collection",
                "Choose a folder of games you have the right to use. No games are included.",
                MaterialTheme.colorScheme.surfaceContainer,
                compact,
                modifier = tileModifier,
                scrollContent = !stacked,
                onClick = onImport,
            )
            WelcomeTile(
                "02",
                "Find your next favorite",
                "Select a game below to see it on the top screen.",
                MaterialTheme.colorScheme.surfaceContainer,
                compact,
                modifier = tileModifier,
                scrollContent = !stacked,
            )
            WelcomeTile(
                "03",
                "Pick up & play",
                "Press Play when you're ready. Your progress stays with you.",
                MaterialTheme.colorScheme.surfaceContainer,
                compact,
                modifier = tileModifier,
                scrollContent = !stacked,
            )
        }
        if (stacked) {
            // Natural-height full-width cards preserve scaled headings and body text.
            // One outer scroll region reaches every step without nested clipped strips.
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tiles()
            }
        } else {
            Row(
                Modifier.fillMaxSize().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tiles()
            }
        }
    }
}

@Composable
private fun WelcomeTile(
    number: String,
    title: String,
    body: String,
    color: Color,
    compact: Boolean,
    modifier: Modifier,
    scrollContent: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier,
        color = color,
        shape = RoundedCornerShape(22.dp),
    ) {
        val contentModifier =
            if (scrollContent) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
        Column(contentModifier.padding(16.dp)) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(if (compact) 5.dp else 14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            if (onClick != null) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                ) {
                    Text("Choose folder")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun LibraryMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.size(
                    56.dp,
                ).background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(18.dp)).padding(14.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

/** Original procedural art is a placeholder, never presented as a gameplay screenshot. */
@Composable
private fun GameArtwork(
    game: Game?,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val base = colors.surfaceContainer
    val cartridge = colors.surfaceContainerHighest
    val outline = colors.outlineVariant
    val label = colors.inverseSurface
    val labelMark = colors.inverseOnSurface
    Box(modifier.background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 10f
            val cx = size.width * .5f
            val cy = size.height * .48f
            val cartW = unit * 5.2f
            val cartH = unit * 5.8f
            drawRoundRect(
                outline.copy(alpha = .45f),
                topLeft = Offset(cx - cartW / 2 + unit * .12f, cy - cartH / 2 + unit * .16f),
                size = Size(cartW, cartH),
                cornerRadius = CornerRadius(unit * .45f),
            )
            drawRoundRect(
                cartridge,
                topLeft = Offset(cx - cartW / 2, cy - cartH / 2),
                size = Size(cartW, cartH),
                cornerRadius = CornerRadius(unit * .45f),
            )
            drawRoundRect(
                label,
                topLeft = Offset(cx - cartW * .36f, cy - cartH * .31f),
                size = Size(cartW * .72f, cartH * .52f),
                cornerRadius = CornerRadius(unit * .2f),
            )
            drawRect(
                labelMark,
                Offset(cx - unit * .11f, cy - cartH * .05f - unit * .48f),
                Size(unit * .22f, unit * .96f),
            )
            drawRect(
                labelMark,
                Offset(cx - unit * .48f, cy - cartH * .05f - unit * .11f),
                Size(unit * .96f, unit * .22f),
            )
            repeat(4) { index ->
                drawRoundRect(
                    outline,
                    Offset(cx - unit * 1.1f + index * unit * .64f, cy + cartH * .34f),
                    Size(unit * .4f, unit * .22f),
                    CornerRadius(unit * .08f),
                )
            }
        }
        if (game?.coverFrontUrl != null) {
            AsyncImage(
                model = game.coverFrontUrl,
                contentDescription = if (preview) "Cover art for ${game.title}" else null,
                contentScale = if (preview) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConsoleHinge() {
    // The real hinge remains excluded from layout and touch, with the same finish as the shell.
    Box(Modifier.fillMaxSize().background(consoleFrameColor()))
}

@Composable
private fun OrientationGuidance(guidance: FoldGuidance) {
    Column(
        Modifier.fillMaxSize().testTag("fold_guidance").windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ScreenRotation, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(
            when (guidance) {
                FoldGuidance.OPEN_FOLDABLE -> "Open your foldable"
                FoldGuidance.WINDOW_TOO_SMALL -> "A little more room to play"
                else -> "Turn to your happy place"
            },
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (guidance) {
                FoldGuidance.OPEN_FOLDABLE ->
                    "Open your folding device and turn it to landscape, with the crease running left to right."
                FoldGuidance.ROTATE_HINGE ->
                    "Rotate your device so the crease runs left to right. " +
                        "Preview goes above; your library goes below."
                FoldGuidance.ROTATE_LANDSCAPE -> "Use EmuUI in landscape, with the top screen above the library."
                FoldGuidance.WINDOW_TOO_SMALL -> "Expand this window to use both screens."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class LauncherShortcut(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean,
    val activate: () -> Unit,
)

private fun LauncherPrimaryAction.isEnabled(): Boolean =
    this != LauncherPrimaryAction.WAIT && this != LauncherPrimaryAction.UNSUPPORTED

private fun LauncherPrimaryAction.label(): String =
    when (this) {
        LauncherPrimaryAction.PLAY -> "Play game"
        LauncherPrimaryAction.IMPORT -> "Add your games"
        LauncherPrimaryAction.RETRY -> "Retry library"
        LauncherPrimaryAction.SHOW_ALL -> "See all games"
        LauncherPrimaryAction.WAIT -> "Please wait"
        LauncherPrimaryAction.UNSUPPORTED -> "Unsupported system"
    }

/** Logical wing focus is visible and announced without stealing keyboard or accessibility focus. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.launcherNavigationFocus(active: Boolean): Modifier {
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(active) { if (active) bringIntoView.bringIntoView() }
    return this.bringIntoViewRequester(bringIntoView).then(
        if (active) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                .semantics { stateDescription = "Ready to confirm with A" }
        } else {
            Modifier
        },
    )
}
