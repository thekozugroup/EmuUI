package com.swordfish.lemuroid.app.mobile.feature.emuui

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Window coordinates in physical pixels, independent of Android and Compose. */
data class FoldRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val x: Int get() = left
    val y: Int get() = top
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x < right && y >= top && y < bottom

    fun translated(
        dx: Int,
        dy: Int,
    ) = FoldRect(left + dx, top + dy, right + dx, bottom + dy)

    fun intersection(other: FoldRect): FoldRect {
        val x = max(left, other.left)
        val y = max(top, other.top)
        return FoldRect(x, y, min(right, other.right).coerceAtLeast(x), min(bottom, other.bottom).coerceAtLeast(y))
    }

    fun inset(pixels: Int): FoldRect {
        val inset = pixels.coerceIn(0, min(width, height) / 2)
        return FoldRect(left + inset, top + inset, right - inset, bottom - inset)
    }
}

/** Physical window-edge insets; independent of layout direction. */
data class FoldInsets(val left: Int = 0, val top: Int = 0, val right: Int = 0, val bottom: Int = 0)

enum class FoldAxis { HORIZONTAL, VERTICAL }

data class FoldBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val axis: FoldAxis,
) {
    fun relativeTo(
        x: Int,
        y: Int,
    ) = copy(left = left - x, top = top - y, right = right - x, bottom = bottom - y)
}

enum class FoldGuidance { OPEN_FOLDABLE, ROTATE_LANDSCAPE, ROTATE_HINGE, WINDOW_TOO_SMALL }

data class FoldLayout(
    val upper: FoldRect,
    val lower: FoldRect,
    val hinge: FoldRect,
    val guidance: FoldGuidance? = null,
    val physicalHinge: Boolean = false,
)

/**
 * A current-window hardware FoldingFeature is required. A wide window, remembered device,
 * model name, or an absent feature is never evidence that the inner display is open.
 * The midpoint below only positions the guidance background, never an enabled console.
 */
object FoldGeometry {
    fun resolve(
        width: Int,
        height: Int,
        fold: FoldBounds?,
        safetyPadding: Int = 0,
    ): FoldLayout {
        val w = width.coerceAtLeast(0)
        val h = height.coerceAtLeast(0)
        val padding = safetyPadding.coerceAtLeast(0)
        // A feature outside this window must not split a multi-window surface.
        val validBounds = fold != null && fold.left <= fold.right && fold.top <= fold.bottom
        val intersects = validBounds && fold!!.right >= 0 && fold.left <= w && fold.bottom >= 0 && fold.top <= h
        val horizontal = intersects && fold?.axis == FoldAxis.HORIZONTAL
        val top = if (horizontal) fold!!.top.coerceIn(0, h) else h / 2
        val bottom = if (horizontal) fold!!.bottom.coerceIn(top, h) else h / 2
        val upperEnd = (top - padding).coerceIn(0, h)
        val lowerStart = (bottom + padding).coerceIn(upperEnd, h)
        val guidance =
            when {
                !intersects -> FoldGuidance.OPEN_FOLDABLE
                w <= h -> FoldGuidance.ROTATE_LANDSCAPE
                fold?.axis == FoldAxis.VERTICAL -> FoldGuidance.ROTATE_HINGE
                fold == null || fold.left > 0 || fold.right < w -> FoldGuidance.WINDOW_TOO_SMALL
                upperEnd == 0 || lowerStart == h || w == 0 -> FoldGuidance.WINDOW_TOO_SMALL
                else -> null
            }
        return FoldLayout(
            upper = FoldRect(0, 0, w, upperEnd),
            lower = FoldRect(0, lowerStart, w, h),
            hinge = FoldRect(0, upperEnd, w, lowerStart),
            guidance = guidance,
            physicalHinge = horizontal,
        )
    }

    /** Insets apply to essential controls and the lower touchscreen, not the upper image. */
    fun safeBounds(
        width: Int,
        height: Int,
        insets: FoldInsets,
    ): FoldRect {
        val w = width.coerceAtLeast(0)
        val h = height.coerceAtLeast(0)
        val left = insets.left.coerceIn(0, w)
        val top = insets.top.coerceIn(0, h)
        return FoldRect(
            left,
            top,
            (w - insets.right.coerceAtLeast(0)).coerceIn(left, w),
            (h - insets.bottom.coerceAtLeast(0)).coerceIn(top, h),
        )
    }

