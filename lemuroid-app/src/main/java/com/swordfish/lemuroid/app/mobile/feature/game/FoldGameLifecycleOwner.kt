package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

/**
 * Keeps one native view/core, but caps its rendering lifecycle while the inner console is
 * unavailable. The real Activity lifecycle remains the upper bound, so a background/menu
 * transition can never be resumed by an arriving fold event.
 */
internal class FoldGameLifecycleOwner(private val host: LifecycleOwner) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    private var consoleAvailable = false
    private val observer = LifecycleEventObserver { _, _ -> synchronize() }
    override val lifecycle: Lifecycle get() = registry

    init {
        host.lifecycle.addObserver(observer)
        synchronize()
    }

    fun setConsoleAvailable(available: Boolean) {
        consoleAvailable = available
        synchronize()
    }

    private fun synchronize() {
        val maximum = if (consoleAvailable) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
        registry.currentState = minOf(host.lifecycle.currentState, maximum)
    }

    fun dispose() {
        host.lifecycle.removeObserver(observer)
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
