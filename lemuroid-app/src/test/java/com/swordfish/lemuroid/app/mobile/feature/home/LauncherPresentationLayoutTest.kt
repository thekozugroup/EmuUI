package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPresentationLayoutTest {
    @Test fun approvedWindowRetainsTheLauncherAtAllRequestedFontScales() {
        for (scale in listOf(1f, 1.5f, 2f)) {
            assertTrue(launcherWindowFits(960f, 352f, 326f, scale))
        }
    }

    @Test fun tinyWidthsAndEitherShortPaneUseWholeWindowGuidance() {
        assertFalse(launcherWindowFits(320f, 352f, 326f, 1f))
        assertFalse(launcherWindowFits(600f, 104f, 326f, 1f))
        assertFalse(launcherWindowFits(960f, 352f, 80f, 1f))
        assertFalse(launcherWindowFits(960f, 250f, 326f, 2f))
        assertFalse(launcherWindowFits(960f, 352f, 220f, 2f))
        assertTrue(launcherWindowFits(600f, 268f, 240f, 2f))
    }

    @Test fun normalTitleKeepsItsWidthAndNarrowTitleClearsBothBottomPills() {
        assertEquals(412, launcherPreviewTitleWidth(936, .44f, 100, 128, 12, 8))
        val titleWidth = launcherPreviewTitleWidth(376, .52f, 138, 58, 12, 8)
        assertEquals(60, titleWidth)
        assertTrue((376 - titleWidth) / 2 >= 12 + 138 + 8)
        assertEquals(0, launcherPreviewTitleWidth(100, .52f, 100, 58, 12, 8))
    }

    @Test fun previewRejectsBothCornerOverlapAndTitleOverlap() {
        assertFalse(launcherPreviewFits(80, 48, 58, 12, 8))
        assertFalse(launcherPreviewFits(104, 64, 156, 12, 8))
        assertFalse(launcherPreviewFits(200, 64, 156, 12, 8))
        assertTrue(launcherPreviewFits(352, 64, 156, 12, 8))
    }

    @Test fun shortLibraryScrollsCompleteBandsAndAnAccessibleShelfWithoutOverlap() {
        for (viewport in 0..400) {
            for (band in listOf(48, 64, 80)) {
                val content = launcherLibraryContentHeight(viewport, band, 80, 20)
                val shelf = content - band * 2 - 40
                assertTrue(content >= viewport)
                assertTrue(shelf >= 80)
                assertTrue(content - 20 - band >= 20 + band + shelf)
            }
        }
        assertEquals(326, launcherLibraryContentHeight(326, 48, 80, 20))
        assertEquals(216, launcherLibraryContentHeight(80, 48, 80, 20))
    }

    @Test fun approvedNearSquareWindowKeepsLargeCards() {
        assertEquals(148f, checkNotNull(launcherGridTileSizeForViewport(528f, 190f, 3)), .01f)
    }

    @Test fun shortViewportFitsOneWholeSquareRowWithBothInsets() {
        assertEquals(50f, checkNotNull(launcherGridTileSizeForViewport(480f, 90f, 3)), .01f)
    }

    @Test fun touchTargetsAreNeverReducedBelowFortyEightDp() {
        assertEquals(48f, checkNotNull(launcherGridTileSizeForViewport(480f, 88f, 3)), .01f)
        assertNull(launcherGridTileSizeForViewport(480f, 87f, 3))
        assertNull(launcherGridTileSizeForViewport(70f, 300f, 1))
        assertNull(launcherGridTileSizeForViewport(480f, 0f, 3))
    }

    @Test fun everyVisibleSquareFitsWidthHeightAndMinimumTarget() {
        for (width in 80..1000 step 13) {
            for (height in 0..420 step 7) {
                val columns = launcherGridColumns(width.toFloat(), 1f, 12)
                val tile = launcherGridTileSizeForViewport(width.toFloat(), height.toFloat(), columns) ?: continue
                assertTrue(tile >= 48f)
                assertTrue(tile <= 148f)
                assertTrue(tile + LAUNCHER_GRID_INSET_DP * 2 <= height + .01f)
                assertTrue(
                    tile * columns + LAUNCHER_GRID_SPACING_DP * (columns - 1) + LAUNCHER_GRID_INSET_DP * 2 <=
                        width + .01f,
                )
            }
        }
    }
}
