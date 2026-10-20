package com.swordfish.lemuroid.app.mobile.feature.gamemenu.states

import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.gamemenu.GameMenuActivity
import com.swordfish.lemuroid.app.shared.gamemenu.GameMenuHelper
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.saves.StatesPreviewManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class GameMenuStatesViewModel(
    private val application: Application,
    private val gameMenuRequest: GameMenuActivity.GameMenuRequest,
    private val statesManager: StatesManager,
    private val disableMissingEntries: Boolean,
    private val statesPreviewManager: StatesPreviewManager,
) : ViewModel() {
    class Factory(
        private val application: Application,
        private val gameMenuRequest: GameMenuActivity.GameMenuRequest,
        private val statesManager: StatesManager,
        private val disableMissingEntries: Boolean,
        private val statesPreviewManager: StatesPreviewManager,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GameMenuStatesViewModel(
                application,
                gameMenuRequest,
                statesManager,
                disableMissingEntries,
                statesPreviewManager,
            ) as T
        }
    }

    data class StateEntry(
        val title: String,
        val description: String,
        val enabled: Boolean,
        val preview: Bitmap?,
    )

    data class State(
        val entries: List<StateEntry> = emptyList(),
        val isLoading: Boolean = true,
        val hasError: Boolean = false,
    )

    private val mutableState = MutableStateFlow(State())
    val uiStates = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        retry()
    }

    @MainThread
    fun retry() {
        loadJob?.cancel()
        mutableState.value = State()
        loadJob =
            viewModelScope.launch {
                try {
                    val entries =
                        withContext(Dispatchers.IO) {
                            val slotsInfo =
                                statesManager.getSavedSlotsInfo(
                                    gameMenuRequest.game,
                                    gameMenuRequest.coreConfig.coreID,
                                )

                            slotsInfo.mapIndexed { index, slotInfo ->
                                val title =
                                    application.applicationContext.getString(
                                        R.string.game_menu_state,
                                        (index + 1).toString(),
                                    )
                                val description =
                                    if (slotInfo.exists) {
                                        GameMenuHelper.getSaveStateDescription(
                                            slotInfo,
                                        )
                                    } else {
                                        "Empty slot"
                                    }
                                val isEnabled = !disableMissingEntries || slotInfo.exists
                                val preview =
                                    try {
                                        GameMenuHelper.getSaveStateBitmap(
                                            application.applicationContext,
                                            statesPreviewManager,
                                            slotInfo,
                                            gameMenuRequest.game,
                                            gameMenuRequest.coreConfig.coreID,
                                            index,
                                        )
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (exception: Exception) {
                                        // A missing/corrupt thumbnail must not hide a usable save slot.
                                        Timber.w(exception, "Could not read save preview for slot %s", index + 1)
                                        null
                                    }

                                StateEntry(title, description, isEnabled, preview)
                            }
                        }

                    currentCoroutineContext().ensureActive()
                    mutableState.value = State(entries = entries, isLoading = false)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    currentCoroutineContext().ensureActive()
                    Timber.w(exception, "Could not read save slots")
                    mutableState.value = State(isLoading = false, hasError = true)
                }
            }
    }
}
