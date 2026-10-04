package com.swordfish.lemuroid.app.mobile.feature.home

import android.os.FileObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.File
import java.io.IOException

/** Selected-title changes only, with no polling. Collection lifetime owns every native watch. */
@Suppress("DEPRECATION") // The String constructor also supports the app's pre-29 devices.
internal fun savedPreviewChanges(
    directory: File,
    fileName: String,
    coreNames: List<String>,
): Flow<Unit> =
    flow {
        while (currentCoroutineContext().isActive) {
            val changes = Channel<Unit>(Channel.CONFLATED)
            val watches = mutableListOf<FileObserver>()
            try {
                val stateRoot = File(directory, "states")
                val previewRoot = File(directory, "state-previews")
                val watchedPaths =
                    listOf(
                        directory to setOf("states", "state-previews"),
                        stateRoot to coreNames.toSet(),
                        previewRoot to coreNames.toSet(),
                    ) +
                        coreNames.flatMap { core ->
                            val stateNames = (1..4).map { "$fileName.slot$it" }.toSet()
                            listOf(
                                File(stateRoot, core) to stateNames,
                                File(previewRoot, core) to stateNames.map { "$it.jpg" }.toSet(),
                            )
                        }
                watchedPaths.forEach { (path, names) ->
                    // Do not follow external-storage links outside the application's directory.
                    val safe =
                        try {
                            val root = directory.canonicalFile
                            val target = path.canonicalFile
                            target == root || target.path.startsWith(root.path + File.separator)
                        } catch (_: IOException) {
                            false
                        } catch (_: SecurityException) {
                            false
                        }
                    if (safe && path.isDirectory) {
                        val watch =
                            object : FileObserver(path.absolutePath, PREVIEW_EVENTS) {
                                override fun onEvent(
                                    event: Int,
                                    path: String?,
                                ) {
                                    if (path == null || path in names) changes.trySend(Unit)
                                }
                            }
                        watches += watch
                        watch.startWatching()
                    }
                }
                // Subscribe before resolving, so replacements during a decode cannot be missed.
                emit(Unit)
                changes.receive()
                // Coalesce save/state/image write bursts. Re-create watches on the next turn to
                // include newly created core directories and recover replaced directories.
                delay(100)
            } finally {
                watches.forEach { it.stopWatching() }
                changes.close()
            }
        }
    }.flowOn(Dispatchers.IO)

private const val PREVIEW_EVENTS =
    FileObserver.CLOSE_WRITE or FileObserver.CREATE or FileObserver.DELETE or
        FileObserver.MOVED_FROM or FileObserver.MOVED_TO or FileObserver.DELETE_SELF or
        FileObserver.MOVE_SELF or FileObserver.ATTRIB
