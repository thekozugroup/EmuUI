package com.swordfish.lemuroid.app.mobile.feature.game

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.swordfish.lemuroid.app.mobile.feature.emuui.WindowCutoutSnapshot
import com.swordfish.lemuroid.app.mobile.feature.emuui.allowDisplayCutouts
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel

class GameActivity : BaseGameActivity() {
    private var displayCutout by mutableStateOf(WindowCutoutSnapshot())

    override fun hideSystemUI() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            show(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        window.allowDisplayCutouts()
        super.onCreate(savedInstanceState)
        // Observe real delivered insets before descendants consume them. Returning them
        // unchanged lets Compose independently protect menus, buttons and the lower pane.
        val content = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            displayCutout = WindowCutoutSnapshot.from(view, insets)
            insets
        }
        ViewCompat.getRootWindowInsets(content)?.let { displayCutout = WindowCutoutSnapshot.from(content, it) }
        ViewCompat.requestApplyInsets(content)
    }

    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        val lightStatusBar = MaterialTheme.colorScheme.surface.luminance() > 0.5f
        SideEffect {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = lightStatusBar
        }
        MobileGameScreen(viewModel, displayCutout)
    }

    override fun getDialogClass() = GameMenuActivity::class.java
}
