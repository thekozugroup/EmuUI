package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.app.Activity
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Keep Android's transient swipe escape; never suppress system navigation gestures. */
fun Activity.enterImmersiveConsole() = window.enterImmersiveConsole()

fun Window.enterImmersiveConsole() {
    WindowCompat.getInsetsController(this, decorView).apply {
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}
