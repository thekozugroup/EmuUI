package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSectionNavigationTest {
    private fun move(
        section: LauncherSection = LauncherSection.GAMES,
        index: Int = 0,
        selected: Int = 0,
        count: Int = 12,
        columns: Int = 3,
        filter: Int = 0,
        shortcuts: Int = 4,
        direction: LauncherDirection,
    ) = launcherNavigationTarget(
        LauncherNavigationTarget(section, index),
        selected,
        count,
        columns,
        filter,
        shortcuts,
        direction,
    )

    @Test fun upFromFirstRowEntersTheCurrentFilter() {
        assertEquals(
            LauncherNavigationTarget(LauncherSection.FILTERS, 2),
            move(selected = 1, filter = 2, direction = LauncherDirection.UP),
        )
    }

    @Test fun downFromLastRowEntersShortcuts() {
        assertEquals(
            LauncherNavigationTarget(LauncherSection.SHORTCUTS, 0),
            move(selected = 10, direction = LauncherDirection.DOWN),
        )
    }

    @Test fun movingInsideTheGridRetainsCenteredRowNavigation() {
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 4),
            move(selected = 1, direction = LauncherDirection.DOWN),
        )
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 1),
            move(selected = 3, count = 4, direction = LauncherDirection.UP),
        )
    }

    @Test fun horizontalGameEdgesNeverWrapOrLeaveTheShelf() {
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 3),
            move(selected = 3, direction = LauncherDirection.LEFT),
        )
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 5),
            move(selected = 5, direction = LauncherDirection.RIGHT),
        )
    }

    @Test fun longLibrariesNavigateEveryRowBeforeEnteringShortcuts() {
        var selected = 0
        for (row in 1..39) {
            val next = move(selected = selected, count = 120, direction = LauncherDirection.DOWN)
            assertEquals(LauncherSection.GAMES, next.section)
            assertEquals(row * 3, next.index)
            selected = next.index
        }
        assertEquals(
            LauncherSection.SHORTCUTS,
            move(selected = selected, count = 120, direction = LauncherDirection.DOWN).section,
        )
    }

    @Test fun filtersHaveBoundedHorizontalFocus() {
        assertEquals(0, move(LauncherSection.FILTERS, direction = LauncherDirection.LEFT).index)
        assertEquals(2, move(LauncherSection.FILTERS, 2, direction = LauncherDirection.RIGHT).index)
        assertEquals(1, move(LauncherSection.FILTERS, 0, direction = LauncherDirection.RIGHT).index)
    }

    @Test fun shortcutsIncludeOptionalSetupAndClampAtBothEnds() {
        assertEquals(0, move(LauncherSection.SHORTCUTS, direction = LauncherDirection.LEFT).index)
        assertEquals(3, move(LauncherSection.SHORTCUTS, 3, direction = LauncherDirection.RIGHT).index)
        assertEquals(4, move(LauncherSection.SHORTCUTS, 3, shortcuts = 5, direction = LauncherDirection.RIGHT).index)
    }

    @Test fun returningFromBothBandsKeepsTheSelectedGame() {
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 7),
            move(LauncherSection.FILTERS, selected = 7, direction = LauncherDirection.DOWN),
        )
        assertEquals(
            LauncherNavigationTarget(LauncherSection.GAMES, 7),
            move(LauncherSection.SHORTCUTS, selected = 7, direction = LauncherDirection.UP),
        )
    }

    @Test fun emptyLibraryStillReachesFiltersAndShortcuts() {
        assertEquals(LauncherSection.FILTERS, move(selected = -1, count = 0, direction = LauncherDirection.UP).section)
        assertEquals(
            LauncherSection.SHORTCUTS,
            move(selected = -1, count = 0, direction = LauncherDirection.DOWN).section,
        )
    }

    @Test fun selectCyclesEverySectionWithoutLosingTheGame() {
        val filters = launcherNextSection(LauncherSection.GAMES, 11, 2)
        assertEquals(LauncherNavigationTarget(LauncherSection.FILTERS, 2), filters)
        val shortcuts = launcherNextSection(filters.section, 11, 2)
        assertEquals(LauncherNavigationTarget(LauncherSection.SHORTCUTS, 0), shortcuts)
        assertEquals(LauncherNavigationTarget(LauncherSection.GAMES, 11), launcherNextSection(shortcuts.section, 11, 2))
    }

    @Test fun unsupportedGameIsNotAPlayablePrimaryAction() {
        assertEquals(LauncherPrimaryAction.UNSUPPORTED, launcherPrimaryAction(false, false, false, 3, 1, false))
    }

    @Test fun emptyFilteredShelfReturnsToAllInsteadOfLaunchingAHiddenGame() {
        assertEquals(LauncherPrimaryAction.SHOW_ALL, launcherPrimaryAction(false, false, false, 3, 0, true))
    }

    @Test fun scanErrorsAlwaysOfferRetryEvenWithOldGamesPresent() {
        assertEquals(LauncherPrimaryAction.RETRY, launcherPrimaryAction(true, false, false, 3, 3, true))
        assertEquals(LauncherPrimaryAction.RETRY, launcherPrimaryAction(true, true, true, 0, 0, true))
    }

    @Test fun emptyLibraryImportsOnlyWhenReady() {
        assertEquals(LauncherPrimaryAction.IMPORT, launcherPrimaryAction(false, false, false, 0, 0, true))
        assertEquals(LauncherPrimaryAction.WAIT, launcherPrimaryAction(false, true, false, 0, 0, true))
        assertEquals(LauncherPrimaryAction.WAIT, launcherPrimaryAction(false, false, true, 0, 0, true))
    }

    @Test fun knownGamesCanStillLaunchDuringBackgroundImport() {
        assertEquals(LauncherPrimaryAction.PLAY, launcherPrimaryAction(false, false, true, 3, 3, true))
    }

    @Test fun allReachableTargetsStayInBoundsAcrossSparseAndLongLibraries() {
        for (count in 0..42) for (columns in 1..6) for (selected in 0 until maxOf(1, count)) {
            LauncherDirection.values().forEach { direction ->
                val target = move(selected = selected, count = count, columns = columns, direction = direction)
                val bound =
                    when (target.section) {
                        LauncherSection.GAMES -> maxOf(1, count)
                        LauncherSection.FILTERS -> LibraryFilter.values().size
                        LauncherSection.SHORTCUTS -> 4
                    }
                assertTrue(target.index in 0 until bound)
            }
        }
    }
}
