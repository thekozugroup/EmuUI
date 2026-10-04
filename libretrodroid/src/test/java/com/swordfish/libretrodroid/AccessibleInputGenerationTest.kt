package com.swordfish.libretrodroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibleInputGenerationTest {
    @Test fun cancellationRejectsAlreadyQueuedTapsBeforeResume() {
        val generation = AccessibleInputGeneration()
        val queuedTap = generation.snapshot()
        generation.invalidate()
        assertFalse(generation.isCurrent(queuedTap))
        assertTrue(generation.isCurrent(generation.snapshot()))
    }

    @Test fun repeatedLifecycleChangesNeverReviveAnOldTicket() {
        val generation = AccessibleInputGeneration()
        val oldTap = generation.snapshot()
        repeat(100) { generation.invalidate() }
        assertFalse(generation.isCurrent(oldTap))
        val newerTap = generation.snapshot()
        assertTrue(generation.isCurrent(newerTap))
        generation.invalidate()
        assertFalse(generation.isCurrent(newerTap))
    }

    @Test fun queuedOldInputIsDroppedWhileNewInputStillExecutes() {
        val generation = AccessibleInputGeneration()
        val oldTap = generation.snapshot()
        var oldTapExecuted = false
        val queuedOldTap = { if (generation.isCurrent(oldTap)) oldTapExecuted = true }
        generation.invalidate()
        val newTap = generation.snapshot()
        var newTapExecuted = false
        queuedOldTap()
        if (generation.isCurrent(newTap)) newTapExecuted = true
        assertFalse(oldTapExecuted)
        assertTrue(newTapExecuted)
    }
}
