package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.view.WindowInsetsCompat

val LocalWindowCutout = staticCompositionLocalOf { WindowCutoutSnapshot() }

/** Keep the window edge-to-edge even when entering or leaving immersive gameplay. */
fun Window.allowDisplayCutouts() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        attributes =
            attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        // Android 9–10 support short-edge cutouts only. The OS may still
                        // letterbox a long-edge cutout; app geometry cannot override that.
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
    }
}

/** Delivered window coordinates, retained before Compose/AndroidView can consume insets. */
data class WindowCutoutSnapshot(
    val bounds: List<FoldRect> = emptyList(),
    val safeInsets: FoldInsets = FoldInsets(),
    val windowWidth: Int = 0,
    val windowHeight: Int = 0,
) {
    fun localOcclusions(
        width: Int,
        height: Int,
        rootX: Int,
        rootY: Int,
    ): List<FoldRect> {
        // Insets can arrive before the new root has measured during rotation/resize.
        // Never clip precise cutout coordinates against that previous root size.
        val precise = bounds.filter { it.width > 0 && it.height > 0 }
        val occlusions =
            precise.ifEmpty {
                FoldGeometry.cutoutOcclusions(width + rootX, height + rootY, emptyList(), safeInsets)
            }
        return occlusions.map { it.translated(-rootX, -rootY) }
    }

    companion object {
        fun from(
            view: View,
            insets: WindowInsetsCompat,
        ): WindowCutoutSnapshot {
            val cutout = insets.displayCutout
            return WindowCutoutSnapshot(
                bounds = cutout?.boundingRects?.map { FoldRect(it.left, it.top, it.right, it.bottom) }.orEmpty(),
                safeInsets =
                    FoldInsets(
                        cutout?.safeInsetLeft ?: 0,
                        cutout?.safeInsetTop ?: 0,
                        cutout?.safeInsetRight ?: 0,
                        cutout?.safeInsetBottom ?: 0,
                    ),
                windowWidth = view.rootView.width,
                windowHeight = view.rootView.height,
            )
        }
    }
}
