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

    @Test fun explicitlySelectedResultOutsideTheFilterIsRevealed() {
        assertEquals(true, revealLibrarySelectionRequest(1, 0, 2, listOf(1, 3)))
    }

    @Test fun ordinaryRouteReturnKeepsAnEmptyFilter() {
        assertEquals(false, revealLibrarySelectionRequest(1, 1, 1, emptyList()))
    }

    @Test fun sameHiddenGameIsRevealedByANewExplicitSelectionEvent() {
        assertEquals(true, revealLibrarySelectionRequest(2, 1, 1, emptyList()))
    }

    @Test fun consumedSelectionEventDoesNotResetLaterFilterChanges() {
        assertEquals(false, revealLibrarySelectionRequest(2, 2, 1, emptyList()))
    }

    @Test fun explicitSelectionAlreadyInTheFilterKeepsThatFilter() {
        assertEquals(false, revealLibrarySelectionRequest(1, 0, 2, listOf(1, 2)))
    }

    @Test fun missingSelectionDoesNotResetTheFilter() {
        assertEquals(false, revealLibrarySelectionRequest(1, 0, null, emptyList()))
    }

    @Test fun firstExternalSelectionCanRevealAHiddenGame() {
        assertEquals(true, revealLibrarySelectionRequest(1, 0, 2, emptyList()))
    }

    @Test fun allGamesIsNotLimitedToUpstreamCarouselSize() {
        val games = (1..40).map { game(it) }
        assertEquals(games, filtered(games, LibraryFilter.ALL))
    }
}
