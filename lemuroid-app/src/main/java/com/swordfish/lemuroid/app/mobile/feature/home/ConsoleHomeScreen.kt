package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ConsoleColors
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
    val posture = rememberFoldPosture()
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val creasePadding = with(density) { 4.dp.roundToPx() }
    val hasSetupNotices =
        state.showNoMicrophonePermissionCard || state.showNoNotificationPermissionCard ||
            state.showDesmumeDeprecatedCard

    BoxWithConstraints(
        modifier =
            modifier.fillMaxSize().background(ConsoleColors.Shell)
                .onGloballyPositioned { rootPosition = it.positionInWindow() },
    ) {
        val geometry =
            FoldGeometry.resolve(
                constraints.maxWidth,
                constraints.maxHeight,
                posture.fold?.relativeTo(rootPosition.x.roundToInt(), rootPosition.y.roundToInt()),
                creasePadding,
            )
        if (geometry.guidance != null) {
            OrientationGuidance(geometry.guidance)
        } else {
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
                    ConsoleHinge(physical = geometry.physicalHinge)
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
                    placeables.forEachIndexed { index, placeable -> placeable.place(bounds[index].x, bounds[index].y) }
                }
            }
        }
    }
    if (showSetup) {
        AlertDialog(
            onDismissRequest = { showSetup = false },
            title = { Text("Make yourself at home") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (state.showNoNotificationPermissionCard) {
                        Text("Allow notifications to see import progress while EmuUI is in the background.")
                        OutlinedButton(onClick = onEnableNotifications) { Text("Enable notifications") }
                    }
                    if (state.showNoMicrophonePermissionCard) {
                        Text("Some Nintendo DS games use the microphone. You can enable it when you need it.")
                        OutlinedButton(onClick = onEnableMicrophone) { Text("Enable microphone") }
                    }
                    if (state.showDesmumeDeprecatedCard) {
                        Text("Your selected DS core is deprecated. Choose a supported core in settings.")
                        OutlinedButton(onClick = {
                            showSetup = false
                            onOpenCoreSelection()
                        }) { Text("Choose core") }
                    }
                    if (!hasSetupNotices) Text("You're all set. Your library is ready to play.")
                }
            },
            confirmButton = { TextButton(onClick = { showSetup = false }) { Text("Done") } },
        )
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
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp),
    ) {
        val compact = maxHeight < 250.dp
        val largeText = LocalDensity.current.fontScale >= 1.3f
        val artworkWeight = if (largeText) 0.35f else 0.46f
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(ConsoleColors.Moss))
                    Text(
                        "emuui",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 25.sp,
                        letterSpacing = (-1.2).sp,
                        color = ConsoleColors.Ink,
                        modifier = Modifier.semantics { contentDescription = "EmuUI home" },
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    if (state.indexInProgress) "ADDING TO YOUR COLLECTION" else "YOUR POCKET ARCADE",
                    style = MaterialTheme.typography.labelSmall,
                    color = ConsoleColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onSyncSaves != null) {
                    IconButton(onClick = onSyncSaves) {
                        Icon(Icons.Outlined.CloudSync, "Sync saved games")
                    }
                }
                IconButton(onClick = onOpenHelp) { Icon(Icons.Outlined.HelpOutline, "Help and supported formats") }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, "Settings") }
            }
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(bottom = 10.dp),
                color = ConsoleColors.Screen,
                shape = RoundedCornerShape(if (compact) 22.dp else 30.dp),
            ) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(artworkWeight).fillMaxHeight().padding(if (compact) 10.dp else 16.dp)
                            .clip(RoundedCornerShape(if (compact) 14.dp else 20.dp)),
                    ) {
                        GameArtwork(game, Modifier.fillMaxSize(), preview = true)
                        Text(
                            if (game == null) "READY WHEN YOU ARE" else "SELECTED GAME",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier =
                                Modifier.align(Alignment.BottomStart).padding(12.dp)
                                    .background(ConsoleColors.Screen.copy(alpha = 0.8f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                    }
                    Column(
                        modifier =
                            Modifier.weight(1f - artworkWeight).fillMaxHeight()
                                .padding(
                                    start = 8.dp,
                                    top = if (compact) 12.dp else 22.dp,
                                    end = if (compact) 16.dp else 26.dp,
                                    bottom = 12.dp,
                                ),
                    ) {
                        // Text may scroll independently; the primary action never scrolls
                        // below the physical upper pane or disappears behind the crease.
                        Column(
                            modifier =
                                Modifier.weight(1f).fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            if (game != null || !largeText) {
                                Text(
                                    game?.systemId?.uppercase() ?: "A LITTLE NOSTALGIA. A LOT OF PLAY.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ConsoleColors.Lime,
                                )
                                Spacer(Modifier.height(if (compact) 4.dp else 10.dp))
                            }
                            Text(
                                game?.title ?: "Good games.\nGreat little moments.",
                                style =
                                    if (compact) {
                                        MaterialTheme.typography.titleLarge
                                    } else {
                                        MaterialTheme.typography.headlineLarge
                                    },
                                color = ConsoleColors.Paper,
                                modifier = Modifier.semantics { heading() },
                            )
                            if (!compact && !largeText) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    game?.developer?.takeIf { it.isNotBlank() }
                                        ?: if (game == null) {
                                            "Your collection, a familiar two-screen home."
                                        } else {
                                            "Choose your next little escape."
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFBDCABD),
                                )
                            }
                        }
                        Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                shapes = ButtonDefaults.shapes(),
                                onClick = { if (game != null) onPlay(game) else onImport() },
                                enabled = game != null || (!state.indexInProgress && !state.isLoading),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = ConsoleColors.Lime,
                                        contentColor = ConsoleColors.Ink,
                                    ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier.weight(1f, fill = false).heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    if (game == null) Icons.Outlined.FolderOpen else Icons.Filled.PlayArrow,
                                    null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (game == null) "Add your games" else "Play game")
                            }
                            if (game != null) {
                                IconButton(onClick = { onOptions(game) }) {
                                    Icon(
                                        Icons.Outlined.MoreHoriz,
                                        "Options for ${game.title}",
                                        tint = ConsoleColors.Paper,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
) {
    BoxWithConstraints(
        Modifier.fillMaxSize()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            ),
    ) {
        val compact = maxHeight < 230.dp
        Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LibraryFilter.values().forEach { section ->
                        FilterChip(
                            selected = filter == section,
                            onClick = { onFilter(section) },
                            label = { Text(section.label) },
                            leadingIcon =
                                if (filter == section) {
                                    { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }
                                } else {
                                    null
                                },
                            shape = RoundedCornerShape(50),
                            border =
                                BorderStroke(
                                    1.dp,
                                    if (filter == section) ConsoleColors.Ink else ConsoleColors.Outline,
                                ),
                            colors =
                                FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ConsoleColors.Ink,
                                    selectedLabelColor = ConsoleColors.Paper,
                                    selectedLeadingIconColor = ConsoleColors.Lime,
                                ),
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                if (onSetup != null) {
                    IconButton(
                        onClick = onSetup,
                    ) { Icon(Icons.Outlined.Tune, "Finish optional setup") }
                }
                IconButton(onClick = onOpenSearch) { Icon(Icons.Outlined.Search, "Search library") }
                IconButton(onClick = onOpenSystems) { Icon(Icons.Outlined.GridView, "Browse by system") }
                IconButton(onClick = onImport, enabled = !state.indexInProgress) {
                    Icon(Icons.Outlined.Add, "Import games folder")
                }
            }
            if (state.indexInProgress && !state.isLoading) {
                LinearWavyProgressIndicator(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = ConsoleColors.Moss,
                    trackColor = ConsoleColors.Outline,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.errorMessage != null ->
                        LibraryMessage(
                            icon = Icons.Outlined.Refresh,
                            title = "Let's try that again",
                            body = state.errorMessage,
                            action = "Retry",
                            onAction = onRetry,
                        )
                    state.isLoading ->
                        Row(
                            Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            // Polygon morph construction can stall a slow device's main thread.
                            // Keep startup progress cheap; expressive motion remains on controls.
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = ConsoleColors.Moss,
                                strokeWidth = 2.dp,
                            )
                            Text("Opening your collection…", style = MaterialTheme.typography.bodyMedium)
                        }
                    state.allGames.isEmpty() && state.indexInProgress ->
                        LibraryMessage(
                            Icons.Outlined.FolderOpen,
                            "Making room for good times",
                            "Scanning your folder. Games will appear here as they're found.",
                        )
                    state.allGames.isEmpty() -> WelcomeSteps(compact = compact, onImport = onImport)
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
                    else -> GameShelf(games, selectedGameId, compact, onSelect, onOptions)
                }
            }
            if (!compact) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${state.allGames.size} ${if (state.allGames.size == 1) "GAME" else "GAMES"}" +
                            "  ·  YOUR COLLECTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConsoleColors.Muted,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (onSetup != null) {
                        TextButton(onClick = onSetup) { Text("Finish setup") }
                    } else {
                        Text(
                            "Select below. Play above.",
                            style = MaterialTheme.typography.labelMedium,
                            color = ConsoleColors.Muted,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GameShelf(
    games: List<Game>,
    selectedGameId: Int?,
    compact: Boolean,
    onSelect: (Game) -> Unit,
    onOptions: (Game) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Reserve the actual scaled text height before allocating decorative artwork.
        // At 100% this matches the original 78dp allowance; larger text gets more room.
        val typography = MaterialTheme.typography
        val textHeight =
            with(LocalDensity.current) {
                typography.titleSmall.lineHeight.toDp() + typography.labelSmall.lineHeight.toDp()
            }
        val artHeight = (maxHeight - 44.dp - textHeight).coerceIn(0.dp, 164.dp)
        LazyRow(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(games, key = { it.id }) { game ->
                val isSelected = game.id == selectedGameId
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) ConsoleColors.Lime else ConsoleColors.Paper,
                    animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                    label = "Cartridge selection",
                )
                val haptic = LocalHapticFeedback.current
                Column(
                    modifier =
                        Modifier.width(if (compact) 188.dp else 158.dp).fillMaxHeight()
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerColor)
                            .border(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) ConsoleColors.Moss else ConsoleColors.Outline,
                                RoundedCornerShape(20.dp),
                            )
                            .onFocusChanged { if (it.isFocused) onSelect(game) }
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
                                    onSelect(game)
                                },
                                onLongClick = { onOptions(game) },
                            )
                            .semantics {
                                selected = isSelected
                                customActions =
                                    listOf(
                                        CustomAccessibilityAction("Game options") {
                                            onOptions(game)
                                            true
                                        },
                                    )
                            }
                            .verticalScroll(rememberScrollState())
                            .padding(7.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(artHeight).clip(RoundedCornerShape(13.dp))) {
                        GameArtwork(game, Modifier.fillMaxSize())
                        if (game.isFavorite) {
                            Icon(
                                Icons.Outlined.Star,
                                "Favorite",
                                tint = ConsoleColors.Ink,
                                modifier =
                                    Modifier.align(Alignment.TopEnd).padding(5.dp)
                                        .background(ConsoleColors.Lime, CircleShape).padding(4.dp).size(16.dp),
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Filled.Check,
                                "Selected",
                                tint = ConsoleColors.Ink,
                                modifier =
                                    Modifier.align(Alignment.BottomEnd).padding(5.dp)
                                        .background(ConsoleColors.Lime, CircleShape).padding(4.dp).size(16.dp),
                            )
                        }
                    }
                    Text(
                        game.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = ConsoleColors.Ink,
                        // Keep the system label visible within a half-height library pane.
                        // The complete title remains in the preview and text semantics.
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp, top = 8.dp),
                    )
                    Text(
                        game.systemId.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ConsoleColors.Muted,
                        modifier = Modifier.padding(start = 5.dp, top = 3.dp, bottom = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeSteps(
    compact: Boolean,
    onImport: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val stacked = LocalDensity.current.fontScale >= 1.3f || maxHeight < 100.dp
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
                ConsoleColors.Lime,
                compact,
                modifier = tileModifier,
                scrollContent = !stacked,
                onClick = onImport,
            )
            WelcomeTile(
                "02",
                "Find your next favorite",
                "Select a game below to see it on the top screen.",
                ConsoleColors.Lilac,
                compact,
                modifier = tileModifier,
                scrollContent = !stacked,
            )
            WelcomeTile(
                "03",
                "Pick up & play",
                "Press Play when you're ready. Your progress stays with you.",
                ConsoleColors.Peach,
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
            Text(number, style = MaterialTheme.typography.labelSmall, color = ConsoleColors.Ink)
            Spacer(Modifier.height(if (compact) 5.dp else 14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = ConsoleColors.Ink)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodySmall, color = ConsoleColors.Ink)
            if (onClick != null) {
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = ConsoleColors.Ink),
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
            tint = ConsoleColors.Moss,
            modifier = Modifier.size(56.dp).background(ConsoleColors.Lime, RoundedCornerShape(18.dp)).padding(14.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = ConsoleColors.Muted)
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
    val palette = listOf(ConsoleColors.Lilac, ConsoleColors.Lime, ConsoleColors.Peach, Color(0xFFBADAD6))
    val seed = game?.title?.hashCode() ?: 0
    val base = palette[(seed and Int.MAX_VALUE) % palette.size]
    Box(modifier.background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension / 10f
            drawCircle(
                Color.White.copy(alpha = 0.45f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.83f, size.height * 0.05f),
            )
            drawCircle(
                ConsoleColors.Moss.copy(alpha = 0.10f),
                radius = size.minDimension * 0.68f,
                center = Offset(size.width * 0.1f, size.height * 0.98f),
            )
            val cx = size.width * 0.52f
            val cy = size.height * 0.48f
            val cartW = unit * 4.2f
            val cartH = unit * 4.9f
            drawRoundRect(
                ConsoleColors.Screen.copy(alpha = 0.12f),
                topLeft = Offset(cx - cartW / 2 + unit * .28f, cy - cartH / 2 + unit * .3f),
                size = Size(cartW, cartH),
                cornerRadius = CornerRadius(unit * .45f),
            )
            drawRoundRect(
                ConsoleColors.Paper,
                topLeft = Offset(cx - cartW / 2, cy - cartH / 2),
                size = Size(cartW, cartH),
                cornerRadius = CornerRadius(unit * .45f),
            )
            drawRoundRect(
                ConsoleColors.Screen,
                topLeft = Offset(cx - cartW * .36f, cy - cartH * .31f),
                size = Size(cartW * .72f, cartH * .52f),
                cornerRadius = CornerRadius(unit * .2f),
            )
            drawCircle(base, unit * .63f, Offset(cx, cy - cartH * .06f))
            drawRect(
                ConsoleColors.Paper,
                Offset(cx - unit * .1f, cy - cartH * .06f - unit * .36f),
                Size(unit * .2f, unit * .72f),
            )
            drawRect(
                ConsoleColors.Paper,
                Offset(cx - unit * .36f, cy - cartH * .06f - unit * .1f),
                Size(unit * .72f, unit * .2f),
            )
            repeat(4) { index ->
                drawRoundRect(
                    ConsoleColors.Outline,
                    Offset(cx - unit * 1.1f + index * unit * .64f, cy + cartH * .34f),
                    Size(unit * .4f, unit * .22f),
                    CornerRadius(unit * .08f),
                )
            }
            // A fine dot grid recalls a handheld LCD without copying game artwork.
            for (x in 1..5) for (y in 1..3) {
                drawCircle(
                    ConsoleColors.Screen.copy(alpha = .13f),
                    unit * .045f,
                    Offset(size.width - unit * (x * .4f + .4f), size.height - unit * (y * .4f + .4f)),
                )
            }
        }
        if (game?.coverFrontUrl != null) {
            AsyncImage(
                model = game.coverFrontUrl,
                contentDescription = "Cover art for ${game.title}",
                contentScale = if (preview) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ConsoleHinge(physical: Boolean) {
    Box(Modifier.fillMaxSize().background(ConsoleColors.Outline)) {
        Box(Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp).background(Color(0xFFB8BEB0)))
        if (!physical) {
            Box(
                Modifier.align(Alignment.Center).width(50.dp).height(3.dp)
                    .clip(CircleShape).background(ConsoleColors.Muted.copy(alpha = .5f)),
            )
        }
    }
}

@Composable
private fun OrientationGuidance(guidance: FoldGuidance) {
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.ScreenRotation, null, Modifier.size(48.dp), tint = ConsoleColors.Moss)
        Spacer(Modifier.height(16.dp))
        Text(
            if (guidance == FoldGuidance.WINDOW_TOO_SMALL) "A little more room to play" else "Turn to your happy place",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            when (guidance) {
                FoldGuidance.ROTATE_HINGE ->
                    "Rotate your device so the crease runs left to right. " +
                        "Preview goes above; your library goes below."
                FoldGuidance.ROTATE_LANDSCAPE -> "Use EmuUI in landscape, with the top screen above the library."
                FoldGuidance.WINDOW_TOO_SMALL -> "Expand this window to use both screens."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
