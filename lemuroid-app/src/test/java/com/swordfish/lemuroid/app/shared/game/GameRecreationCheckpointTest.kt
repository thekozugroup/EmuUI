package com.swordfish.lemuroid.app.shared.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRecreationCheckpointTest {
    @Test fun checkpointIsNotConsumedWhileWaitingForFirstFrame() {
        val checkpoint = GameRecreationCheckpoint<ByteArray>()
        val snapshot = byteArrayOf(1, 2, 3)
        checkpoint.beginRecreation(snapshot)
        assertSame(snapshot, checkpoint.beginRestore()!!.snapshot)
        assertSame(snapshot, checkpoint.beginRestore()!!.snapshot)
    }

    @Test fun rapidRecreationKeepsLastPlayableState() {
        val checkpoint = GameRecreationCheckpoint<ByteArray>()
        val snapshot = byteArrayOf(42)
        checkpoint.beginRecreation(snapshot)
        val staleSurface = checkpoint.beginRestore()!!
        repeat(20) { checkpoint.beginRecreation() }
        assertFalse(checkpoint.isCurrent(staleSurface))
        assertFalse(checkpoint.complete(staleSurface))
        val currentSurface = checkpoint.beginRestore()!!
        assertSame(snapshot, currentSurface.snapshot)
        assertTrue(checkpoint.complete(currentSurface))
        assertNull(checkpoint.beginRestore())
    }

    @Test fun newerPlayableSnapshotReplacesOldOne() {
        val checkpoint = GameRecreationCheckpoint<ByteArray>()
        checkpoint.beginRecreation(byteArrayOf(1))
        val old = checkpoint.beginRestore()!!
        val newer = byteArrayOf(2)
        checkpoint.beginRecreation(newer)
        assertFalse(checkpoint.complete(old))
        assertSame(newer, checkpoint.beginRestore()!!.snapshot)
    }

    @Test fun successfulRestoreIsConsumedExactlyOnce() {
        val checkpoint = GameRecreationCheckpoint<ByteArray>()
        checkpoint.beginRecreation(byteArrayOf(1))
        val ticket = checkpoint.beginRestore()!!
        assertTrue(checkpoint.complete(ticket))
        assertFalse(checkpoint.complete(ticket))
        assertNull(checkpoint.beginRestore())
    }

    @Test fun missingSnapshotAllowsPersistedAutosaveFallback() {
        val checkpoint = GameRecreationCheckpoint<ByteArray>()
        checkpoint.beginRecreation()
        assertNull(checkpoint.beginRestore())
        checkpoint.beginRecreation(byteArrayOf(7))
        assertNotNull(checkpoint.beginRestore())
    }
}
