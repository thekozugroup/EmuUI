@file:Suppress("UNUSED", "INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.swordfish.lemuroid.app.mobile.feature.gamemenu

import com.swordfish.lemuroid.app.mobile.feature.emuui.enterImmersiveConsole
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGuidance
import com.swordfish.lemuroid.app.mobile.feature.emuui.allowDisplayCutouts
import com.swordfish.lemuroid.app.mobile.feature.emuui.rememberFoldPosture
import com.swordfish.lemuroid.app.mobile.feature.game.ConsoleRegion
import com.swordfish.lemuroid.app.mobile.feature.game.FoldGameGuidance
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions.GameMenuCoreOptionsScreen
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.coreoptions.GameMenuCoreOptionsViewModel
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.states.GameMenuStatesScreen
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.states.GameMenuStatesViewModel
import com.swordfish.lemuroid.app.mobile.feature.home.LauncherControlWing
import com.swordfish.lemuroid.app.mobile.feature.home.LauncherDirection
import com.swordfish.lemuroid.app.mobile.feature.home.activateLauncherMenuFocus
import com.swordfish.lemuroid.app.mobile.feature.home.moveLauncherMenuFocus
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.shared.GameMenuContract
import com.swordfish.lemuroid.app.shared.coreoptions.LemuroidCoreOption
import com.swordfish.lemuroid.app.shared.input.InputDeviceManager
import com.swordfish.lemuroid.app.utils.android.settings.ConsoleDialogRegion
import com.swordfish.lemuroid.app.utils.android.settings.LocalConsoleDialogRegion
import com.swordfish.lemuroid.common.kotlin.serializable
import com.swordfish.lemuroid.lib.android.RetrogradeComponentActivity
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import java.security.InvalidParameterException
import javax.inject.Inject
import kotlin.math.roundToInt

class GameMenuActivity : RetrogradeComponentActivity() {
    @Inject
    lateinit var inputDeviceManager: InputDeviceManager

    @Inject
    lateinit var statesManager: StatesManager

    @Inject
    lateinit var statesPreviewManager: StatesPreviewManager

    data class GameMenuRequest(
        val coreOptions: List<LemuroidCoreOption>,
        val advancedCoreOptions: List<LemuroidCoreOption>,
        val game: Game,
        val coreConfig: SystemCoreConfig,
        val audioEnabled: Boolean,
        val fastForwardSupported: Boolean,
        val fastForwardEnabled: Boolean,
        val numDisks: Int,
        val currentDisk: Int,
        val currentTiltConfiguration: TiltConfiguration,
        val allTiltConfigurations: List<TiltConfiguration>,
    )

    override fun onResume() {
        super.onResume()
        com.swordfish.lemuroid.app.mobile.feature.emuui.ConsoleOrientation.apply(this)
        enterImmersiveConsole()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveConsole()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            SystemBarStyle.dark(Color.TRANSPARENT),
            SystemBarStyle.dark(Color.TRANSPARENT),
        )

        window.allowDisplayCutouts()

        val extras = intent.extras

        val gameMenuRequest =
            GameMenuRequest(
                coreOptions =
                    intent.serializable<Array<LemuroidCoreOption>>(GameMenuContract.EXTRA_CORE_OPTIONS)
                        ?.toList()
                        ?: throw InvalidParameterException("Missing EXTRA_CORE_OPTIONS"),
                advancedCoreOptions =
                    intent.serializable<Array<LemuroidCoreOption>>(GameMenuContract.EXTRA_ADVANCED_CORE_OPTIONS)
                        ?.toList()
                        ?: throw InvalidParameterException("Missing EXTRA_ADVANCED_CORE_OPTIONS"),
                game =
                    intent.serializable<Game>(GameMenuContract.EXTRA_GAME)
                        ?: throw InvalidParameterException("Missing EXTRA_GAME"),
                coreConfig =
                    intent.serializable<SystemCoreConfig>(GameMenuContract.EXTRA_SYSTEM_CORE_CONFIG)
                        ?: throw InvalidParameterException("Missing EXTRA_SYSTEM_CORE_CONFIG"),
                audioEnabled =
                    extras?.getBoolean(GameMenuContract.EXTRA_AUDIO_ENABLED, false) ?: false,
                fastForwardSupported =
                    extras?.getBoolean(GameMenuContract.EXTRA_FAST_FORWARD_SUPPORTED, false) ?: false,
                fastForwardEnabled =
                    extras?.getBoolean(GameMenuContract.EXTRA_FAST_FORWARD, false) ?: false,
                numDisks =
                    extras?.getInt(GameMenuContract.EXTRA_DISKS, 0) ?: 0,
                currentDisk =
                    extras?.getInt(GameMenuContract.EXTRA_CURRENT_DISK, 0) ?: 0,
                currentTiltConfiguration =
                    intent.serializable<TiltConfiguration>(GameMenuContract.EXTRA_CURRENT_TILT_CONFIG)
                        ?: TiltConfiguration.Disabled,
                allTiltConfigurations =
                    intent.serializable<Array<TiltConfiguration>>(GameMenuContract.EXTRA_TILT_ALL_CONFIGS)
                        ?.toList()
                        ?: emptyList(),
            )

        setContent {
            GameMenuScreen(gameMenuRequest)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun GameMenuScreen(gameMenuRequest: GameMenuRequest) {
        AppTheme {
            val navController = rememberNavController()
            val navBackStackEntry = navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry.value?.destination

            val currentRoute =
                currentDestination?.route
                    ?.let { GameMenuRoute.findByRoute(it) }
                    ?: GameMenuRoute.HOME

            SideMenu(
                gameTitle = gameMenuRequest.game.title,
                onBack = { if (currentRoute.canGoBack()) navController.popBackStack() else onResult { } },
                onMenuHome = { navController.popBackStack(GameMenuRoute.HOME.route, false) },
                onOptions = { navController.navigate(GameMenuRoute.OPTIONS.route) { launchSingleTop = true } },
            ) {
                TopAppBar(
                    title = { Text(stringResource(currentRoute.titleId)) },
                    windowInsets = WindowInsets(0.dp),
                    navigationIcon = {
                        AnimatedContent(targetState = currentRoute.canGoBack(), label = "Back") { canGoBack ->
                            if (canGoBack) {
                                IconButton(onClick = { navController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        stringResource(R.string.back),
                                    )
                                }
                            } else {
                                IconButton(onClick = { onResult { } }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        stringResource(R.string.close),
                                    )
                                }
                            }
                        }
                    },
                )
                Divider(modifier = Modifier.fillMaxWidth())
                NavHost(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    navController = navController,
                    startDestination = GameMenuRoute.HOME.route,
                    enterTransition = { fadeIn() },
                    exitTransition = { fadeOut() },
                ) {
                    composable(GameMenuRoute.HOME) {
                        GameMenuHomeScreen(navController, gameMenuRequest, ::onResult)
                    }
                    composable(GameMenuRoute.SAVE) {
                        GameMenuStatesScreen(
                            viewModel(
                                factory =
                                    GameMenuStatesViewModel.Factory(
                                        application,
                                        gameMenuRequest,
                                        statesManager,
                                        false,
                                        statesPreviewManager,
                                    ),
                            ),
                            onStateClicked = {
                                onResult { putExtra(GameMenuContract.RESULT_SAVE, it) }
                            },
                        )
                    }
                    composable(GameMenuRoute.LOAD) {
                        GameMenuStatesScreen(
                            viewModel(
                                factory =
                                    GameMenuStatesViewModel.Factory(
                                        application,
                                        gameMenuRequest,
                                        statesManager,
                                        true,
                                        statesPreviewManager,
                                    ),
                            ),
                            onStateClicked = {
                                onResult { putExtra(GameMenuContract.RESULT_LOAD, it) }
                            },
                        )
                    }
                    composable(GameMenuRoute.OPTIONS) {
                        GameMenuCoreOptionsScreen(
                            viewModel(
                                factory = GameMenuCoreOptionsViewModel.Factory(inputDeviceManager),
                            ),
                            gameMenuRequest,
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun SideMenu(
        gameTitle: String,
        onBack: () -> Unit,
        onMenuHome: () -> Unit,
        onOptions: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val posture = rememberFoldPosture()
        val density = LocalDensity.current
        val root = remember { mutableStateOf(IntOffset.Zero) }
        val insets = WindowInsets.safeDrawing
        val focusManager = LocalFocusManager.current
        val inputModeManager = LocalInputModeManager.current
        val centerFocus = remember { FocusRequester() }
        val centerHasFocus = remember { mutableStateOf(false) }
        val view = LocalView.current
        val navigate: (LauncherDirection) -> Unit = { direction ->
            moveLauncherMenuFocus(
                inputModeManager,
                focusManager,
                centerFocus,
                { centerHasFocus.value },
                when (direction) {
                    LauncherDirection.UP -> FocusDirection.Up
                    LauncherDirection.DOWN -> FocusDirection.Down
                    LauncherDirection.LEFT -> FocusDirection.Left
                    LauncherDirection.RIGHT -> FocusDirection.Right
                },
            )
        }
        val activate = {
            activateLauncherMenuFocus(inputModeManager, centerFocus, { centerHasFocus.value }, view)
        }
        val cycleFocus: (Boolean) -> Unit = { forward ->
            moveLauncherMenuFocus(
                inputModeManager,
                focusManager,
                centerFocus,
                { centerHasFocus.value },
                if (forward) FocusDirection.Next else FocusDirection.Previous,
            )
        }
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxSize().onGloballyPositioned {
                    val position = it.positionInWindow()
                    root.value = IntOffset(position.x.roundToInt(), position.y.roundToInt())
                },
        ) {
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val raw =
                FoldGeometry.resolve(
                    width,
                    height,
                    posture.fold?.relativeTo(root.value.x, root.value.y),
                    with(density) { 8.dp.roundToPx() },
                )
            val left = insets.getLeft(density, LayoutDirection.Ltr)
            val right = (width - insets.getRight(density, LayoutDirection.Ltr)).coerceAtLeast(left)
            val layout =
                raw.copy(
                    upper = raw.upper.copy(left = left, right = right, top = insets.getTop(density)),
                    lower = raw.lower.copy(left = left, right = right, bottom = height - insets.getBottom(density)),
                )
            val guidance =
                layout.guidance ?: if (layout.lower.height < with(density) { 144.dp.roundToPx() }) {
                    FoldGuidance.WINDOW_TOO_SMALL
                } else {
                    null
                }
            if (guidance != null) {
                FoldGameGuidance(guidance, { onResult { } })
                return@BoxWithConstraints
            }
            val console =
                FoldGeometry.lowerConsole(
                    layout.lower,
                    minOf(with(density) { 172.dp.roundToPx() }, width / 4),
                )
            val centerPanel =
                FoldGeometry.displayPanel(
                    console.center,
                    with(density) { 6.dp.roundToPx() },
                    with(density) { 24.dp.roundToPx() },
                )
            // This Activity is translucent: retain the actual paused native display above
            // the crease, rather than replacing it with a placeholder or another core.
            ConsoleRegion(layout.upper, "emuui_menu_upper") {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(8.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Text("Paused · $gameTitle", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
            ConsoleRegion(layout.lower) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            }
            for ((isLeft, region) in listOf(true to console.leftControls, false to console.rightControls)) {
                val tag = if (isLeft) "emuui_menu_left_controls" else "emuui_menu_right_controls"
                ConsoleRegion(region, tag) {
                    LauncherControlWing(
                        left = isLeft,
                        libraryActive = false,
                        canNavigate = true,
                        canPlay = true,
                        hasGame = true,
                        onNavigate = navigate,
                        onPlay = activate,
                        onBack = onBack,
                        onMenu = onOptions,
                        onSearch = onMenuHome,
                        onCycleFilter = cycleFocus,
                        onStart = { onResult { } },
                        startDescription = "Start, resume game",
                        searchDescription = "X, game menu home",
                        menuDescription = "Y, core options",
                    )
                }
            }
            ConsoleRegion(centerPanel.bounds, "emuui_menu_center") {
                Surface(
                    modifier =
                        Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                            .focusRequester(centerFocus)
                            .onFocusChanged { centerHasFocus.value = it.hasFocus }
                            .focusProperties { exit = { FocusRequester.Cancel } }.focusGroup(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(24.dp),
                ) {
                    val dialogBounds =
                        centerPanel.bounds.copy(
                            left = centerPanel.bounds.left + root.value.x,
                            top = centerPanel.bounds.top + root.value.y,
                            right = centerPanel.bounds.right + root.value.x,
                            bottom = centerPanel.bounds.bottom + root.value.y,
                        )
                    CompositionLocalProvider(
                        LocalConsoleDialogRegion provides
                            ConsoleDialogRegion(
                                dialogBounds,
                                console.leftControls.translated(root.value.x, root.value.y),
                                console.rightControls.translated(root.value.x, root.value.y),
                            ),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) { content() }
                    }
                }
            }
            ConsoleRegion(raw.hinge, "emuui_menu_hinge") {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface))
            }
        }
    }

    private fun onResult(block: Intent.() -> Unit) {
        val resultIntent = Intent()
        resultIntent.block()
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    @dagger.Module
    abstract class Module
}
