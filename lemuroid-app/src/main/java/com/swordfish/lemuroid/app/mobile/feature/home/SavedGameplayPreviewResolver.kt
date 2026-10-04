package com.swordfish.lemuroid.app.mobile.feature.home

import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Read-only access to the existing StatesManager / StatesPreviewManager slot contract.
 * A state cannot be rendered without running its core, so only an already saved image is used.
 * Autosaves currently have no image; they are deliberately not treated as screenshot sources.
 */
internal class SavedGameplayPreviewResolver(
    private val statesDirectory: File,
    private val previewsDirectory: File,
) {
    data class Preview<T>(
        val image: T,
        val slot: Int,
        val coreName: String,
        val savedAt: Long,
        val revision: String,
    )

    private data class Stamp(val length: Long, val modified: Long)

    private data class Candidate(
        val state: File,
        val screenshot: File,
        val stateStamp: Stamp,
        val screenshotStamp: Stamp,
        val slot: Int,
        val coreName: String,
    )

    /** Decode is injected so selection, invalidation and disk persistence are JVM-testable. */
    fun <T : Any> resolve(
        fileName: String,
        coreNames: List<String>,
        decode: (ByteArray) -> T?,
    ): Preview<T>? {
        if (!isPathSegment(fileName)) return null
        return safely {
            val candidates =
                coreNames.distinct().flatMap { coreName ->
                    val stateFolder = safeChild(statesDirectory, coreName) ?: return@flatMap emptyList()
                    val imageFolder = safeChild(previewsDirectory, coreName) ?: return@flatMap emptyList()
                    (1..SLOT_COUNT).mapNotNull { slot ->
                        val state = safeChild(stateFolder, "$fileName.slot$slot") ?: return@mapNotNull null
                        val screenshot = safeChild(imageFolder, "$fileName.slot$slot.jpg") ?: return@mapNotNull null
                        val stateStamp = stamp(state) ?: return@mapNotNull null
                        val imageStamp = stamp(screenshot) ?: return@mapNotNull null
                        // Saving the state precedes capture. An older image belongs to an overwritten slot.
                        if (imageStamp.modified < stateStamp.modified || imageStamp.length > MAX_IMAGE_BYTES) {
                            return@mapNotNull null
                        }
                        Candidate(state, screenshot, stateStamp, imageStamp, slot, coreName)
                    }
                }.sortedWith(
                    compareByDescending<Candidate> { it.stateStamp.modified }
                        .thenByDescending { it.screenshotStamp.modified }
                        .thenBy { it.coreName }
                        .thenBy { it.slot },
                )

            candidates.firstNotNullOfOrNull { candidate ->
                safely candidateRead@{
                    val bytes =
                        readBounded(candidate.screenshot, candidate.screenshotStamp.length.toInt())
                            ?: return@candidateRead null
                    if (stamp(candidate.state) != candidate.stateStamp ||
                        stamp(candidate.screenshot) != candidate.screenshotStamp
                    ) {
                        return@candidateRead null
                    }
                    val image = decode(bytes) ?: return@candidateRead null
                    if (stamp(candidate.state) != candidate.stateStamp ||
                        stamp(candidate.screenshot) != candidate.screenshotStamp
                    ) {
                        return@candidateRead null
                    }
                    // No filename-only image cache: a replaced slot (even with identical file stats)
                    // must have a new identity. Every resolve re-reads the small image from disk.
                    val digest =
                        MessageDigest.getInstance("SHA-256").digest(bytes)
                            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
                    Preview(
                        image = image,
                        slot = candidate.slot,
                        coreName = candidate.coreName,
                        savedAt = candidate.stateStamp.modified,
                        revision = "${candidate.coreName}:${candidate.slot}:${candidate.stateStamp}:$digest",
                    )
                }
            }
        }
    }

    private fun safeChild(
        parent: File,
        name: String,
    ): File? {
        if (!isPathSegment(name)) return null
        val file = File(parent, name)
        return file.takeIf { it.canonicalFile.parentFile == parent.canonicalFile }
    }

    private fun stamp(file: File): Stamp? {
        if (!file.isFile || !file.canRead()) return null
        val length = file.length()
        val modified = file.lastModified()
        return if (length > 0 && modified > 0) Stamp(length, modified) else null
    }

    private fun readBounded(
        file: File,
        expectedSize: Int,
    ): ByteArray? {
        val bytes = ByteArray(expectedSize)
        file.inputStream().use { input ->
            var offset = 0
            while (offset < bytes.size) {
                val count = input.read(bytes, offset, bytes.size - offset)
                if (count <= 0) return null
                offset += count
            }
            // A concurrent writer growing the file cannot trigger an unbounded allocation.
            if (input.read() != -1) return null
        }
        return bytes
    }

    private fun <T> safely(block: () -> T): T? =
        try {
            block()
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }

    companion object {
        // Same four slots and naming as StatesManager; there is no autosave thumbnail contract.
        private const val SLOT_COUNT = 4
        private const val MAX_IMAGE_BYTES = 4L * 1024 * 1024

        private fun isPathSegment(value: String): Boolean =
            value.isNotBlank() && value != "." && value != ".." &&
                value.none { it == '/' || it == '\\' || it == '\u0000' }
    }
}
