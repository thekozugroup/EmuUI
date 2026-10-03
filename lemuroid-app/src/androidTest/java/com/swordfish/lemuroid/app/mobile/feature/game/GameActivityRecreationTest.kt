package com.swordfish.lemuroid.app.mobile.feature.game

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Process
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel
import com.swordfish.lemuroid.app.shared.game.viewmodel.GameViewModelRetroGameView
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.libretrodroid.GLRetroView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * Requires an already imported lawful fixture and its downloaded core. No rows, permissions,
 * files or core preferences are fabricated. Run only with GameProcessTestRunner (see qa README).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class GameActivityRecreationTest {
    @Test
    fun readyGameRecreatesTwiceWithRetainedViewModelAndNewNativeView() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Select GameProcessTestRunner explicitly", instrumentation is GameProcessTestRunner)
        val context = instrumentation.targetContext
        val processName =
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                .runningAppProcesses.single { it.pid == Process.myPid() }.processName
        assertEquals("The test must run alongside the real GameActivity", "${context.packageName}:game", processName)

        val arguments = InstrumentationRegistry.getArguments()
        val title = arguments.getString("gameTitle") ?: "EmuUI_DS_Legacy_QA"
        val coreName = arguments.getString("coreName") ?: "MELONDS"
        val game = readImportedGame(context, title)
        val core = GameSystem.findById(game.systemId).systemCoreConfigs.single { it.coreID.name == coreName }
        val intent =
            Intent(context, GameActivity::class.java).apply {
                // Existing BaseGameActivity launch contract; no production test hook is needed.
                putExtra("GAME", game)
                putExtra("LOAD_SAVE", true)
                putExtra("LEANBACK", false)
                putExtra("EXTRA_SYSTEM_CORE_CONFIG", core)
            }
        val scenario = ActivityScenario.launch<GameActivity>(intent)
        try {
            val initialActivity = AtomicReference<GameActivity>()
            val initialModel = AtomicReference<BaseGameScreenViewModel>()
            scenario.onActivity {
                initialActivity.set(it)
                initialModel.set(ViewModelProvider(it)[BaseGameScreenViewModel::class.java])
            }
            val model = initialModel.get()
            var oldActivity = initialActivity.get()
            var oldView = awaitUsableCore(model)
            captureCheckpoint(context, "00-before.png")
            repeat(2) { index ->
                scenario.recreate()
                assertEquals(Lifecycle.State.RESUMED, scenario.state)
                scenario.onActivity { activity ->
                    assertNotSame("recreate must replace the Activity", oldActivity, activity)
                    assertSame(
                        "recreate must retain the ViewModel",
                        model,
                        ViewModelProvider(activity)[BaseGameScreenViewModel::class.java],
                    )
                    assertEquals(game.id, (activity.intent.getSerializableExtra("GAME") as Game).id)
                    oldActivity = activity
                }
                val newView = awaitUsableCore(model)
                assertNotSame("A recreated Activity must create a fresh native view", oldView, newView)
                oldView = newView
                captureCheckpoint(context, "0${index + 1}-after-recreate.png")
            }
        } finally {
            // GameService normally exits :game when the final Activity is destroyed. Stop the
            // service before scenario.close so that exitProcess cannot swallow JUnit results.
            // Recreation itself uses the unmodified Activity and live service throughout.
            instrumentation.runOnMainSync { context.stopService(Intent(context, GameService::class.java)) }
            instrumentation.waitForIdleSync()
            scenario.close()
        }
    }

    private fun captureCheckpoint(
        context: Context,
        filename: String,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Screenshot capture failed" }
        try {
            val directory = checkNotNull(context.getExternalFilesDir("qa-recreation"))
            check(directory.isDirectory || directory.mkdirs()) { "Unable to create QA screenshot directory" }
            File(directory, filename).outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Screenshot encoding failed" }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun readImportedGame(
        context: Context,
        title: String,
    ): Game =
        runBlocking {
            withContext(Dispatchers.IO) {
                check(context.getDatabasePath(RetrogradeDatabase.DB_NAME).isFile) {
                    "Import the lawful QA fixture through the app before running this test"
                }
                val database =
                    Room.databaseBuilder(
                        context,
                        RetrogradeDatabase::class.java,
                        RetrogradeDatabase.DB_NAME,
                    ).build()
                try {
                    val matches =
                        database.gameDao().observeLibrary().first().filter {
                            it.title == title || it.fileName == title
                        }
                    check(matches.size == 1) { "Expected one imported game matching '$title'; found ${matches.size}" }
                    matches.single()
                } finally {
                    database.close()
                }
            }
        }

    private fun awaitUsableCore(model: BaseGameScreenViewModel): GLRetroView =
        runBlocking {
            withTimeout(180_000) {
                model.getGameState().first { it == GameViewModelRetroGameView.GameState.Ready }
                val view = model.retroGameView.retroGameViewFlow()
                view.getGLRetroEvents().filterIsInstance<GLRetroView.GLRetroEvents.FrameRendered>().take(3).toList()
                assertTrue("The real core must remain serializable after rendering", view.serializeState().isNotEmpty())
                view
            }
        }
}
