package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.AsyncImage
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.Backdrop
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ConsoleColors
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.DialogBlurBehind
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.FrostDefaults
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.FrostedSurface
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalBlurAvailable
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.SoftElevation
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.backdropSource
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.rememberBackdrop
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.softShadow
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val AMBIENT_SETTLE_MILLIS = 150L

/** The two halves are measured against the physical window crease, never side by side. */
@OptIn(FlowPreview::class)
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
    // Holding a direction on a gamepad selects a game every key repeat; let the backdrop settle.
    val latestSelection by rememberUpdatedState(selectedGame)
    var ambientGame by remember { mutableStateOf(selectedGame) }
    LaunchedEffect(Unit) {
        snapshotFlow { latestSelection }.debounce(AMBIENT_SETTLE_MILLIS).collect { ambientGame = it }
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
        AmbientBackdrop(ambientGame, Modifier.fillMaxSize())
        val geometry =
            FoldGeometry.resolve(
                constraints.maxWidth,
                constraints.maxHeight,
                posture.fold?.relativeTo(rootPosition.x.roundToInt(), rootPosition.y.roundToInt()),
                creasePadding,
            )
        if (geometry.guidance != null) {
            // Before the first window layout report a clamshell may still be turning; stay quiet.
            if (posture.known) OrientationGuidance(geometry.guidance)
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
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = ConsoleColors.Paper,
            title = { Text("Make yourself at home") },
            text = {
                DialogBlurBehind()
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

/**
 * The selected game's artwork, heavily blurred, tints the whole console the way Android's
 * shade takes on the wallpaper. Cards above it are translucent, so they pick up the same hue.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalAnimationApi::class)
@Composable
private fun AmbientBackdrop(
    game: Game?,
    modifier: Modifier,
) {
    updateTransition(targetState = game, label = "Ambient artwork").Crossfade(
        modifier = modifier,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        // Favourite or last-played changes must not fade identical artwork.
        contentKey = { it?.coverFrontUrl ?: it?.title },
    ) { shown ->
        Box(Modifier.fillMaxSize()) {
            // Blurring the placeholder would smear its dark screen glyph; use its palette instead.
            if (shown?.coverFrontUrl != null && LocalBlurAvailable.current) {
                BlurredArtwork(shown, Modifier.fillMaxSize(), alpha = 0.62f)
            } else {
                PaletteField(shown, Modifier.fillMaxSize(), alpha = 0.62f)
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            ConsoleColors.Shell.copy(alpha = 0.42f),
                            ConsoleColors.Shell.copy(alpha = 0.58f),
                            ConsoleColors.Shell.copy(alpha = 0.8f),
                        ),
                    ),
                ),
            )
        }
    }
}

/**
 * Artwork as a soft field of colour. It is drawn at a quarter of the size and scaled up, so the
 * GPU blurs a sixteenth of the pixels. Without blur, broad gradients carry the same palette.
 */
@Composable
private fun BlurredArtwork(
    game: Game?,
    modifier: Modifier,
    alpha: Float,
) {
    val blurAvailable = LocalBlurAvailable.current
    // A decorative copy: TalkBack already reads the real artwork.
    BoxWithConstraints(modifier.clearAndSetSemantics {}) {
        if (blurAvailable) {
            val downscale = 4f
            Box(
                Modifier.align(Alignment.Center)
                    .requiredSize(maxWidth / downscale, maxHeight / downscale)
                    .graphicsLayer {
                        // Overscan hides the darker rim a clamped blur leaves at its edges.
                        scaleX = downscale * 1.3f
                        scaleY = downscale * 1.3f
                        this.alpha = alpha
                    }
                    .blur(14.dp, BlurredEdgeTreatment.Rectangle),
            ) {
                GameArtwork(game, Modifier.fillMaxSize())
            }
        } else {
            PaletteField(game, Modifier.fillMaxSize(), alpha)
        }
    }
}

/** Broad soft gradients in a title's placeholder colour: a blur-free field of the same hue. */
@Composable
private fun PaletteField(
    game: Game?,
    modifier: Modifier,
    alpha: Float,
) {
    val base = artworkBaseColor(game)
    Canvas(modifier) {
        drawRect(base.copy(alpha = alpha * 0.55f))
        drawCircle(
            Brush.radialGradient(
                listOf(base.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.15f),
                radius = size.maxDimension * 0.6f,
            ),
            radius = size.maxDimension * 0.6f,
            center = Offset(size.width * 0.2f, size.height * 0.15f),
        )
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = alpha * 0.6f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.7f),
                radius = size.maxDimension * 0.5f,
            ),
            radius = size.maxDimension * 0.5f,
            center = Offset(size.width * 0.85f, size.height * 0.7f),
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
        val narrow = maxWidth < 520.dp
        val largeText = LocalDensity.current.fontScale >= 1.3f
        val artworkWeight = if (largeText || narrow) 0.35f else 0.46f
        val cardShape = if (compact) MaterialTheme.shapes.large else MaterialTheme.shapes.extraLarge
        val tileShape = RoundedCornerShape(if (compact) 16.dp else 24.dp)
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
                    color = ConsoleColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (onSyncSaves != null) {
                    IconButton(onClick = onSyncSaves, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Outlined.CloudSync, "Sync saved games")
                    }
                }
                IconButton(onClick = onOpenHelp, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Help and supported formats")
                }
                IconButton(onClick = onOpenSettings, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Settings, "Settings")
                }
            }
            // A media-style card: the artwork, blurred, becomes the screen's own backlight.
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(bottom = 10.dp)
                    .softShadow(cardShape, SoftElevation.Raised)
                    .clip(cardShape)
                    .background(ConsoleColors.Screen),
            ) {
                BlurredArtwork(game, Modifier.fillMaxSize(), alpha = 0.7f)
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            // Darkest behind the text column, whichever side it is on.
                            listOf(ConsoleColors.Screen.copy(alpha = 0.58f), ConsoleColors.Screen.copy(alpha = 0.88f))
                                .let { if (LocalLayoutDirection.current == LayoutDirection.Rtl) it.reversed() else it },
                        ),
                    ),
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    val artBackdrop = rememberBackdrop()
                    Box(
                        Modifier.weight(artworkWeight).fillMaxHeight().padding(if (compact) 10.dp else 16.dp)
                            .softShadow(tileShape, SoftElevation.Raised, color = Color.Black)
                            .clip(tileShape),
                    ) {
                        GameArtwork(
                            game,
                            Modifier.fillMaxSize().backdropSource(artBackdrop),
                            preview = true,
                            onImageLoaded = artBackdrop::invalidate,
                        )
                        FrostedSurface(
                            shape = CircleShape,
                            backdrop = artBackdrop,
                            // At least 4.5:1 for white text even over the palest placeholder.
                            tint = ConsoleColors.Screen.copy(alpha = 0.74f),
                            fallbackTint = ConsoleColors.Screen.copy(alpha = 0.82f),
                            blurRadius = 12.dp,
                            elevation = SoftElevation.None,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        ) {
                            Text(
                                when {
                                    game == null -> if (narrow) "READY" else "READY WHEN YOU ARE"
                                    narrow -> "SELECTED"
                                    else -> "SELECTED GAME"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            )
                        }
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
                                    if (compact || narrow) {
                                        MaterialTheme.typography.titleLargeEmphasized
                                    } else {
                                        MaterialTheme.typography.headlineLargeEmphasized
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
                        PrimaryAction(state, game, narrow, onPlay, onOptions, onImport)
                    }
                }
            }
        }
    }
}

