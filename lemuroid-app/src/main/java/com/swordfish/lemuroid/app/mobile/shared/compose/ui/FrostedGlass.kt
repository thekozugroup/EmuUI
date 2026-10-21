package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.app.ActivityManager
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.util.function.Consumer
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Whether frosted surfaces may blur live content. It follows Android's own window-blur switch,
 * which turns off in battery saver, on unsupported hardware and with the developer option.
 */
val LocalBlurAvailable = staticCompositionLocalOf { false }

@Composable
internal fun rememberBlurAvailable(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) rememberCrossWindowBlurEnabled() else false

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val windowManager = remember(context) { context.getSystemService(WindowManager::class.java) }
    val lowRam = remember(context) { context.getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: true }
    var enabled by remember(windowManager) { mutableStateOf(windowManager?.isCrossWindowBlurEnabled == true) }
    DisposableEffect(windowManager) {
        val listener = Consumer<Boolean> { enabled = it }
        windowManager?.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager?.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled && !lowRam
}

/** Content that frosted surfaces drawn above it can see through. It must not contain them. */
@Stable
class Backdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
    internal var version by mutableIntStateOf(0)

    /**
     * Call when content inside the source moves or changes without redrawing the source itself,
     * such as a scroll or an image load. The renderer caches blurred layers until told otherwise.
     */
    fun invalidate() {
        version++
    }
}

@Composable
fun rememberBackdrop(): Backdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { Backdrop(layer) }
}

/** Records this content once per frame so any number of [FrostedSurface]s can sample it. */
fun Modifier.backdropSource(backdrop: Backdrop): Modifier =
    onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            backdrop.layer.record { this@drawWithContent.drawContent() }
            drawLayer(backdrop.layer)
        }

/**
 * A translucent card in the style of Android 17's blurred system surfaces: a warm, lightly
 * accent-tinted wash over a blur of whatever sits behind, with no glass edge or refraction.
 * With a [backdrop], live content behind is blurred. When the system has blur turned off
 * (Battery Saver, "Reduce blur effects", thermal limits) the card turns opaque, as SystemUI does.
 */
@Composable
fun FrostedSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    backdrop: Backdrop? = null,
    tint: Color = FrostDefaults.Tint,
    fallbackTint: Color = FrostDefaults.FallbackTint,
    blurRadius: Dp = FrostDefaults.BlurRadius,
    elevation: SoftElevation = SoftElevation.Resting,
    backdropBase: Color = ConsoleColors.Shell,
    content: @Composable BoxScope.() -> Unit,
) {
    val blurAvailable = LocalBlurAvailable.current
    val source = backdrop?.takeIf { blurAvailable }
    val glass = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier =
            modifier
                .softShadow(shape, elevation)
                .then(if (source != null) Modifier.onGloballyPositioned { origin = it.positionInRoot() } else Modifier)
                .drawWithCache {
                    val outline =
                        Path().apply {
                            addOutline(
                                shape.createOutline(size, layoutDirection, this@drawWithCache),
                            )
                        }
                    val radius = blurRadius.toPx()
                    // Sample past our own edges so the blur does not clamp to a hard border.
                    val pad = ceil(radius * 2f).toInt()
                    onDrawBehind {
                        if (source != null) {
                            // Reading the version re-records the blur whenever the source reports a change.
                            source.version
                            glass.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                            glass.record(
                                IntSize(size.width.roundToInt() + pad * 2, size.height.roundToInt() + pad * 2),
                            ) {
                                // An opaque base, so gaps in the source cannot let sharp content through.
                                drawRect(backdropBase)
                                translate(source.origin.x - origin.x + pad, source.origin.y - origin.y + pad) {
                                    drawLayer(source.layer)
                                }
                            }
                            clipPath(outline) { translate(-pad.toFloat(), -pad.toFloat()) { drawLayer(glass) } }
                        }
                        drawPath(outline, if (blurAvailable) tint else fallbackTint)
                    }
                }
                .clip(shape),
        content = content,
    )
}

/** Tints follow Android 17's shade: a neutral wash with a faint accent, over a 24dp blur. */
object FrostDefaults {
    val BlurRadius = 24.dp
    val Tint = ConsoleColors.Lime.copy(alpha = 0.12f).compositeOver(ConsoleColors.Paper.copy(alpha = 0.62f))
    val FallbackTint = ConsoleColors.Paper
}
