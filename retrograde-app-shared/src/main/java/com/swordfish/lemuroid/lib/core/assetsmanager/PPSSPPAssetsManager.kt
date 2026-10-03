package com.swordfish.lemuroid.lib.core.assetsmanager

import android.content.SharedPreferences
import android.net.Uri
import com.swordfish.lemuroid.lib.core.CoreUpdater
import com.swordfish.lemuroid.lib.core.installCoreAssetArchive
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class PPSSPPAssetsManager : CoreID.AssetsManager {
    override suspend fun clearAssets(directoriesManager: DirectoriesManager) {
        getAssetsDirectory(directoriesManager).deleteRecursively()
    }

    override suspend fun retrieveAssetsIfNeeded(
        coreUpdaterApi: CoreUpdater.CoreManagerApi,
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ) {
        if (!updatedRequested(directoriesManager, sharedPreferences)) {
            return
        }

        withContext(Dispatchers.IO) {
            val destination = getAssetsDirectory(directoriesManager)
            val response = coreUpdaterApi.downloadFile(PPSSPP_ASSETS_URL.toString())
            installCoreAssetArchive(response, destination)
            val versionSaved =
                sharedPreferences.edit()
                    .putString(PPSSPP_ASSETS_VERSION_KEY, PPSSPP_ASSETS_VERSION)
                    .commit()
            if (!versionSaved) throw IOException("Could not save core asset version")
        }
    }

    private suspend fun updatedRequested(
        directoriesManager: DirectoriesManager,
        sharedPreferences: SharedPreferences,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val directoryExists = getAssetsDirectory(directoriesManager).exists()

            val currentVersion = sharedPreferences.getString(PPSSPP_ASSETS_VERSION_KEY, "none")
            val hasCurrentVersion = currentVersion == PPSSPP_ASSETS_VERSION

            !directoryExists || !hasCurrentVersion
        }

    private suspend fun getAssetsDirectory(directoriesManager: DirectoriesManager): File {
        return withContext(Dispatchers.IO) {
            File(directoriesManager.getSystemDirectory(), PPSSPP_ASSETS_FOLDER_NAME)
        }
    }

    companion object {
        const val PPSSPP_ASSETS_VERSION = "1.15"

        val PPSSPP_ASSETS_URL: Uri =
            Uri.parse("https://github.com/Swordfish90/LemuroidCores/")
                .buildUpon()
                .appendEncodedPath("raw/$PPSSPP_ASSETS_VERSION/assets/ppsspp.zip")
                .build()

        const val PPSSPP_ASSETS_VERSION_KEY = "ppsspp_assets_version_key"

        const val PPSSPP_ASSETS_FOLDER_NAME = "PPSSPP"
    }
}