/** Play and its options form one split button; an empty library offers import instead. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrimaryAction(
    state: HomeViewModel.UIState,
    game: Game?,
    narrow: Boolean,
    onPlay: (Game) -> Unit,
    onOptions: (Game) -> Unit,
    onImport: () -> Unit,
) {
    val colors =
        ButtonDefaults.buttonColors(
            containerColor = ConsoleColors.Lime,
            contentColor = ConsoleColors.Ink,
        )
    val noElevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
    if (game == null) {
        Button(
            shapes = ButtonDefaults.shapes(),
            onClick = onImport,
            enabled = !state.indexInProgress && !state.isLoading,
            colors = colors,
            elevation = noElevation,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(Icons.Outlined.FolderOpen, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Add your games")
        }
        return
    }
    // Narrow clamshell panes get the small split button, so both halves always fit.
    val height = if (narrow) SplitButtonDefaults.SmallContainerHeight else SplitButtonDefaults.MediumContainerHeight
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(
                onClick = { onPlay(game) },
                shapes = SplitButtonDefaults.leadingButtonShapesFor(height),
                colors = colors,
                elevation = noElevation,
                contentPadding = SplitButtonDefaults.leadingButtonContentPaddingFor(height),
                modifier = Modifier.heightIn(min = maxOf(height, 48.dp)),
            ) {
                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (narrow) "Play" else "Play game", style = MaterialTheme.typography.labelLargeEmphasized)
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = { onOptions(game) },
                shapes = SplitButtonDefaults.trailingButtonShapesFor(height),
                colors = colors,
                elevation = noElevation,
                contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(height),
                modifier = Modifier.heightIn(min = maxOf(height, 48.dp)),
            ) {
                Icon(Icons.Outlined.MoreHoriz, "Options for ${game.title}")
            }
        },
    )
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
        // A vertical toolbar floats over the shelf when it fits; otherwise actions stay inline.
        val toolbarActions = if (onSetup != null) 4 else 3
        val floatingTools = maxHeight - 64.dp >= 56.dp * toolbarActions + 24.dp
        val shelfEnd = if (floatingTools) 96.dp else 16.dp
        val shelfBackdrop = rememberBackdrop()
        val showCount = !compact && maxWidth >= 600.dp
        Column(Modifier.fillMaxSize().padding(top = 4.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryFilters(filter, onFilter, Modifier.weight(1f))
                if (showCount) {
                    Text(
                        "${state.allGames.size} ${if (state.allGames.size == 1) "GAME" else "GAMES"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ConsoleColors.Ink,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                if (onSetup != null && !compact) {
                    TextButton(
                        onClick = onSetup,
                        colors = ButtonDefaults.textButtonColors(contentColor = ConsoleColors.Ink),
                    ) { Text("Finish setup") }
                }
                if (!floatingTools) {
                    if (onSetup != null) {
                        IconButton(onClick = onSetup, shapes = IconButtonDefaults.shapes()) {
                            Icon(Icons.Outlined.Tune, "Finish optional setup")
                        }
                    }
                    IconButton(onClick = onOpenSearch, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Outlined.Search, "Search library")
                    }
                    IconButton(onClick = onOpenSystems, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Outlined.GridView, "Browse by system")
                    }
                    IconButton(
                        onClick = onImport,
                        enabled = !state.indexInProgress,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Outlined.Add, "Import games folder")
                    }
                }
            }
            if (state.indexInProgress && !state.isLoading) {
                LinearWavyProgressIndicator(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    color = ConsoleColors.Moss,
                    trackColor = ConsoleColors.Outline,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Everything the toolbar floats over is recorded, so it blurs whatever scrolls by.
                Box(Modifier.fillMaxSize().backdropSource(shelfBackdrop)) {
                    when {
                        state.errorMessage != null ->
                            LibraryMessage(
                                icon = Icons.Outlined.Refresh,
                                title = "Let's try that again",
                                body = state.errorMessage,
                                endInset = shelfEnd,
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
                                endInset = shelfEnd,
                            )
                        state.allGames.isEmpty() ->
                            WelcomeSteps(
                                compact = compact,
                                endInset = shelfEnd,
                                backdrop = shelfBackdrop,
                                onImport = onImport,
                            )
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
                                endInset = shelfEnd,
                                action = "See all games",
                                onAction = { onFilter(LibraryFilter.ALL) },
                            )
                        else -> GameShelf(games, selectedGameId, compact, shelfEnd, shelfBackdrop, onSelect, onOptions)
                    }
                }
                if (floatingTools) {
                    LibraryToolbar(
                        backdrop = shelfBackdrop,
                        importEnabled = !state.indexInProgress,
                        onSetup = onSetup,
                        onOpenSearch = onOpenSearch,
                        onOpenSystems = onOpenSystems,
                        onImport = onImport,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 8.dp),
                    )
                }
            }
        }
    }
}

/** A connected button group: one choice, pill-shaped when chosen, squared toward neighbours. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryFilters(
    filter: LibraryFilter,
    onFilter: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = LibraryFilter.values()
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sections.forEachIndexed { index, section ->
            val checked = filter == section
            ToggleButton(
                checked = checked,
                onCheckedChange = { onFilter(section) },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        sections.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                colors =
                    ToggleButtonDefaults.toggleButtonColors(
                        containerColor = ConsoleColors.Paper.copy(alpha = 0.72f),
                        contentColor = ConsoleColors.Ink,
                        checkedContainerColor = ConsoleColors.Ink,
                        checkedContentColor = ConsoleColors.Paper,
                    ),
                elevation = null,
                border = if (checked) null else BorderStroke(1.dp, ConsoleColors.Ink.copy(alpha = 0.08f)),
                modifier = Modifier.heightIn(min = 48.dp).semantics { selected = checked },
            ) {
                if (checked) {
                    Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = ConsoleColors.Lime)
                    Spacer(Modifier.width(6.dp))
                }
                Text(section.label, maxLines = 1)
            }
        }
    }
}

/** Library actions float over the shelf on frosted glass, blurring cartridges that pass under. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryToolbar(
    backdrop: Backdrop,
    importEnabled: Boolean,
    onSetup: (() -> Unit)?,
    onOpenSearch: () -> Unit,
    onOpenSystems: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = FloatingToolbarDefaults.ContainerShape
    FrostedSurface(
        shape = shape,
        backdrop = backdrop,
        elevation = SoftElevation.Floating,
        modifier = modifier,
    ) {
        VerticalFloatingToolbar(
            expanded = true,
            shape = shape,
            colors =
                FloatingToolbarDefaults.standardFloatingToolbarColors(
                    toolbarContainerColor = Color.Transparent,
                    toolbarContentColor = ConsoleColors.Ink,
                ),
            expandedShadowElevation = 0.dp,
            collapsedShadowElevation = 0.dp,
        ) {
            if (onSetup != null) {
                IconButton(onClick = onSetup, shapes = IconButtonDefaults.shapes()) {
                    Icon(Icons.Outlined.Tune, "Finish optional setup")
                }
            }
            IconButton(onClick = onOpenSearch, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Search, "Search library")
            }
            IconButton(onClick = onOpenSystems, shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.GridView, "Browse by system")
            }
            FilledIconButton(
                onClick = onImport,
                enabled = importEnabled,
                shapes = IconButtonDefaults.shapes(),
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = ConsoleColors.Lime,
                        contentColor = ConsoleColors.Ink,
                    ),
            ) {
                Icon(Icons.Outlined.Add, "Import games folder")
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
    endInset: Dp,
    backdrop: Backdrop,
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
        // Square art grows with the pane, so cards never stretch around empty space.
        val artHeight = (maxHeight - 60.dp - textHeight).coerceIn(0.dp, 220.dp)
        val cardWidth = maxOf(if (compact) 188.dp else 160.dp, artHeight + 16.dp)
        val cardShape = RoundedCornerShape(26.dp)
        val favoriteShape = MaterialShapes.Cookie9Sided.toShape()
        val selectedShape = MaterialShapes.Sunny.toShape()
        val endInsetPx = with(LocalDensity.current) { endInset.toPx() }
        val listState = rememberLazyListState()
        LaunchedEffect(listState, backdrop) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { backdrop.invalidate() }
        }
        LazyRow(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = endInset, top = 8.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(games, key = { it.id }) { game ->
                val isSelected = game.id == selectedGameId
                val motion = MaterialTheme.motionScheme
                val tint by animateColorAsState(
                    targetValue = if (isSelected) ConsoleColors.Lime.copy(alpha = 0.94f) else FrostDefaults.Tint,
                    animationSpec = motion.fastEffectsSpec(),
                    label = "Cartridge selection",
                )
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (pressed) 0.95f else 1f,
                    animationSpec = motion.fastSpatialSpec(),
                    label = "Cartridge press",
                )
                val haptic = LocalHapticFeedback.current
                val bringIntoView = remember { BringIntoViewRequester() }
                val scope = rememberCoroutineScope()
                var cardSize by remember { mutableStateOf(Size.Zero) }
                FrostedSurface(
                    shape = cardShape,
                    tint = tint,
                    fallbackTint = if (isSelected) ConsoleColors.Lime else FrostDefaults.FallbackTint,
                    elevation = if (isSelected) SoftElevation.Raised else SoftElevation.Resting,
                    modifier =
                        Modifier.width(cardWidth)
                            .bringIntoViewRequester(bringIntoView)
                            .onSizeChanged { cardSize = it.toSize() }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            },
                ) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, ConsoleColors.Moss.copy(alpha = 0.7f), cardShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .onFocusChanged {
                                    if (!it.isFocused) return@onFocusChanged
                                    onSelect(game)
                                    // Include the toolbar inset, so a focused card never parks under it.
                                    scope.launch {
                                        bringIntoView.bringIntoView(
                                            Rect(Offset.Zero, cardSize.copy(width = cardSize.width + endInsetPx)),
                                        )
                                    }
                                }
                                .combinedClickable(
                                    interactionSource = interaction,
                                    indication = ripple(),
                                    role = Role.Button,
                                    onClickLabel = "Preview ${game.title}",
                                    onLongClickLabel = "Game options",
                                    onClick = {
                                        if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                .padding(8.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(artHeight).clip(RoundedCornerShape(18.dp))) {
                            GameArtwork(game, Modifier.fillMaxSize())
                            if (game.isFavorite) {
                                Icon(
                                    Icons.Outlined.Star,
                                    "Favorite",
                                    tint = ConsoleColors.Ink,
                                    modifier =
                                        Modifier.align(Alignment.TopEnd).padding(6.dp)
                                            .background(ConsoleColors.Lime, favoriteShape).padding(5.dp).size(14.dp),
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    "Selected",
                                    tint = ConsoleColors.Lime,
                                    modifier =
                                        Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                            .background(ConsoleColors.Ink, selectedShape).padding(6.dp).size(14.dp),
                                )
                            }
                        }
                        Text(
                            game.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ConsoleColors.Ink,
                            // Keep the system label visible within a half-height library pane.
                            // The complete title remains in the preview and text semantics.
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 9.dp),
                        )
                        Text(
                            game.systemId.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ConsoleColors.Ink,
                            modifier = Modifier.padding(start = 6.dp, top = 3.dp, bottom = 3.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSteps(
    compact: Boolean,
    endInset: Dp,
    backdrop: Backdrop,
    onImport: () -> Unit,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(scroll, backdrop) { snapshotFlow { scroll.value }.collect { backdrop.invalidate() } }
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
                Modifier.fillMaxSize().verticalScroll(scroll)
                    .padding(start = 16.dp, end = endInset, top = 8.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                tiles()
            }
        } else {
            Row(
                Modifier.fillMaxSize().horizontalScroll(scroll)
                    .padding(start = 16.dp, end = endInset, top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                tiles()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val badgeShape = MaterialShapes.Cookie4Sided.toShape()
    FrostedSurface(
        shape = RoundedCornerShape(26.dp),
        tint = color.copy(alpha = 0.82f),
        fallbackTint = color,
        modifier = modifier,
    ) {
        val contentModifier =
            if (scrollContent) {
                Modifier.verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
        Column(contentModifier.padding(16.dp)) {
            Box(Modifier.size(34.dp).background(ConsoleColors.Ink, badgeShape), contentAlignment = Alignment.Center) {
                Text(number, style = MaterialTheme.typography.labelSmall, color = ConsoleColors.Paper)
            }
            Spacer(Modifier.height(if (compact) 6.dp else 14.dp))
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized, color = ConsoleColors.Ink)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryMessage(
    icon: ImageVector,
    title: String,
    body: String,
    endInset: Dp,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val iconShape = MaterialShapes.Cookie9Sided.toShape()
    Row(
        Modifier.fillMaxSize().padding(start = 24.dp, end = endInset.coerceAtLeast(24.dp), top = 8.dp, bottom = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Icon(
            icon,
            null,
            tint = ConsoleColors.Moss,
            modifier = Modifier.size(60.dp).background(ConsoleColors.Lime, iconShape).padding(16.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMediumEmphasized)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = ConsoleColors.Ink)
            if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
        }
    }
}

private val ArtworkPalette = listOf(ConsoleColors.Lilac, ConsoleColors.Lime, ConsoleColors.Peach, Color(0xFFBADAD6))

/** Each title keeps one stable placeholder colour, shared by its card and the ambient backdrop. */
private fun artworkBaseColor(game: Game?): Color {
    val seed = game?.title?.hashCode() ?: 0
    return ArtworkPalette[(seed and Int.MAX_VALUE) % ArtworkPalette.size]
}

