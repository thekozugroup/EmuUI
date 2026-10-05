package com.swordfish.lemuroid.app.shared.billing

import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Read-only export of explicitly supplied save roots. Never traverses a symlink. */
object SaveArchive {
    fun write(roots: Map<String, File>, output: OutputStream): Int {
        var count = 0
        ZipOutputStream(output).use { zip ->
            roots.forEach { (label, directory) ->
                require(label.matches(Regex("[a-z-]+")))
                val root = directory.canonicalFile
                if (!root.isDirectory) return@forEach
                root.walkTopDown().onEnter { it.canonicalFile == it.absoluteFile }.forEach { file ->
                    if (!file.isFile || file.canonicalFile != file.absoluteFile) return@forEach
                    val relative = file.relativeTo(root).invariantSeparatorsPath
                    val length = file.length()
                    val modified = file.lastModified()
                    zip.putNextEntry(ZipEntry("$label/$relative"))
                    val copied = file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    if (copied != length || file.length() != length || file.lastModified() != modified) {
                        throw IOException("Save changed during export; close the game and retry")
                    }
                    count++
                }
            }
        }
        return count
    }
}
