package com.swordfish.lemuroid.app.shared.library

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import retrofit2.HttpException
import java.io.IOException

internal enum class CoreUpdateOutcome { SUCCESS, RETRY, FAILURE }

// WorkManager counts the first attempt as zero. Keep its existing backoff policy.
private const val MAX_CORE_UPDATE_ATTEMPTS = 3

internal suspend fun performCoreUpdateAttempt(
    runAttemptCount: Int,
    onFailure: (Exception) -> Unit,
    update: suspend () -> Unit,
): CoreUpdateOutcome {
    currentCoroutineContext().ensureActive()
    return try {
        update()
        currentCoroutineContext().ensureActive()
        CoreUpdateOutcome.SUCCESS
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        onFailure(error)
        val transientFailure =
            when (error) {
                is IOException -> true
                is HttpException -> error.code() in 500..599 || error.code() == 408 || error.code() == 429
                else -> false
            }
        if (transientFailure && runAttemptCount < MAX_CORE_UPDATE_ATTEMPTS - 1) {
            CoreUpdateOutcome.RETRY
        } else {
            CoreUpdateOutcome.FAILURE
        }
    }
}
