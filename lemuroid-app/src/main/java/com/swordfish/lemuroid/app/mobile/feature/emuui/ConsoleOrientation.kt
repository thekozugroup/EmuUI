package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Surface
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

/** FoldingFeature has no rear-camera-side metadata. Calibrate the actual device, never its model name. */
object ConsoleOrientation {
    const val KEY = "emuui_calibrated_orientation"
    const val DEVICE_KEY = "emuui_orientation_device"
    private val deviceKey get() = "${Build.MANUFACTURER}/${Build.MODEL}"

    fun calibrate(activity: Activity) {
        if (activity.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) return
        @Suppress("DEPRECATION")
        val rotation = activity.windowManager.defaultDisplay.rotation
        val value = landscapeRequest(rotation)
        SharedPreferencesHelper.getSharedPreferences(activity).edit()
            .putInt(KEY, value).putString(DEVICE_KEY, deviceKey).apply()
        activity.requestedOrientation = value
    }

    /** Both natural-landscape and natural-portrait devices: derive from observed landscape rotation. */
    fun landscapeRequest(rotation: Int): Int = when (rotation) {
        Surface.ROTATION_180, Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    fun apply(activity: Activity) {
        val prefs = SharedPreferencesHelper.getSharedPreferences(activity)
        if (prefs.getString(DEVICE_KEY, null) != deviceKey) return
        val value = prefs.getInt(KEY, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        if (value == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || value == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
            activity.requestedOrientation = value
        }
    }

    fun clear(activity: Activity) {
        SharedPreferencesHelper.getSharedPreferences(activity).edit().remove(KEY).remove(DEVICE_KEY).apply()
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
