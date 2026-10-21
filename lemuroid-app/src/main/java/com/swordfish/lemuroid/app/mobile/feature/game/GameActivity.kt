package com.swordfish.lemuroid.app.mobile.feature.game

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import com.swordfish.lemuroid.app.mobile.feature.emuui.CreaseOrientationController
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity
import com.swordfish.lemuroid.app.shared.game.BaseGameActivity.Companion.EXTRA_SCREEN_ORIENTATION
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel

class GameActivity : BaseGameActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Follow a clamshell launcher into portrait before the first frame, not after a turn.
        val launcherOrientation =
            intent.getIntExtra(
                EXTRA_SCREEN_ORIENTATION,
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            )
        if (savedInstanceState == null && launcherOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
            requestedOrientation = launcherOrientation
        }
        super.onCreate(savedInstanceState)
        CreaseOrientationController.attach(this)
    }

    @Composable
    override fun GameScreen(viewModel: BaseGameScreenViewModel) {
        MobileGameScreen(viewModel)
    }

    override fun getDialogClass() = GameMenuActivity::class.java
}
