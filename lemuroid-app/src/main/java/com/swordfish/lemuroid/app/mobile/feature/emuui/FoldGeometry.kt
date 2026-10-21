package com.swordfish.lemuroid.app.mobile.feature.emuui

import kotlin.math.floor
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
}

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

enum class FoldGuidance { ROTATE_LANDSCAPE, ROTATE_HINGE, WINDOW_TOO_SMALL }

/** The orientation a console window should request so its crease runs left to right. */
enum class CreaseOrientation { KEEP, PORTRAIT, LANDSCAPE }

data class FoldLayout(
    val upper: FoldRect,
    val lower: FoldRect,
    val hinge: FoldRect,
    val guidance: FoldGuidance? = null,
    val physicalHinge: Boolean = false,
)

/** Uses the actual horizontal crease, including zero-height folds and off-centre hinges. */
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
        val intersects = fold != null && fold.right >= 0 && fold.left <= w && fold.bottom >= 0 && fold.top <= h
        val horizontal = intersects && fold?.axis == FoldAxis.HORIZONTAL
        val top = if (horizontal) fold!!.top.coerceIn(0, h) else h / 2
        val bottom = if (horizontal) fold!!.bottom.coerceIn(top, h) else h / 2
        val upperEnd = (top - padding).coerceIn(0, h)
        val lowerStart = (bottom + padding).coerceIn(upperEnd, h)
        val guidance =
            when {
                // A clamshell's crease is horizontal in portrait: display above, controls below.
                w <= h && !horizontal -> FoldGuidance.ROTATE_LANDSCAPE
                intersects && fold?.axis == FoldAxis.VERTICAL -> FoldGuidance.ROTATE_HINGE
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

    /**
     * Book-style foldables are horizontal in landscape. Clamshells report a vertical crease in
     * landscape, so they are turned to portrait instead. Large screens letterbox rather than
     * rotate, so a request only happens where the system honours it ([orientationHonoured]):
     * a small, non-letterboxed screen. Portrait is released once no crease is reported (closed,
     * cover screen) or the request can no longer take effect, and a stale vertical report while
     * already portrait is ignored, so a rotation in progress cannot oscillate.
     */
    fun creaseOrientation(
        landscape: Boolean,
        crease: FoldAxis?,
        portraitRequested: Boolean,
        orientationHonoured: Boolean,
    ): CreaseOrientation =
        when {
            !orientationHonoured -> if (portraitRequested) CreaseOrientation.LANDSCAPE else CreaseOrientation.KEEP
            crease == FoldAxis.VERTICAL && landscape -> CreaseOrientation.PORTRAIT
            crease == null && portraitRequested -> CreaseOrientation.LANDSCAPE
            else -> CreaseOrientation.KEEP
        }

    /**
     * DS cores produce one top/bottom framebuffer. Reserve a native-pixel gap that covers
     * the physical hinge, and size BOTH screens to the smaller pane. No crop or stretch.
     * The lower screen leaves room on each side for controls, or below it when
     * [bottomControlsHeight] is set. Returns null if it cannot fit.
     */
    fun dualScreen(
        layout: FoldLayout,
        sideControlsWidth: Int,
        gapStep: Int = 1,
        maxGap: Int = 126,
        bottomControlsHeight: Int = 0,
    ): DualScreenLayout? {
        if (layout.guidance != null || gapStep <= 0) return null
        val availableWidth = layout.lower.width - sideControlsWidth.coerceAtLeast(0) * 2
        if (availableWidth <= 0) return null
        val centerY = (layout.hinge.top + layout.hinge.bottom) / 2f
        // Portrait clamshells keep the controls in a strip under the lower screen instead.
        val lowerLimit = layout.lower.bottom - bottomControlsHeight.coerceAtLeast(0)
        val outerHalfHeight = min(centerY - layout.upper.top, lowerLimit - centerY)
        var scale = 0f
        var finalGap = -1
        // Search the small legal core-option range. This also handles quantisation when
        // fitting the total framebuffer changes the initial pixel-to-screen scale.
        for (gap in 0..maxGap step gapStep) {
            val candidateScale = min(availableWidth / 256f, outerHalfHeight / (192f + gap / 2f))
            if (candidateScale > 0f && gap * candidateScale >= layout.hinge.height) {
                scale = candidateScale
                finalGap = gap
                break
            }
        }
        if (finalGap < 0) return null
        val screenWidth = 256f * scale
        val screenHeight = 192f * scale
        val gapHeight = finalGap * scale
        val left = layout.lower.left + (layout.lower.width - screenWidth) / 2f
        val top = centerY - gapHeight / 2f - screenHeight
        val lowerTop = centerY + gapHeight / 2f
        return DualScreenLayout(
            viewport = ScreenRect(left, top, left + screenWidth, lowerTop + screenHeight),
            upperScreen = ScreenRect(left, top, left + screenWidth, top + screenHeight),
            lowerScreen = ScreenRect(left, lowerTop, left + screenWidth, lowerTop + screenHeight),
            nativeGap = finalGap,
        )
    }
}

data class ScreenRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    fun contains(
        x: Float,
        y: Float,
    ): Boolean = x >= left && x < right && y >= top && y < bottom
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
    ): Pair<Int, Int>? {
        if (!lowerScreen.contains(x, y)) return null
        return floor((x - lowerScreen.left) * 256f / lowerScreen.width).toInt().coerceIn(0, 255) to
            floor((y - lowerScreen.top) * 192f / lowerScreen.height).toInt().coerceIn(0, 191)
    }
}
