package com.swordfish.lemuroid.app.mobile.feature.game

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.swordfish.lemuroid.app.mobile.feature.emuui.ScreenRect
import com.swordfish.libretrodroid.GLRetroView

/** One stylus, lower DS screen only. Never let a hinge/control touch reach the core. */
internal class FoldTouchGate : View.OnTouchListener {
    private var target: ScreenRect? = null
    private var pointerId: Int? = null

    fun update(
        view: GLRetroView,
        target: ScreenRect?,
    ) {
        if (this.target != target) release(view)
        this.target = target
    }

    fun release(view: GLRetroView) {
        if (pointerId == null) return
        pointerId = null
        val time = SystemClock.uptimeMillis()
        val release = MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, 0f, 0f, 0)
        view.onTouchEvent(release)
        release.recycle()
    }

    override fun onTouch(
        view: View,
        event: MotionEvent,
    ): Boolean {
        val gameView = view as GLRetroView
        val screen = target ?: return true
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            release(gameView)
            if (!screen.contains(event.x, event.y)) return true
            pointerId = event.getPointerId(0)
        }
        val id = pointerId ?: return true
        val index = event.findPointerIndex(id)
        if (index < 0 || event.actionMasked == MotionEvent.ACTION_CANCEL ||
            event.actionMasked == MotionEvent.ACTION_UP ||
            (event.actionMasked == MotionEvent.ACTION_POINTER_UP && event.getPointerId(event.actionIndex) == id)
        ) {
            release(gameView)
            return true
        }
        val x = event.getX(index)
        val y = event.getY(index)
        if (!screen.contains(x, y)) {
            release(gameView)
            return true
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
            // GLRetroView normalizes window coordinates; LibretroDroid applies the SAME
            // viewport/letterboxing transform used for rendering before calling the core.
            val mapped = MotionEvent.obtain(event.downTime, event.eventTime, event.actionMasked, x, y, event.metaState)
            gameView.onTouchEvent(mapped)
            mapped.recycle()
        }
        return true
    }
}
