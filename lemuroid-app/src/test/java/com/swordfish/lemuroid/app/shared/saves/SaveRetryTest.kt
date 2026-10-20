package com.swordfish.lemuroid.app.shared.saves

import com.swordfish.lemuroid.common.kotlin.runCatchingWithRetry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SaveRetryTest {
    @Test
    fun cancellationEscapesWithoutRetryingAWrite() =
        runBlocking {
            val cancellation = CancellationException("Leaving game")
            var writes = 0
            try {
                runCatchingWithRetry(3) {
                    writes++
                    throw cancellation
                }
                fail("Cancellation must not become an ordinary failed Result")
            } catch (actual: CancellationException) {
                assertSame(cancellation, actual)
                assertEquals(1, writes)
            }
        }

    @Test
    fun exhaustedDiskFailureIsRetainedForTheSaveCaller() =
        runBlocking {
            val diskFailure = IOException("No storage available")
            var writes = 0
            val result =
                runCatchingWithRetry(3) {
                    writes++
                    throw diskFailure
                }
            assertSame(diskFailure, result.exceptionOrNull())
            assertEquals(3, writes)
        }

    @Test
    fun transientFailureStopsAfterTheFirstSuccessfulCommit() =
        runBlocking {
            var writes = 0
            val result =
                runCatchingWithRetry(3) {
                    writes++
                    if (writes == 1) throw IOException("Temporary failure")
                    "committed"
                }
            assertEquals("committed", result.getOrThrow())
            assertEquals(2, writes)
        }

    @Test
    fun fatalErrorsAreNotRetriedAsDiskFailures() =
        runBlocking {
            val fatal = AssertionError("Programming error")
            var writes = 0
            try {
                runCatchingWithRetry(3) {
                    writes++
                    throw fatal
                }
                fail("Fatal errors must escape")
            } catch (actual: AssertionError) {
                assertSame(fatal, actual)
                assertEquals(1, writes)
            }
        }
}
