package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp

/** Continuous feathering around the rounded artwork, without stroked rings or API31 blur. */
internal fun DrawScope.drawArtworkGlow(accent: Color, strength: Float) {
    if (strength <= 0f) return
    val spread = 16.dp.toPx()
    val radius = minOf(12.dp.toPx(), size.minDimension / 2)
    val edge = accent.copy(alpha = .32f * strength)
    val feather = accent.copy(alpha = .08f * strength)
    val clear = accent.copy(alpha = 0f)
    val inward = arrayOf(0f to clear, .5f to feather, 1f to edge)
    val outward = arrayOf(0f to edge, .5f to feather, 1f to clear)
    drawRect(Brush.verticalGradient(*inward, startY = -spread, endY = 0f),
        Offset(radius, -spread), Size((size.width - radius * 2).coerceAtLeast(0f), spread))
    drawRect(Brush.verticalGradient(*outward, startY = size.height, endY = size.height + spread),
        Offset(radius, size.height), Size((size.width - radius * 2).coerceAtLeast(0f), spread))
    drawRect(Brush.horizontalGradient(*inward, startX = -spread, endX = 0f),
        Offset(-spread, radius), Size(spread, (size.height - radius * 2).coerceAtLeast(0f)))
    drawRect(Brush.horizontalGradient(*outward, startX = size.width, endX = size.width + spread),
        Offset(size.width, radius), Size(spread, (size.height - radius * 2).coerceAtLeast(0f)))
    val outer = radius + spread
    for ((center, bounds) in listOf(
        Offset(radius, radius) to floatArrayOf(-spread, -spread, radius, radius),
        Offset(size.width - radius, radius) to floatArrayOf(size.width - radius, -spread, size.width + spread, radius),
        Offset(radius, size.height - radius) to floatArrayOf(-spread, size.height - radius, radius, size.height + spread),
        Offset(size.width - radius, size.height - radius) to floatArrayOf(size.width - radius, size.height - radius, size.width + spread, size.height + spread),
    )) {
        clipRect(bounds[0], bounds[1], bounds[2], bounds[3]) {
            drawRect(
                Brush.radialGradient(0f to edge, radius / outer to edge,
                    (radius + spread / 2) / outer to feather, 1f to clear, center = center, radius = outer),
                topLeft = Offset(center.x - outer, center.y - outer), size = Size(outer * 2, outer * 2),
            )
        }
    }
}
