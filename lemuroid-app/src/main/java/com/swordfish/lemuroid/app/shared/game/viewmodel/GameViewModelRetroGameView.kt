package com.swordfish.lemuroid.app.shared.game.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.swordfish.lemuroid.BuildConfig
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.settings.SettingsManager
import com.swordfish.lemuroid.app.shared.game.GameRecreationCheckpoint
import com.swordfish.lemuroid.app.shared.game.ShaderChooser
import com.swordfish.lemuroid.app.shared.rumble.RumbleManager
import com.swordfish.lemuroid.app.shared.settings.HDModeQuality
import com.swordfish.lemuroid.common.coroutines.MutableStateProperty
import com.swordfish.lemuroid.common.coroutines.launchOnState
import com.swordfish.lemuroid.common.view.disableTouchEvents
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.game.GameLoader
import com.swordfish.lemuroid.lib.game.GameLoaderError
import com.swordfish.lemuroid.lib.game.GameLoaderException
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.RomFiles
import com.swordfish.libretrodroid.GLRetroView
import com.swordfish.libretrodroid.GLRetroViewData
import com.swordfish.libretrodroid.ImmersiveMode
import com.swordfish.libretrodroid.Variable
import com.swordfish.libretrodroid.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class GameViewModelRetroGameView(
    private val appContext: Context,
    private val system: GameSystem,
    private val systemCoreConfig: SystemCoreConfig,
    private val settingsManager: SettingsManager,
    private val coreVariablesManager: CoreVariablesManager,
    private val sideEffects: GameViewModelSideEffects,
    private val rumbleManager: RumbleManager,
    private val scope: CoroutineScope,
) : DefaultLifecycleObserver {
    sealed interface GameState {
        data object Uninitialized : GameState

        data class Loading(val message: String) : GameState

        data class Loaded(
            val gameData: GameLoader.GameData,
            val retroViewData: GLRetroViewData,
        ) : GameState

        data object Ready : GameState
    }

    private val gameState: MutableStateFlow<GameState> = MutableStateFlow(GameState.Uninitialized)
    private var hasRenderedFrame = false
    private var frameReadyJob: Job? = null
    private var restoringView: GLRetroView? = null
    private var nativeLifecycle: Lifecycle? = null
    private var retainedGameData: GameLoader.GameData? = null
    private val recreationCheckpoint = GameRecreationCheckpoint<RecreationSnapshot>()

    private data class RecreationSnapshot(
        val sram: ByteArray?,
        val state: ByteArray?,
        val audioEnabled: Boolean,
        val frameSpeed: Int,
        val disk: Int,
    )

    val acceptsTouchInput: Boolean get() = system.hasTouchScreen
    val canCaptureState: Boolean
        get() =
            hasRenderedFrame && restoringView == null && gameState.value == GameState.Ready &&
                nativeLifecycle?.currentState?.isAtLeast(Lifecycle.State.CREATED) == true
    val isNativeViewResumed: Boolean
        get() = nativeLifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true

    fun existingRetroView(): GLRetroView? = if (gameState.value == GameState.Ready) retroGameView else null

    /** Called before Activity.onStop dispatch, while the paused core still exists. */
    fun prepareActivityRecreation(captureLiveState: Boolean = true) {
        val view = retroGameView
        val snapshot =
            if (captureLiveState && view != null && hasRenderedFrame && restoringView !== view) {
                RecreationSnapshot(
                    sram = runCatching { view.serializeSRAM(false) }.onFailure { Timber.w(it) }.getOrNull(),
                    state =
                        if (systemCoreConfig.statesSupported) {
                            runCatching { view.serializeState(false) }.onFailure { Timber.w(it) }.getOrNull()
                        } else {
                            null
                        },
                    audioEnabled = view.audioEnabled,
                    frameSpeed = view.frameSpeed,
                    disk = runCatching { view.getCurrentDisk(false) }.getOrDefault(0),
                )
            } else {
                null
            }
        recreationCheckpoint.beginRecreation(snapshot)
        frameReadyJob?.cancel()
        frameReadyJob = null
        restoringView = null
        // Re-open ROM/VFS handles through GameLoader. They were consumed by the old core.
        hasRenderedFrame = false
        retainedGameData = null
        nativeLifecycle = null
        retroGameView = null
        gameState.value = GameState.Uninitialized
    }

    suspend fun restoreActivityRecreation(): Boolean {
        val ticket = recreationCheckpoint.beginRestore() ?: return false
        val snapshot = ticket.snapshot
        val view = retroGameViewFlow()
        view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
        if (retroGameView !== view || !recreationCheckpoint.isCurrent(ticket)) return true
        var restoredState = false
        runCatching {
            snapshot.sram?.let { view.unserializeSRAM(it) }
            if (snapshot.disk > 0) view.changeDisk(snapshot.disk)
            snapshot.state?.let { state ->
                restoredState = view.unserializeState(state)
                if (!restoredState) Timber.w("Core declined recreation state; trying the persisted autosave")
            }
            view.audioEnabled = snapshot.audioEnabled
            view.frameSpeed = snapshot.frameSpeed
            // Do not consume a checkpoint before the new surface is usable. A repeated
            // recreate cancels the waiting job and leaves this checkpoint for the next view.
            if (restoredState || snapshot.state == null) recreationCheckpoint.complete(ticket)
        }.onFailure { Timber.e(it, "Unable to restore activity recreation snapshot") }
        // A missing/declined state must not suppress the normal persisted-autosave path.
        return restoredState
    }

    fun completeInitialization(view: GLRetroView) {
        if (retroGameView !== view) return
        restoringView = null
        // A failed live-state restore may have fallen back to the persisted autosave.
        recreationCheckpoint.beginRestore()?.let { recreationCheckpoint.complete(it) }
    }

    private val retroGameViewFlow = MutableStateFlow<GLRetroView?>(null)
    var retroGameView: GLRetroView? by MutableStateProperty(retroGameViewFlow)

    fun getGameState(): Flow<GameState> {
        return gameState.debounce(200)
    }

    suspend fun initialize(
        applicationContext: Context,
        game: Game,
        systemCoreConfig: SystemCoreConfig,
        gameLoader: GameLoader,
        requestLoadSave: Boolean,
    ) {
        val currentState = gameState.value
        if (currentState != GameState.Uninitialized) return

        val autoSaveEnabled = settingsManager.autoSave()
        val filter = settingsManager.screenFilter()
        val hdMode = settingsManager.hdMode()
        val hdModeQuality = settingsManager.hdModeQuality()
        val lowLatencyAudio = settingsManager.lowLatencyAudio()
        val enableRumble = settingsManager.enableRumble()
        val directLoad = settingsManager.allowDirectGameLoad()
        val enableImmersiveMode = settingsManager.enableImmersiveMode()

        val hasMicrophonePermission =
            ContextCompat.checkSelfPermission(
                applicationContext,
                android.Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED

        val enableMicrophone = systemCoreConfig.supportsMicrophone && hasMicrophonePermission

        val loadingStatesFlow =
            gameLoader.load(
                applicationContext,
                game,
                requestLoadSave && autoSaveEnabled,
                systemCoreConfig,
                directLoad,
            )

        loadingStatesFlow
            .flowOn(Dispatchers.IO)
            .catch {
                val message =
                    if (it is GameLoaderException) {
                        getErrorMessage(it.error)
                    } else {
                        ""
                    }
                sideEffects.requestFailureFinish(message)
            }
            .debounce(200)
            .collect { loadingState ->
                gameState.value =
                    if (loadingState is GameLoader.LoadingState.Ready) {
                        Timber.i("Setting state to loaded")
                        val retroViewData =
                            buildRetroViewData(
                                applicationContext,
                                systemCoreConfig,
                                loadingState.gameData,
                                hdMode,
                                hdModeQuality,
                                filter,
                                lowLatencyAudio,
                                enableRumble,
                                enableMicrophone,
                                enableImmersiveMode,
                            )
                        GameState.Loaded(
                            gameData = loadingState.gameData,
                            retroViewData = retroViewData,
                        )
                    } else {
                        GameState.Loading(getLoadingMessage(loadingState))
                    }
            }
    }

    fun createRetroView(
        context: Context,
        lifecycle: LifecycleOwner,
    ): Pair<GameLoader.GameData, GLRetroView> {
        val existing = existingRetroView()
        if (existing != null && retainedGameData != null) {
            (existing.parent as? ViewGroup)?.removeView(existing)
            return retainedGameData!! to existing
        }
        val currentState = gameState.value
        if (currentState !is GameState.Loaded) throw IllegalStateException("Game is not loaded.")

        // Mobile presentation overrides must be present at native create/load,
        // before any frame or save-state restore can expose a different DS layout.
        currentState.retroViewData.variables =
            (currentState.retroViewData.variables.toList() + consoleLayoutVariables.map { Variable(it.key, it.value) })
                .associateBy { it.key }.values.toTypedArray()
        val result =
            GLRetroView(context, currentState.retroViewData)
                .apply {
                    isFocusable = false
                    isFocusableInTouchMode = false
                }

        if (!system.hasTouchScreen) {
            result.disableTouchEvents()
        }

        nativeLifecycle = lifecycle.lifecycle
        lifecycle.lifecycle.addObserver(result)

        if (BuildConfig.DEBUG) {
            runCatching {
                printRetroVariables(result)
            }
        }

        retainedGameData = currentState.gameData
        retroGameViewFlow.value = result
        gameState.value = GameState.Ready
        restoringView = result
        frameReadyJob?.cancel()
        frameReadyJob =
            scope.launch {
                result.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
                if (retroGameView === result) hasRenderedFrame = true
            }

        return currentState.gameData to result
    }

    suspend fun retroGameViewFlow() =
        retroGameViewFlow
            .filterNotNull()
            .first()

    suspend fun waitRetroGameViewInitialized() {
        retroGameViewFlow()
    }

    suspend inline fun <reified T> waitGLEvent() {
        val retroView = retroGameViewFlow()
        retroView.getGLRetroEvents()
            .filterIsInstance<T>()
            .first()
    }

    private fun buildRetroViewData(
        appContext: Context,
        systemCoreConfig: SystemCoreConfig,
        gameData: GameLoader.GameData,
        hdMode: Boolean,
        hdModeQuality: HDModeQuality,
        screenFilter: String,
        lowLatencyAudio: Boolean,
        requestRumble: Boolean,
        requestMicrophone: Boolean,
        enableImmersiveMode: Boolean,
    ): GLRetroViewData {
        return GLRetroViewData(appContext).apply {
            coreFilePath = gameData.coreLibrary

            when (val gameFiles = gameData.gameFiles) {
                is RomFiles.Standard -> {
                    gameFilePath = gameFiles.files.first().absolutePath
                }

                is RomFiles.Virtual -> {
                    gameVirtualFiles = gameFiles.files.map { VirtualFile(it.filePath, it.fd) }
                }
            }

            systemDirectory = gameData.systemDirectory.absolutePath
            savesDirectory = gameData.savesDirectory.absolutePath
            variables = gameData.coreVariables.map { Variable(it.key, it.value) }.toTypedArray()
            saveRAMState = gameData.saveRAMData
            shader =
                ShaderChooser.getShaderForSystem(
                    appContext,
                    hdMode,
                    hdModeQuality,
                    screenFilter,
                    GameSystem.findById(gameData.game.systemId),
                )
            preferLowLatencyAudio = lowLatencyAudio
            rumbleEventsEnabled = requestRumble
            skipDuplicateFrames = systemCoreConfig.skipDuplicateFrames
            enableMicrophone = requestMicrophone
            immersiveMode = buildImmersiveModeConfiguration(enableImmersiveMode)
        }
    }

    private fun buildImmersiveModeConfiguration(enableImmersiveMode: Boolean): ImmersiveMode? {
        return if (enableImmersiveMode) {
            ImmersiveMode(blendFactor = 0.05f)
        } else {
            null
        }
    }

    private fun getLoadingMessage(loadingState: GameLoader.LoadingState): String {
        return when (loadingState) {
            is GameLoader.LoadingState.LoadingCore -> {
                appContext.getString(com.swordfish.lemuroid.ext.R.string.game_loading_download_core)
            }

            is GameLoader.LoadingState.LoadingGame -> {
                appContext.getString(R.string.game_loading_preparing_game)
            }

            else -> ""
        }
    }

    private fun printRetroVariables(retroGameView: GLRetroView) {
        scope.launch {
            // Some cores do not immediately call SET_VARIABLES so we might need to wait a little bit
            delay(1.seconds)
            retroGameView.getVariables().forEach {
                Timber.i("Libretro variable: $it")
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        owner.launchOnState(Lifecycle.State.STARTED) {
            initializeRetroGameViewErrorsFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeCoreVariablesFlow()
        }

        owner.launchOnState(Lifecycle.State.RESUMED) {
            initializeRumbleFlow()
        }
    }

    private suspend fun initializeCoreVariablesFlow() {
        try {
            waitRetroGameViewInitialized()
            val options = coreVariablesManager.getOptionsForCore(system.id, systemCoreConfig)
            updateCoreVariables(options)
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private suspend fun initializeRumbleFlow() {
        val retroGameView = retroGameViewFlow()
        val rumbleEvents = retroGameView.getRumbleEvents()
        rumbleManager.collectAndProcessRumbleEvents(systemCoreConfig, rumbleEvents)
    }

    private suspend fun initializeRetroGameViewErrorsFlow() {
        retroGameViewFlow().getGLRetroErrors()
            .catch { Timber.e(it, "Exception in GLRetroErrors. Ironic.") }
            .collect { handleRetroViewError(it) }
    }

    // Presentation-only overrides. Keep user preferences and save-state data untouched,
    // and reapply these after returning from the game menu / activity resume.
    private var consoleLayoutVariables = emptyList<CoreVariable>()

    fun setConsoleScreenGap(gap: Int) {
        consoleLayoutVariables =
            when (systemCoreConfig.coreID) {
                CoreID.MELONDS ->
                    listOf(
                        CoreVariable("melonds_screen_layout1", "top-bottom"),
                        CoreVariable("melonds_number_of_screen_layouts", "1"),
                        CoreVariable("melonds_screen_gap", gap.coerceIn(0, 126).toString()),
                    )
                CoreID.DESMUME ->
                    listOf(
                        CoreVariable("desmume_screens_layout", "top/bottom"),
                        CoreVariable("desmume_screens_gap", gap.coerceIn(0, 100).toString()),
                    )
                else -> emptyList()
            }
        updateCoreVariables(emptyList())
    }

    private fun updateCoreVariables(options: List<CoreVariable>) {
        val updatedVariables =
            (options + consoleLayoutVariables).associateBy { it.key }.values.map { Variable(it.key, it.value) }
                .toTypedArray()

        updatedVariables.forEach {
            Timber.i("Updating core variable: ${it.key} ${it.value}")
        }

        retroGameView?.updateVariables(*updatedVariables)
    }

    private fun handleRetroViewError(errorCode: Int) {
        Timber.e("Error in GLRetroView $errorCode")
        val gameLoaderError =
            when (errorCode) {
                GLRetroView.ERROR_GL_NOT_COMPATIBLE -> GameLoaderError.GLIncompatible
                GLRetroView.ERROR_LOAD_GAME -> GameLoaderError.LoadGame
                GLRetroView.ERROR_LOAD_LIBRARY -> GameLoaderError.LoadCore
                GLRetroView.ERROR_SERIALIZATION -> GameLoaderError.Saves
                else -> GameLoaderError.Generic
            }

        sideEffects.requestFailureFinish(getErrorMessage(gameLoaderError))
    }

    private fun getErrorMessage(gameError: GameLoaderError): String {
        val message =
            when (gameError) {
                is GameLoaderError.GLIncompatible -> {
                    appContext.getString(R.string.game_loader_error_gl_incompatible)
                }
                is GameLoaderError.Generic -> {
                    appContext.getString(R.string.game_loader_error_generic)
                }
                is GameLoaderError.LoadCore -> {
                    appContext.getString(com.swordfish.lemuroid.ext.R.string.game_loader_error_load_core)
                }
                is GameLoaderError.LoadGame -> {
                    appContext.getString(R.string.game_loader_error_load_game)
                }
                is GameLoaderError.Saves -> {
                    appContext.getString(R.string.game_loader_error_save)
                }
                is GameLoaderError.UnsupportedArchitecture -> {
                    appContext.getString(R.string.game_loader_error_unsupported_architecture)
                }
                is GameLoaderError.MissingBiosFiles -> {
                    appContext.getString(R.string.game_loader_error_missing_bios, gameError.missingFiles)
                }
            }

        return message
    }
}
