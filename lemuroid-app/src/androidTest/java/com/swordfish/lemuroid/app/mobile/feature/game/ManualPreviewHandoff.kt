package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AtomicFile
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** Durable, opt-in bridge between game-process production save and main-process preview verification. */
internal object ManualPreviewHandoff {
    suspend fun create(
        context: Context,
        game: Game,
        coreName: String,
        run: String,
        model: BaseGameScreenViewModel,
    ) {
        require(game.fileName in setOf("EmuUI_DS_Legacy_QA.nds", "EmuUI_DS_Legacy_Long_Title_QA.nds")) {
            "Only the coordinator's original lawful DS fixture may be used"
        }
        val root = checkNotNull(context.getExternalFilesDir(null))
        val directory = directory(context, run)
        check(directory.mkdir()) { "Use a fresh manual-preview run; an existing backup must never be overwritten" }
        val paths =
            listOf(
                "states/$coreName/${game.fileName}.slot4",
                "states/$coreName/${game.fileName}.slot4.metadata",
                "state-previews/$coreName/${game.fileName}.slot4.jpg",
            )
        val files = JSONArray()
        paths.forEachIndexed { index, path ->
            val file = File(root, path)
            check(!File(file.path + ".bak").exists() && !File(file.path + ".new").exists()) {
                "Pre-existing atomic-save recovery files need coordinator review"
            }
            val snapshot = fingerprint(file).put("path", path).put("backup", "$index.before")
            if (file.exists()) file.copyTo(File(directory, "$index.before"))
            files.put(snapshot)
        }
        val manifest =
            JSONObject().put("gameId", game.id).put("gameFileName", game.fileName)
                .put("gameTitle", game.title).put("coreName", coreName).put("files", files)
                .put("created", false).put("restored", false)
        write(directory, manifest)
        try {
            // This is the exact production method used after choosing a manual save slot in the game menu.
            model.saveSlot(3)
            val state = File(root, paths[0])
            val image = File(root, paths[2])
            check(state.length() > 0 && image.length() > 0 && image.lastModified() >= state.lastModified()) {
                "The production manual-save path did not create a fresh paired screenshot"
            }
            val bitmap = checkNotNull(BitmapFactory.decodeFile(image.path)) { "Production preview JPEG did not decode" }
            try {
                val samples = mutableSetOf<Int>()
                for (y in 0 until bitmap.height step 3) for (x in 0 until bitmap.width step 3) {
                    samples += bitmap.getPixel(x, y)
                }
                check(samples.size > 8) { "Production screenshot must contain a rendered frame, not a blank surface" }
                image.copyTo(File(directory, "production-preview.jpg"))
                manifest.put("imageWidth", bitmap.width).put("imageHeight", bitmap.height)
                    .put("sampledColors", samples.size)
            } finally {
                bitmap.recycle()
            }
            paths.forEachIndexed {
                    index,
                    path,
                ->
                files.getJSONObject(index).put("produced", fingerprint(File(root, path)))
            }
            manifest.put("created", true).put("createdAtEpochMs", System.currentTimeMillis())
            write(directory, manifest)
        } catch (failure: Throwable) {
            restore(context, run, allowPartialCreate = true)
            throw failure
        }
    }

    fun directory(
        context: Context,
        run: String,
    ): File {
        require(run.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,90}")))
        val parent = File(checkNotNull(context.getExternalFilesDir(null)), "qa-manual-preview")
        check(parent.isDirectory || parent.mkdirs())
        return File(parent, run)
    }

    fun read(
        context: Context,
        run: String,
    ): JSONObject =
        JSONObject(
            AtomicFile(File(directory(context, run), "handoff.json")).openRead().bufferedReader().use { it.readText() },
        )

    fun restore(
        context: Context,
        run: String,
        allowPartialCreate: Boolean = false,
    ) {
        val directory = directory(context, run)
        val manifest = read(context, run)
        val root = checkNotNull(context.getExternalFilesDir(null))
        val files = manifest.getJSONArray("files")
        for (index in 0 until files.length()) {
            val saved = files.getJSONObject(index)
            val target = File(root, saved.getString("path"))
            val current = fingerprint(target)
            check(allowPartialCreate || same(current, saved) || same(current, saved.getJSONObject("produced"))) {
                "Slot changed outside the test; refusing to overwrite it. Inspect the durable handoff manifest."
            }
            if (saved.getBoolean("exists")) {
                val backup = File(directory, saved.getString("backup"))
                check(hash(backup) == saved.getString("sha256")) { "Backup integrity failed; no restoration attempted" }
                check(target.parentFile!!.isDirectory || target.parentFile!!.mkdirs())
                target.writeBytes(backup.readBytes())
                check(target.setLastModified(saved.getLong("modified")))
            } else {
                check(!target.exists() || target.delete())
            }
            check(same(fingerprint(target), saved)) { "Exact slot restoration failed" }
        }
        manifest.put("restored", true).put("restoredAtEpochMs", System.currentTimeMillis())
        write(directory, manifest)
    }

    fun fingerprint(file: File): JSONObject =
        JSONObject().put("exists", file.isFile).put("length", file.length()).put("modified", file.lastModified())
            .put("sha256", if (file.isFile) hash(file) else "")

    private fun same(
        actual: JSONObject,
        expected: JSONObject,
    ): Boolean =
        actual.getBoolean("exists") == expected.getBoolean("exists") &&
            actual.getLong("length") == expected.getLong("length") &&
            actual.getLong("modified") == expected.getLong("modified") &&
            actual.getString("sha256") == expected.getString("sha256")

    private fun hash(file: File) =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private fun write(
        directory: File,
        manifest: JSONObject,
    ) {
        val atomic = AtomicFile(File(directory, "handoff.json"))
        val stream = atomic.startWrite()
        try {
            stream.write(manifest.toString(2).toByteArray())
            atomic.finishWrite(stream)
        } catch (failure: Throwable) {
            atomic.failWrite(stream)
            throw failure
        }
    }
}
