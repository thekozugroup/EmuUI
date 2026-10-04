package com.swordfish.lemuroid.app.mobile.feature.game

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/** Real blocked first launch; never publishes an open feature or fabricates save data. */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class BlockedGameSaveSafetyTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createEmptyComposeRule()

    @Test
    fun neverOpenedGamePreservesExistingSavesOnBackgroundAndExit() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Select GameProcessTestRunner explicitly", instrumentation is GameProcessTestRunner)
        val context = instrumentation.targetContext
        val title = InstrumentationRegistry.getArguments().getString("gameTitle") ?: "EmuUI_DS_Legacy_QA"
        val game = readImportedGame(context, title)
        val core = GameSystem.findById(game.systemId).systemCoreConfigs.single { it.coreID.name == "MELONDS" }
        val root = checkNotNull(context.getExternalFilesDir(null))
        val files =
            linkedMapOf(
                "autosave" to File(root, "states/${core.coreID.coreName}/${game.fileName}.state"),
                "metadata" to File(root, "states/${core.coreID.coreName}/${game.fileName}.state.metadata"),
                "sram" to File(root, "saves/${game.fileName.substringBeforeLast(".")}.srm"),
            )
        assertTrue("An existing original-fixture autosave is required", files.getValue("autosave").isFile)
        val before = files.mapValues { fingerprint(it.value) }
        windowInfo.overrideWindowLayoutInfo(TestWindowLayoutInfo())
        val scenario =
            ActivityScenario.launch<GameActivity>(
                Intent(context, GameActivity::class.java).apply {
                    putExtra("GAME", game)
                    putExtra("LOAD_SAVE", true)
                    putExtra("LEANBACK", false)
                    putExtra("EXTRA_SYSTEM_CORE_CONFIG", core)
                },
            )
        val model = AtomicReference<BaseGameScreenViewModel>()
        val evidence = File(root, "qa-save-safety").apply { mkdirs() }
        try {
            scenario.onActivity { model.set(ViewModelProvider(it)[BaseGameScreenViewModel::class.java]) }
            waitForGuidance()
            // Pump Compose through Loaded before waiting on native events. A blocking
            // flow wait here would prevent the test clock from composing AndroidView.
            compose.waitUntil(120_000) { model.get().retroGameView.existingRetroView() != null }
            compose.waitForIdle()
            val view = checkNotNull(model.get().retroGameView.existingRetroView())
            assertUnrenderedAndPaused(model.get(), view)
            val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
            try {
                File(evidence, "initial-no-fold-guide.png").outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                bitmap.recycle()
            }
            scenario.moveToState(Lifecycle.State.CREATED)
            val drained = CompletableDeferred<Unit>()
            GameService.schedule { drained.complete(Unit) }
            runBlocking { withTimeout(30_000) { drained.await() } }
            val afterBackground = files.mapValues { fingerprint(it.value) }
            assertEquals("Blocked background must preserve exact save bytes and timestamps", before, afterBackground)
            scenario.moveToState(Lifecycle.State.RESUMED)
            waitForGuidance()
            assertUnrenderedAndPaused(model.get(), view)
            // The production exit action still runs. Stopping this service only prevents
            // its deliberate exitProcess from swallowing the instrumented runner result.
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            compose.onNodeWithText("Return to library").performClick()
            compose.waitUntil(30_000) { scenario.state == Lifecycle.State.DESTROYED }
            val afterExit = files.mapValues { fingerprint(it.value) }
            assertEquals("Blocked exit must preserve exact save bytes and timestamps", before, afterExit)
            val json =
                JSONObject()
                    .put("evidenceType", "Actual imported DS launch with no fold metadata and no native frame")
                    .put("canCaptureStateBeforeBackground", false)
                    .put("nativeLifecycleResumed", false)
                    .put("frameRenderedEventObserved", false)
                    .put("existingSramPresent", files.getValue("sram").isFile)
                    .put("before", JSONObject(before))
                    .put("afterBackground", JSONObject(afterBackground))
                    .put("afterExit", JSONObject(afterExit))
                    .put("capturedAtEpochMs", System.currentTimeMillis())
            File(evidence, "save-preservation.json").writeText(json.toString(2))
        } finally {
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
    }

    private fun assertUnrenderedAndPaused(
        model: BaseGameScreenViewModel,
        view: GLRetroView,
    ) {
        compose.waitForIdle()
        assertFalse("Blocked native lifecycle must not be resumed", model.retroGameView.isNativeViewResumed)
        assertFalse("Initial blocked core must not be eligible to overwrite saves", model.retroGameView.canCaptureState)
        val frame =
            runBlocking {
                withTimeoutOrNull(1_500) {
                    view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().first()
                }
            }
        assertNull("No native frame may have rendered behind initial guidance", frame)
    }

    private fun waitForGuidance() {
        compose.waitUntil(60_000) {
            runCatching {
                compose.onAllNodes(hasTestTag("emuui_game_posture_guidance")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun fingerprint(file: File): String {
        if (!file.isFile) return "ABSENT"
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val sha256 = digest.joinToString("") { "%02x".format(it) }
        return "${file.length()}:${file.lastModified()}:$sha256"
    }

    private fun readImportedGame(
        context: Context,
        title: String,
    ): Game =
        runBlocking {
            withContext(Dispatchers.IO) {
                check(context.getDatabasePath(RetrogradeDatabase.DB_NAME).isFile)
                val db =
                    Room.databaseBuilder(context, RetrogradeDatabase::class.java, RetrogradeDatabase.DB_NAME).build()
                try {
                    db.gameDao().observeLibrary().first().filter { it.title == title || it.fileName == title }.single()
                } finally {
                    db.close()
                }
            }
        }
}
