package com.swordfish.lemuroid.app.shared.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withTimeoutOrNull

/** Bounds a component's lifetime without cancelling the process-owned submission it observes. */
suspend fun finishSubmissionWithin(
    submitted: Deferred<Unit>,
    timeoutMillis: Long,
    onTimeout: () -> Unit,
    onFailure: (Exception) -> Unit,
    finish: () -> Unit,
) {
    try {
        if (withTimeoutOrNull(timeoutMillis) { submitted.await() } == null) onTimeout()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        onFailure(failure)
    } finally {
        finish()
    }
}
