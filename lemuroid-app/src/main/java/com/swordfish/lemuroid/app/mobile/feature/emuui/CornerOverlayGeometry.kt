package com.swordfish.lemuroid.app.mobile.feature.emuui

import kotlin.math.abs

data class CornerOverlayPlacement(val left: FoldRect, val right: FoldRect, val compactBrand: Boolean)

/** One aligned row below the status bar; precise camera bounds only displace nearby controls. */
object CornerOverlayGeometry {
    fun resolve(
        bounds: FoldRect,
        fullBrandWidth: Int,
        compactBrandWidth: Int,
        toolsWidth: Int,
        height: Int,
        inset: Int,
        gap: Int,
        occlusions: List<FoldRect>,
    ): CornerOverlayPlacement? {
        val start = bounds.left + inset
        val end = bounds.right - inset
        val fullBrand = FoldRect(start, bounds.top, start + fullBrandWidth, bounds.top + height)
        val compact =
            fullBrandWidth + toolsWidth + gap > end - start ||
                occlusions.any { fullBrand.overlaps(it, gap) }
        val brandWidth = if (compact) compactBrandWidth else fullBrandWidth
        if (height <= 0 || brandWidth + toolsWidth + gap > end - start) return null
        // Prefer moving horizontally alongside the camera to adding another full safe-area band.
        val leftXs = (listOf(start) + occlusions.flatMap { listOf(it.left - gap - brandWidth, it.right + gap) })
        val rightXs =
            listOf(end - toolsWidth) + occlusions.flatMap { listOf(it.left - gap - toolsWidth, it.right + gap) }
        val tops = (listOf(bounds.top) + occlusions.map { it.bottom + gap }).distinct().sorted()
        for (top in tops.filter { it >= bounds.top && it + height <= bounds.bottom }) {
            val placements =
                leftXs.flatMap { left ->
                    rightXs.mapNotNull { right ->
                        val a = FoldRect(left, top, left + brandWidth, top + height)
                        val b = FoldRect(right, top, right + toolsWidth, top + height)
                        if (left < start || b.right > end || a.right + gap > right ||
                            occlusions.any { a.overlaps(it, gap) || b.overlaps(it, gap) }
                        ) {
                            null
                        } else {
                            CornerOverlayPlacement(a, b, compact)
                        }
                    }
                }
            placements.minByOrNull { abs(it.left.left - start) + abs(end - it.right.right) }?.let { return it }
        }
        return null
    }

    private fun FoldRect.overlaps(
        other: FoldRect,
        clearance: Int,
    ) = left < other.right + clearance && right + clearance > other.left &&
        top < other.bottom + clearance && bottom + clearance > other.top
}
