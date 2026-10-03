package com.swordfish.lemuroid.app.mobile.feature.emuui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoldGeometryTest {
    @Test fun flatLandscapeKeepsConsolePanes() {
        val layout = FoldGeometry.resolve(1200, 800, null, 12)
        assertNull(layout.guidance)
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

    @Test fun outOfWindowFoldIsIgnoredInMultiWindow() {
        val layout = FoldGeometry.resolve(1200, 500, FoldBounds(0, 700, 1200, 720, FoldAxis.HORIZONTAL), 10)
        assertFalse(layout.physicalHinge)
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
        val screens = FoldGeometry.dualScreen(FoldGeometry.resolve(1200, 800, null, 10), 260)!!
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
        val flat = FoldGeometry.resolve(1400, 900, null, 12)
        val half = FoldGeometry.resolve(1400, 900, FoldBounds(0, 434, 1400, 466, FoldAxis.HORIZONTAL), 12)
        repeat(100) {
            assertEquals(flat, FoldGeometry.resolve(1400, 900, null, 12))
            assertEquals(half, FoldGeometry.resolve(1400, 900, FoldBounds(0, 434, 1400, 466, FoldAxis.HORIZONTAL), 12))
            assertNotNull(FoldGeometry.dualScreen(half, 300))
        }
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
}