    /**
     * Use precise camera/notch rectangles whenever Android reports them. If an OEM only
     * supplies safe insets, keep those edge strips clear rather than guessing the hole.
     */
    fun cutoutOcclusions(
        width: Int,
        height: Int,
        bounds: List<FoldRect>,
        safeInsets: FoldInsets,
    ): List<FoldRect> {
        val window = FoldRect(0, 0, width.coerceAtLeast(0), height.coerceAtLeast(0))
        val reported = bounds.map { it.intersection(window) }.filter { it.width > 0 && it.height > 0 }
        if (reported.isNotEmpty()) return reported
        val safe = safeBounds(window.width, window.height, safeInsets)
        return listOf(
            window.copy(right = safe.left),
            window.copy(bottom = safe.top),
            window.copy(left = safe.right),
            window.copy(top = safe.bottom),
        ).filter { it.width > 0 && it.height > 0 }
    }

    /** Fixed lower wings keep both touch controls visible, even with a hardware gamepad. */
    fun lowerConsole(
        lower: FoldRect,
        sideControlsWidth: Int,
        contentGap: Int = 0,
    ): LowerConsoleLayout {
        val side = sideControlsWidth.coerceIn(0, lower.width / 2)
        val gap = contentGap.coerceIn(0, (lower.width - side * 2) / 2)
        return LowerConsoleLayout(
            leftControls = lower.copy(right = lower.left + side),
            center = lower.copy(left = lower.left + side + gap, right = lower.right - side - gap),
            rightControls = lower.copy(left = lower.right - side),
        )
    }

    /**
     * The native viewport stays rectangular and entirely inside this rounded backing.
     * Half a radius on both axes clears the corner arc; one extra pixel also protects
     * outward-rounded native raster edges. Never apply a rounded clip to game pixels.
     */
    fun displayPanel(
        region: FoldRect,
        outerInset: Int,
        cornerRadius: Int,
    ): DisplayPanelLayout {
        val bounds = region.inset(outerInset)
        val radius = cornerRadius.coerceIn(0, min(bounds.width, bounds.height) / 2)
        val contentInset = (radius + 1) / 2 + 1
        return DisplayPanelLayout(bounds, bounds.inset(contentInset), radius)
    }

    /**
     * Largest centered, aspect-preserving rectangle inside the actual rounded backing.
     * Letterboxing often already clears the corner arcs, so a blanket radius inset would
     * unnecessarily shrink the image. The guard keeps native raster rounding off the arc.
     */
    fun fitInsidePanel(
        panel: DisplayPanelLayout,
        aspectRatio: Float,
        rasterGuard: Int = 1,
        occlusions: List<FoldRect> = emptyList(),
    ): ScreenRect? {
        if (!aspectRatio.isFinite() || aspectRatio <= 0f) return null
        val bounds = panel.bounds
        val guard = rasterGuard.coerceAtLeast(0).toDouble()
        val ratio = aspectRatio.toDouble()
        val maximumHeight = min((bounds.width - guard * 2) / ratio, bounds.height - guard * 2)
        if (maximumHeight <= 0.0) return null
        val radius = panel.cornerRadius.toDouble()

        fun clearsCorners(height: Double): Boolean {
            val dx = max(0.0, radius - (bounds.width - height * ratio) / 2 + guard)
            val dy = max(0.0, radius - (bounds.height - height) / 2 + guard)
            return dx * dx + dy * dy <= radius * radius
        }
        var low = 0.0
        var high = maximumHeight
        // The four corners are symmetric and clearance decreases monotonically with size.
        repeat(48) {
            val candidate = (low + high) / 2
            if (clearsCorners(candidate)) low = candidate else high = candidate
        }
        if (low <= 0.0) return null
        val centerX = (bounds.left.toDouble() + bounds.right) / 2
        val centerY = (bounds.top.toDouble() + bounds.bottom) / 2
        val centered =
            ScreenRect(
                (centerX - low * ratio / 2).toFloat(),
                (centerY - low / 2).toFloat(),
                (centerX + low * ratio / 2).toFloat(),
                (centerY + low / 2).toFloat(),
            ).takeIf { it.width > 0f && it.height > 0f } ?: return null
        val obstacles = occlusions.map { it.intersection(bounds) }.filter { it.width > 0 && it.height > 0 }
        if (obstacles.none { centered.overlaps(it, guard.toFloat()) }) return centered
        return fitAvoidingOcclusions(panel, ratio, guard, obstacles)
    }

