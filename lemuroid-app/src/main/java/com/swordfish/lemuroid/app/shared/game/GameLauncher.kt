package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import android.widget.Toast
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.app.utils.android.displayErrorDialog
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ): Boolean {
        if (!GameSystem.isAvailable(game.systemId)) {
            Toast.makeText(activity, R.string.system_unavailable_this_release, Toast.LENGTH_LONG).show()
            return false
        }
        // Block before starting the native core, including external shortcuts and context actions.
        if (game.systemId == SystemID.NINTENDO_3DS.dbname) {
            Toast.makeText(
                activity,
                "3DS split-screen layout is not supported in EmuUI yet. Nintendo DS uses both screens.",
                Toast.LENGTH_LONG,
            ).show()
            return false
        }
        if (GameProcessLock.isHeldByAnotherProcess(activity.applicationContext)) {
            activity.displayToast(R.string.game_process_another_game_running)
            return false
        }

        GlobalScope.launch {
            try {
                val system = GameSystem.findById(game.systemId)
                val coreConfig = coresSelection.getCoreConfigForSystem(system)
                launchWithBackgroundRecovery(
                    prepare = { gameLaunchTaskHandler.handleGameStart(activity.applicationContext) },
                    launch = {
                        withContext(Dispatchers.Main) {
                            if (!activity.isFinishing && !activity.isDestroyed) {
                                BaseGameActivity.launchGame(activity, coreConfig, game, loadSave, leanback)
                                true
                            } else {
                                false
                            }
                        }
                    },
                    recover = { gameLaunchTaskHandler.handleGameLaunchAborted(activity.applicationContext) },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Timber.e(failure, "Unable to prepare game launch")
                withContext(Dispatchers.Main) {
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.displayErrorDialog(
                            "EmuUI could not prepare this game. Please try starting it again.",
                            activity.getString(R.string.ok),
                        ) {
                            if (activity is ExternalGameLauncherActivity) activity.finish()
                        }
                    }
                }
            }
        }

        return true
    }
}
