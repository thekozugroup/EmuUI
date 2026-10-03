package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConsoleLibraryTest {
    private data class TestGame(val id: Int, val favorite: Boolean = false, val played: Long? = null)

    private fun game(
        id: Int,
        favorite: Boolean = false,
        played: Long? = null,
    ) = TestGame(id, favorite, played)

    private fun selection(
        games: List<TestGame>,
        selectedId: Int?,
    ) = resolveLibrarySelection(games, selectedId, TestGame::id)

    private fun filtered(
        games: List<TestGame>,
        filter: LibraryFilter,
    ) = filterLibrary(games, filter, TestGame::favorite, TestGame::played)

    @Test fun emptyLibraryHasNoSelection() {
        assertNull(selection(emptyList(), 4))
    }

    @Test fun restoredSelectionSurvivesReordering() {
        val selected = game(7)
        assertEquals(selected, selection(listOf(game(2), game(9), selected), 7))
    }

    @Test fun removedSelectionFallsBackToFirstRemainingGame() {
        val first = game(2)
        assertEquals(first, selection(listOf(first, game(9)), 7))
    }

    @Test fun firstVisitSelectsFirstGame() {
        val first = game(2)
        assertEquals(first, selection(listOf(first), null))
    }

    @Test fun recentIncludesFavoritesAndSortsNewestFirst() {
        val favorite = game(1, favorite = true, played = 50)
        val newest = game(2, played = 100)
        assertEquals(listOf(newest, favorite), filtered(listOf(favorite, game(3), newest), LibraryFilter.RECENT))
    }

    @Test fun favoritesNeverIncludesUnfavoritedGames() {
        val favorite = game(2, favorite = true)
        assertEquals(listOf(favorite), filtered(listOf(game(1), favorite), LibraryFilter.FAVORITES))
    }

    @Test fun allGamesIsNotLimitedToUpstreamCarouselSize() {
        val games = (1..40).map { game(it) }
        assertEquals(games, filtered(games, LibraryFilter.ALL))
    }
}
