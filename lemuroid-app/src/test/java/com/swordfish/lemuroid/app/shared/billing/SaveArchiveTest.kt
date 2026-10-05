package com.swordfish.lemuroid.app.shared.billing

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipInputStream
import org.junit.Assert.*
import org.junit.Test

class SaveArchiveTest {
    @Test fun exportPreservesOriginalsAndSkipsSymlinksOutsideSaveRoot() {
        val temporary = Files.createTempDirectory("emuui-save-export").toFile()
        try {
            val root = temporary.resolve("saves").apply { mkdir() }
            val original = root.resolve("homebrew.sav").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val unrelated = temporary.resolve("private.txt").apply { writeText("not a save") }
            Files.createSymbolicLink(root.resolve("outside.sav").toPath(), unrelated.toPath())
            val output = ByteArrayOutputStream()
            assertEquals(1, SaveArchive.write(mapOf("saves" to root), output))
            ZipInputStream(output.toByteArray().inputStream()).use {
                assertEquals("saves/homebrew.sav", it.nextEntry.name)
                assertArrayEquals(byteArrayOf(1, 2, 3), it.readBytes())
                assertNull(it.nextEntry)
            }
            assertArrayEquals(byteArrayOf(1, 2, 3), original.readBytes())
            assertEquals("not a save", unrelated.readText())
        } finally { temporary.deleteRecursively() }
    }

    @Test fun emptySaveDirectoryProducesValidEmptyZip() {
        val missing = Files.createTempDirectory("emuui-empty-export").toFile()
        try {
            val output = ByteArrayOutputStream()
            assertEquals(0, SaveArchive.write(mapOf("saves" to missing.resolve("absent")), output))
            ZipInputStream(output.toByteArray().inputStream()).use { assertNull(it.nextEntry) }
        } finally { missing.deleteRecursively() }
    }
}
