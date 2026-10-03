package com.swordfish.lemuroid.app.shared.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BoundedSubmissionTest {
    @Test
    fun successFinishesOnce() =
        runBlocking {
            val command = CompletableDeferred(Unit)
            var finished = 0
            finishSubmissionWithin(command, 1_000, { error("timed out") }, { throw it }, { finished++ })
            assertEquals(1, finished)
        }

    @Test
    fun failedSubmissionReportsAndFinishesOnce() =
        runBlocking {
            val failure = IllegalStateException("database unavailable")
            val command = CompletableDeferred<Unit>().apply { completeExceptionally(failure) }
            var observed: Exception? = null
            var finished = 0
            finishSubmissionWithin(command, 1_000, { error("timed out") }, { observed = it }, { finished++ })
            // await() can recover a copied stack trace when JVM assertions enable coroutine debug.
            assertEquals(failure.javaClass, observed?.javaClass)
            assertEquals(failure.message, observed?.message)
            assertTrue(generateSequence<Throwable>(observed) { it.cause }.any { it === failure })
            assertEquals(1, finished)
        }

    @Test
    fun deadlineFinishesWithoutCancellingAcceptedCommand() =
        runBlocking {
            val command = CompletableDeferred<Unit>()
            var timedOut = 0
            var finished = 0
            finishSubmissionWithin(command, 1, { timedOut++ }, { throw it }, { finished++ })
            assertEquals(1, timedOut)
            assertEquals(1, finished)
            assertFalse(command.isCompleted)
            assertFalse(command.isCancelled)
            assertTrue(command.complete(Unit))
            command.await()
        }

    @Test
    fun coroutineCancellationIsRethrownAfterFinishing() =
        runBlocking {
            val command = CompletableDeferred<Unit>().apply { cancel() }
            var finished = 0
            val failure =
                runCatching {
                    finishSubmissionWithin(
                        command,
                        1_000,
                        { error("timed out") },
                        { error("swallowed cancellation") },
                        { finished++ },
                    )
                }.exceptionOrNull()
            assertTrue(failure is CancellationException)
            assertEquals(1, finished)
        }

    @Test
    fun failingErrorReporterStillFinishes() =
        runBlocking {
            val command = CompletableDeferred<Unit>().apply { completeExceptionally(IllegalStateException("failed")) }
            val reportFailure = IllegalArgumentException("reporter failed")
            var finished = 0
            val failure =
                runCatching {
                    finishSubmissionWithin(
                        command,
                        1_000,
                        { error("timed out") },
                        { throw reportFailure },
                        { finished++ },
                    )
                }.exceptionOrNull()
            assertSame(reportFailure, failure)
            assertEquals(1, finished)
        }
}
