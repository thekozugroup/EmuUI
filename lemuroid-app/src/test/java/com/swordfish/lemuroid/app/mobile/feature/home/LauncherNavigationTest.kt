package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherNavigationTest {
    @Test fun emptyLibraryCannotNavigateOrProduceAnInvalidSelection() {
        LauncherDirection.values().forEach { assertNull(launcherSelectionIndex(0, -1, 4, it)) }
    }

    @Test fun filteredOutSelectionStartsAtFirstVisibleGame() {
        LauncherDirection.values().forEach { assertEquals(0, launcherSelectionIndex(8, -1, 4, it)) }
    }

    @Test fun directionalNavigationMatchesRowMajorGrid() {
        assertEquals(0, launcherSelectionIndex(12, 4, 4, LauncherDirection.UP))
        assertEquals(8, launcherSelectionIndex(12, 4, 4, LauncherDirection.DOWN))
        assertEquals(5, launcherSelectionIndex(12, 4, 4, LauncherDirection.RIGHT))
        assertEquals(4, launcherSelectionIndex(12, 5, 4, LauncherDirection.LEFT))
    }

    @Test fun horizontalEdgesDoNotWrapAcrossRows() {
        assertEquals(4, launcherSelectionIndex(12, 4, 4, LauncherDirection.LEFT))
        assertEquals(7, launcherSelectionIndex(12, 7, 4, LauncherDirection.RIGHT))
    }

    @Test fun incompleteLastRowClampsToExistingGame() {
        assertEquals(9, launcherSelectionIndex(10, 7, 4, LauncherDirection.DOWN))
        assertEquals(9, launcherSelectionIndex(10, 9, 4, LauncherDirection.DOWN))
        assertEquals(9, launcherSelectionIndex(10, 9, 4, LauncherDirection.RIGHT))
        assertEquals(3, launcherSelectionIndex(10, 3, 4, LauncherDirection.UP))
    }

    @Test fun verticalNavigationFollowsTheCenteredIncompleteRow() {
        assertEquals(1, launcherSelectionIndex(4, 3, 3, LauncherDirection.UP))
        assertEquals(3, launcherSelectionIndex(4, 1, 3, LauncherDirection.DOWN))
        assertEquals(2, launcherSelectionIndex(5, 4, 3, LauncherDirection.UP))
        assertEquals(4, launcherSelectionIndex(5, 2, 3, LauncherDirection.DOWN))
        assertEquals(3, launcherSelectionIndex(4, 3, 3, LauncherDirection.LEFT))
    }

    @Test fun invalidColumnCountIsSafeSingleColumn() {
        assertEquals(1, launcherSelectionIndex(3, 0, 0, LauncherDirection.DOWN))
    }

    @Test fun largeTextUsesWiderTilesAndNarrowWidthsKeepOneColumn() {
        assertEquals(3, launcherGridColumns(460f, 1f))
        assertEquals(2, launcherGridColumns(460f, 1.5f))
        assertEquals(1, launcherGridColumns(30f, 2f))
    }

    @Test fun sparseCollectionUsesOnlyPopulatedColumns() {
        assertEquals(3, launcherGridColumns(540f, 1f, 3))
        assertEquals(2, launcherGridColumns(540f, 1f, 2))
        assertEquals(1, launcherGridColumns(540f, 1f, 1))
        assertEquals(1, launcherGridColumns(540f, 1f, 0))
    }

    @Test fun largeCollectionsFormMultipleRowsWithTheSameNavigationStride() {
        val columns = launcherGridColumns(540f, 1f, 12)
        assertEquals(3, columns)
        assertEquals(3, launcherSelectionIndex(12, 0, columns, LauncherDirection.DOWN))
        assertEquals(8, launcherSelectionIndex(12, 11, columns, LauncherDirection.UP))
        assertEquals(11, launcherSelectionIndex(12, 11, columns, LauncherDirection.RIGHT))
    }

    @Test fun iconSquaresAreLargerThanTheOldSeventyEightDpMinimumAndStayBounded() {
        assertEquals(148f, launcherGridTileSize(540f, 3), 0.01f)
        assertEquals(88f, launcherGridTileSize(128f, 1), 0.01f)
        assertEquals(148f, launcherGridTileSize(900f, 1), 0.01f)
        assertEquals(1f, launcherGridTileSize(8f, 0), 0.01f)
    }

    @Test fun gridNeverOverflowsItsHorizontalInsets() {
        for (width in 160..1200 step 8) {
            for (count in 1..24) {
                val columns = launcherGridColumns(width.toFloat(), 1f, count)
                val occupied =
                    launcherGridTileSize(width.toFloat(), columns) * columns +
                        LAUNCHER_GRID_SPACING_DP * (columns - 1) + LAUNCHER_GRID_INSET_DP * 2
                org.junit.Assert.assertTrue(occupied <= width + .01f)
            }
        }
    }

    @Test fun shoulderFiltersCycleInBothDirections() {
        assertEquals(LibraryFilter.RECENT, cycleLibraryFilter(LibraryFilter.ALL, true))
        assertEquals(LibraryFilter.ALL, cycleLibraryFilter(LibraryFilter.FAVORITES, true))
        assertEquals(LibraryFilter.FAVORITES, cycleLibraryFilter(LibraryFilter.ALL, false))
    }
}
