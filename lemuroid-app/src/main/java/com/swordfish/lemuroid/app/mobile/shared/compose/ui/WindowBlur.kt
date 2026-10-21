package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider

/** Radii follow Android 17's system surfaces; dims keep text legible with and without blur. */
object WindowBlurDefaults {
    /** Dialogs and sheets. Android 17 blurs behind its own bottom sheets at 24 to 30dp. */
    val Radius = 24.dp

    /** Full-screen pause surfaces, matching the 34dp blur behind Android 17's power menu. */
    val PauseRadius = 34.dp
    const val BLURRED_DIM = 0.28f
    const val UNBLURRED_DIM = 0.6f
}

/**
 * Blurs whatever sits behind the window, as Android's own dialogs and shade do on Android 12+.
 * Without cross-window blur the window falls back to a stronger dim. Safe on any API level.
 */
fun Window.applyBlurBehind(
    radiusPx: Int,
    blurEnabled: Boolean,
    blurredDim: Float = WindowBlurDefaults.BLURRED_DIM,
    unblurredDim: Float = WindowBlurDefaults.UNBLURRED_DIM,
) {
    addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurEnabled) {
        addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        attributes = attributes.apply { blurBehindRadius = radiusPx }
        setDimAmount(blurredDim)
    } else {
        clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
        setDimAmount(unblurredDim)
    }
}

/** Place inside any Compose dialog or sheet content to blur the screen behind its window. */
@Composable
fun DialogBlurBehind(radius: Dp = WindowBlurDefaults.Radius) {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val radiusPx = with(LocalDensity.current) { radius.roundToPx() }
    val blurEnabled = LocalBlurAvailable.current
    DisposableEffect(window, radiusPx, blurEnabled) {
        window.applyBlurBehind(radiusPx, blurEnabled)
        onDispose { }
    }
}
