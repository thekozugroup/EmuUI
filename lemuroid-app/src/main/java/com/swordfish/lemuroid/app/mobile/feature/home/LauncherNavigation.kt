package com.swordfish.lemuroid.app.mobile.feature.home

import kotlin.math.roundToInt

internal enum class LauncherDirection { UP, DOWN, LEFT, RIGHT }

/** Discrete D-pad navigation follows centered rows. Horizontal edges never jump between rows. */
internal fun launcherSelectionIndex(
    count: Int,
    selectedIndex: Int,
    columns: Int,
    direction: LauncherDirection,
): Int? {
    if (count <= 0) return null
    if (selectedIndex !in 0 until count) return 0
    val span = columns.coerceAtLeast(1)
    val rowStart = selectedIndex / span * span

    fun adjacentRow(targetStart: Int): Int {
        if (targetStart !in 0 until count) return selectedIndex
        val currentLength = minOf(span, count - rowStart)
        val targetLength = minOf(span, count - targetStart)
        val visualColumn = selectedIndex - rowStart + (span - currentLength) / 2f
        val targetColumn = (visualColumn - (span - targetLength) / 2f).roundToInt()
        return targetStart + targetColumn.coerceIn(0, targetLength - 1)
    }
    return when (direction) {
        LauncherDirection.LEFT -> (selectedIndex - 1).coerceAtLeast(rowStart)
        LauncherDirection.RIGHT -> (selectedIndex + 1).coerceAtMost(minOf(rowStart + span - 1, count - 1))
        LauncherDirection.UP -> adjacentRow(rowStart - span)
        LauncherDirection.DOWN -> adjacentRow(rowStart + span)
    }
}

/** Shared by the library and modal region so controls retain the same physical space. */
internal fun launcherControlWingWidthDp(widthDp: Float): Float = (widthDp * .20f).coerceIn(156f, 176f)

internal const val LAUNCHER_PANEL_INSET_DP = 6f
internal const val LAUNCHER_GRID_SPACING_DP = 14f
internal const val LAUNCHER_GRID_INSET_DP = 16f

/** Bounded launcher minimums include full-size wings, safe text and an 80dp square-card viewport. */
internal fun launcherWindowFits(
    widthDp: Float,
    previewHeightDp: Float,
    libraryHeightDp: Float,
    fontScale: Float,
): Boolean {
    val scale = fontScale.coerceAtLeast(1f)
    return widthDp >= 600f && previewHeightDp >= maxOf(192f, 108f + 80f * scale) &&
        libraryHeightDp >= maxOf(216f, 160f + 40f * scale)
}

internal fun launcherPreviewTitleWidth(
    width: Int,
    preferredFraction: Float,
    leftPillWidth: Int,
    rightPillWidth: Int,
    inset: Int,
    gap: Int,
): Int =
    minOf((width * preferredFraction).roundToInt(), width - 2 * (maxOf(leftPillWidth, rightPillWidth) + inset + gap))
        .coerceAtLeast(0)

internal fun launcherPreviewFits(
    height: Int,
    cornerHeight: Int,
    titleHeight: Int,
    inset: Int,
    gap: Int,
): Boolean = height >= maxOf(cornerHeight * 2 + inset * 2 + gap, cornerHeight + inset + gap + titleHeight)

/** Scroll the whole panel if needed; never overlap bands or collapse the shelf to zero. */
internal fun launcherLibraryContentHeight(
    viewportHeight: Int,
    bandHeight: Int,
    minimumShelfHeight: Int,
    inset: Int,
): Int = maxOf(viewportHeight, bandHeight * 2 + minimumShelfHeight + inset * 2)

/** The same column count drives centered touch rows and discrete D-pad movement. */
internal fun launcherGridColumns(
    widthDp: Float,
    fontScale: Float,
    count: Int = Int.MAX_VALUE,
): Int {
    val minimumTile = if (fontScale >= 1.3f) 144f else 128f
    val usableWidth = (widthDp - LAUNCHER_GRID_INSET_DP * 2).coerceAtLeast(0f)
    val availableColumns = ((usableWidth + LAUNCHER_GRID_SPACING_DP) / (minimumTile + LAUNCHER_GRID_SPACING_DP)).toInt()
    return availableColumns.coerceIn(1, minOf(6, count.coerceAtLeast(1)))
}

/** Large square icons, capped so sparse libraries do not stretch into enormous cards. */
internal fun launcherGridTileSize(
    widthDp: Float,
    columns: Int,
): Float {
    val span = columns.coerceAtLeast(1)
    val spacing = LAUNCHER_GRID_SPACING_DP * (span - 1)
    return ((widthDp - LAUNCHER_GRID_INSET_DP * 2 - spacing) / span).coerceIn(1f, 148f)
}

