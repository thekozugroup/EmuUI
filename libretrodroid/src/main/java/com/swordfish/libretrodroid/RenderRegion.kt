/* SPDX-License-Identifier: GPL-3.0-or-later */
package com.swordfish.libretrodroid

import android.graphics.RectF

/**
 * A crop of the unrotated core image and its exact destination in the GL view.
 * Both rectangles use normalized [0, 1] coordinates with a top-left origin.
 * The caller fits each destination to the source's display aspect ratio.
 * [GLRetroView.setRenderRegions] copies the rectangles before queuing them.
 */
data class RenderRegion(val source: RectF, val destination: RectF)

internal fun packRenderRegions(regions: List<RenderRegion>): FloatArray {
    require(regions.size <= 8) { "At most eight render regions are supported" }
    return FloatArray(regions.size * 8).also { packed ->
        regions.forEachIndexed { index, region ->
            listOf(region.source, region.destination).forEachIndexed { rectIndex, rect ->
                require(
                    rect.left.isFinite() && rect.top.isFinite() &&
                        rect.right.isFinite() && rect.bottom.isFinite() &&
                        rect.left >= 0f && rect.top >= 0f &&
                        rect.right <= 1f && rect.bottom <= 1f &&
                        rect.width() > 0f && rect.height() > 0f,
                ) { "Render rectangles must be finite, nonempty and within [0, 1]" }
                val offset = index * 8 + rectIndex * 4
                packed[offset] = rect.left
                packed[offset + 1] = rect.top
                packed[offset + 2] = rect.width()
                packed[offset + 3] = rect.height()
            }
        }
    }
}
