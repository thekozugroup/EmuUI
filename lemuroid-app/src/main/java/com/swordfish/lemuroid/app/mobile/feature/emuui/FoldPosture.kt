package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/** A feature reported for this window is positive evidence of its opened folding display. */
data class FoldPosture(val fold: FoldBounds? = null, val isHalfOpened: Boolean = false) {
    val isOpenInnerDisplay: Boolean get() = fold != null
}

/** Tracks window-local hardware geometry; the emulation view is never keyed to posture. */
@Composable
fun rememberFoldPosture(): FoldPosture {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val posture =
        remember(
            context,
            lifecycleOwner,
            configuration.orientation,
            configuration.screenWidthDp,
            configuration.screenHeightDp,
        ) {
            mutableStateOf(FoldPosture())
        }
    LaunchedEffect(context, lifecycleOwner, posture) {
        // Reset stale geometry when the hosting activity or lifecycle changes.
        posture.value = FoldPosture()
        val activity = context.findActivity() ?: return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Moving to the cover screen or another display must not reuse the last open fold.
            posture.value = FoldPosture()
            try {
                WindowInfoTracker.getOrCreate(context).windowLayoutInfo(activity).collect { info ->
                    val feature =
                        info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull {
                            it.state == FoldingFeature.State.FLAT || it.state == FoldingFeature.State.HALF_OPENED
                        }
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
                // Missing OEM data, a cover/external display, and an ordinary phone all fail closed.
                Timber.w(exception, "Fold posture unavailable; showing open-inner-display guidance")
            } finally {
                posture.value = FoldPosture()
            }
        }
    }
    return posture.value
}

internal fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> if (baseContext !== this) baseContext.findActivity() else null
        else -> null
    }
