package com.swordfish.lemuroid.app.shared.library

import com.swordfish.lemuroid.lib.core.installCoreAssetArchive
import com.swordfish.lemuroid.lib.core.storeCoreDownload
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CoreDownloadsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun alreadyCancelledCoreDownloadStillClosesResponseBody() =
        runBlocking {
            var closed = false
            val stream =
                object : ByteArrayInputStream(byteArrayOf(1)) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            val error =
                thrownBy {
                    withContext(Job()) {
                        currentCoroutineContext().cancel()
                        storeCoreDownload(Response.success(stream as InputStream), destination("core.so"))
                    }
                }
            assertTrue(error is CancellationException)
            assertTrue(closed)
            assertFalse(destination("core.so").exists())
            assertNoStagingFiles()
        }

    @Test
    fun alreadyCancelledAssetDownloadStillClosesResponseBody() =
        runBlocking {
            var closed = false
            val assets = oldAssets()
            val stream =
                object : ByteArrayInputStream(archiveBytes("new.txt" to "new")) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            val error =
                thrownBy {
                    withContext(Job()) {
                        currentCoroutineContext().cancel()
                        installCoreAssetArchive(Response.success(stream as InputStream), assets)
                    }
                }
            assertTrue(error is CancellationException)
            assertTrue(closed)
            assertOldAssets(assets)
        }

    @Test
    fun completeDownloadIsPublishedAndStreamClosed() =
        runBlocking {
            var closed = false
            val bytes = byteArrayOf(1, 2, 3)
            val stream =
                object : ByteArrayInputStream(bytes) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            val destination = destination("core.so")
            storeCoreDownload(Response.success(stream as InputStream), destination)
            assertArrayEquals(bytes, destination.readBytes())
            assertTrue(closed)
            assertNoStagingFiles()
        }

    @Test
    fun unsuccessfulHttpResponseRetainsStatusAndDoesNotPublish() =
        runBlocking {
            val destination = destination("core.so")
            val error = thrownBy { storeCoreDownload(Response.error(404, "not found".toResponseBody()), destination) }
            assertTrue(error is HttpException)
            assertEquals(404, (error as HttpException).code())
            assertFalse(destination.exists())
            assertNoStagingFiles()
        }

    @Test
    fun missingBodyCannotBeReportedAsInstalled() =
        runBlocking {
            val destination = destination("core.so")
            assertTrue(thrownBy { storeCoreDownload(Response.success(null), destination) } is IOException)
            assertFalse(destination.exists())
            assertNoStagingFiles()
        }

    @Test
    fun emptyBodyCannotBecomeACachedCore() =
        runBlocking {
            val destination = destination("core.so")
            assertTrue(
                thrownBy { storeCoreDownload(Response.success(ByteArrayInputStream(byteArrayOf())), destination) }
                    is IOException,
            )
            assertFalse(destination.exists())
            assertNoStagingFiles()
        }

    @Test
    fun interruptedCopyLeavesExistingCoreIntact() =
        runBlocking {
            val destination = destination("core.so").apply { writeText("working core") }
            val stream =
                object : InputStream() {
                    var first = true

                    override fun read(): Int = throw IOException("Connection lost")

                    override fun read(
                        buffer: ByteArray,
                        offset: Int,
                        length: Int,
                    ): Int {
                        if (!first) throw IOException("Connection lost")
                        first = false
                        buffer[offset] = 1
                        return 1
                    }
                }
            assertTrue(thrownBy { storeCoreDownload(Response.success(stream), destination) } is IOException)
            assertEquals("working core", destination.readText())
            assertNoStagingFiles()
        }

    @Test
    fun cancellationDuringCopyCleansPartialCore() =
        runBlocking {
            val destination = destination("core.so")
            val error =
                thrownBy {
                    withContext(Job()) {
                        val job = currentCoroutineContext()
                        val stream =
                            object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
                                override fun read(
                                    buffer: ByteArray,
                                    offset: Int,
                                    length: Int,
                                ): Int {
                                    job.cancel()
                                    return super.read(buffer, offset, length)
                                }
                            }
                        storeCoreDownload(Response.success(stream as InputStream), destination)
                    }
                }
            assertTrue(error is CancellationException)
            assertFalse(destination.exists())
            assertNoStagingFiles()
        }

    @Test
    fun validArchiveReplacesOldAssetsAndCreatesMissingParentDirectories() =
        runBlocking {
            val assets = oldAssets()
            installCoreAssetArchive(archive("shaders/new.glsl" to "shader"), assets)
            assertEquals("shader", File(assets, "shaders/new.glsl").readText())
            assertFalse(File(assets, "old.txt").exists())
            assertNoStagingFiles()
        }

    @Test
    fun httpFailureLeavesValidCachedAssetsIntact() =
        runBlocking {
            val assets = oldAssets()
            val error =
                thrownBy {
                    installCoreAssetArchive(Response.error(503, "unavailable".toResponseBody()), assets)
                }
            assertTrue(error is HttpException)
            assertEquals(503, (error as HttpException).code())
            assertOldAssets(assets)
        }

    @Test
    fun missingAssetBodyLeavesValidCachedAssetsIntact() =
        runBlocking {
            val assets = oldAssets()
            assertTrue(thrownBy { installCoreAssetArchive(Response.success(null), assets) } is IOException)
            assertOldAssets(assets)
        }

    @Test
    fun emptyArchiveLeavesValidCachedAssetsIntact() =
        runBlocking {
            val assets = oldAssets()
            assertTrue(thrownBy { installCoreAssetArchive(archive(), assets) } is IOException)
            assertOldAssets(assets)
        }

    @Test
    fun malformedArchiveLeavesValidCachedAssetsIntact() =
        runBlocking {
            val assets = oldAssets()
            val invalid: InputStream = ByteArrayInputStream("not a zip".toByteArray())
            assertTrue(thrownBy { installCoreAssetArchive(Response.success(invalid), assets) } is IOException)
            assertOldAssets(assets)
        }

    @Test
    fun archiveTruncatedAfterFirstEntryCannotReplaceCachedAssets() =
        runBlocking {
            val complete = archiveBytes("first.txt" to "first", "second.txt" to "second")
            val secondHeader =
                (1 until complete.size - 3).first { index ->
                    complete[index] == 0x50.toByte() && complete[index + 1] == 0x4b.toByte() &&
                        complete[index + 2] == 0x03.toByte() && complete[index + 3] == 0x04.toByte()
                }
            // ZipInputStream treats EOF before/inside the next local header as an ordinary end of archive.
            for (headerBytes in 0 until 30) {
                val assets = oldAssets()
                val truncated: InputStream = ByteArrayInputStream(complete.copyOf(secondHeader + headerBytes))
                assertTrue(thrownBy { installCoreAssetArchive(Response.success(truncated), assets) } is IOException)
                assertOldAssets(assets)
            }
        }

    @Test
    fun everyTruncatedPrefixOfAnArchivePreservesCachedAssets() =
        runBlocking {
            val complete = archiveBytes("first.txt" to "first", "second.txt" to "second")
            // Includes cuts inside file data, the central directory, and the end-of-central-directory record.
            for (length in complete.indices) {
                val assets = oldAssets()
                val truncated: InputStream = ByteArrayInputStream(complete.copyOf(length))
                val error = thrownBy { installCoreAssetArchive(Response.success(truncated), assets) }
                assertTrue("Truncated archive length $length must fail", error is IOException)
                assertOldAssets(assets)
            }
        }

    @Test
    fun archiveWithIncorrectEntryChecksumPreservesCachedAssets() =
        runBlocking {
            assertCorruptEntryPreservesAssets(16)
        }

    @Test
    fun archiveWithIncorrectEntrySizePreservesCachedAssets() =
        runBlocking {
            assertCorruptEntryPreservesAssets(24)
        }

    private suspend fun assertCorruptEntryPreservesAssets(fieldOffset: Int) {
        val complete = archiveBytes("new.txt" to "replacement")
        val centralHeader =
            complete.indices.first { index ->
                complete[index] == 0x50.toByte() && complete[index + 1] == 0x4b.toByte() &&
                    complete[index + 2] == 0x01.toByte() && complete[index + 3] == 0x02.toByte()
            }
        complete[centralHeader + fieldOffset] = (complete[centralHeader + fieldOffset].toInt() xor 1).toByte()
        val assets = oldAssets()
        val corrupted: InputStream = ByteArrayInputStream(complete)
        assertTrue(thrownBy { installCoreAssetArchive(Response.success(corrupted), assets) } is IOException)
        assertOldAssets(assets)
    }

    @Test
    fun traversalArchiveCannotWriteOutsideStagingOrReplaceCachedAssets() =
        runBlocking {
            val assets = oldAssets()
            val error = thrownBy { installCoreAssetArchive(archive("../escaped.txt" to "untrusted"), assets) }
            assertTrue(error is IOException)
            assertFalse(destination("escaped.txt").exists())
            assertOldAssets(assets)
        }

    @Test
    fun cancelledAssetDownloadLeavesValidCachedAssetsIntact() =
        runBlocking {
            val assets = oldAssets()
            val error =
                thrownBy {
                    withContext(Job()) {
                        val job = currentCoroutineContext()
                        val zip =
                            object : ByteArrayInputStream(archiveBytes("new.txt" to "replacement")) {
                                override fun read(
                                    buffer: ByteArray,
                                    offset: Int,
                                    length: Int,
                                ): Int {
                                    job.cancel()
                                    return super.read(buffer, offset, length)
                                }
                            }
                        installCoreAssetArchive(Response.success(zip as InputStream), assets)
                    }
                }
            assertTrue(error is CancellationException)
            assertOldAssets(assets)
        }

    private fun destination(name: String) = File(temporaryFolder.root, name)

    private fun oldAssets() =
        destination("PPSSPP").apply {
            mkdirs()
            File(this, "old.txt").writeText("working assets")
        }

    private fun assertOldAssets(assets: File) {
        assertEquals("working assets", File(assets, "old.txt").readText())
        assertEquals(listOf("old.txt"), assets.listFiles()!!.map { it.name })
        assertNoStagingFiles()
    }

    private fun assertNoStagingFiles() {
        assertFalse(temporaryFolder.root.listFiles()!!.any { it.name.endsWith(".tmp") })
    }

    private fun archive(vararg files: Pair<String, String>): Response<InputStream> =
        Response.success(ByteArrayInputStream(archiveBytes(*files)))

    private fun archiveBytes(vararg files: Pair<String, String>): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            files.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private suspend fun thrownBy(block: suspend () -> Unit): Throwable {
        try {
            block()
        } catch (error: Throwable) {
            return error
        }
        throw AssertionError("Expected an exception")
    }
}
