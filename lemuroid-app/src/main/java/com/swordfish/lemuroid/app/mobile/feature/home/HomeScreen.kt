package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    selectedGameId: Int?,
    onGameSelected: (Game) -> Unit,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSystems: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenHelp: () -> Unit,
    onSyncSaves: (() -> Unit)? = null,
    libraryActive: Boolean = true,
    onBack: (() -> Unit)? = null,
    centerContent: @Composable (library: @Composable () -> Unit) -> Unit = { it() },
) {
    val context = LocalContext.current
    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.updatePermissions(context.applicationContext)
    }
    // A declined optional permission never ejects the user into Android settings.
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.updatePermissions(context.applicationContext)
        }
    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState()).value
    ConsoleHomeScreen(
        modifier = modifier,
        libraryActive = libraryActive,
        onBack = onBack,
        centerContent = centerContent,
        state = state,
        selectedGameId = selectedGameId,
        onGameSelected = onGameSelected,
        onPlay = onGameClick,
        onGameOptions = onGameLongClick,
        onImport = { viewModel.changeLocalStorageFolder(context) },
        onRetry = viewModel::retry,
        onOpenSettings = onOpenSettings,
        onOpenSystems = onOpenSystems,
        onOpenSearch = onOpenSearch,
        onOpenHelp = onOpenHelp,
        onOpenCoreSelection = onOpenCoreSelection,
        onSyncSaves = onSyncSaves,
        onEnableNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onEnableMicrophone = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
    )
}