    /**
     * A camera cannot display pixels. Fit the complete image around its reported bounds
     * rather than extending game pixels behind it or reserving its whole screen-edge band.
     * Maximal empty rectangles cover every legal rectangular viewport. For each one,
     * choose the closest feasible center to the panel center and retain the true backing's
     * rounded corners; no artificial rounding is added at a cutout's safe boundary.
     */
    private fun fitAvoidingOcclusions(
        panel: DisplayPanelLayout,
        ratio: Double,
        guard: Double,
        obstacles: List<FoldRect>,
    ): ScreenRect? {
        var regions = listOf(panel.bounds)
        for (obstacle in obstacles) {
            regions =
                regions.flatMap { region ->
                    val cut = region.intersection(obstacle)
                    if (cut.width == 0 || cut.height == 0) {
                        listOf(region)
                    } else {
                        listOf(
                            region.copy(right = cut.left),
                            region.copy(left = cut.right),
                            region.copy(bottom = cut.top),
                            region.copy(top = cut.bottom),
                        ).filter { it.width > guard * 2 && it.height > guard * 2 }
                    }
                }.distinct().let { candidates ->
                    candidates.filter { candidate ->
                        candidates.none { other ->
                            other != candidate && candidate.left >= other.left && candidate.top >= other.top &&
                                candidate.right <= other.right && candidate.bottom <= other.bottom
                        }
                    }
                }
        }
        val bounds = panel.bounds
        val centerX = (bounds.left.toDouble() + bounds.right) / 2
        val centerY = (bounds.top.toDouble() + bounds.bottom) / 2
        val radius = panel.cornerRadius.toDouble()
        var best: ScreenRect? = null
        var bestDistance = Double.POSITIVE_INFINITY
        for (region in regions) {
            var low = 0.0
            var high = min((region.width - guard * 2) / ratio, region.height - guard * 2)
            if (high <= 0) continue

            fun fit(height: Double): ScreenRect? {
                val halfWidth = height * ratio / 2
                val halfHeight = height / 2
                val minimumX = region.left + halfWidth + guard
                val maximumX = region.right - halfWidth - guard
                val minimumY = region.top + halfHeight + guard
                val maximumY = region.bottom - halfHeight - guard
                if (minimumX > maximumX || minimumY > maximumY) return null
                val x = centerX.coerceIn(minimumX, maximumX)
                val y = centerY.coerceIn(minimumY, maximumY)
                for (edgeX in listOf(x - halfWidth - guard, x + halfWidth + guard)) {
                    for (edgeY in listOf(y - halfHeight - guard, y + halfHeight + guard)) {
                        val dx = edgeX - edgeX.coerceIn(bounds.left + radius, bounds.right - radius)
                        val dy = edgeY - edgeY.coerceIn(bounds.top + radius, bounds.bottom - radius)
                        if (dx * dx + dy * dy > radius * radius + 0.000001) return null
                    }
                }
                return ScreenRect(
                    (x - halfWidth).toFloat(),
                    (y - halfHeight).toFloat(),
                    (x + halfWidth).toFloat(),
                    (y + halfHeight).toFloat(),
                )
            }
            repeat(48) {
                val candidate = (low + high) / 2
                if (fit(candidate) != null) low = candidate else high = candidate
            }
            val screen = fit(low)?.takeIf { it.width > 0f && it.height > 0f } ?: continue
            val dx = (screen.left + screen.right) / 2 - centerX
            val dy = (screen.top + screen.bottom) / 2 - centerY
            val distance = dx * dx + dy * dy
            val previous = best
            if (previous == null || screen.height > previous.height + 0.001f ||
                (abs(screen.height - previous.height) <= 0.001f && distance < bestDistance)
            ) {
                best = screen
                bestDistance = distance
            }
        }
        return best
    }

    /** Leave control wings comfortable; keep the lower image at most 94% of the upper width. */
    fun balancedDualScreen(
        upper: DisplayPanelLayout,
        lower: DisplayPanelLayout,
        upperOcclusions: List<FoldRect> = emptyList(),
    ): IndependentDualScreenLayout? {
        val fitted = independentDualScreen(upper, lower, upperOcclusions) ?: return null
        val bottom = fitted.lowerScreen
        val scale = minOf(1f, fitted.upperScreen.width * 0.94f / bottom.width)
        val cx = (bottom.left + bottom.right) / 2f
        val cy = (bottom.top + bottom.bottom) / 2f
        return fitted.copy(lowerScreen = ScreenRect(
            cx - bottom.width * scale / 2f, cy - bottom.height * scale / 2f,
            cx + bottom.width * scale / 2f, cy + bottom.height * scale / 2f,
        ))
    }

    /** Requires a renderer that can place two source screens independently. */
    fun independentDualScreen(
        upper: DisplayPanelLayout,
        lower: DisplayPanelLayout,
        upperOcclusions: List<FoldRect> = emptyList(),
    ): IndependentDualScreenLayout? {
        if (upper.bounds.bottom > lower.bounds.top) return null
        val upperScreen = fitInsidePanel(upper, 4f / 3f, occlusions = upperOcclusions) ?: return null
        val lowerScreen = fitInsidePanel(lower, 4f / 3f) ?: return null
        return IndependentDualScreenLayout(upperScreen, lowerScreen)
    }

