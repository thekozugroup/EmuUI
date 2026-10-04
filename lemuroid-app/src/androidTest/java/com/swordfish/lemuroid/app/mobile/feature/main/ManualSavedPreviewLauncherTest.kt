package com.swordfish.lemuroid.app.mobile.feature.main

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.feature.game.ManualPreviewHandoff
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Stage two of opt-in production manual-save proof. Run with the standard runner after package force-stop,
 * using the same qaManualPreviewRun passed to NativeFoldLayoutTest. Always restores exact slot files.
 * stage=restore-only is a recovery route if the coordinator cannot finish launcher verification.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ManualSavedPreviewLauncherTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun productionManualSaveRendersWithoutStartingCoreAndSurvivesRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val args = InstrumentationRegistry.getArguments()
        val run = checkNotNull(args.getString("qaManualPreviewRun")) { "Use the matching opt-in native save run" }
        if (args.getString("stage") == "restore-only") {
            ManualPreviewHandoff.restore(context, run)
            return
        }
        val directory = ManualPreviewHandoff.directory(context, run)
        val manifest = ManualPreviewHandoff.read(context, run)
        check(manifest.getBoolean("created") && !manifest.getBoolean("restored"))
        val report =
            JSONObject().put("source", "Real MainActivity after production ViewModel manual slot save")
                .put("gameId", manifest.getInt("gameId")).put("passed", false)
        try {
            assertNoGameProcess(context)
            val game = runBlocking { compose.activity.retrogradeDb.gameDao().selectById(manifest.getInt("gameId")) }
            checkNotNull(game)
            assertEquals(manifest.getString("gameFileName"), game.fileName)
            publish()
            waitForTag("launcher_preview")
            compose.onNodeWithText("All games").performClick()
            compose.onNodeWithTag("launcher_game_${game.id}").performClick().assertIsSelected()
            waitForTag("launcher_saved_gameplay_preview")
            compose.onNodeWithContentDescription("Saved gameplay for ${game.title}").assertIsDisplayed()
            capture(File(directory, "launcher-production-preview.png"))
            assertNoGameProcess(context)
            compose.activityRule.scenario.recreate()
            publish()
            waitForTag("launcher_saved_gameplay_preview")
            compose.onNodeWithContentDescription("Saved gameplay for ${game.title}").assertIsDisplayed()
            compose.onNodeWithTag("launcher_game_${game.id}").assertIsSelected()
            capture(File(directory, "launcher-preview-after-recreation.png"))
            assertNoGameProcess(context)
            val root = checkNotNull(context.getExternalFilesDir(null))
            val files = manifest.getJSONArray("files")
            for (index in 0 until files.length()) {
                val record = files.getJSONObject(index)
                val actual = ManualPreviewHandoff.fingerprint(File(root, record.getString("path")))
                val expected = record.getJSONObject("produced")
                assertEquals(
                    "Launcher must not rewrite the paired file",
                    expected.getString("sha256"),
                    actual.getString("sha256"),
                )
                assertEquals(expected.getLong("modified"), actual.getLong("modified"))
            }
            report.put("passed", true).put("nativeProcessStarted", false).put("activityRecreated", true)
        } finally {
            File(directory, "launcher-result.json").writeText(report.toString(2))
            ManualPreviewHandoff.restore(context, run)
        }
        assertTrue(
            "Original slot bytes and timestamps must be restored",
            ManualPreviewHandoff.read(context, run).getBoolean("restored"),
        )
    }

    private fun assertNoGameProcess(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        assertTrue(
            "Force-stop between stages; static launcher preview must never start a game core process",
            manager.runningAppProcesses.none { it.processName == "${context.packageName}:game" },
        )
    }

    private fun capture(file: File) {
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun waitForTag(tag: String) {
        compose.waitUntil(60_000) {
            runCatching { compose.onNodeWithTag(tag).assertIsDisplayed() }.isSuccess
        }
    }

    private fun publish() {
        compose.waitForIdle()
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        size = 24,
                        state = FoldingFeature.State.HALF_OPENED,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
    }
}
