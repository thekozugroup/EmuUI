package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.swordfish.lemuroid.ext.feature.core.CoreUpdaterImpl
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import java.io.File
import java.util.zip.ZipInputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit

/** Tests real Play asset installation in isolated test storage; no game/save content is opened. */
@RunWith(AndroidJUnit4::class)
class BundledPspAssetsTest {
    @Test
    fun playInstallsExactBundledAssetsWithoutNetworkAndPreservesExistingFiles() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val id = "psp-assets-qa-${System.nanoTime()}"
        val root = File(app.cacheDir, id).apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getFilesDir(): File = root
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                app.getSharedPreferences(id, mode)
        }
        val sentinel = File(root, "saves/owned-by-this-test.sav").apply {
            parentFile!!.mkdirs()
            writeText("preserve")
        }
        val retrofit = Retrofit.Builder().baseUrl("https://invalid.example/")
            .callFactory { throw AssertionError("Play assets attempted a network request") }.build()
        val updater = CoreUpdaterImpl(DirectoriesManager(context), retrofit)
        val method = updater.javaClass.getDeclaredMethod(
            "installAssets", Context::class.java, List::class.java, Continuation::class.java,
        ).apply { isAccessible = true }
        suspend fun install() = suspendCoroutineUninterceptedOrReturn<Unit> { continuation ->
            val result = method.invoke(updater, context, listOf(CoreID.PPSSPP), continuation)
            if (result === COROUTINE_SUSPENDED) result else Unit
        }
        try {
            install()
            val destination = File(root, "system/PPSSPP")
            var count = 0
            ZipInputStream(app.assets.open("core-assets/ppsspp.zip")).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        assertArrayEquals(entry.name, zip.readBytes(), File(destination, entry.name).readBytes())
                        count++
                    }
                }
            }
            assertEquals(137, count)
            val existing = File(destination, "qa-existing-content.txt").apply { writeText("keep") }
            install() // Same recorded version must preserve an existing asset install.
            assertEquals("keep", existing.readText())
            assertEquals("preserve", sentinel.readText())
        } finally {
            root.deleteRecursively() // Only this test's unique cache directory.
            app.getSharedPreferences(id, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
