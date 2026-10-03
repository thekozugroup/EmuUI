package com.swordfish.lemuroid.app.mobile.feature.home

import androidx.work.WorkInfo

/**
 * Reduces an unordered snapshot of this app's uniquely named APPEND_OR_REPLACE scan chain.
 *
 * Successful predecessors remain in the chain, so they must not hide a later failure.
 * In WorkManager 2.9.0, APPEND_OR_REPLACE deletes the old named WorkSpecs before inserting
 * a replacement when a leaf has failed or been cancelled (EnqueueRunnable.java). Therefore
 * successful retries do not retain an old failure in the next named snapshot. No ordering,
 * timestamps, or assumptions about the position of the current request are needed here.
 *
 * Pending work wins while a scan is queued/running/blocked. Cancellation alone is not an
 * error: the user can cancel scans through the existing settings flow.
 */
internal fun hasFailedLibraryScan(states: List<WorkInfo.State>): Boolean =
    states.any { it == WorkInfo.State.FAILED } && states.none { !it.isFinished }
