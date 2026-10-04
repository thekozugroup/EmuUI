package com.swordfish.libretrodroid

import java.util.concurrent.atomic.AtomicLong

/** Invalidates queued input without waiting for the emulation thread. */
internal class AccessibleInputGeneration {
    private val generation = AtomicLong(0)

    fun snapshot(): Long = generation.get()

    fun isCurrent(ticket: Long): Boolean = generation.get() == ticket

    fun invalidate() {
        generation.incrementAndGet()
    }
}
