package com.swordfish.lemuroid.app.shared.library

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class CoreUpdateAttemptTest {
    @Test
    fun completedDownloadIsSuccessful() =
        runBlocking {
            var calls = 0
            val outcome = performCoreUpdateAttempt(0, { throw AssertionError(it) }) { calls++ }
            assertEquals(CoreUpdateOutcome.SUCCESS, outcome)
            assertEquals(1, calls)
        }

    @Test
    fun dnsFailureRetriesTwiceThenFails() =
        runBlocking {
            val failure = UnknownHostException("github.com")
            val logged = mutableListOf<Exception>()
            val outcomes =
                (0..3).map { attempt ->
                    performCoreUpdateAttempt(attempt, logged::add) { throw failure }
                }
            assertEquals(
                listOf(
                    CoreUpdateOutcome.RETRY,
                    CoreUpdateOutcome.RETRY,
                    CoreUpdateOutcome.FAILURE,
                    CoreUpdateOutcome.FAILURE,
                ),
                outcomes,
            )
            assertEquals(4, logged.size)
            logged.forEach { assertSame(failure, it) }
        }

    @Test
    fun transientIoFailuresRetry() =
        runBlocking {
            listOf(IOException("Disconnected"), SocketTimeoutException()).forEach { error ->
                assertEquals(CoreUpdateOutcome.RETRY, performCoreUpdateAttempt(0, {}) { throw error })
            }
        }

    @Test
    fun transientHttpFailuresUseTheSameBoundedRetry() =
        runBlocking {
            listOf(408, 429, 500, 502, 503, 504).forEach { code ->
                val error = HttpException(Response.error<Unit>(code, "error".toResponseBody()))
                assertEquals(CoreUpdateOutcome.RETRY, performCoreUpdateAttempt(0, {}) { throw error })
                assertEquals(CoreUpdateOutcome.FAILURE, performCoreUpdateAttempt(2, {}) { throw error })
            }
        }

    @Test
    fun permanentHttpFailuresDoNotRetry() =
        runBlocking {
            listOf(400, 401, 403, 404, 410, 422).forEach { code ->
                val error = HttpException(Response.error<Unit>(code, "error".toResponseBody()))
                assertEquals(CoreUpdateOutcome.FAILURE, performCoreUpdateAttempt(0, {}) { throw error })
            }
        }

    @Test
    fun unexpectedApplicationFailureIsNotReportedAsSuccessOrRetried() =
        runBlocking {
            assertEquals(
                CoreUpdateOutcome.FAILURE,
                performCoreUpdateAttempt(0, {}) { throw IllegalStateException("Missing core configuration") },
            )
        }

    @Test
    fun aLaterSuccessfulAttemptIsSuccessful() =
        runBlocking {
            assertEquals(CoreUpdateOutcome.RETRY, performCoreUpdateAttempt(0, {}) { throw IOException() })
            assertEquals(CoreUpdateOutcome.SUCCESS, performCoreUpdateAttempt(1, {}) {})
        }

    @Test
    fun cancellationIsRethrownWithoutLoggingFailure() =
        runBlocking {
            val cancellation = CancellationException("Stopped")
            var logged = false
            val thrown = thrownBy { performCoreUpdateAttempt(0, { logged = true }) { throw cancellation } }
            assertSame(cancellation, thrown)
            assertFalse(logged)
        }

    @Test
    fun cancellationDuringANonSuspendingDownloadCannotReturnSuccess() =
        runBlocking {
            verifyCancellationWins { currentCoroutineContext().cancel() }
        }

    @Test
    fun cancellationWinsOverConcurrentIoFailure() =
        runBlocking {
            verifyCancellationWins {
                currentCoroutineContext().cancel()
                throw IOException("Connection closed after stop")
            }
        }

    @Test
    fun fatalErrorsAreNotSwallowed() =
        runBlocking {
            val error = AssertionError("Broken invariant")
            assertSame(error, thrownBy { performCoreUpdateAttempt(0, {}) { throw error } })
        }

    private suspend fun verifyCancellationWins(update: suspend () -> Unit) {
        var returned = false
        var logged = false
        val thrown =
            thrownBy {
                withContext(Job()) {
                    performCoreUpdateAttempt(0, { logged = true }, update)
                    returned = true
                }
            }
        assertTrue(thrown is CancellationException)
        assertFalse(returned)
        assertFalse(logged)
    }

    private suspend fun thrownBy(block: suspend () -> Unit): Throwable {
        try {
            block()
        } catch (error: Throwable) {
            return error
        }
        throw AssertionError("Expected an exception")
    }
}
