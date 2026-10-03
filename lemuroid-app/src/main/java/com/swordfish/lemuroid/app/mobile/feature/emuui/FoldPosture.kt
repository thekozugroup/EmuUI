package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.CancellationException
import timber.log.Timber

data class FoldPosture(val fold: FoldBounds? = null, val isHalfOpened: Boolean = false)

/** Tracks window-local hardware geometry; the emulation view is never keyed to posture. */
@Composable
fun rememberFoldPosture(): FoldPosture {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val posture = remember(context, lifecycleOwner) { mutableStateOf(FoldPosture()) }
    LaunchedEffect(context, lifecycleOwner) {
        // Reset stale geometry when the hosting activity or lifecycle changes.
        posture.value = FoldPosture()
        val activity = context.findActivity() ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                WindowInfoTracker.getOrCreate(context).windowLayoutInfo(activity).collect { info ->
                    val feature = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                    posture.value =
                        if (feature == null) {
                            FoldPosture()
                        } else {
                            val bounds = feature.bounds
                            FoldPosture(
                                FoldBounds(
                                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                                    if (feature.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                                        FoldAxis.HORIZONTAL
                                    } else {
                                        FoldAxis.VERTICAL
                                    },
                                ),
                                feature.state == FoldingFeature.State.HALF_OPENED,
                            )
                        }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // Older non-folding devices may not expose a WindowManager extension.
                Timber.w(exception, "Fold posture unavailable; using the flat console layout")
                posture.value = FoldPosture()
            }
        }
    }
    return posture.value
}

private fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> if (baseContext !== this) baseContext.findActivity() else null
        else -> null
    }
