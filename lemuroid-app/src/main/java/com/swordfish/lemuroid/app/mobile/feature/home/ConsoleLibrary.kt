package com.swordfish.lemuroid.app.mobile.feature.home

internal enum class LibraryFilter(val label: String) {
    ALL("All games"),
    RECENT("Recently played"),
    FAVORITES("Favorites"),
}

/** Recent includes favorites too; nothing silently falls out of the console's library. */
internal fun <T> filterLibrary(
    games: List<T>,
    filter: LibraryFilter,
    isFavorite: (T) -> Boolean,
    lastPlayed: (T) -> Long?,
): List<T> =
    when (filter) {
        LibraryFilter.ALL -> games
        LibraryFilter.RECENT -> games.filter { lastPlayed(it) != null }.sortedByDescending(lastPlayed)
        LibraryFilter.FAVORITES -> games.filter(isFavorite)
    }

/** Stable database identity survives sorting, imports, filtering and activity recreation. */
internal fun <T> resolveLibrarySelection(
    games: List<T>,
    selectedId: Int?,
    id: (T) -> Int,
): T? = games.firstOrNull { id(it) == selectedId } ?: games.firstOrNull()