/** At least one complete square row must fit; smaller-than-touch-target windows need guidance. */
internal fun launcherGridTileSizeForViewport(
    widthDp: Float,
    heightDp: Float,
    columns: Int,
): Float? {
    val availableHeight = heightDp - LAUNCHER_GRID_INSET_DP * 2
    val tileSize = minOf(launcherGridTileSize(widthDp, columns), availableHeight)
    return tileSize.takeIf { it >= 48f }
}

internal fun cycleLibraryFilter(
    filter: LibraryFilter,
    forward: Boolean,
): LibraryFilter {
    val filters = LibraryFilter.values()
    val direction = if (forward) 1 else -1
    return filters[(filter.ordinal + direction + filters.size) % filters.size]
}

internal enum class LauncherSection { GAMES, FILTERS, SHORTCUTS }

internal data class LauncherNavigationTarget(
    val section: LauncherSection = LauncherSection.GAMES,
    val index: Int = 0,
)

/** Deterministic side-control focus; library navigation never sends events to the emulation core. */
internal fun launcherNavigationTarget(
    current: LauncherNavigationTarget,
    selectedIndex: Int,
    gameCount: Int,
    columns: Int,
    filterIndex: Int,
    shortcutCount: Int,
    direction: LauncherDirection,
): LauncherNavigationTarget {
    val span = columns.coerceAtLeast(1)
    val gameIndex = selectedIndex.coerceIn(0, (gameCount - 1).coerceAtLeast(0))
    return when (current.section) {
        LauncherSection.GAMES ->
            when {
                direction == LauncherDirection.UP && gameIndex < span ->
                    LauncherNavigationTarget(LauncherSection.GAMES, gameIndex)
                direction == LauncherDirection.DOWN && (gameCount == 0 || gameIndex / span == (gameCount - 1) / span) ->
                    LauncherNavigationTarget(LauncherSection.SHORTCUTS, 0)
                else ->
                    LauncherNavigationTarget(
                        LauncherSection.GAMES,
                        launcherSelectionIndex(gameCount, selectedIndex, span, direction) ?: 0,
                    )
            }
        LauncherSection.FILTERS ->
            when (direction) {
                LauncherDirection.DOWN -> LauncherNavigationTarget(LauncherSection.GAMES, gameIndex)
                LauncherDirection.LEFT -> current.copy(index = (current.index - 1).coerceAtLeast(0))
                LauncherDirection.RIGHT ->
                    current.copy(
                        index = (current.index + 1).coerceAtMost(LibraryFilter.values().lastIndex),
                    )
                LauncherDirection.UP -> current
            }
        LauncherSection.SHORTCUTS ->
            when (direction) {
                LauncherDirection.UP -> LauncherNavigationTarget(LauncherSection.GAMES, gameIndex)
                LauncherDirection.LEFT -> current.copy(index = (current.index - 1).coerceAtLeast(0))
                LauncherDirection.RIGHT ->
                    current.copy(
                        index = (current.index + 1).coerceAtMost((shortcutCount - 1).coerceAtLeast(0)),
                    )
                LauncherDirection.DOWN -> current
            }
    }
}

internal fun launcherNextSection(
    section: LauncherSection,
    selectedIndex: Int,
    filterIndex: Int,
): LauncherNavigationTarget =
    when (section) {
        LauncherSection.GAMES -> LauncherNavigationTarget(LauncherSection.SHORTCUTS, 0)
        LauncherSection.FILTERS -> LauncherNavigationTarget(LauncherSection.SHORTCUTS, 0)
        LauncherSection.SHORTCUTS -> LauncherNavigationTarget(LauncherSection.GAMES, selectedIndex.coerceAtLeast(0))
    }

internal enum class LauncherPrimaryAction { PLAY, IMPORT, RETRY, SHOW_ALL, WAIT, UNSUPPORTED }

/** Empty filters and failed scans must never launch an invisible, stale selection. */
internal fun launcherPrimaryAction(
    hasError: Boolean,
    isLoading: Boolean,
    isScanning: Boolean,
    totalGames: Int,
    visibleGames: Int,
    supported: Boolean,
): LauncherPrimaryAction =
    when {
        hasError -> LauncherPrimaryAction.RETRY
        isLoading -> LauncherPrimaryAction.WAIT
        visibleGames > 0 -> if (supported) LauncherPrimaryAction.PLAY else LauncherPrimaryAction.UNSUPPORTED
        totalGames > 0 -> LauncherPrimaryAction.SHOW_ALL
        isScanning -> LauncherPrimaryAction.WAIT
        else -> LauncherPrimaryAction.IMPORT
    }
