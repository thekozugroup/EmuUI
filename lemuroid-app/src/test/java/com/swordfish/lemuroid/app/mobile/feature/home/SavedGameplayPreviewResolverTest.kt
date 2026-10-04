package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class SavedGameplayPreviewResolverTest {
    @get:Rule val temporary = TemporaryFolder()
    private val states get() = File(temporary.root, "states")
    private val previews get() = File(temporary.root, "state-previews")
    private val resolver get() = SavedGameplayPreviewResolver(states, previews)
    private val fileName = "Original test game.nes"

    private fun pair(
        slot: Int,
        time: Long,
        image: String = "image-$slot",
        core: String = "fceumm",
    ): Pair<File, File> {
        val state = File(states, "$core/$fileName.slot$slot")
        val preview = File(previews, "$core/$fileName.slot$slot.jpg")
        state.parentFile!!.mkdirs()
        preview.parentFile!!.mkdirs()
        state.writeText("original synthetic state $slot")
        preview.writeText(image)
        check(state.setLastModified(time))
        check(preview.setLastModified(time + 1000))
        return state to preview
    }

    private fun resolve(
        name: String = fileName,
        cores: List<String> = listOf("fceumm"),
    ) = resolver.resolve(name, cores) { bytes ->
        bytes.toString(Charsets.UTF_8).takeUnless { it.startsWith("corrupt") }
    }

    @Test fun missingDirectoriesFallBackWithoutCreatingAnything() {
        assertNull(resolve())
        assertFalse(states.exists())
        assertFalse(previews.exists())
    }

    @Test fun latestStateWinsRatherThanHighestSlotOrLatestImageTimestamp() {
        val older = pair(4, 10_000)
        pair(1, 20_000)
        check(older.second.setLastModified(30_000))
        val result = resolve()!!
        assertEquals(1, result.slot)
        assertEquals(20_000L, result.savedAt)
    }

    @Test fun equalTimestampsUseStableCoreAndSlotOrder() {
        pair(4, 10_000)
        pair(1, 10_000)
        assertEquals(1, resolve()!!.slot)
    }

    @Test fun corruptNewestImageFallsBackToOlderValidPair() {
        pair(1, 10_000)
        pair(2, 20_000, "corrupt-image")
        assertEquals(1, resolve()!!.slot)
    }

    @Test fun screenshotFromAnOverwrittenSlotIsNotDisplayed() {
        val files = pair(1, 10_000)
        check(files.first.setLastModified(30_000))
        assertNull(resolve())
    }

    @Test fun missingOrEmptyStateCannotBorrowAnOldScreenshot() {
        val missing = pair(1, 10_000)
        check(missing.first.delete())
        assertNull(resolve())
        val empty = pair(1, 10_000)
        empty.first.writeBytes(byteArrayOf())
        assertNull(resolve())
    }

    @Test fun missingEmptyAndCorruptImagesFallBack() {
        val missing = pair(1, 10_000)
        check(missing.second.delete())
        assertNull(resolve())
        pair(1, 10_000, "")
        assertNull(resolve())
        pair(1, 10_000, "corrupt-image")
        assertNull(resolve())
    }

    @Test fun oversizedImagesAreRejectedBeforeDecoding() {
        val files = pair(1, 10_000)
        files.second.writeBytes(ByteArray(4 * 1024 * 1024 + 1))
        check(files.second.setLastModified(11_000))
        var decoded = false
        val result =
            resolver.resolve(fileName, listOf("fceumm")) {
                decoded = true
                "decoded"
            }
        assertNull(result)
        assertFalse(decoded)
    }

    @Test fun latestAvailableScreenshotCanComeFromAnotherSupportedCore() {
        pair(1, 10_000)
        pair(2, 20_000, core = "other_supported_core")
        assertEquals("other_supported_core", resolve(cores = listOf("fceumm", "other_supported_core"))!!.coreName)
        assertEquals("fceumm", resolve()!!.coreName)
    }

    @Test fun pathTraversalAndAbsoluteNamesAreRejected() {
        pair(1, 10_000)
        listOf("", ".", "..", "../$fileName", "/$fileName", "folder\\$fileName", "bad\u0000name")
            .forEach { assertNull(resolve(name = it)) }
        listOf("..", "../fceumm", "/fceumm", "folder\\fceumm")
            .forEach { assertNull(resolve(cores = listOf(it))) }
    }

    @Test fun symlinkedFilesOutsideTheSaveDirectoryAreRejected() {
        val files = pair(1, 10_000)
        val external = File(temporary.root, "external.jpg").apply { writeText("external image") }
        check(external.setLastModified(11_000))
        check(files.second.delete())
        Files.createSymbolicLink(files.second.toPath(), external.toPath())
        assertNull(resolve())
    }

    @Test fun changedStateDuringDecodeDoesNotPublishAnObsoleteImage() {
        val files = pair(1, 10_000)
        val result =
            resolver.resolve(fileName, listOf("fceumm")) {
                check(files.first.setLastModified(30_000))
                "decoded"
            }
        assertNull(result)
    }

    @Test fun replacementWithSameSizeAndTimestampInvalidatesImageIdentity() {
        val files = pair(1, 10_000, "image-A")
        val before = resolve()!!
        files.second.writeText("image-B")
        check(files.second.setLastModified(11_000))
        val after = resolve()!!
        assertEquals("image-B", after.image)
        assertNotEquals(before.revision, after.revision)
    }

    @Test fun diskImagesSurviveANewResolverAndMissingResultsAreNeverCached() {
        assertNull(resolve())
        val files = pair(1, 10_000)
        val first = resolve()!!
        val fresh =
            SavedGameplayPreviewResolver(states, previews)
                .resolve(fileName, listOf("fceumm")) { it.toString(Charsets.UTF_8) }!!
        assertEquals(first, fresh)
        check(files.second.delete())
        assertNull(resolve())
    }

    @Test fun resolvingNeverChangesSaveOrScreenshotContentsAndTimestamps() {
        val files = pair(1, 10_000)
        val before = files.toList().map { Triple(it.readBytes().toList(), it.lastModified(), it.length()) }
        repeat(3) { assertTrue(resolve() != null) }
        val after = files.toList().map { Triple(it.readBytes().toList(), it.lastModified(), it.length()) }
        assertEquals(before, after)
    }

    @Test fun autosaveWithoutAManualScreenshotDoesNotPretendToHaveAPreview() {
        val auto = File(states, "fceumm/$fileName.state")
        auto.parentFile!!.mkdirs()
        auto.writeText("synthetic autosave")
        assertNull(resolve())
    }
}
