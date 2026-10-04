package com.swordfish.lemuroid.app.mobile.feature.emuui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class CutoutGeometryTest {
    @Test fun essentialInsetsDoNotChangeRawFoldOrRelaxItsHardwareGate() {
        val raw = FoldGeometry.resolve(1400, 1100, FoldBounds(0, 530, 1400, 550, FoldAxis.HORIZONTAL), 12)
        val safe = FoldGeometry.safeBounds(1400, 1100, FoldInsets(24, 72, 36, 48))
        assertEquals(FoldRect(24, 72, 1364, 1052), safe)
        assertEquals(FoldRect(24, 72, 1364, 518), raw.upper.intersection(safe))
        assertEquals(FoldRect(24, 562, 1364, 1052), raw.lower.intersection(safe))
        assertEquals(FoldRect(0, 0, 1400, 518), raw.upper)
        assertEquals(FoldGuidance.OPEN_FOLDABLE, FoldGeometry.resolve(1400, 1100, null).guidance)
    }

    @Test fun malformedInsetsClampToTheWindowWithoutInvertingBounds() {
        assertEquals(FoldRect(0, 0, 1400, 1100), FoldGeometry.safeBounds(1400, 1100, FoldInsets(-10, -10, -10, -10)))
        assertEquals(
            FoldRect(1400, 1100, 1400, 1100),
            FoldGeometry.safeBounds(1400, 1100, FoldInsets(9000, 9000, 9000, 9000)),
        )
        assertEquals(FoldRect(0, 0, 0, 0), FoldGeometry.safeBounds(-1, -1, FoldInsets(10, 10, 10, 10)))
    }

    @Test fun preciseCornerCutoutReclaimsTheWholeTopBandForActualNativePixels() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val notch = FoldRect(40, 0, 110, 80)
        val screen = FoldGeometry.fitInsidePanel(panel, 4f / 3f, occlusions = listOf(notch))!!
        val oldInsetFit = FoldGeometry.fitInsidePanel(panel.copy(bounds = panel.bounds.copy(top = 80)), 4f / 3f)!!
        assertEquals(FoldGeometry.fitInsidePanel(panel, 4f / 3f), screen)
        assertEquals(502f, screen.height, 0.001f)
        assertTrue(screen.height > oldInsetFit.height + 70f)
        assertFalse(screen.overlaps(notch))
        assertSafe(screen, panel, listOf(notch))
    }

    @Test fun aNarrowTopCameraCanKeepMaximumSizeByMovingTheCompleteImageBesideIt() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val notch = FoldRect(650, 0, 660, 70)
        val screen = FoldGeometry.fitInsidePanel(panel, 4f / 3f, occlusions = listOf(notch))!!
        assertEquals(502f, screen.height, 0.001f)
        assertTrue(screen.left >= 661f)
        assertEquals(252f, (screen.top + screen.bottom) / 2, 0.001f)
        assertSafe(screen, panel, listOf(notch))
    }

    @Test fun broadTopNotchFitsBelowItWithoutStretchingOrCroppingTheImage() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val notch = FoldRect(0, 0, 1400, 60)
        val screen = FoldGeometry.fitInsidePanel(panel, 4f / 3f, occlusions = listOf(notch))!!
        assertEquals(61f, screen.top, 0.001f)
        assertEquals(503f, screen.bottom, 0.001f)
        assertEquals(4f / 3f, screen.width / screen.height, 0.00001f)
        assertSafe(screen, panel, listOf(notch))
    }

    @Test fun fullyOccludedPanelHasNoNativeViewport() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 400, 300), 0, 24)
        assertNull(FoldGeometry.fitInsidePanel(panel, 4f / 3f, occlusions = listOf(panel.bounds)))
    }

    @Test fun irrelevantOrEmptyCutoutsDoNotMoveTheImage() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        assertEquals(
            FoldGeometry.fitInsidePanel(panel, 1f),
            FoldGeometry.fitInsidePanel(
                panel,
                1f,
                occlusions = listOf(FoldRect(-80, 0, -10, 70), FoldRect(0, 520, 1400, 550), FoldRect(700, 0, 700, 90)),
            ),
        )
    }

    @Test fun exactBoundsWinOverConservativeEdgeInsetsAndUnknownShapesKeepTheSafeBands() {
        val notch = FoldRect(20, 0, 60, 80)
        assertEquals(listOf(notch), FoldGeometry.cutoutOcclusions(1400, 1100, listOf(notch), FoldInsets(top = 80)))
        assertEquals(
            listOf(FoldRect(0, 0, 1400, 80), FoldRect(0, 1050, 1400, 1100)),
            FoldGeometry.cutoutOcclusions(1400, 1100, emptyList(), FoldInsets(top = 80, bottom = 50)),
        )
        assertTrue(FoldGeometry.cutoutOcclusions(1400, 1100, emptyList(), FoldInsets()).isEmpty())
    }

    @Test fun cutoutSnapshotUsesWindowCoordinatesAndSubtractsTheActualRootOffset() {
        val snapshot =
            WindowCutoutSnapshot(
                bounds = listOf(FoldRect(90, 40, 130, 90)),
                safeInsets = FoldInsets(top = 90),
                windowWidth = 1500,
                windowHeight = 1200,
            )
        assertEquals(listOf(FoldRect(40, 10, 80, 60)), snapshot.localOcclusions(1400, 1100, 50, 30))
    }

    @Test fun resizingOrRemovingCutoutRestoresTheOriginalMaximumFit() {
        val original = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val resized = FoldGeometry.displayPanel(FoldRect(0, 0, 1600, 604), 0, 36)
        val notch = listOf(FoldRect(0, 0, 1400, 80))
        val limited = FoldGeometry.fitInsidePanel(original, 4f / 3f, occlusions = notch)!!
        val larger = FoldGeometry.fitInsidePanel(resized, 4f / 3f, occlusions = notch)!!
        val restored = FoldGeometry.fitInsidePanel(original, 4f / 3f, occlusions = emptyList())!!
        assertTrue(larger.height > limited.height)
        assertEquals(502f, restored.height, 0.001f)
        assertEquals(252f, (restored.top + restored.bottom) / 2, 0.001f)
    }

    @Test fun upperCutoutNeverChangesLowerIndependentScaleCenterOrTouchMapping() {
        val upper = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val lower = FoldGeometry.displayPanel(FoldRect(450, 624, 950, 1128), 0, 36)
        val ordinary = FoldGeometry.independentDualScreen(upper, lower)!!
        val withCutout = FoldGeometry.independentDualScreen(upper, lower, listOf(FoldRect(0, 0, 1400, 80)))!!
        assertEquals(ordinary.lowerScreen, withCutout.lowerScreen)
        assertEquals(876f, (withCutout.lowerScreen.top + withCutout.lowerScreen.bottom) / 2, 0.001f)
        assertEquals(128 to 96, withCutout.touchCoordinates(700f, 876f))
        assertEquals(0 to 0, withCutout.touchCoordinates(withCutout.lowerScreen.left, withCutout.lowerScreen.top))
        assertEquals(
            255 to 191,
            withCutout.touchCoordinates(withCutout.lowerScreen.right - 0.01f, withCutout.lowerScreen.bottom - 0.01f),
        )
        assertNull(withCutout.touchCoordinates(withCutout.upperScreen.left, withCutout.upperScreen.top))
        assertNull(withCutout.touchCoordinates(700f, 570f))
    }

    @Test fun multipleCutoutSweepKeepsEveryNativeCornerAndRasterEdgeVisible() {
        for (ratio in listOf(0.5f, 1f, 4f / 3f, 16f / 9f, 2.4f)) {
            for (width in listOf(400, 900, 1400)) {
                for (height in listOf(240, 504, 700)) {
                    val panel = FoldGeometry.displayPanel(FoldRect(10, 20, width + 10, height + 20), 0, 36)
                    val obstacles =
                        listOf(
                            FoldRect(width / 2 - 25, 0, width / 2 + 25, 75),
                            FoldRect(width - 60, height - 40, width + 20, height + 40),
                        )
                    val screen = FoldGeometry.fitInsidePanel(panel, ratio, occlusions = obstacles)!!
                    assertEquals(ratio, screen.width / screen.height, 0.00002f)
                    assertSafe(screen, panel, obstacles)
                }
            }
        }
    }

    @Test fun smallRoundedPanelFitMatchesAnIndependentPixelGridUpperBound() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 120, 90), 0, 12)
        for (ratio in listOf(0.75f, 1f, 4f / 3f, 2f)) {
            for (obstacles in listOf(
                listOf(FoldRect(50, 0, 70, 20)),
                listOf(FoldRect(0, 0, 20, 30), FoldRect(100, 70, 120, 90)),
                listOf(FoldRect(20, 0, 30, 15), FoldRect(90, 0, 100, 15)),
            )) {
                val screen = FoldGeometry.fitInsidePanel(panel, ratio, occlusions = obstacles)!!
                assertSafe(screen, panel, obstacles)
                val taller = screen.height + 1.5f
                for (x in 1..119) {
                    for (y in 1..89) {
                        val candidate = ScreenRect(x.toFloat(), y.toFloat(), x + taller * ratio, y + taller)
                        assertFalse(
                            "A larger complete image must not fit: $candidate / $screen",
                            isSafe(candidate, panel, obstacles),
                        )
                    }
                }
            }
        }
    }

    private fun assertSafe(
        screen: ScreenRect,
        panel: DisplayPanelLayout,
        obstacles: List<FoldRect>,
    ) = assertTrue(
        "Complete image must clear rounded backing and all cutouts: $screen / $obstacles",
        isSafe(screen, panel, obstacles),
    )

    private fun isSafe(
        screen: ScreenRect,
        panel: DisplayPanelLayout,
        obstacles: List<FoldRect>,
    ): Boolean {
        // Tolerate Float storage error only; the physical one-pixel raster guard remains.
        if (obstacles.any { screen.overlaps(it, 0.999f) }) return false
        val bounds = panel.bounds
        val radius = panel.cornerRadius.toFloat()
        for (x in listOf(screen.left - 1, screen.right + 1)) {
            for (y in listOf(screen.top - 1, screen.bottom + 1)) {
                if (x < bounds.left - 0.001f || x > bounds.right + 0.001f ||
                    y < bounds.top - 0.001f || y > bounds.bottom + 0.001f
                ) {
                    return false
                }
                val dx = x - max(bounds.left + radius, min(x, bounds.right - radius))
                val dy = y - max(bounds.top + radius, min(y, bounds.bottom - radius))
                if (dx * dx + dy * dy > radius * radius + 0.02f) return false
            }
        }
        return true
    }
}
