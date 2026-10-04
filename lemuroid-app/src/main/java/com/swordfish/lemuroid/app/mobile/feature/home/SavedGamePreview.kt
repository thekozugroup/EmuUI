package com.swordfish.lemuroid.app.mobile.feature.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import java.io.File

/** A saved still image only. Browsing never opens a ROM, starts a core or changes a save. */
@Composable
internal fun SavedGamePreview(
    game: Game?,
    modifier: Modifier = Modifier,
    onPreviewAvailable: (Boolean) -> Unit = {},
    fallback: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // Remembering by identity prevents even a single frame of the previous selected game's image.
    var preview by remember(game?.id, game?.fileUri, game?.fileName, game?.systemId) {
        mutableStateOf<SavedGameplayPreviewResolver.Preview<ImageBitmap>?>(null)
    }
    LaunchedEffect(context, lifecycle, game?.id, game?.fileUri, game?.fileName, game?.systemId, game?.lastPlayedAt) {
        preview = null
        val selected = game ?: return@LaunchedEffect
        val system =
            GameSystem.all().firstOrNull { it.id.dbname == selected.systemId }
                ?: return@LaunchedEffect
        val cores = system.systemCoreConfigs.map { it.coreID.coreName }
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // Returning from gameplay (or recreating the Activity) re-reads the persisted files.
            // No in-memory failure/success cache can hide a new, removed or replaced screenshot.
            try {
                val directory =
                    withContext(Dispatchers.IO) { context.getExternalFilesDir(null) }
                        ?: return@repeatOnLifecycle
                savedPreviewChanges(directory, selected.fileName, cores).collectLatest {
                    preview =
                        withContext(Dispatchers.IO) {
                            SavedGameplayPreviewResolver(File(directory, "states"), File(directory, "state-previews"))
                                .resolve(selected.fileName, cores, ::decodeSavedImage)
                        }
                }
            } catch (_: SecurityException) {
                // External app storage can become unavailable while a launcher remains open.
                preview = null
            } finally {
                preview = null
            }
        }
    }
    SideEffect { onPreviewAvailable(preview != null) }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        val saved = preview
        if (saved == null || game == null) {
            fallback()
        } else {
            Image(
                bitmap = saved.image,
                contentDescription = "Saved gameplay for ${game.title}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().testTag("launcher_saved_gameplay_preview"),
            )
        }
    }
}

private fun decodeSavedImage(bytes: ByteArray): ImageBitmap? {
    // Existing slot previews are JPEGs. Reject incomplete writes before the decoder can accept
    // a truncated JPEG as a partially grey image.
    if (bytes.size < 4 || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte() ||
        bytes[bytes.lastIndex - 1] != 0xff.toByte() || bytes.last() != 0xd9.toByte()
    ) {
        return null
    }
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 ||
            bounds.outWidth.toLong() * bounds.outHeight > 16_777_216L
        ) {
            return null
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1024) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
    } catch (_: RuntimeException) {
        null
    }
}
