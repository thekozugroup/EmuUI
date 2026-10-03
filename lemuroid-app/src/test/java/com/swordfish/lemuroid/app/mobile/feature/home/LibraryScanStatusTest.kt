package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.work.WorkInfo.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryScanStatusTest {
    @Test
    fun noScansDoesNotShowAnError() {
        assertFalse(hasFailedLibraryScan(emptyList()))
    }

    @Test
    fun firstScanFailureShowsAnError() {
        assertTrue(hasFailedLibraryScan(listOf(State.FAILED)))
    }

    @Test
    fun successfulPredecessorDoesNotHideLaterFailure() {
        assertTrue(hasFailedLibraryScan(listOf(State.SUCCEEDED, State.FAILED)))
    }

    @Test
    fun failureDoesNotDependOnWorkManagerListOrdering() {
        val snapshots =
            listOf(
                listOf(State.SUCCEEDED, State.FAILED, State.CANCELLED),
                listOf(State.FAILED, State.CANCELLED, State.SUCCEEDED),
                listOf(State.CANCELLED, State.SUCCEEDED, State.FAILED),
                listOf(State.SUCCEEDED, State.CANCELLED, State.FAILED),
            )
        snapshots.forEach { assertTrue("Snapshot $it must show failure", hasFailedLibraryScan(it)) }
    }

    @Test
    fun activeScanStatesSuppressTerminalErrorWhileWorkContinues() {
        listOf(State.ENQUEUED, State.RUNNING, State.BLOCKED).forEach { activeState ->
            assertFalse(
                "Active state $activeState must suppress the terminal error",
                hasFailedLibraryScan(listOf(State.SUCCEEDED, State.FAILED, activeState)),
            )
        }
    }

    @Test
    fun successfulHistoryDoesNotShowAnError() {
        assertFalse(hasFailedLibraryScan(listOf(State.SUCCEEDED, State.SUCCEEDED)))
    }

    @Test
    fun successfulRetryReplacesFailedChainAndClearsError() {
        // WorkManager APPEND_OR_REPLACE deletes the failed named chain when retry is enqueued.
        // The reducer consumes each fresh snapshot, without remembering the old failure.
        val snapshots =
            listOf(
                listOf(State.SUCCEEDED, State.FAILED),
                listOf(State.ENQUEUED),
                listOf(State.RUNNING),
                listOf(State.SUCCEEDED),
                listOf(State.SUCCEEDED, State.SUCCEEDED),
            )
        assertEquals(listOf(true, false, false, false, false), snapshots.map(::hasFailedLibraryScan))
    }

    @Test
    fun cancellationAloneIsNotReportedAsScanFailure() {
        assertFalse(hasFailedLibraryScan(listOf(State.CANCELLED)))
        assertFalse(hasFailedLibraryScan(listOf(State.SUCCEEDED, State.CANCELLED)))
    }
}