/** Original procedural art is a placeholder, never presented as a gameplay screenshot. */
@Composable
private fun GameArtwork(
    game: Game?,
    modifier: Modifier = Modifier,
    preview: Boolean = false,
    onImageLoaded: () -> Unit = {},
) {
    val base = artworkBaseColor(game)
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
                onSuccess = { onImageLoaded() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** A faint seam over the ambient backdrop; on flat devices a small grip marks the virtual fold. */
@Composable
private fun ConsoleHinge(physical: Boolean) {
    Box(Modifier.fillMaxSize().background(ConsoleColors.Ink.copy(alpha = 0.035f))) {
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(1.dp)
                .background(ConsoleColors.Ink.copy(alpha = 0.08f)),
        )
        if (!physical) {
            Box(
                Modifier.align(Alignment.Center).width(50.dp).height(3.dp)
                    .clip(CircleShape).background(ConsoleColors.Muted.copy(alpha = .5f)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OrientationGuidance(guidance: FoldGuidance) {
    val iconShape = MaterialShapes.Sunny.toShape()
    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        FrostedSurface(
            shape = MaterialTheme.shapes.extraLarge,
            elevation = SoftElevation.Raised,
            modifier = Modifier.widthIn(max = 520.dp),
        ) {
            Column(
                Modifier.padding(28.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.ScreenRotation,
                    null,
                    Modifier.size(72.dp).background(ConsoleColors.Lime, iconShape).padding(18.dp),
                    tint = ConsoleColors.Moss,
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    if (guidance == FoldGuidance.WINDOW_TOO_SMALL) {
                        "A little more room to play"
                    } else {
                        "Turn to your happy place"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (guidance) {
                        FoldGuidance.ROTATE_HINGE ->
                            "Rotate your device so the crease runs left to right. " +
                                "Preview goes above; your library goes below."
                        FoldGuidance.ROTATE_LANDSCAPE ->
                            "Use EmuUI in landscape, with the top screen above the library."
                        FoldGuidance.WINDOW_TOO_SMALL -> "Expand this window to use both screens."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