    /**
     * Compatibility path for one stacked native DS framebuffer. Both screens must have
     * the same scale. Search every legal core gap for the largest scale that fits, while
     * anchoring the lower screen at the exact vertical center of its safe pane. A layout
     * that needs a larger native gap is rejected instead of cropping or shifting a screen.
     */
    fun dualScreen(
        layout: FoldLayout,
        sideControlsWidth: Int,
        gapStep: Int = 1,
        maxGap: Int = 126,
    ): DualScreenLayout? {
        if (layout.guidance != null || gapStep <= 0) return null
        val side = sideControlsWidth.coerceAtLeast(0)
        val availableLeft = max(layout.upper.left, layout.lower.left + side)
        val availableRight = min(layout.upper.right, layout.lower.right - side)
        val availableWidth = availableRight - availableLeft
        if (availableWidth <= 0 || layout.upper.height <= 0 || layout.lower.height <= 0) return null
        val lowerCenter = (layout.lower.top.toDouble() + layout.lower.bottom) / 2
        val upperCenter = (layout.upper.top.toDouble() + layout.upper.bottom) / 2
        var scale = 0.0
        var finalGap = -1
        var upperCenterError = Double.POSITIVE_INFINITY
        for (gap in 0..maxGap step gapStep) {
            val candidateScale =
                minOf(
                    availableWidth / 256.0,
                    layout.lower.height / 192.0,
                    (lowerCenter - layout.upper.top) / (288.0 + gap),
                )
            val upperBottom = lowerCenter - (96.0 + gap) * candidateScale
            if (candidateScale <= 0.0 || upperBottom > layout.upper.bottom + 0.000001) continue
            val centerError = abs(lowerCenter - (192.0 + gap) * candidateScale - upperCenter)
            if (candidateScale > scale || (candidateScale == scale && centerError < upperCenterError)) {
                scale = candidateScale
                finalGap = gap
                upperCenterError = centerError
            }
        }
        if (finalGap < 0) return null
        val screenWidth = 256.0 * scale
        val screenHeight = 192.0 * scale
        val left = availableLeft + (availableWidth - screenWidth) / 2
        val top = lowerCenter - (288.0 + finalGap) * scale
        val lowerTop = lowerCenter - screenHeight / 2
        return DualScreenLayout(
            viewport =
                ScreenRect(
                    left.toFloat(),
                    top.toFloat(),
                    (left + screenWidth).toFloat(),
                    (lowerTop + screenHeight).toFloat(),
                ),
            upperScreen =
                ScreenRect(
                    left.toFloat(),
                    top.toFloat(),
                    (left + screenWidth).toFloat(),
                    (top + screenHeight).toFloat(),
                ),
            lowerScreen =
                ScreenRect(
                    left.toFloat(),
                    lowerTop.toFloat(),
                    (left + screenWidth).toFloat(),
                    (lowerTop + screenHeight).toFloat(),
                ),
            nativeGap = finalGap,
        )
    }
}

data class LowerConsoleLayout(val leftControls: FoldRect, val center: FoldRect, val rightControls: FoldRect)

data class DisplayPanelLayout(val bounds: FoldRect, val content: FoldRect, val cornerRadius: Int)

data class ScreenRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x < right && y >= top && y < bottom

    fun overlaps(
        rect: FoldRect,
        guard: Float = 0f,
    ): Boolean =
        left - guard < rect.right && right + guard > rect.left &&
            top - guard < rect.bottom && bottom + guard > rect.top

    /** Inward rounding keeps a subsequent integer layout inside these safe bounds. */
    fun enclosedPixels() = FoldRect(ceil(left).toInt(), ceil(top).toInt(), floor(right).toInt(), floor(bottom).toInt())
}

data class IndependentDualScreenLayout(
    val upperScreen: ScreenRect,
    val lowerScreen: ScreenRect,
) {
    fun touchCoordinates(
        x: Float,
        y: Float,
    ): Pair<Int, Int>? = lowerTouchCoordinates(lowerScreen, x, y)
}

data class DualScreenLayout(
    val viewport: ScreenRect,
    val upperScreen: ScreenRect,
    val lowerScreen: ScreenRect,
    val nativeGap: Int,
) {
    /** Useful for tests and stylus overlays; rejects hinge, letterbox and control touches. */
    fun touchCoordinates(
        x: Float,
        y: Float,
    ): Pair<Int, Int>? = lowerTouchCoordinates(lowerScreen, x, y)
}

private fun lowerTouchCoordinates(
    screen: ScreenRect,
    x: Float,
    y: Float,
): Pair<Int, Int>? {
    if (!screen.contains(x, y)) return null
    // Center-relative arithmetic avoids a one-pixel bias when fractional viewport edges
    // are rounded to Float: the exact display center always maps to DS pixel (128, 96).
    val centerX = (screen.left + screen.right) / 2f
    val centerY = (screen.top + screen.bottom) / 2f
    return floor(128.0 + (x - centerX) * 256.0 / screen.width).toInt().coerceIn(0, 255) to
        floor(96.0 + (y - centerY) * 192.0 / screen.height).toInt().coerceIn(0, 191)
}
