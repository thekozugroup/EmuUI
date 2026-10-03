import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Enumeration
import java.util.zip.GZIPInputStream

/** Lossless, offline preparation of the unchanged upstream database asset. */
@CacheableTask
abstract class PrepareLibretroMetadata : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archiveFiles: ConfigurableFileCollection

    @get:Input
    abstract val expectedArchiveSha256: Property<String>

    @get:Input
    abstract val expectedSha256: Property<String>

    @get:Input
    abstract val expectedSize: Property<Long>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun prepare() {
        val parts = archiveFiles.files.sortedBy { it.name }
        if (parts.isEmpty()) throw GradleException("Metadata archive parts are missing")
        val archiveDigest = MessageDigest.getInstance("SHA-256")
        val archiveBuffer = ByteArray(64 * 1024)
        parts.forEach { part ->
            part.inputStream().use { input ->
                while (true) {
                    val count = input.read(archiveBuffer)
                    if (count < 0) break
                    archiveDigest.update(archiveBuffer, 0, count)
                }
            }
        }
        if (archiveDigest.digest().hex() != expectedArchiveSha256.get()) {
            throw GradleException("Metadata archive parts do not match the pinned checksum")
        }
        val directory = outputDirectory.get().asFile.toPath()
        Files.createDirectories(directory)
        val destination = directory.resolve("libretro-db.sqlite")
        // A killed build must not leave a temporary database in AGP's asset source.
        val stagingDirectory = temporaryDir.toPath()
        Files.createDirectories(stagingDirectory)
        val temporary = Files.createTempFile(stagingDirectory, "metadata-", ".sqlite")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var size = 0L
            val iterator = parts.iterator()
            val streams =
                object : Enumeration<InputStream> {
                    override fun hasMoreElements() = iterator.hasNext()

                    override fun nextElement(): InputStream = iterator.next().inputStream().buffered()
                }
            SequenceInputStream(streams).use { compressed ->
                GZIPInputStream(compressed).use { input ->
                    Files.newOutputStream(temporary).buffered().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue
                            size += count
                            if (size > expectedSize.get()) {
                                throw GradleException("Metadata archive exceeds its expected size")
                            }
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
            }
            val actualSha256 = digest.digest().hex()
            if (size != expectedSize.get() || actualSha256 != expectedSha256.get()) {
                throw GradleException("Metadata checksum or size does not match the pinned upstream asset")
            }
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun ByteArray.hex() = joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
