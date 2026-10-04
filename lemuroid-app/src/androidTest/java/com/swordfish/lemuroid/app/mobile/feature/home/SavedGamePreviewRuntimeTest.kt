package com.swordfish.lemuroid.app.mobile.feature.home

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.swordfish.lemuroid.app.mobile.feature.main.MainActivity
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.AppTheme
import com.swordfish.lemuroid.lib.library.db.entity.Game
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.abs

/**
 * Production preview composable and real Android image decoding/lifecycle using original solid-color JPEGs.
 * UUID filenames are never inserted into Room or passed to a core. Paired state markers are intentionally
 * not emulator saves. Only files created by this test are removed; existing states/previews stay untouched.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class SavedGamePreviewRuntimeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val selected = mutableStateOf<Game?>(null)
    private val ownedFiles = mutableListOf<File>()
    private val unique = UUID.randomUUID().toString()
    private val timestamp = System.currentTimeMillis() - 60_000L

    @After
    fun removeOnlyOwnedFixtureFiles() {
        try {
            Log.i(TAG, "fixture=$unique stage=stop-before-cleanup")
            if (compose.activityRule.scenario.state == Lifecycle.State.RESUMED) {
                compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
                compose.waitForIdle()
            }
        } finally {
            ownedFiles.forEach { file ->
                assertTrue("Remove only this test's UUID-named fixture: ${file.name}", !file.exists() || file.delete())
            }
            Log.i(TAG, "fixture=$unique stage=owned-files-removed")
        }
    }

    @Test
    fun selectionNeverShowsAnotherGameAndDiskPreviewSurvivesActivityRecreation() {
        val first = game(1)
        val second = game(2)
        val firstPair = pair(first, 1, GREEN, timestamp)
        pair(second, 1, RED, timestamp)
        val originalStateHash = hash(firstPair.first)
        val originalPreviewHash = hash(firstPair.second)
        selected.value = first
        installContent()
        expectImage(first, GREEN)
        compose.runOnIdle { selected.value = second }
        expectImage(second, RED)
        compose.runOnIdle { selected.value = game(3) }
        expectFallback()
        compose.runOnIdle { selected.value = first }
        expectImage(first, GREEN)
        compose.activityRule.scenario.recreate()
        // Rebuild the test-only host after a real Activity recreation. The production reader must
        // rediscover the same bytes on disk; no Bitmap or remembered preview is carried across.
        installContent()
        expectImage(first, GREEN)
        assertEquals("Preview reads must not write paired state", originalStateHash, hash(firstPair.first))
        assertEquals("Preview reads must not rewrite image", originalPreviewHash, hash(firstPair.second))
    }

    @Test
    fun resumeRefreshFallsBackForStaleMissingEmptyAndCorruptPreview() {
        val game = game(1)
        val (state, preview) = pair(game, 1, GREEN, timestamp)
        val originalStateHash = hash(state)
        selected.value = game
        installContent()
        expectImage(game, GREEN)

        resumeAfter {
            writeImage(preview, RED)
            check(preview.setLastModified(timestamp - 1_000))
        }
        expectFallback()
        resumeAfter { check(preview.setLastModified(timestamp + 1_000)) }
        expectImage(game, RED)
        resumeAfter {
            preview.writeText("Original corrupt image fixture")
            check(preview.setLastModified(timestamp + 2_000))
        }
        expectFallback()
        resumeAfter {
            preview.writeBytes(byteArrayOf())
            check(preview.setLastModified(timestamp + 3_000))
        }
        expectFallback()
        resumeAfter { check(preview.delete()) }
        expectFallback()
        assertEquals("Fallback paths must not alter the save", originalStateHash, hash(state))
    }

    @Test
    fun foregroundFileReplacementAndRemovalRefreshWithoutChangingSelection() {
        val game = game(1)
        val (state, preview) = pair(game, 1, GREEN, timestamp)
        val originalStateHash = hash(state)
        selected.value = game
        installContent()
        expectImage(game, GREEN)
        writeImage(preview, RED)
        check(preview.setLastModified(timestamp + 1_000))
        expectImage(game, RED)
        preview.writeText("Corrupt while the launcher remains resumed")
        check(preview.setLastModified(timestamp + 2_000))
        expectFallback()
        writeImage(preview, BLUE)
        check(preview.setLastModified(timestamp + 3_000))
        expectImage(game, BLUE)
        check(preview.delete())
        expectFallback()
        assertEquals("File observation remains read-only", originalStateHash, hash(state))
    }

    @Test
    fun newestUsablePairWinsAndUnpairedOrInvalidNewerImagesAreSkipped() {
        val game = game(1)
        pair(game, 1, GREEN, timestamp)
        pair(game, 2, RED, timestamp + 5_000)
        val stale = pair(game, 3, BLUE, timestamp + 10_000)
        check(stale.second.setLastModified(timestamp))
        val corrupt = pair(game, 4, BLUE, timestamp + 15_000)
        corrupt.second.writeText("Not a JPEG")
        check(corrupt.second.setLastModified(timestamp + 16_000))
        selected.value = game
        installContent()
        expectImage(game, RED)

        // The remaining green image has no paired state after removal and cannot substitute.
        resumeAfter {
            val stateOne = stateFile(game, 1)
            val stateTwo = stateFile(game, 2)
            check(stateOne.delete())
            stateTwo.writeBytes(byteArrayOf())
        }
        expectFallback()
        compose.runOnIdle { selected.value = game(7).copy(systemId = "unknown-original-fixture") }
        expectFallback()
        compose.runOnIdle { selected.value = null }
        expectFallback()
    }

    private fun installContent() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    SavedGamePreview(game = selected.value, modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().background(Color.Magenta).testTag(FALLBACK))
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun resumeAfter(change: () -> Unit) {
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        change()
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
    }

    private fun expectFallback() {
        Log.i(TAG, "fixture=$unique stage=await-fallback")
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithTag(FALLBACK).assertIsDisplayed()
                compose.onAllNodes(hasTestTag(PREVIEW)).fetchSemanticsNodes().isEmpty()
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithTag(PREVIEW).assertDoesNotExist()
        Log.i(TAG, "fixture=$unique stage=fallback-confirmed")
    }

    private fun expectImage(
        game: Game,
        expected: Int,
    ) {
        Log.i(TAG, "fixture=$unique stage=await-image game=${game.id} expected=$expected")
        compose.waitUntil(30_000) {
            try {
                compose.onNodeWithContentDescription("Saved gameplay for ${game.title}").assertIsDisplayed()
                val pixels = compose.onNodeWithTag(PREVIEW).captureToImage().toPixelMap()
                val actual = pixels[pixels.width / 2, pixels.height / 2]
                val red = (expected shr 16 and 255) / 255f
                val green = (expected shr 8 and 255) / 255f
                val blue = (expected and 255) / 255f
                abs(actual.red - red) < .04f && abs(actual.green - green) < .04f && abs(actual.blue - blue) < .04f
            } catch (_: AssertionError) {
                false
            }
        }
        compose.onNodeWithTag(FALLBACK).assertDoesNotExist()
        Log.i(TAG, "fixture=$unique stage=image-confirmed game=${game.id} expected=$expected")
    }

    private fun pair(
        game: Game,
        slot: Int,
        color: Int,
        modifiedAt: Long,
    ): Pair<File, File> {
        val state = stateFile(game, slot)
        val preview =
            File(checkNotNull(context.getExternalFilesDir("state-previews")), "fceumm/${game.fileName}.slot$slot.jpg")
        for (file in listOf(state, preview)) {
            check(!file.exists()) { "Refusing to overwrite an existing file" }
            check(file.parentFile!!.isDirectory || file.parentFile!!.mkdirs())
            ownedFiles += file
        }
        state.writeText("Original QA preview-pair marker $unique; not an emulator save")
        check(state.setLastModified(modifiedAt))
        writeImage(preview, color)
        check(preview.setLastModified(modifiedAt + 500))
        return state to preview
    }

    private fun stateFile(
        game: Game,
        slot: Int,
    ) = File(checkNotNull(context.getExternalFilesDir("states")), "fceumm/${game.fileName}.slot$slot")

    private fun writeImage(
        file: File,
        color: Int,
    ) {
        val bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(color)
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }

    private fun game(index: Int) =
        Game(
            id = -index,
            fileName = "OriginalPreviewFixture-$unique-$index.nes",
            fileUri = "content://emuui.invalid/preview/$unique/$index",
            title = "Original preview fixture $index",
            systemId = "nes",
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )

    private fun hash(file: File) =
        MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "SavedPreviewRuntimeQA"
        const val PREVIEW = "launcher_saved_gameplay_preview"
        const val FALLBACK = "qa_saved_preview_fallback"
        const val GREEN = 0xFF20D060.toInt()
        const val RED = 0xFFD03020.toInt()
        const val BLUE = 0xFF2030D0.toInt()
    }
}
