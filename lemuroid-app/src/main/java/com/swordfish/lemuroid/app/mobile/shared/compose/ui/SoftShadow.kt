package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.util.LruCache
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One blurred copy of a surface's outline. Negative spread tucks it underneath the surface. */
@Immutable
data class ShadowLayer(val offsetY: Dp, val blur: Dp, val spread: Dp, val alpha: Float)

/**
 * EmuUI's quiet elevation scale. Every level pairs a tight contact shadow with a wide, faint
 * ambient one, both offset straight down, so hierarchy reads without a visible edge.
 */
@Immutable
data class SoftElevation(val layers: List<ShadowLayer>) {
    companion object {
        val None = SoftElevation(emptyList())

        /** Cards resting on the console shell. */
        val Resting =
            SoftElevation(
                listOf(
                    ShadowLayer(offsetY = 1.dp, blur = 3.dp, spread = 0.dp, alpha = 0.045f),
                    ShadowLayer(offsetY = 6.dp, blur = 18.dp, spread = (-5).dp, alpha = 0.06f),
                ),
            )

        /** The selected cartridge and primary panels. */
        val Raised =
            SoftElevation(
                listOf(
                    ShadowLayer(offsetY = 1.dp, blur = 4.dp, spread = 0.dp, alpha = 0.045f),
                    ShadowLayer(offsetY = 8.dp, blur = 26.dp, spread = (-8).dp, alpha = 0.065f),
                ),
            )

        /** Toolbars and panels that float above scrolling content. */
        val Floating =
            SoftElevation(
                listOf(
                    ShadowLayer(offsetY = 2.dp, blur = 6.dp, spread = 0.dp, alpha = 0.05f),
                    ShadowLayer(offsetY = 12.dp, blur = 32.dp, spread = (-10).dp, alpha = 0.08f),
                ),
            )
    }
}

/**
 * Draws [elevation] around the outline of [shape]. The area under the shape itself is clipped
 * out, so translucent surfaces never show a grey shadow through their body. Shadows render once
 * in software at reduced scale and are cached, so they look identical on every API level.
 */
fun Modifier.softShadow(
    shape: Shape,
    elevation: SoftElevation,
    color: Color = ConsoleColors.Ink,
): Modifier =
    if (elevation.layers.isEmpty()) {
        this
    } else {
        drawWithCache {
            val shadows =
                elevation.layers.mapNotNull { layer ->
                    SoftShadowCache.get(shape, size, layoutDirection, density, layer, color)
                }
            val body = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            onDrawBehind {
                clipPath(body, ClipOp.Difference) {
                    shadows.forEach { shadow ->
                        drawImage(
                            shadow.image,
                            dstOffset = shadow.offset,
                            dstSize = shadow.size,
                            filterQuality = FilterQuality.Low,
                        )
                    }
                }
            }
        }
    }

private class RenderedShadow(val image: ImageBitmap, val offset: IntOffset, val size: IntSize)

private object SoftShadowCache {
    private data class Key(
        val shape: Shape,
        val width: Int,
        val height: Int,
        val layoutDirection: LayoutDirection,
        val density: Float,
        val layer: ShadowLayer,
        val color: Int,
    )

    // Shadows are blurry, so a quarter-resolution mask is visually lossless. At that scale a
    // full-screen layer on an unfolded foldable is about 1 MB, so 12 MB holds several screens.
    private val cache =
        object : LruCache<Key, RenderedShadow>(12 * 1024 * 1024) {
            override fun sizeOf(
                key: Key,
                value: RenderedShadow,
            ) = value.image.width * value.image.height * 4
        }

    fun get(
        shape: Shape,
        size: Size,
        layoutDirection: LayoutDirection,
        density: Float,
        layer: ShadowLayer,
        color: Color,
    ): RenderedShadow? {
        if (size.width < 1f || size.height < 1f || layer.alpha <= 0f) return null
        val key =
            Key(
                shape,
                size.width.roundToInt(),
                size.height.roundToInt(),
                layoutDirection,
                density,
                layer,
                color.toArgb(),
            )
        cache.get(key)?.let { return it }
        return render(key, size, color).also { cache.put(key, it) }
    }

    private fun render(
        key: Key,
        size: Size,
        color: Color,
    ): RenderedShadow {
        val density = androidx.compose.ui.unit.Density(key.density)
        val blur = with(density) { key.layer.blur.toPx() }
        val spread = with(density) { key.layer.spread.toPx() }
        val offsetY = with(density) { key.layer.offsetY.toPx() }
        // Bilinear scaling keeps a quarter-scale blur smooth; only near-sharp layers need more.
        val scale = if (blur >= 4f) 0.25f else 0.5f
        // A Gaussian visibly ends just under two radii from its source edge.
        val margin = ceil(blur * 1.8f + 2f)
        val shapeSize = Size((size.width + spread * 2).coerceAtLeast(1f), (size.height + spread * 2).coerceAtLeast(1f))
        val fullWidth = shapeSize.width + margin * 2
        val fullHeight = shapeSize.height + margin * 2
        val bitmap =
            Bitmap.createBitmap(
                ceil(fullWidth * scale).toInt().coerceAtLeast(1),
                ceil(fullHeight * scale).toInt().coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
        val path = Path().apply { addOutline(key.shape.createOutline(shapeSize, key.layoutDirection, density)) }
        val androidPath =
            path.asAndroidPath().apply {
                transform(
                    Matrix().apply {
                        postTranslate(margin, margin)
                        postScale(scale, scale)
                    },
                )
            }
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color.copy(alpha = color.alpha * key.layer.alpha).toArgb()
                if (blur * scale > 0.5f) maskFilter = BlurMaskFilter(blur * scale, BlurMaskFilter.Blur.NORMAL)
            }
        android.graphics.Canvas(bitmap).drawPath(androidPath, paint)
        return RenderedShadow(
            image = bitmap.asImageBitmap(),
            offset = IntOffset((-spread - margin).roundToInt(), (-spread - margin + offsetY).roundToInt()),
            size = IntSize(fullWidth.roundToInt(), fullHeight.roundToInt()),
        )
    }
}
