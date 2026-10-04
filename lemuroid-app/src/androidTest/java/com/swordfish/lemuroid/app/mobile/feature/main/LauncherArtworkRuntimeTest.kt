package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.ThemePreferences
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.Callable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Real MainActivity / production Coil / existing owned QA row.
 * No replacement composition, posture bypass, row insertion, network art, or game launch.
 * Run only with the coordinator's API29 emulator and existing lawful fixtures.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LauncherArtworkRuntimeTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val args get() = InstrumentationRegistry.getArguments()
    private val db get() = compose.activity.retrogradeDb
    private val preferences get() = SharedPreferencesHelper.getSharedPreferences(context)
    private val density get() = compose.activity.resources.displayMetrics.density
    private lateinit var directory: File
    private lateinit var manifest: JSONObject

    @Test
    fun originalCacheArtworkCropsAndFillsCardsInBothThemes() {
        assertEquals("This bounded palette proof targets the API29 neutral fallback", 29, Build.VERSION.SDK_INT)
        val run = requireNotNull(args.getString("qaCaptureRun")) { "Supply a unique qaCaptureRun" }
        require(run.matches(Regex("[A-Za-z0-9._-]+")) && run != "." && run != "..")
        directory = File(checkNotNull(context.getExternalFilesDir("qa-artwork")), run).apply { mkdirs() }
        val manifestFile = File(directory, "restore-manifest.json")
        val stage = args.getString("stage") ?: "capture"
        if (stage == "restore-only") {
            check(manifestFile.isFile) { "No manifest: never guess the original URL" }
            manifest = JSONObject(AtomicFile(manifestFile).openRead().bufferedReader().use { it.readText() })
            restoreCover()
            // A process interruption may also have left the last chosen theme active.
            openConsole()
            restoreTheme()
            return
        }
        require(stage == "capture") { "stage must be capture or restore-only" }
        check(!manifestFile.exists()) { "Use a new run, or stage=restore-only for the existing manifest" }
        val games = runBlocking { db.gameDao().observeLibrary().first() }
        val original = games.single { it.fileName == "EmuUI_DS_Legacy_Long_Title_QA.nds" }
        check(original.title == "EmuUI_DS_Legacy_Long_Title_QA" && original.systemId == "nds")
        val alternate = games.firstOrNull { it.fileName in setOf("EmuUI_QA.nes", "EmuUI_QA_Second.nes") }
        checkNotNull(alternate) { "A second existing owned lawful QA fixture is required" }
        val artworkFile = File(context.cacheDir, "emuui-original-qa-artwork-$run.png")
        check(!artworkFile.exists()) { "Refusing to overwrite an existing cache file" }
        val source = makeOriginalArtwork()
        try {
            artworkFile.outputStream().use { check(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val temporaryUrl = Uri.fromFile(artworkFile).toString()
            manifest =
                JSONObject()
                    .put("run", run)
                    .put("evidenceType", "Original diagnostic PNG through production Coil; no acquisition claim")
                    .put("createdAtEpochMs", System.currentTimeMillis())
                    .put("originalRow", rowJson(original)).put("temporaryCoverFrontUrl", temporaryUrl)
                    .put(
                        "originalThemeValue",
                        preferences.getString(ThemePreferences.THEME_MODE_KEY, null) ?: JSONObject.NULL,
                    )
                    .put("libraryIdsBefore", JSONArray(games.map { it.id }))
                    .put("coverRestored", false).put("themeRestored", false)
            saveManifest() // Durable before touching Room, including the null-vs-string original cover.
            try {
                db.runInTransaction {
                    val current = checkNotNull(db.gameDao().selectByFileUri(original.fileUri))
                    check(current.id == original.id && current.coverFrontUrl == original.coverFrontUrl)
                    db.gameDao().update(listOf(current.copy(coverFrontUrl = temporaryUrl)))
                }
                assertEquals(temporaryUrl, runBlocking { db.gameDao().selectById(original.id) }?.coverFrontUrl)
                openConsole() // Wait for actual composition before the replay=0 publisher emits.
                for (theme in listOf("Light", "Dark")) {
                    chooseTheme(theme)
                    waitForTag("launcher_game_${alternate.id}")
                    compose.onNodeWithTag("launcher_game_${alternate.id}").performClick().assertIsSelected()
                    compose.onNodeWithTag("launcher_game_${original.id}").assertIsNotSelected()
                    val unselectedWidth = verifyAndCapture(theme, false, original, source)
                    compose.onNodeWithTag("launcher_game_${original.id}")
                        .performClick().assertIsSelected().assertTextContains(original.title)
                    val selectedWidth = verifyAndCapture(theme, true, original, source)
                    compose.onNodeWithContentDescription(
                        "Selected",
                        substring = false,
                        useUnmergedTree = true,
                    ).assertDoesNotExist()
                    assertTrue("Selection must visibly widen the outline", selectedWidth >= unselectedWidth + density)
                }
                manifest.put("allPixelAndGeometryAssertionsPassed", true)
                saveManifest()
            } finally {
                // Always repair the row first; a UI/theme failure must not block cover restoration.
                try {
                    restoreCover()
                } finally {
                    restoreTheme()
                }
            }
        } finally {
            source.recycle()
            // Keep only this test-created cache PNG for diagnostics/recovery; it is no longer referenced
            // after successful restoration. No ROM, core, save, imported file or other row is written.
        }
    }

    private fun restoreCover() {
        val original = manifest.getJSONObject("originalRow")
        check(original.getString("fileName") == "EmuUI_DS_Legacy_Long_Title_QA.nds")
        val originalUrl = if (original.isNull("coverFrontUrl")) null else original.getString("coverFrontUrl")
        val temporaryUrl = manifest.getString("temporaryCoverFrontUrl")
        val beforeAndAfter =
            db.runInTransaction(
                Callable {
                    val current = checkNotNull(db.gameDao().selectByFileUri(original.getString("fileUri")))
                    check(current.id == original.getInt("id")) { "Fixture identity changed; no row was written" }
                    check(current.coverFrontUrl == temporaryUrl || current.coverFrontUrl == originalUrl) {
                        "Cover changed outside this test; refusing to overwrite it. Inspect the saved manifest."
                    }
                    val desired = current.copy(coverFrontUrl = originalUrl)
                    if (current != desired) db.gameDao().update(listOf(desired))
                    val restored = checkNotNull(db.gameDao().selectByFileUri(current.fileUri))
                    assertEquals("Restoration must change only coverFrontUrl on the fresh row", desired, restored)
                    current to restored
                },
            )
        manifest.put("rowImmediatelyBeforeRestore", rowJson(beforeAndAfter.first))
            .put("rowAfterRestore", rowJson(beforeAndAfter.second))
            .put("coverRestored", true).put("restoredAtEpochMs", System.currentTimeMillis())
        val idsAfter = runBlocking { db.gameDao().observeLibrary().first() }.map { it.id }.sorted()
        val beforeIds = manifest.getJSONArray("libraryIdsBefore")
        assertEquals(
            "No fixture rows may be added or removed",
            (0 until beforeIds.length()).map { beforeIds.getInt(it) }.sorted(),
            idsAfter,
        )
        saveManifest()
        File(directory, "row-restoration.json").writeText(
            JSONObject().put("verified", true).put("onlyCoverFieldRestored", true)
                .put("before", rowJson(beforeAndAfter.first)).put("after", rowJson(beforeAndAfter.second)).toString(2),
        )
    }

    private fun restoreTheme() {
        val original = if (manifest.isNull("originalThemeValue")) "system" else manifest.getString("originalThemeValue")
        val label =
            when (original) {
                "light" -> "Light"
                "dark" -> "Dark"
                else -> "System"
            }
        // All theme changes, including cleanup, use the real public Settings route.
        chooseTheme(label)
        manifest.put("themeRestored", true).put("restoredThemeValue", label.lowercase())
            .put("absentThemeKeyNormalizedToSystemByUi", manifest.isNull("originalThemeValue"))
        saveManifest()
    }

    private fun chooseTheme(label: String) {
        waitForTag("launcher_preview")
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Theme").performScrollTo().performClick()
        waitForTag("console_settings_dialog")
        compose.onNode(
            hasAnyAncestor(hasTestTag("console_settings_dialog")) and hasText(label),
        ).performClick()
        compose.waitUntil(30_000) { preferences.getString(ThemePreferences.THEME_MODE_KEY, null) == label.lowercase() }
        compose.onNodeWithContentDescription("Back").performClick()
        waitForTag("launcher_preview")
        compose.waitForIdle()
    }

    private fun openConsole() {
        waitForTag("fold_guidance")
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
        waitForTag("launcher_preview")
    }

    private fun verifyAndCapture(
        theme: String,
        selected: Boolean,
        game: Game,
        source: Bitmap,
    ): Int {
        val tag = "launcher_game_${game.id}"
        waitForTag(tag)
        val card = bounds(tag)
        val art = bounds("launcher_game_art_${game.id}")
        assertTrue("Artwork must occupy the complete card, without an inset/footer", sameRect(card, art))
        assertTrue("The Crop proof needs a square card", abs(card.width - card.height) <= 2f)
        assertTrue("Use the approved near-square QA window with cards at least 96dp", card.width >= 96f * density)
        val center = bounds("launcher_library_center")
        val left = bounds("launcher_left_controls")
        val right = bounds("launcher_right_controls")
        val preview = bounds("launcher_preview")
        assertTrue(
            "Card must remain in the central library",
            card.left >= center.left && card.right <= center.right &&
                card.top >= center.top && card.bottom <= center.bottom,
        )
        assertTrue(
            "Wings must bracket the library below the preview",
            left.right <= center.left + 1f && right.left >= center.right - 1f &&
                left.top >= preview.bottom && right.top >= preview.bottom,
        )
        val borderColor =
            Color.parseColor(
                if (theme == "Light") {
                    if (selected) "#4C5963" else "#C5C7C8"
                } else {
                    if (selected) "#C1CDD6" else "#45484B"
                },
            )
        val offset = IntArray(2)
        compose.runOnIdle {
            val inWindow = IntArray(2)
            compose.activity.window.decorView.getLocationOnScreen(offset)
            compose.activity.window.decorView.getLocationInWindow(inWindow)
            offset[0] -= inWindow[0]
            offset[1] -= inWindow[1]
        }
        val physical =
            Rect(
                card.left + offset[0],
                card.top + offset[1],
                card.right + offset[0],
                card.bottom + offset[1],
            )
        var lastFailure = "No screenshot"
        var report = JSONObject()
        var accepted: Bitmap? = null
        var last: Bitmap? = null
        var borderWidth = 0
        try {
            // Pixel polling proves asynchronous Coil completion. It does not trust the URL or tag.
            compose.waitUntil(30_000) {
                last?.recycle()
                last = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                runCatching {
                    report = inspectPixels(checkNotNull(last), source, physical, borderColor, selected)
                    borderWidth = report.getInt("borderWidthPx")
                    true
                }.getOrElse {
                    lastFailure = it.message ?: it.toString()
                    false
                }
            }
            accepted = last
            last = null
        } catch (failure: Throwable) {
            val failureName = "${theme.lowercase()}-${if (selected) "selected" else "unselected"}-failure.png"
            last?.let { writePng(File(directory, failureName), it) }
            File(directory, "pixel-failure.txt").writeText(lastFailure)
            throw AssertionError("Production artwork pixel verification failed: $lastFailure", failure)
        } finally {
            last?.recycle()
        }
        val name = "${theme.lowercase()}-${if (selected) "selected" else "unselected"}"
        val screenshot = checkNotNull(accepted)
        try {
            writePng(File(directory, "$name.png"), screenshot)
            val regions = JSONObject()
            listOf(
                "launcher_preview",
                "launcher_preview_surface",
                "launcher_library_center",
                "launcher_left_controls",
                "launcher_right_controls",
                "launcher_title_plaque",
                tag,
                "launcher_game_art_${game.id}",
            ).forEach { regions.put(it, rectJson(bounds(it))) }
            report.put(
                "evidenceType",
                "Actual MainActivity and production Coil, original local PNG, injected WindowManager metadata",
            )
                .put("theme", theme).put("selected", selected)
                .put("themePreference", preferences.getString(ThemePreferences.THEME_MODE_KEY, null))
                .put("screenWidth", screenshot.width).put("screenHeight", screenshot.height)
                .put("density", density).put("windowOriginOnScreenX", offset[0]).put("windowOriginOnScreenY", offset[1])
                .put("capturedAtEpochMs", System.currentTimeMillis()).put("boundsInWindow", regions)
                .put(
                    "previewNote",
                    "Production preview uses Fit plus theme gradient and overlays; card pixel proof is Crop",
                )
            File(directory, "$name.json").writeText(report.toString(2))
        } finally {
            screenshot.recycle()
        }
        return borderWidth
    }

    private fun inspectPixels(
        bitmap: Bitmap,
        source: Bitmap,
        card: Rect,
        border: Int,
        selected: Boolean,
    ): JSONObject {
        val inset = 3f * density + 4f // Outside the selected 3dp outline and inside all four image edges.
        val samples =
            listOf(
                "near-top-left" to Pair(card.width * .25f, inset),
                "near-top-right" to Pair(card.width * .56f, inset),
                "near-bottom-left" to Pair(card.width * .25f, card.height - inset),
                "near-left-upper" to Pair(inset, card.height * .25f),
                "near-left-lower" to Pair(inset, card.height * .75f),
                "near-right-upper" to Pair(card.width - inset, card.height * .38f),
                "quadrant-lower-right" to Pair(card.width * .56f, card.height * .60f),
            ) +
                if (selected) {
                    // Former checkmark area: inside the 3dp border, outside the rounded corner.
                    listOf(
                        "former-badge-center" to Pair(card.width * .88f, card.height * .88f),
                        "former-badge-left" to Pair(card.width * .84f, card.height * .88f),
                        "former-badge-top" to Pair(card.width * .88f, card.height * .84f),
                    )
                } else {
                    emptyList()
                }
        val scale = max(card.width / source.width, card.height / source.height)
        val cropX = (source.width * scale - card.width) / 2f
        val cropY = (source.height * scale - card.height) / 2f
        val pixelReport = JSONArray()
        for ((label, point) in samples) {
            val sx = ((point.first + cropX) / scale).roundToInt().coerceIn(0, source.width - 1)
            val sy = ((point.second + cropY) / scale).roundToInt().coerceIn(0, source.height - 1)
            val expected = source.getPixel(sx, sy)
            val x = (card.left + point.first).roundToInt()
            val y = (card.top + point.second).roundToInt()
            val actual = bitmap.getPixel(x, y)
            assertTrue(
                "$label must show source Crop pixel at ($sx,$sy), " +
                    "expected ${hex(expected)} got ${hex(actual)}",
                closeColor(actual, expected, 22),
            )
            pixelReport.put(
                JSONObject().put("sample", label).put("x", x).put("y", y)
                    .put("sourceX", sx).put("sourceY", sy)
                    .put("expected", hex(expected)).put("actual", hex(actual)),
            )
        }
        // Straight top edge, away from corner rounding and every selected/favorite overlay.
        val x = (card.left + card.width * .30f).roundToInt()
        val edgeY = card.top.roundToInt()
        val scan = (0..(6f * density).roundToInt()).map { bitmap.getPixel(x, edgeY + it) }
        val matching = scan.withIndex().filter { closeColor(it.value, border, 20) }.map { it.index }
        check(matching.isNotEmpty()) {
            "Visible ${if (selected) "selected" else "unselected"} outline must cover the artwork edge"
        }
        val width = matching.size
        val expectedWidth = if (selected) 3f * density else density
        assertTrue("Outline width $width must approximate ${expectedWidth}px", abs(width - expectedWidth) <= 1.5f)
        assertTrue("Outline must start at the card edge", matching.first() <= 1)
        return JSONObject().put("samples", pixelReport).put("cropScale", scale)
            .put("cropOffsetX", cropX).put("cropOffsetY", cropY)
            .put("borderWidthPx", width).put("expectedBorderColor", hex(border))
            .put("borderScan", JSONArray(scan.map(::hex)))
    }

    private fun makeOriginalArtwork(): Bitmap {
        val bitmap = Bitmap.createBitmap(640, 400, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        fun block(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            color: String,
        ) {
            paint.color = Color.parseColor(color)
            canvas.drawRect(left, top, right, bottom, paint)
        }
        block(0f, 0f, 320f, 200f, "#ED3B68")
        block(320f, 0f, 640f, 200f, "#00B5E2")
        block(0f, 200f, 320f, 400f, "#FFC13B")
        block(320f, 200f, 640f, 400f, "#8246D8")
        block(0f, 0f, 24f, 400f, "#91FF00") // Source left/right edges are visibly cropped out on square cards.
        block(616f, 0f, 640f, 400f, "#102010")
        block(24f, 0f, 616f, 12f, "#FF00FF")
        block(24f, 388f, 616f, 400f, "#FFFFFF")
        paint.color = Color.BLACK
        paint.textSize = 23f
        paint.isAntiAlias = true
        canvas.drawText("ORIGINAL QA SAMPLE • NO GAME ART", 70f, 195f, paint)
        paint.textSize = 17f
        canvas.drawText("TL", 65f, 50f, paint)
        canvas.drawText("TR", 545f, 50f, paint)
        canvas.drawText("BL", 65f, 365f, paint)
        canvas.drawText("BR", 545f, 365f, paint)
        return bitmap
    }

    private fun bounds(tag: String) =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInWindow

    private fun waitForTag(tag: String) {
        compose.waitUntil(60_000) {
            runCatching {
                compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
                bounds(tag).let { it.width > 0 && it.height > 0 }
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private fun sameRect(
        a: Rect,
        b: Rect,
    ) = abs(a.left - b.left) <= 1f && abs(a.top - b.top) <= 1f &&
        abs(a.right - b.right) <= 1f && abs(a.bottom - b.bottom) <= 1f

    private fun closeColor(
        a: Int,
        b: Int,
        tolerance: Int,
    ) = abs(Color.red(a) - Color.red(b)) <= tolerance &&
        abs(Color.green(a) - Color.green(b)) <= tolerance &&
        abs(Color.blue(a) - Color.blue(b)) <= tolerance

    private fun hex(color: Int) = String.format("#%08X", color)

    private fun rectJson(r: Rect) =
        JSONObject().put("left", r.left).put("top", r.top).put("right", r.right).put("bottom", r.bottom)

    private fun rowJson(g: Game) =
        JSONObject().put("id", g.id).put("fileName", g.fileName).put("fileUri", g.fileUri).put("title", g.title)
            .put("systemId", g.systemId).put("developer", g.developer ?: JSONObject.NULL)
            .put("coverFrontUrl", g.coverFrontUrl ?: JSONObject.NULL).put("lastIndexedAt", g.lastIndexedAt)
            .put("lastPlayedAt", g.lastPlayedAt ?: JSONObject.NULL).put("isFavorite", g.isFavorite)

    private fun writePng(
        file: File,
        bitmap: Bitmap,
    ) = file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }

    private fun saveManifest() {
        val file = AtomicFile(File(directory, "restore-manifest.json"))
        val output = file.startWrite()
        try {
            output.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (failure: Throwable) {
            file.failWrite(output)
            throw failure
        }
    }
}
