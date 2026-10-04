package com.swordfish.lemuroid.app.mobile.feature.emuui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldGeometryTest {
    @Test fun unrepresentablyThinAspectDoesNotProduceDegenerateViewport() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1440, 600), 0, 36)
        assertNull(FoldGeometry.fitInsidePanel(panel, Float.MAX_VALUE))
        assertNull(FoldGeometry.fitInsidePanel(panel, Float.MIN_VALUE))
    }

    @Test fun ordinaryLandscapePhoneRequiresOpenInnerDisplay() {
        val layout = FoldGeometry.resolve(1200, 800, null, 12)
        assertEquals(FoldGuidance.OPEN_FOLDABLE, layout.guidance)
        assertNull(FoldGeometry.dualScreen(layout, 240))
        assertEquals(FoldRect(0, 0, 1200, 388), layout.upper)
        assertEquals(FoldRect(0, 412, 1200, 800), layout.lower)
        assertFalse(layout.physicalHinge)
    }

    @Test fun zeroHeightPhysicalCreaseStillGetsSafeBand() {
        val fold = FoldBounds(0, 370, 1200, 370, FoldAxis.HORIZONTAL)
        val layout = FoldGeometry.resolve(1200, 800, fold, 16)
        assertEquals(354, layout.upper.bottom)
        assertEquals(386, layout.lower.top)
        assertTrue(layout.physicalHinge)
    }

    @Test fun hingeWidthAndOffCenterPositionAreHonored() {
        val layout = FoldGeometry.resolve(1800, 1200, FoldBounds(0, 480, 1800, 512, FoldAxis.HORIZONTAL), 20)
        assertEquals(460, layout.upper.bottom)
        assertEquals(532, layout.lower.top)
        assertEquals(72, layout.hinge.height)
        assertFalse(layout.upper.contains(500f, 480f))
        assertFalse(layout.lower.contains(500f, 500f))
    }

    @Test fun verticalHingeRequiresRotationInsteadOfBookMode() {
        assertEquals(
            FoldGuidance.ROTATE_HINGE,
            FoldGeometry.resolve(1200, 800, FoldBounds(590, 0, 610, 800, FoldAxis.VERTICAL)).guidance,
        )
    }

    @Test fun portraitRequiresLandscapeEvenWithAHinge() {
        assertEquals(
            FoldGuidance.ROTATE_LANDSCAPE,
            FoldGeometry.resolve(800, 1200, FoldBounds(0, 590, 800, 610, FoldAxis.HORIZONTAL)).guidance,
        )
    }

    @Test fun windowOffsetIsAppliedBeforeResolving() {
        val localFold = FoldBounds(100, 600, 1300, 620, FoldAxis.HORIZONTAL).relativeTo(100, 200)
        val layout = FoldGeometry.resolve(1200, 800, localFold, 10)
        assertEquals(390, layout.upper.bottom)
        assertEquals(430, layout.lower.top)
    }

    @Test fun outOfWindowFoldDoesNotEnableConsoleInMultiWindow() {
        val layout = FoldGeometry.resolve(1200, 500, FoldBounds(0, 700, 1200, 720, FoldAxis.HORIZONTAL), 10)
        assertFalse(layout.physicalHinge)
        assertEquals(FoldGuidance.OPEN_FOLDABLE, layout.guidance)
        assertEquals(240, layout.upper.bottom)
    }

    @Test fun degeneratePaneRequestsLargerWindow() {
        assertEquals(
            FoldGuidance.WINDOW_TOO_SMALL,
            FoldGeometry.resolve(1200, 600, FoldBounds(0, 0, 1200, 30, FoldAxis.HORIZONTAL), 10).guidance,
        )
    }

    @Test fun dualScreensClearHingeAndReserveControls() {
        val layout = FoldGeometry.resolve(1800, 1200, FoldBounds(0, 500, 1800, 534, FoldAxis.HORIZONTAL), 18)
        val screens = FoldGeometry.dualScreen(layout, 380)!!
        assertTrue(screens.upperScreen.top >= layout.upper.top)
        assertTrue(screens.upperScreen.bottom <= layout.upper.bottom + 0.01f)
        assertTrue(screens.lowerScreen.top >= layout.lower.top - 0.01f)
        assertTrue(screens.lowerScreen.bottom <= layout.lower.bottom)
        assertTrue(screens.lowerScreen.left >= 380)
        assertTrue(screens.lowerScreen.right <= 1420)
        assertEquals(4f / 3f, screens.upperScreen.width / screens.upperScreen.height, 0.001f)
        assertEquals(screens.upperScreen.height, screens.lowerScreen.height, 0.001f)
    }

    @Test fun touchMapsLowerScreenCornersAndCenter() {
        val screens =
            FoldGeometry.dualScreen(
                FoldGeometry.resolve(1200, 800, FoldBounds(0, 400, 1200, 400, FoldAxis.HORIZONTAL), 10),
                260,
            )!!
        val touch = screens.lowerScreen
        assertEquals(0 to 0, screens.touchCoordinates(touch.left, touch.top))
        assertEquals(
            128 to 96,
            screens.touchCoordinates((touch.left + touch.right) / 2f, (touch.top + touch.bottom) / 2f),
        )
        assertEquals(255 to 191, screens.touchCoordinates(touch.right - 0.01f, touch.bottom - 0.01f))
        assertNull(screens.touchCoordinates(touch.right, touch.bottom))
        assertNull(screens.touchCoordinates(5f, touch.top))
        assertNull(screens.touchCoordinates(touch.left, 400f))
        assertNull(screens.touchCoordinates(screens.upperScreen.left, screens.upperScreen.top))
    }

    @Test fun largeHingeNeverSilentlyClipsScreens() {
        val layout = FoldGeometry.resolve(1200, 800, FoldBounds(0, 250, 1200, 550, FoldAxis.HORIZONTAL), 10)
        assertNull(FoldGeometry.dualScreen(layout, 400, maxGap = 100))
    }

    @Test fun insetsAndAsymmetricPanesKeepScreensInsideSafeBounds() {
        val raw = FoldGeometry.resolve(1600, 1000, FoldBounds(0, 460, 1600, 480, FoldAxis.HORIZONTAL), 10)
        val safe =
            raw.copy(
                upper = raw.upper.copy(left = 40, right = 1560, top = 50),
                lower = raw.lower.copy(left = 40, right = 1560, bottom = 970),
            )
        val screens = FoldGeometry.dualScreen(safe, 320)!!
        assertTrue(screens.upperScreen.top >= 50)
        assertTrue(screens.lowerScreen.bottom <= 970)
        assertEquals(800f, (screens.lowerScreen.left + screens.lowerScreen.right) / 2f, 0.01f)
    }

    @Test fun repeatedFlatHalfOpenAndReversedLandscapeAreDeterministic() {
        val flat = FoldGeometry.resolve(1400, 900, FoldBounds(0, 450, 1400, 450, FoldAxis.HORIZONTAL), 12)
        val half = FoldGeometry.resolve(1400, 900, FoldBounds(0, 434, 1400, 466, FoldAxis.HORIZONTAL), 12)
        repeat(100) {
            assertEquals(flat, FoldGeometry.resolve(1400, 900, FoldBounds(0, 450, 1400, 450, FoldAxis.HORIZONTAL), 12))
            assertEquals(half, FoldGeometry.resolve(1400, 900, FoldBounds(0, 434, 1400, 466, FoldAxis.HORIZONTAL), 12))
            assertNotNull(FoldGeometry.dualScreen(half, 300))
        }
    }

    @Test fun missingFoldNeverEnablesPortraitCoverExternalOrTabletWindows() {
        for ((width, height) in listOf(400 to 900, 900 to 400, 1600 to 1000, 1000 to 1000)) {
            assertEquals(FoldGuidance.OPEN_FOLDABLE, FoldGeometry.resolve(width, height, null).guidance)
        }
    }

    @Test fun malformedAndPartialHardwareBoundsCannotEnableConsole() {
        assertEquals(
            FoldGuidance.OPEN_FOLDABLE,
            FoldGeometry.resolve(1200, 800, FoldBounds(0, 410, 1200, 400, FoldAxis.HORIZONTAL)).guidance,
        )
        assertEquals(
            FoldGuidance.WINDOW_TOO_SMALL,
            FoldGeometry.resolve(1200, 800, FoldBounds(10, 400, 1100, 400, FoldAxis.HORIZONTAL)).guidance,
        )
    }

    @Test fun lowerWingsRespectSafeInsetsAndReserveCenter() {
        val lower = FoldRect(24, 420, 1176, 780)
        val console = FoldGeometry.lowerConsole(lower, 300, 12)
        assertEquals(FoldRect(24, 420, 324, 780), console.leftControls)
        assertEquals(FoldRect(336, 420, 864, 780), console.center)
        assertEquals(FoldRect(876, 420, 1176, 780), console.rightControls)
    }

    @Test fun oversizedWingsAndNegativeGapsNeverOverlap() {
        val lower = FoldRect(20, 410, 100, 800)
        val console = FoldGeometry.lowerConsole(lower, 1000, -4)
        assertEquals(console.leftControls.right, console.center.left)
        assertEquals(console.center.right, console.rightControls.left)
        assertEquals(0, console.center.width)
        assertEquals(lower.right, console.rightControls.right)
    }

    @Test fun dualScreenPropertySweepNeverOverlapsHinge() {
        for (hingeY in 200..700 step 25) {
            for (hingeHeight in 0..80 step 8) {
                val layout =
                    FoldGeometry.resolve(
                        1600,
                        1000,
                        FoldBounds(0, hingeY, 1600, hingeY + hingeHeight, FoldAxis.HORIZONTAL),
                        12,
                    )
                val screens = FoldGeometry.dualScreen(layout, 350) ?: continue
                assertTrue("top: $layout", screens.upperScreen.top >= -0.01f)
                assertTrue("upper: $layout", screens.upperScreen.bottom <= layout.upper.bottom + 0.01f)
                assertTrue("lower: $layout", screens.lowerScreen.top >= layout.lower.top - 0.01f)
                assertTrue("bottom: $layout", screens.lowerScreen.bottom <= 1000.01f)
            }
        }
    }

    @Test fun roundedPanelKeepsTheCompleteRectangularViewportClearOfCorners() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1200, 380), 12, 24)
        assertEquals(FoldRect(12, 12, 1188, 368), panel.bounds)
        assertEquals(FoldRect(25, 25, 1175, 355), panel.content)
        assertEquals(24, panel.cornerRadius)
        assertSafePanelCorners(panel)
    }

    @Test fun roundedPanelInsetsAndRadiusClampWithoutInvertingSmallRegions() {
        for (width in 0..150 step 3) {
            for (height in 0..100 step 5) {
                val region = FoldRect(40, 60, 40 + width, 60 + height)
                val panel = FoldGeometry.displayPanel(region, 6, 48)
                assertTrue(panel.bounds.left >= region.left && panel.bounds.right <= region.right)
                assertTrue(panel.bounds.top >= region.top && panel.bounds.bottom <= region.bottom)
                assertTrue(panel.content.left <= panel.content.right && panel.content.top <= panel.content.bottom)
                if (panel.content.width > 2 && panel.content.height > 2) assertSafePanelCorners(panel)
            }
        }
    }

    @Test fun dualScreensFitRoundedPanelSafeRectanglesAndKeepNativeTouchCorners() {
        val raw = FoldGeometry.resolve(1800, 1200, FoldBounds(0, 500, 1800, 534, FoldAxis.HORIZONTAL), 18)
        val console = FoldGeometry.lowerConsole(raw.lower, 380)
        val upper = FoldGeometry.displayPanel(raw.upper, 12, 24)
        val lower = FoldGeometry.displayPanel(console.center, 6, 24)
        val safe =
            raw.copy(
                upper = upper.content,
                lower = lower.content,
                hinge = raw.hinge.copy(top = upper.content.bottom, bottom = lower.content.top),
            )
        val screens = FoldGeometry.dualScreen(safe, 0)!!
        assertScreenInside(screens.upperScreen, upper.content)
        assertScreenInside(screens.lowerScreen, lower.content)
        assertEquals(4f / 3f, screens.upperScreen.width / screens.upperScreen.height, 0.001f)
        assertEquals(screens.upperScreen.width, screens.lowerScreen.width, 0.001f)
        assertEquals(0 to 0, screens.touchCoordinates(screens.lowerScreen.left, screens.lowerScreen.top))
        assertEquals(
            255 to 191,
            screens.touchCoordinates(screens.lowerScreen.right - 0.01f, screens.lowerScreen.bottom - 0.01f),
        )
        assertNull(screens.touchCoordinates(lower.bounds.left.toFloat(), lower.bounds.top.toFloat()))
    }

    @Test fun asymmetricPanelWidthsUseOnlyTheirSharedHorizontalRegion() {
        val raw = FoldGeometry.resolve(1600, 1000, FoldBounds(0, 490, 1600, 510, FoldAxis.HORIZONTAL), 12)
        val safe =
            raw.copy(
                upper = raw.upper.copy(left = 500, right = 1250),
                lower = raw.lower.copy(left = 350, right = 1100),
            )
        val screens = FoldGeometry.dualScreen(safe, 0)!!
        assertScreenInside(screens.upperScreen, safe.upper)
        assertScreenInside(screens.lowerScreen, safe.lower)
        assertEquals(800f, (screens.viewport.left + screens.viewport.right) / 2f, 0.01f)
    }

    @Test fun roundedPanelPropertySweepNeverCropsAScreenOrTouchesTheHinge() {
        var fitted = 0
        for (hingeY in 200..700 step 25) {
            for (hingeHeight in 0..80 step 8) {
                val raw =
                    FoldGeometry.resolve(
                        1600,
                        1000,
                        FoldBounds(0, hingeY, 1600, hingeY + hingeHeight, FoldAxis.HORIZONTAL),
                        12,
                    )
                val upper = FoldGeometry.displayPanel(raw.upper, 12, 24)
                val lower = FoldGeometry.displayPanel(FoldGeometry.lowerConsole(raw.lower, 350).center, 6, 24)
                val safe =
                    raw.copy(
                        upper = upper.content,
                        lower = lower.content,
                        hinge = raw.hinge.copy(top = upper.content.bottom, bottom = lower.content.top),
                    )
                val screens = FoldGeometry.dualScreen(safe, 0) ?: continue
                fitted++
                assertScreenInside(screens.upperScreen, upper.content)
                assertScreenInside(screens.lowerScreen, lower.content)
                assertTrue(screens.upperScreen.bottom < raw.hinge.top)
                assertTrue(screens.lowerScreen.top > raw.hinge.bottom)
                assertSafePanelCorners(upper)
                assertSafePanelCorners(lower)
            }
        }
        assertTrue("The sweep must exercise working layouts, not only rejection", fitted > 100)
    }

    @Test fun roundedFitUsesFullHeightWhenLetterboxingAlreadyClearsCorners() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val screen = FoldGeometry.fitInsidePanel(panel, 4f / 3f)!!
        assertEquals(502f, screen.height, 0.001f)
        assertEquals(4f / 3f, screen.width / screen.height, 0.00001f)
        assertEquals(700f, (screen.left + screen.right) / 2f, 0.001f)
        assertEquals(252f, (screen.top + screen.bottom) / 2f, 0.001f)
        assertTrue(screen.height > panel.content.height)
        assertRoundedScreenSafe(screen, panel)
    }

    @Test fun roundedFitUsesFullWidthWhenVerticalLetterboxingClearsCorners() {
        val panel = FoldGeometry.displayPanel(FoldRect(20, 40, 320, 700), 0, 30)
        val screen = FoldGeometry.fitInsidePanel(panel, 16f / 9f)!!
        assertEquals(298f, screen.width, 0.001f)
        assertEquals(16f / 9f, screen.width / screen.height, 0.00001f)
        assertRoundedScreenSafe(screen, panel)
    }

    @Test fun roundedFitTouchesTheCornerArcOnlyAfterItsRasterGuard() {
        val panel = FoldGeometry.displayPanel(FoldRect(20, 40, 420, 340), 0, 40)
        val screen = FoldGeometry.fitInsidePanel(panel, 4f / 3f)!!
        assertTrue(screen.width < panel.bounds.width - 2f)
        assertTrue(screen.height < panel.bounds.height - 2f)
        assertRoundedScreenSafe(screen, panel)
        assertTrue("A larger centered FIT must no longer be safe", !roundedScreenSafe(enlarge(screen, 1.001f), panel))
    }

    @Test fun roundedFitRejectsEmptyRegionsAndInvalidRatios() {
        val panel = FoldGeometry.displayPanel(FoldRect(0, 0, 400, 300), 0, 24)
        for (ratio in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertNull(FoldGeometry.fitInsidePanel(panel, ratio))
        }
        assertNull(FoldGeometry.fitInsidePanel(FoldGeometry.displayPanel(FoldRect(0, 0, 1, 100), 0, 0), 1f))
        val square = FoldGeometry.fitInsidePanel(panel.copy(cornerRadius = 0), 4f / 3f)!!
        assertEquals(298f, square.height, 0.001f)
        assertRoundedScreenSafe(square, panel.copy(cornerRadius = 0))
    }

    @Test fun roundedFitSupportsReportedPortraitSquareAndWideCoreRatios() {
        for (width in listOf(100, 320, 640, 1400)) {
            for (height in listOf(100, 240, 504, 800)) {
                for (ratio in listOf(0.5f, 0.75f, 1f, 4f / 3f, 16f / 9f, 2.4f)) {
                    val panel = FoldGeometry.displayPanel(FoldRect(30, 40, 30 + width, 40 + height), 6, 24)
                    val screen = FoldGeometry.fitInsidePanel(panel, ratio)!!
                    assertRoundedScreenSafe(screen, panel)
                    assertEquals(ratio, screen.width / screen.height, 0.00001f)
                    assertEquals(
                        (panel.bounds.left + panel.bounds.right) / 2f,
                        (screen.left + screen.right) / 2f,
                        0.001f,
                    )
                    assertEquals(
                        (panel.bounds.top + panel.bounds.bottom) / 2f,
                        (screen.top + screen.bottom) / 2f,
                        0.001f,
                    )
                    assertTrue(!roundedScreenSafe(enlarge(screen, 1.001f), panel))
                }
            }
        }
    }

    @Test fun independentScreensMaximizeEachPanelWithoutSharingTheControlWidthLimit() {
        val upper = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 504), 0, 36)
        val lower = FoldGeometry.displayPanel(FoldRect(450, 624, 950, 1128), 0, 36)
        val screens = FoldGeometry.independentDualScreen(upper, lower)!!
        assertEquals(502f, screens.upperScreen.height, 0.001f)
        assertEquals(498f, screens.lowerScreen.width, 0.001f)
        assertTrue(screens.upperScreen.width > screens.lowerScreen.width)
        assertEquals(876f, (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2f, 0.001f)
        assertEquals(700f, (screens.lowerScreen.left + screens.lowerScreen.right) / 2f, 0.001f)
        assertRoundedScreenSafe(screens.upperScreen, upper)
        assertRoundedScreenSafe(screens.lowerScreen, lower)
        assertEquals(0 to 0, screens.touchCoordinates(screens.lowerScreen.left, screens.lowerScreen.top))
        assertEquals(128 to 96, screens.touchCoordinates(700f, 876f))
        assertEquals(
            255 to 191,
            screens.touchCoordinates(screens.lowerScreen.right - 0.01f, screens.lowerScreen.bottom - 0.01f),
        )
        assertNull(screens.touchCoordinates(screens.upperScreen.left, screens.upperScreen.top))
        assertNull(screens.touchCoordinates(700f, 570f))
        assertNull(screens.touchCoordinates(450f, 624f))
    }

    @Test fun independentScreensRejectOverlappingPanelRegions() {
        val upper = FoldGeometry.displayPanel(FoldRect(0, 0, 1400, 600), 0, 36)
        val lower = FoldGeometry.displayPanel(FoldRect(300, 590, 1100, 1000), 0, 36)
        assertNull(FoldGeometry.independentDualScreen(upper, lower))
    }

    @Test fun independentRoundedFitSweepMaximizesBothScreensAndCentersLowerAcrossAllFolds() {
        var fitted = 0
        for (hingeY in 200..700 step 25) {
            for (hingeHeight in 0..80 step 8) {
                val raw =
                    FoldGeometry.resolve(
                        1600,
                        1000,
                        FoldBounds(0, hingeY, 1600, hingeY + hingeHeight, FoldAxis.HORIZONTAL),
                        12,
                    )
                val upper = FoldGeometry.displayPanel(raw.upper, 12, 24)
                val center = FoldGeometry.lowerConsole(raw.lower, 350).center
                // The lower panel is horizontally inset only: its vertical center remains
                // the center of the safe lower half, and all of that height stays available.
                val lower =
                    FoldGeometry.displayPanel(
                        center.copy(left = center.left + 6, right = center.right - 6),
                        0,
                        24,
                    )
                val screens = FoldGeometry.independentDualScreen(upper, lower)!!
                fitted++
                assertRoundedScreenSafe(screens.upperScreen, upper)
                assertRoundedScreenSafe(screens.lowerScreen, lower)
                assertEquals(4f / 3f, screens.upperScreen.width / screens.upperScreen.height, 0.00001f)
                assertEquals(4f / 3f, screens.lowerScreen.width / screens.lowerScreen.height, 0.00001f)
                assertEquals(
                    (raw.lower.top + raw.lower.bottom) / 2f,
                    (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2f,
                    0.001f,
                )
                assertTrue(screens.upperScreen.bottom < raw.hinge.top)
                assertTrue(screens.lowerScreen.top > raw.hinge.bottom)
                assertTrue(!roundedScreenSafe(enlarge(screens.upperScreen, 1.001f), upper))
                assertTrue(!roundedScreenSafe(enlarge(screens.lowerScreen, 1.001f), lower))
                assertEquals(
                    128 to 96,
                    screens.touchCoordinates(
                        (screens.lowerScreen.left + screens.lowerScreen.right) / 2,
                        (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2,
                    ),
                )
            }
        }
        assertEquals(231, fitted)
    }

    @Test fun coupledFallbackCentersLowerAndFindsMaximumAcrossAllLegalGaps() {
        val layout =
            FoldLayout(FoldRect(18, 54, 1422, 558), FoldRect(350, 624, 1090, 1128), FoldRect(0, 558, 1440, 624))
        val screens = FoldGeometry.dualScreen(layout, 0)!!
        assertEquals(876f, (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2f, 0.001f)
        assertTrue(screens.upperScreen.height > 500f)
        assertScreenInside(screens.upperScreen, layout.upper)
        assertScreenInside(screens.lowerScreen, layout.lower)
        // A slightly larger common scale must violate at least one constraint for every
        // allowed integer gap, including gaps that are not the selected option.
        val larger = screens.upperScreen.height / 192.0 + 0.001
        for (gap in 0..126) {
            val upperTop = 876.0 - (288 + gap) * larger
            val upperBottom = 876.0 - (96 + gap) * larger
            assertTrue(larger * 256 > 740 || larger * 192 > 504 || upperTop < 54 || upperBottom > 558)
        }
    }

    @Test fun coupledFallbackSweepKeepsLowerExactlyCentered() {
        var fitted = 0
        for (hingeY in 200..700 step 25) {
            for (hingeHeight in 0..80 step 8) {
                val layout =
                    FoldGeometry.resolve(
                        1600,
                        1000,
                        FoldBounds(0, hingeY, 1600, hingeY + hingeHeight, FoldAxis.HORIZONTAL),
                        12,
                    )
                val screens = FoldGeometry.dualScreen(layout, 350) ?: continue
                fitted++
                assertEquals(
                    (layout.lower.top + layout.lower.bottom) / 2f,
                    (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2f,
                    0.001f,
                )
                assertScreenInside(screens.upperScreen, layout.upper)
                assertScreenInside(screens.lowerScreen, layout.lower)
                assertEquals(
                    128 to 96,
                    screens.touchCoordinates(
                        (screens.lowerScreen.left + screens.lowerScreen.right) / 2,
                        (screens.lowerScreen.top + screens.lowerScreen.bottom) / 2,
                    ),
                )
            }
        }
        assertTrue(fitted > 100)
    }

    private fun enlarge(
        screen: ScreenRect,
        factor: Float,
    ): ScreenRect {
        val extraWidth = screen.width * (factor - 1) / 2
        val extraHeight = screen.height * (factor - 1) / 2
        return ScreenRect(
            screen.left - extraWidth,
            screen.top - extraHeight,
            screen.right + extraWidth,
            screen.bottom + extraHeight,
        )
    }

    private fun assertRoundedScreenSafe(
        screen: ScreenRect,
        panel: DisplayPanelLayout,
    ) {
        assertTrue(
            "Screen and raster guard must fit its rounded panel: $screen / $panel",
            roundedScreenSafe(screen, panel),
        )
    }

    private fun roundedScreenSafe(
        screen: ScreenRect,
        panel: DisplayPanelLayout,
    ): Boolean {
        val bounds = panel.bounds
        val radius = panel.cornerRadius.toFloat()
        for (x in listOf(screen.left - 1, screen.right + 1)) {
            for (y in listOf(screen.top - 1, screen.bottom + 1)) {
                if (x < bounds.left - 0.001f || x > bounds.right + 0.001f ||
                    y < bounds.top - 0.001f || y > bounds.bottom + 0.001f
                ) {
                    return false
                }
                val centerX = x.coerceIn(bounds.left + radius, bounds.right - radius)
                val centerY = y.coerceIn(bounds.top + radius, bounds.bottom - radius)
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy > radius * radius + 0.01f) return false
            }
        }
        return true
    }

    private fun assertScreenInside(
        screen: ScreenRect,
        bounds: FoldRect,
    ) {
        assertTrue(screen.left >= bounds.left - 0.01f && screen.right <= bounds.right + 0.01f)
        assertTrue(screen.top >= bounds.top - 0.01f && screen.bottom <= bounds.bottom + 0.01f)
    }

    private fun assertSafePanelCorners(panel: DisplayPanelLayout) {
        val radius = panel.cornerRadius.toFloat()
        // Even the extra one-pixel uncovered raster guard fits inside the rounded panel.
        for ((x, centerX) in listOf(
            panel.content.left - 1f to panel.bounds.left + radius,
            panel.content.right + 1f to panel.bounds.right - radius,
        )) {
            for ((y, centerY) in listOf(
                panel.content.top - 1f to panel.bounds.top + radius,
                panel.content.bottom + 1f to panel.bounds.bottom - radius,
            )) {
                val dx = x - centerX
                val dy = y - centerY
                assertTrue(
                    "Game raster corner must be inside rounded backing: $panel",
                    dx * dx + dy * dy <= radius * radius,
                )
            }
        }
    }
}
