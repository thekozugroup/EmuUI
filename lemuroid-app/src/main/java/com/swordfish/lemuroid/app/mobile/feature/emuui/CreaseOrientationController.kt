package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Clamshell foldables report a vertical crease in a landscape window, which the console layout
 * cannot use. This turns such windows to portrait, where the crease runs left to right, and hands
 * back to the manifest's landscape default once no crease is reported.
 */
internal object CreaseOrientationController {
    private const val LARGE_SCREEN_DP = 600

    // Turning a clamshell to portrait is the point: it is the only way its crease runs left to right.
    @SuppressLint("SourceLockedOrientationActivity")
    fun attach(activity: ComponentActivity) {
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
                        // The system ignores orientation requests in multi-window; do not fight it.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && activity.isInMultiWindowMode) {
                            return@collect
                        }
                        val crease =
                            info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()?.let {
                                if (it.orientation == FoldingFeature.Orientation.HORIZONTAL) {
                                    FoldAxis.HORIZONTAL
                                } else {
                                    FoldAxis.VERTICAL
                                }
                            }
                        val metrics = WindowMetricsCalculator.getOrCreate()
                        val bounds = metrics.computeCurrentWindowMetrics(activity).bounds
                        val maximum = metrics.computeMaximumWindowMetrics(activity).bounds
                        // Large screens letterbox fixed-orientation apps instead of rotating, so a
                        // request there would only shrink the window. Only small screens qualify.
                        val honoured =
                            activity.resources.configuration.smallestScreenWidthDp < LARGE_SCREEN_DP &&
                                bounds.width() == maximum.width() && bounds.height() == maximum.height()
                        val portraitRequested =
                            activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        val decision =
                            FoldGeometry.creaseOrientation(
                                bounds.width() > bounds.height(),
                                crease,
                                portraitRequested,
                                honoured,
                            )
                        if (decision != CreaseOrientation.KEEP && !RequestBudget.tryAcquire()) {
                            Timber.w("Crease orientation changed too often; keeping the current orientation")
                            return@collect
                        }
                        when (decision) {
                            CreaseOrientation.PORTRAIT ->
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                            CreaseOrientation.LANDSCAPE ->
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            CreaseOrientation.KEEP -> Unit
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    // Older non-folding devices may not expose a WindowManager extension.
                    Timber.w(exception, "Fold posture unavailable; keeping the manifest orientation")
                }
            }
        }
    }
}

/**
 * A process-wide backstop against rotation loops on hardware that reports postures in ways the
 * decision does not anticipate: at most four requests in ten seconds, then a minute of rest.
 */
private object RequestBudget {
    private const val WINDOW_MS = 10_000L
    private const val MAX_REQUESTS = 4
    private const val REST_MS = 60_000L
    private val recent = ArrayDeque<Long>()
    private var restUntil = 0L

    @Synchronized
    fun tryAcquire(now: Long = SystemClock.elapsedRealtime()): Boolean {
        if (now < restUntil) return false
        while (recent.isNotEmpty() && now - recent.first() > WINDOW_MS) recent.removeFirst()
        if (recent.size >= MAX_REQUESTS) {
            restUntil = now + REST_MS
            recent.clear()
            return false
        }
        recent.addLast(now)
        return true
    }
}
