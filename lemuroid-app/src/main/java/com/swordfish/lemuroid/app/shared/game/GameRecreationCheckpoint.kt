package com.swordfish.lemuroid.app.shared.game

/**
 * A memory-only checkpoint is consumed only by the surface generation that restored it.
 * If a second recreation happens before a first frame, the previous checkpoint survives.
 * Accessed on the main thread by the activity/view-model lifecycle.
 */
internal class GameRecreationCheckpoint<T : Any> {
    data class Ticket<T>(val generation: Int, val snapshot: T)

    private var generation = 0
    private var snapshot: T? = null

    fun beginRecreation(newSnapshot: T? = null) {
        generation++
        if (newSnapshot != null) snapshot = newSnapshot
    }

    fun beginRestore(): Ticket<T>? = snapshot?.let { Ticket(generation, it) }

    fun isCurrent(ticket: Ticket<T>): Boolean = ticket.generation == generation && snapshot === ticket.snapshot

    fun complete(ticket: Ticket<T>): Boolean {
        if (!isCurrent(ticket)) return false
        snapshot = null
        return true
    }
}
