package com.swordfish.lemuroid.app.mobile.screenshots

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldBounds
import com.swordfish.lemuroid.app.mobile.feature.emuui.FoldGeometry
import com.swordfish.lemuroid.app.mobile.feature.game.ConsoleRegion
import com.swordfish.lemuroid.app.mobile.feature.game.GameOverlayActions
import com.swordfish.lemuroid.app.mobile.feature.game.PadContainer
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuHomeScreen
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuRoute
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuSideMenu
import com.swordfish.lemuroid.app.mobile.feature.home.ConsoleHomeScreen
import com.swordfish.lemuroid.app.mobile.feature.home.HomeViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LocalBlurAvailable
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.touchinput.radial.LemuroidPadTheme
import com.swordfish.touchinput.radial.LocalLemuroidPadTheme
import com.swordfish.touchinput.radial.sensors.TiltConfiguration
import com.swordfish.touchinput.radial.settings.TouchControllerID
import com.swordfish.touchinput.radial.settings.TouchControllerSettingsManager
import gg.padkit.PadKit
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Duration

/**
 * Renders the real Compose UI for store listings and documentation. It is skipped unless the
 * build passes -PemuuiScreenshots. Every game is an original placeholder: no commercial titles,
 * cover art, console names or logos. The in-game frame is original pixel art standing in for
 * the player's own game, and the menu's blurred backdrop stands in for Android's window blur.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = Application::class)
class StoreScreenshots {
    @get:Rule
    val windowInfo = WindowLayoutInfoPublisherRule()

    private val outputDir = File(System.getProperty("emuui.screenshotDir") ?: "build/store-renders")

    /** A window, in dp, with an optional horizontal crease through its middle. */
    private data class Device(val name: String, val widthDp: Int, val heightDp: Int, val dpi: Int, val crease: Boolean)

    private val devices =
        listOf(
            // Pixel 11 Pro Fold inner display, landscape, 2152 x 2076 px; 390 dpi as on the 9 and 10 Pro Fold.
            Device("fold-inner", 883, 852, 390, crease = true),
            // Galaxy Z Fold7-class inner display, landscape, 2184 x 1968 px at 420 dpi.
            Device("bookfold-inner", 832, 750, 420, crease = true),
            // Clamshell inner display turned to portrait with the crease left to right, 1080 x 2520 px.
            Device("clamshell", 411, 960, 420, crease = true),
            // Store canvases rendered directly at 16:9 so nothing is padded or stretched.
            Device("tablet-16x9", 1280, 720, 320, crease = true),
            Device("phone-16x9", 768, 432, 400, crease = false),
        )

    @Test
    fun renderStoreScreens() {
        assumeTrue(
            "Pass -PemuuiScreenshots to render store screenshots",
            System.getProperty("emuui.screenshots") == "true",
        )
        outputDir.mkdirs()
        for (device in devices) {
            render(device, "home") { HomeScene(SampleLibrary.games, selected = SampleLibrary.games[1].id) }
            render(device, "welcome") { HomeScene(emptyList(), selected = null) }
            render(device, "game") { GameScene() }
            render(device, "menu") { MenuScene() }
        }
    }

    private fun render(
        device: Device,
        scene: String,
        content: @Composable () -> Unit,
    ) {
        val orientation = if (device.widthDp > device.heightDp) "land" else "port"
        RuntimeEnvironment.setQualifiers("w${device.widthDp}dp-h${device.heightDp}dp-$orientation-${device.dpi}dpi")
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        val activity = controller.get()
        activity.setContent {
            AppTheme {
                // The captured window cannot query SurfaceFlinger, so render as on a blur-capable device.
                CompositionLocalProvider(LocalBlurAvailable provides true) { content() }
            }
        }
        // The test publisher does not replay, so publish once the posture collector is listening.
        shadowOf(Looper.getMainLooper()).idle()
        windowInfo.overrideWindowLayoutInfo(
            if (device.crease) {
                TestWindowLayoutInfo(
                    listOf(
                        androidx.window.testing.layout.FoldingFeature(
                            activity = activity,
                            size = 0,
                            state = FoldingFeature.State.HALF_OPENED,
                            orientation = FoldingFeature.Orientation.HORIZONTAL,
                        ),
                    ),
                )
            } else {
                TestWindowLayoutInfo()
            },
        )
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(3))
        val view = activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        var copied = false
        PixelCopy.request(
            activity.window,
            bitmap,
            { copied = it == PixelCopy.SUCCESS },
            Handler(Looper.getMainLooper()),
        )
        shadowOf(Looper.getMainLooper()).idle()
        check(copied) { "PixelCopy failed for ${device.name}-$scene" }
        File(
            outputDir,
            "${device.name}-$scene.png",
        ).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        controller.pause().stop().destroy()
    }
}

