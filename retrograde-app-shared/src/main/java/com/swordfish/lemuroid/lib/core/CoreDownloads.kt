package com.swordfish.lemuroid.lib.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.CheckedInputStream
import java.util.zip.ZipFile

private fun <T> Response<T>.requireDownloadBody(): T {
    if (!isSuccessful) {
        errorBody()?.close()
        throw HttpException(this)
    }
    return body() ?: throw IOException("Download response contained no body")
}

/** Only publish a nonempty, completely written core; failures leave no cached partial file. */
suspend fun storeCoreDownload(
    response: Response<InputStream>,
    destination: File,
) = response.requireDownloadBody().use { input ->
    withContext(Dispatchers.IO) {
        val staging = destination.stagingFile()
        try {
            val bytes = staging.outputStream().use { input.copyDownloadTo(it) }
            if (bytes == 0L) throw IOException("Core download was empty")
            currentCoroutineContext().ensureActive()
            if (!staging.renameTo(destination)) throw IOException("Could not install downloaded core")
        } finally {
            staging.delete()
        }
    }
}

/** Extract before replacing cached assets, so a failed download cannot destroy a working install. */
suspend fun installCoreAssetArchive(
    response: Response<InputStream>,
    destination: File,
) = response.requireDownloadBody().use { input ->
    withContext(Dispatchers.IO) {
        val staging = destination.stagingFile()
        val archive = destination.stagingFile()
        try {
            archive.outputStream().use { input.copyDownloadTo(it) }
            if (!staging.mkdirs()) throw IOException("Could not create asset staging directory")
            // ZipInputStream can mistake a truncated local header for EOF. ZipFile requires the central directory.
            ZipFile(archive).use { extractCoreAssets(it, staging) }
            currentCoroutineContext().ensureActive()
            replaceAssets(staging, destination)
        } finally {
            archive.delete()
            staging.deleteRecursively()
        }
    }
}

private suspend fun extractCoreAssets(
    archive: ZipFile,
    staging: File,
) {
    val root = staging.canonicalPath + File.separator
    var extractedBytes = 0L
    val entries = archive.entries()
    while (entries.hasMoreElements()) {
        currentCoroutineContext().ensureActive()
        val entry = entries.nextElement()
        val file = File(staging, entry.name)
        if (!file.canonicalPath.startsWith(root)) throw IOException("Invalid asset archive path")
        if (entry.isDirectory) {
            if (entry.size != 0L) throw IOException("Invalid asset archive directory")
            file.requireDirectory()
        } else {
            file.parentFile!!.requireDirectory()
            val checksum = CRC32()
            val bytes =
                CheckedInputStream(archive.getInputStream(entry), checksum).use { input ->
                    file.outputStream().use { input.copyDownloadTo(it) }
                }
            if (bytes != entry.size || checksum.value != entry.crc) {
                throw IOException("Asset archive entry failed integrity check")
            }
            extractedBytes += bytes
        }
    }
    if (extractedBytes == 0L) throw IOException("Asset archive contained no data")
}

private fun replaceAssets(
    staging: File,
    destination: File,
) {
    val backup = destination.stagingFile()
    val hadPreviousAssets = destination.exists()
    if (hadPreviousAssets && !destination.renameTo(backup)) {
        throw IOException("Could not preserve previous core assets")
    }
    if (!staging.renameTo(destination)) {
        val failure = IOException("Could not install downloaded core assets")
        if (hadPreviousAssets && !backup.renameTo(destination)) {
            failure.addSuppressed(IOException("Previous core assets remain at ${backup.name}"))
        }
        throw failure
    }
    backup.deleteRecursively()
}

private fun File.stagingFile(): File {
    val parent = absoluteFile.parentFile!!
    parent.requireDirectory()
    return File(parent, ".$name.${UUID.randomUUID()}.tmp")
}

private fun File.requireDirectory() {
    if (!isDirectory && !mkdirs()) throw IOException("Could not create download directory")
}

private suspend fun InputStream.copyDownloadTo(output: OutputStream): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = read(buffer)
        if (count < 0) return total
        output.write(buffer, 0, count)
        total += count
    }
}
