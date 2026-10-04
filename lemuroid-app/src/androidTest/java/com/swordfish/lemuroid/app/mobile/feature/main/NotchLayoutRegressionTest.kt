package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Insets
import android.graphics.Rect
import android.os.Build
import android.view.DisplayCutout
import android.view.View
import android.view.WindowInsets
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/** Delivered framework cutouts on a real emulator; not physical camera or OEM certification. */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class NotchLayoutRegressionTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launcherCornersUseOneStatusInsetAndOnlyCrowdedBrandCompacts() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseline = InstrumentationRegistry.getArguments().getString("baseline") == "true"
        val content = compose.activity.findViewById<View>(android.R.id.content)
        val original = content.rootWindowInsets
        val statusBarTop = original.getInsets(WindowInsets.Type.statusBars()).top
        val output = File(instrumentation.targetContext.getExternalFilesDir(null), "local-notch-qa").apply { mkdirs() }
        val evidence = JSONObject().put("baseline", baseline).put("cutoutSource", "test-only framework dispatch")

        fun publish() {
            windowInfo.overrideWindowLayoutInfo(
                TestWindowLayoutInfo(
                    listOf(
                        TestFoldingFeature(
                            activity = compose.activity,
                            state = FoldingFeature.State.FLAT,
                            orientation = FoldingFeature.Orientation.HORIZONTAL,
                        ),
                    ),
                ),
            )
            compose.waitUntil(30_000) {
                compose.onAllNodes(hasTestTag("launcher_corner_brand"), useUnmergedTree = true)
                    .fetchSemanticsNodes().any { it.boundsInWindow.width > 0 }
            }
        }
        publish()
        val width = content.width
        val cameraBottom = 96
        val cases =
            listOf(
                "normal" to null,
                "corner" to Rect(70, 0, 150, cameraBottom),
                "center" to Rect(width / 2 - 40, 0, width / 2 + 40, cameraBottom),
                "broad" to Rect(0, 0, width, cameraBottom),
                "restored" to null,
            )
        try {
            for ((name, camera) in cases) {
                val delivered =
                    WindowInsets.Builder(original)
                        .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, statusBarTop, 0, 0))
                        .setVisible(WindowInsets.Type.statusBars(), true)
                        .setDisplayCutout(camera?.let { DisplayCutout(Rect(0, cameraBottom, 0, 0), listOf(it)) })
                        .build()
                instrumentation.runOnMainSync { content.dispatchApplyWindowInsets(delivered) }
                compose.waitForIdle()
                val brand = compose.onNodeWithTag("launcher_corner_brand", useUnmergedTree = true).fetchSemanticsNode()
                val tools = compose.onNodeWithTag("launcher_corner_tools", useUnmergedTree = true).fetchSemanticsNode()
                val rects = listOf("brand" to brand, "tools" to tools)
                val row = JSONObject()
                for ((label, node) in rects) {
                    val r = node.boundsInWindow
                    row.put(
                        label,
                        JSONObject().put(
                            "left",
                            r.left,
                        ).put("top", r.top).put("right", r.right).put("bottom", r.bottom),
                    )
                }
                evidence.put(name, row)
                output.resolve("launcher-$name.png").outputStream().use {
                    instrumentation.uiAutomation.takeScreenshot().compress(
                        android.graphics.Bitmap.CompressFormat.PNG,
                        100,
                        it,
                    )
                }
                if (!baseline) {
                    val a = brand.boundsInWindow
                    val b = tools.boundsInWindow
                    val density = compose.activity.resources.displayMetrics.density
                    assertEquals("Top actions share an edge", a.top, b.top, 1f)
                    assertEquals("Top actions share a height", a.height, b.height, 1f)
                    assertTrue("Logo retains 48dp target", a.width >= 48 * density - 1 && a.height >= 48 * density - 1)
                    if (camera != null) {
                        for (r in listOf(a, b)) {
                            assertFalse(
                                "Action must avoid camera",
                                r.left < camera.right && r.right > camera.left &&
                                    r.top < camera.bottom && r.bottom > camera.top,
                            )
                        }
                    }
                    if (name == "normal" || name == "center" || name == "restored") {
                        assertEquals("No redundant top margin after status bar", statusBarTop.toFloat(), a.top, 1f)
                        compose.onNodeWithTag("launcher_brand_label", useUnmergedTree = true).assertIsDisplayed()
                    } else {
                        assertTrue(
                            "Crowded brand has no visible text",
                            compose.onAllNodes(hasTestTag("launcher_brand_label"), useUnmergedTree = true)
                                .fetchSemanticsNodes().none { it.boundsInWindow.width > 0 },
                        )
                    }
                    for (tag in listOf("launcher_corner_count", "launcher_corner_play")) {
                        val lower =
                            compose.onNodeWithTag(
                                tag,
                                useUnmergedTree = true,
                            ).fetchSemanticsNode().boundsInWindow
                        assertEquals("All floating corners share height", a.height, lower.height, 1f)
                    }
                }
            }
            if (!baseline) {
                compose.onNodeWithTag("launcher_corner_brand", useUnmergedTree = true).performClick()
                compose.onNodeWithTag("launcher_corner_brand", useUnmergedTree = true).assertIsDisplayed()
            }
        } finally {
            output.resolve("launcher-geometry.json").writeText(evidence.toString(2))
            instrumentation.runOnMainSync { content.dispatchApplyWindowInsets(original) }
        }
    }
}