@Composable
private fun HomeScene(
    games: List<Game>,
    selected: Int?,
) {
    ConsoleHomeScreen(
        modifier = Modifier,
        state = HomeViewModel.UIState(allGames = games, isLoading = false, indexInProgress = false),
        selectedGameId = selected,
        onGameSelected = {},
        onPlay = {},
        onGameOptions = {},
        onImport = {},
        onRetry = {},
        onOpenSettings = {},
        onOpenSystems = {},
        onOpenSearch = {},
        onOpenHelp = {},
        onOpenCoreSelection = {},
        onSyncSaves = null,
        onEnableNotifications = {},
        onEnableMicrophone = {},
    )
}

/** The handheld layout of the game screen: display above the crease, controls below. */
@Composable
private fun GameScene() {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val density = LocalDensity.current
        val layout =
            FoldGeometry.resolve(
                constraints.maxWidth,
                constraints.maxHeight,
                FoldBounds(
                    0,
                    constraints.maxHeight / 2,
                    constraints.maxWidth,
                    constraints.maxHeight / 2,
                    com.swordfish.lemuroid.app.mobile.feature.emuui.FoldAxis.HORIZONTAL,
                ),
                with(density) { 8.dp.roundToPx() },
            )
        ConsoleRegion(layout.upper) {
            HomebrewFrame(Modifier.fillMaxSize().wrapContentSize().aspectRatio(4f / 3f))
            GameOverlayActions(onMenu = {}, accessibleControls = false, onToggleAccessibleControls = {})
        }
        run {
            val pads = TouchControllerID.getConfig(TouchControllerID.SNES)
            val settings = TouchControllerSettingsManager.Settings()
            CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {
                PadKit(Modifier.fillMaxSize()) {
                    ConsoleRegion(layout.lower.copy(right = layout.lower.width / 2)) {
                        PadContainer(Modifier.fillMaxSize())
                        pads.leftComposable(this@PadKit, Modifier.fillMaxSize(), settings)
                    }
                    ConsoleRegion(layout.lower.copy(left = layout.lower.width / 2)) {
                        PadContainer(Modifier.fillMaxSize())
                        pads.rightComposable(this@PadKit, Modifier.fillMaxSize(), settings)
                    }
                }
            }
        }
    }
}

/** The pause menu over the game. On a device the window manager blurs the paused frame. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MenuScene() {
    Box(Modifier.fillMaxSize()) {
        // Stand-in for the window manager's 34dp blur and 30% dim behind the menu window.
        Box(Modifier.fillMaxSize().blur(34.dp, BlurredEdgeTreatment.Rectangle)) { GameScene() }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        GameMenuSideMenu {
            TopAppBar(
                title = {
                    Text(
                        stringResource(GameMenuRoute.HOME.titleId),
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Close, null)
                    }
                },
            )
            GameMenuHomeScreen(rememberNavController(), SampleLibrary.menuRequest, ::ignoreResult)
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun ignoreResult(block: Intent.() -> Unit) = Unit

private object SampleLibrary {
    private const val DEVELOPER = "Original homebrew sample"

    // Original titles invented for these screenshots. Any resemblance to real games is unintended.
    val games =
        listOf(
            "Tidepool Courier" to true,
            "Lantern Grove" to true,
            "Paper Kite Rally" to false,
            "Quartz Garden" to false,
            "Velvet Signal" to true,
            "Moss Lane Mysteries" to false,
            "Nimbus Post Office" to false,
            "Pocket Lighthouse" to false,
        ).mapIndexed { index, (title, favorite) ->
            Game(
                id = index + 1,
                fileName = "sample-$index.zip",
                fileUri = "content://emuui.sample/$index",
                title = title,
                systemId = "dos",
                developer = DEVELOPER,
                coverFrontUrl = null,
                lastIndexedAt = 0L,
                lastPlayedAt = if (index < 4) 1_000L - index else null,
                isFavorite = favorite,
            )
        }

    val menuRequest =
        GameMenuActivity.GameMenuRequest(
            coreOptions = emptyList(),
            advancedCoreOptions = emptyList(),
            game = games[1],
            coreConfig = SystemCoreConfig(CoreID.DOSBOX_PURE, hashMapOf()),
            audioEnabled = true,
            fastForwardSupported = true,
            fastForwardEnabled = false,
            numDisks = 1,
            currentDisk = 0,
            currentTiltConfiguration = TiltConfiguration.Disabled,
            allTiltConfigurations = emptyList(),
        )
}

/**
 * Original pixel art at 256 x 192: a lantern-lit grove at dusk. It stands in for the player's own
 * game; nothing here comes from a commercial title.
 */
