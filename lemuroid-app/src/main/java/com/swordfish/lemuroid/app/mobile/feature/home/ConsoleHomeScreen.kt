package com.swordfish.lemuroid.app.mobile.feature.home

import android.view.KeyEvent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldRect
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
    libraryActive: Boolean = true,
    onBack: (() -> Unit)? = null,
    centerContent: (@Composable (@Composable () -> Unit) -> Unit)? = null,
) {
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var showSetup by rememberSaveable { mutableStateOf(false) }
    val selectedGame = resolveLibrarySelection(state.allGames, selectedGameId) { it.id }
    val games =
        remember(state.allGames, filter) {
            filterLibrary(state.allGames, filter, Game::isFavorite, Game::lastPlayedAt)
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
            modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
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
                (lowerWindowBounds.width.toDp() * .22f).coerceIn(156.dp, 204.dp).roundToPx()
            }
        val dialogBounds =
            if (guidance == null) {
                FoldGeometry.lowerConsole(
                    lowerWindowBounds,
                    dialogWingWidth,
                    with(density) { 6.dp.roundToPx() },
                ).center.let { center ->
                    val inset = with(density) { LAUNCHER_PANEL_INSET_DP.dp.roundToPx() }
                    FoldRect(center.left + inset, center.top + inset, center.right - inset, center.bottom - inset)
                }
            } else {
                null
            }
        CompositionLocalProvider(LocalConsoleDialogRegion provides ConsoleDialogRegion(dialogBounds)) {
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
                            onPlay = onPlay,
                            onOptions = onGameOptions,
                            onImport = onImport,
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
                            onPlay = { if (selectedGame != null) onPlay(selectedGame) else onImport() },
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
                            canLaunch = selectedGame?.systemId != "3ds",
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
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onSyncSaves: (() -> Unit)?,
) {
    Box(
        Modifier.fillMaxSize().testTag("launcher_preview")
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp),
    ) {
        val largeText = LocalDensity.current.fontScale >= 1.3f
        Surface(
            Modifier.fillMaxSize().testTag("launcher_preview_surface"),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(24.dp),
        ) {
            Box(Modifier.fillMaxSize()) {
                // A full-width artwork canvas is the upper screen, never a split text/cover card.
                GameArtwork(game, Modifier.fillMaxSize(), preview = true)
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
                PreviewOverlayLayout(Modifier.fillMaxSize(), if (largeText) .52f else .44f) {
                    Surface(
                        modifier = Modifier.testTag("launcher_corner_brand"),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .94f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 2.dp,
                    ) {
                        Row(
                            Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(7.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "emuui",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 23.sp,
                                letterSpacing = (-1).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier =
                                    Modifier.testTag("launcher_brand_label")
                                        .semantics { contentDescription = "EmuUI home" },
                            )
                            if (!largeText) {
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
                        onClick = { if (game != null) onPlay(game) else onImport() },
                        enabled =
                            game?.systemId != "3ds" && (game != null || (!state.indexInProgress && !state.isLoading)),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier =
                            Modifier.heightIn(min = 48.dp).testTag("launcher_corner_play")
                                .semantics { contentDescription = if (game == null) "Add your games" else "Play game" },
                    ) {
                        Icon(
                            if (game == null) Icons.Outlined.FolderOpen else Icons.Filled.PlayArrow,
                            null,
                            Modifier.size(20.dp),
                        )
                        if (!largeText) {
                            Spacer(Modifier.width(6.dp))
                            Text(if (game == null) "Add your games" else "Play game")
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
                                game?.developer?.takeIf { it.isNotBlank() }
                                    ?: game?.systemId?.uppercase()
                                    ?: "YOUR COLLECTION · YOUR CONSOLE",
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
                            Modifier.align(Alignment.CenterEnd).padding(10.dp)
                                .background(
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

/** Shared measurement reserves the title between the lower pills and below the top controls. */
@Composable
private fun PreviewOverlayLayout(
    modifier: Modifier,
    titleFraction: Float,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val inset = 12.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val cornerWidth = ((constraints.maxWidth - inset * 2 - gap) / 2).coerceAtLeast(0)
        val cornerHeight = maxOf(48.dp.roundToPx(), measurables.take(4).maxOf { it.maxIntrinsicHeight(cornerWidth) })
        val corners =
            measurables.take(4).map {
                it.measure(Constraints(maxWidth = cornerWidth, minHeight = cornerHeight, maxHeight = cornerHeight))
            }
        val titleWidth =
            launcherPreviewTitleWidth(
                constraints.maxWidth,
                titleFraction,
                corners[2].width,
                corners[3].width,
                inset,
                gap,
            )
        val titleHeight = measurables[4].maxIntrinsicHeight(titleWidth)
        val title = measurables[4].measure(Constraints.fixed(titleWidth, titleHeight))
        val fits = titleWidth > 0 && launcherPreviewFits(constraints.maxHeight, cornerHeight, titleHeight, inset, gap)
        val guidance =
            if (!fits) measurables[5].measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight)) else null
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (fits) {
                corners[0].placeRelative(inset, inset)
                corners[1].placeRelative(constraints.maxWidth - inset - corners[1].width, inset)
                corners[2].placeRelative(inset, constraints.maxHeight - inset - cornerHeight)
                corners[3].placeRelative(
                    constraints.maxWidth - inset - corners[3].width,
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
    canLaunch: Boolean,
    libraryActive: Boolean,
    centerContent: (@Composable (@Composable () -> Unit) -> Unit)?,
    setupContent: (@Composable () -> Unit)?,
) {
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val centerFocusRequester = remember { FocusRequester() }
    var centerHasFocus by remember { mutableStateOf(false) }
    val view = LocalView.current
    BoxWithConstraints(
        Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        ),
    ) {
        val wingWidth = with(density) { (maxWidth * .22f).coerceIn(156.dp, 204.dp).roundToPx() }
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
        val onNavigate: (LauncherDirection) -> Unit = { direction ->
            if (libraryActive) {
                launcherSelectionIndex(games.size, selectedIndex, columns, direction)?.let { onSelect(games[it]) }
            } else {
                if (!centerHasFocus) centerFocusRequester.requestFocus()
                focusManager.moveFocus(
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
                onPlay()
            } else {
                if (!centerHasFocus) centerFocusRequester.requestFocus()
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
                view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
                Unit
            }
        }
        val cycleFilterOrFocus: (Boolean) -> Unit = { forward ->
            if (libraryActive) {
                onFilter(cycleLibraryFilter(filter, forward))
            } else {
                if (!centerHasFocus) centerFocusRequester.requestFocus()
                focusManager.moveFocus(if (forward) FocusDirection.Next else FocusDirection.Previous)
            }
        }
        Layout(content = {
            Box(Modifier.fillMaxSize().testTag("launcher_left_controls")) {
                LauncherControlWing(
                    true, libraryActive, games.isNotEmpty() || !libraryActive,
                    (canLaunch && (selectedGameId != null || (!state.indexInProgress && !state.isLoading))) ||
                        !libraryActive,
                    selectedGameId != null, onNavigate, activate, onBack, onMenu, onOpenSearch,
                    cycleFilterOrFocus,
                )
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
                            state, games, selectedGameId, filter, columns, onFilter, onSelect, onOptions,
                            onImport, onRetry, onOpenSearch, onOpenSystems, onOpenSettings, onSetup,
                        )
                    }
                }
                if (centerContent != null) centerContent(library) else library()
            }
            Box(Modifier.fillMaxSize().testTag("launcher_right_controls")) {
                LauncherControlWing(
                    false, libraryActive, games.isNotEmpty() || !libraryActive,
                    (canLaunch && (selectedGameId != null || (!state.indexInProgress && !state.isLoading))) ||
                        !libraryActive,
                    selectedGameId != null, onNavigate, activate, onBack, onMenu, onOpenSearch,
                    cycleFilterOrFocus,
                )
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
    onOpenSearch: () -> Unit,
    onOpenSystems: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetup: (() -> Unit)?,
) {
    BalancedLibraryLayout {
        Row(
            Modifier.horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp).testTag("launcher_filters"),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryFilter.values().forEach { section ->
                FilterChip(
                    selected = filter == section,
                    onClick = { onFilter(section) },
                    label = { Text(section.label) },
                    shape = RoundedCornerShape(50),
                    border =
                        BorderStroke(
                            1.dp,
                            if (filter == section) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
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
            IconButton(
                onClick = onImport,
                enabled = !state.indexInProgress,
            ) { Icon(Icons.Outlined.Add, "Import games folder") }
            IconButton(onClick = onOpenSearch) { Icon(Icons.Outlined.Search, "Search library") }
            IconButton(onClick = onOpenSystems) { Icon(Icons.Outlined.GridView, "Browse by system") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Tune, "Library settings") }
            if (onSetup != null) {
                IconButton(
                    onClick = onSetup,
                ) { Icon(Icons.Outlined.HelpOutline, "Finish optional setup") }
            }
        }
    }
}

/** Matched bands keep the filters and dock equally inset and centered around the game shelf. */
@Composable
private fun BalancedLibraryLayout(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = constraints.maxHeight
        Layout(content = content, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                measurables,
                constraints,
            ->
            val inset = 20.dp.roundToPx()
            val bandHeight =
                maxOf(
                    48.dp.roundToPx(),
                    measurables[0].maxIntrinsicHeight(constraints.maxWidth),
                    measurables[2].maxIntrinsicHeight(constraints.maxWidth),
                )
            val contentHeight = launcherLibraryContentHeight(viewportHeight, bandHeight, 80.dp.roundToPx(), inset)
            val bandConstraints =
                Constraints(maxWidth = constraints.maxWidth, minHeight = bandHeight, maxHeight = bandHeight)
            val filters = measurables[0].measure(bandConstraints)
            val dock = measurables[2].measure(bandConstraints)
            val shelf =
                measurables[1].measure(
                    Constraints.fixed(constraints.maxWidth, contentHeight - inset * 2 - bandHeight * 2),
                )
            layout(constraints.maxWidth, contentHeight) {
                filters.placeRelative((constraints.maxWidth - filters.width) / 2, inset)
                shelf.placeRelative(0, inset + bandHeight)
                dock.placeRelative((constraints.maxWidth - dock.width) / 2, contentHeight - inset - bandHeight)
            }
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
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.testTag("launcher_game_${game.id}").clip(shape)
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
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
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