@Composable
private fun HomebrewFrame(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val px = size.width / 256f

        fun cell(
            x: Int,
            y: Int,
            w: Int,
            h: Int,
            color: Color,
        ) = drawRect(color, Offset(x * px, y * px), Size(w * px, h * px))

        val sky = listOf(Color(0xFF1B2440), Color(0xFF243058), Color(0xFF34406E), Color(0xFF4C4F86), Color(0xFF6E5F96))
        sky.forEachIndexed { band, color -> cell(0, band * 20, 256, 20, color) }
        cell(0, 100, 256, 92, Color(0xFF6E5F96))
        // Moon and stars.
        cell(196, 22, 18, 18, Color(0xFFF4EFC8))
        cell(192, 26, 26, 10, Color(0xFFF4EFC8))
        cell(200, 18, 10, 26, Color(0xFFF4EFC8))
        listOf(18 to 14, 44 to 30, 70 to 10, 110 to 24, 150 to 12, 236 to 50, 128 to 44, 30 to 56).forEach { (x, y) ->
            cell(x, y, 2, 2, Color(0xFFE8E4FF))
        }
        // Far hills, near hills, ground.
        for (x in 0 until 256 step 8) {
            val far = 98 + ((x / 8) % 5) * 3 - (if ((x / 16) % 3 == 0) 8 else 0)
            cell(x, far, 8, 192 - far, Color(0xFF3F5B6E))
        }
        for (x in 0 until 256 step 8) {
            val near = 126 + ((x / 8 + 2) % 4) * 4
            cell(x, near, 8, 192 - near, Color(0xFF2F4A3B))
        }
        cell(0, 160, 256, 32, Color(0xFF486B35))
        cell(0, 160, 256, 4, Color(0xFFD0EB82))
        for (x in 0 until 256 step 16) cell(x + 4, 170 + (x / 16 % 2) * 6, 6, 2, Color(0xFF3A5A2A))
        // Pines.
        listOf(24, 58, 170, 222).forEach { tx ->
            for (row in 0 until 7) cell(tx - row * 2, 112 + row * 6, 8 + row * 4, 6, Color(0xFF213A2C))
            cell(tx + 2, 154, 4, 6, Color(0xFF4A3424))
        }
        // Lanterns on a string.
        for (x in 40 until 220 step 2) cell(x, 92 + ((x - 130) * (x - 130)) / 900, 2, 1, Color(0xFF8F86B8))
        listOf(64, 100, 136, 172, 208).forEach { lx ->
            val ly = 94 + ((lx - 130) * (lx - 130)) / 900
            cell(lx - 3, ly, 6, 7, Color(0xFFF0CCAD))
            cell(lx - 2, ly + 1, 4, 5, Color(0xFFFFE9A8))
            cell(lx - 6, ly - 2, 12, 11, Color(0x33FFE9A8))
        }
        // The courier, mid-jump, with a parcel.
        val sx = 112
        val sy = 132
        val sprite =
            listOf(
                "..LLLL..",
                ".LLLLLL.",
                ".LSSSSL.",
                ".SSEESS.",
                "..SSSS..",
                ".BBBBBB.",
                "BBBPPBBB",
                "S.BPPB.S",
                "..B..B..",
                ".DD..DD.",
            )
        val palette =
            mapOf(
                'L' to Color(0xFFD0EB82),
                'S' to Color(0xFFF0CCAD),
                'E' to Color(0xFF202D29),
                'B' to Color(0xFF615A79),
                'P' to Color(0xFFF4EFC8),
                'D' to Color(0xFF252D2B),
            )
        sprite.forEachIndexed { row, line ->
            line.forEachIndexed { col, ch -> palette[ch]?.let { cell(sx + col * 2, sy + row * 2, 2, 2, it) } }
        }
        // Coins and a heads-up bar.
        listOf(140 to 120, 152 to 112, 164 to 108).forEach { (x, y) ->
            cell(x, y, 6, 6, Color(0xFFFFD866))
            cell(x + 1, y + 1, 2, 4, Color(0xFFFFF4C2))
        }
        cell(8, 8, 60, 8, Color(0x80202D29))
        repeat(3) { cell(11 + it * 9, 10, 6, 4, Color(0xFFF0CCAD)) }
        cell(196, 8, 52, 8, Color(0x80202D29))
        repeat(5) { cell(199 + it * 9, 10, 6, 4, Color(0xFFFFD866)) }
    }
}
