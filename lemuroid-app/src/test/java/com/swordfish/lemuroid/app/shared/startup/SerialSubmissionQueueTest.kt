package com.swordfish.lemuroid.app.shared.startup

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SerialSubmissionQueueTest {
    @Test
    fun submissionOnlySchedulesOneDrainAndReturnsBeforeAnyWork() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) { throw AssertionError(it) }
        val events = mutableListOf<Int>()
        val first = queue.submit { events += 1 }
        val second = queue.submit { events += 2 }

        assertTrue(events.isEmpty())
        assertFalse(first.isCompleted)
        assertFalse(second.isCompleted)
        assertEquals(1, executor.pendingCount)

        executor.runNext()

        assertEquals(listOf(1, 2), events)
        assertSucceeded(first)
        assertSucceeded(second)
        assertEquals(0, executor.pendingCount)
    }

    @Test
    fun idleQueueSchedulesAnotherDrain() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) {}
        val first = queue.submit {}
        executor.runNext()
        assertSucceeded(first)

        val second = queue.submit {}
        assertFalse(second.isCompleted)
        assertEquals(1, executor.pendingCount)
        executor.runNext()
        assertSucceeded(second)
    }

    @Test
    fun barrierCompletesAfterPredecessorsAndBeforeLaterCommands() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) {}
        val events = mutableListOf<String>()
        val initialization = queue.submit { events += "initialized" }
        val submission = queue.submit { events += "submitted" }
        val barrier = queue.submit {}
        val observation =
            queue.submit {
                assertSucceeded(initialization)
                assertSucceeded(submission)
                assertSucceeded(barrier)
                events += "observed"
            }

        assertFalse(barrier.isCompleted)
        executor.runNext()

        assertEquals(listOf("initialized", "submitted", "observed"), events)
        assertSucceeded(observation)
    }

    @Test(timeout = 15000)
    fun blockingInitializationPreservesOrderWithoutBlockingOtherSubmitters() {
        val workers = Executors.newFixedThreadPool(4)
        val submitter = Executors.newSingleThreadExecutor()
        val initializationStarted = CountDownLatch(1)
        val releaseInitialization = CountDownLatch(1)
        val finished = CountDownLatch(1)
        val active = AtomicInteger()
        val maximumActive = AtomicInteger()
        val events = CopyOnWriteArrayList<Int>()
        val failures = CopyOnWriteArrayList<Throwable>()
        val queue = SerialSubmissionQueue(workers) { failures += it }

        try {
            val initialization =
                queue.submit {
                    maximumActive.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                    initializationStarted.countDown()
                    try {
                        check(releaseInitialization.await(5, TimeUnit.SECONDS))
                        events += 0
                    } finally {
                        active.decrementAndGet()
                    }
                }
            assertTrue(initializationStarted.await(5, TimeUnit.SECONDS))

            // If submit waits for the held initializer, get() fails before it is released.
            val accepted =
                submitter.submit<List<Deferred<Unit>>> {
                    (1..20).map { index ->
                        queue.submit {
                            maximumActive.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                            try {
                                events += index
                            } finally {
                                active.decrementAndGet()
                            }
                        }
                    } + queue.submit { finished.countDown() }
                }.get(2, TimeUnit.SECONDS)

            assertTrue(events.isEmpty())
            assertFalse(initialization.isCompleted)
            assertTrue(accepted.none { it.isCompleted })
            assertEquals(1L, finished.count)

            releaseInitialization.countDown()
            assertTrue(finished.await(5, TimeUnit.SECONDS))
            runBlocking { accepted.last().await() }

            assertEquals((0..20).toList(), events.toList())
            assertEquals(1, maximumActive.get())
            assertTrue(failures.isEmpty())
            assertSucceeded(initialization)
            accepted.forEach(::assertSucceeded)
        } finally {
            releaseInitialization.countDown()
            submitter.shutdownNow()
            workers.shutdownNow()
            assertTrue(submitter.awaitTermination(5, TimeUnit.SECONDS))
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun failedCommandReportsItsFailureAndDoesNotPoisonTheQueue() {
        val executor = ManualExecutor()
        val failures = mutableListOf<Throwable>()
        val queue = SerialSubmissionQueue(executor) { failures += it }
        val failure = IllegalStateException("initialization failed")
        val first = queue.submit { throw failure }
        val later = queue.submit {}

        executor.runNext()

        assertFailedWith(first, failure)
        assertEquals(listOf(failure), failures)
        assertSucceeded(later)
    }

    @Test
    fun throwingFailureReporterDoesNotPreventLaterCommands() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) { error("reporter failed") }
        val failure = IllegalStateException("command failed")
        val first = queue.submit { throw failure }
        val later = queue.submit {}

        executor.runNext()

        assertFailedWith(first, failure)
        assertSucceeded(later)
    }

    @Test
    fun reentrantSubmissionGoesBehindAlreadyAcceptedCommands() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) {}
        val events = mutableListOf<Int>()
        lateinit var nested: Deferred<Unit>
        val first =
            queue.submit {
                events += 1
                nested = queue.submit { events += 3 }
            }
        val second = queue.submit { events += 2 }

        executor.runNext()

        assertEquals(listOf(1, 2, 3), events)
        listOf(first, second, nested).forEach(::assertSucceeded)
        assertEquals(0, executor.pendingCount)
    }

    @Test
    fun cancellingObservationDoesNotRemoveAnAcceptedCommand() {
        val executor = ManualExecutor()
        val queue = SerialSubmissionQueue(executor) {}
        val events = mutableListOf<Int>()
        val cancelled = queue.submit { events += 1 }
        val later = queue.submit { events += 2 }
        val barrier = queue.submit {}

        cancelled.cancel()

        assertTrue(cancelled.isCancelled)
        assertFalse(barrier.isCompleted)
        executor.runNext()

        assertEquals(listOf(1, 2), events)
        assertTrue(cancelled.isCancelled)
        assertSucceeded(later)
        assertSucceeded(barrier)
    }

    @Test
    fun failureOfCancelledCommandIsStillReported() {
        val executor = ManualExecutor()
        val failures = mutableListOf<Throwable>()
        val queue = SerialSubmissionQueue(executor) { failures += it }
        val failure = IllegalStateException("cancelled observer")
        val cancelled = queue.submit { throw failure }
        val later = queue.submit {}
        cancelled.cancel()

        executor.runNext()

        assertEquals(listOf(failure), failures)
        assertSucceeded(later)
    }

    @Test
    fun rejectedDrainFailsAllAcceptedCommandsAndAllowsLaterRetry() {
        val executor = ManualExecutor()
        val failures = mutableListOf<Throwable>()
        val rejection = RejectedExecutionException("executor unavailable")
        var reject = true
        var executed = 0
        lateinit var queue: SerialSubmissionQueue
        lateinit var acceptedDuringDispatch: Deferred<Unit>
        val rejectingExecutor =
            Executor { drain ->
                if (reject) {
                    reject = false
                    // Deterministically model acceptance between scheduling and its rejection.
                    acceptedDuringDispatch = queue.submit { executed++ }
                    throw rejection
                }
                executor.execute(drain)
            }
        queue = SerialSubmissionQueue(rejectingExecutor) { failures += it }

        val rejected = queue.submit { executed++ }

        assertFailedWith(rejected, rejection)
        assertFailedWith(acceptedDuringDispatch, rejection)
        assertEquals(listOf(rejection, rejection), failures)
        assertEquals(0, executed)

        val retry = queue.submit { executed++ }
        assertFalse(retry.isCompleted)
        executor.runNext()
        assertSucceeded(retry)
        assertEquals(1, executed)
    }

    private fun assertSucceeded(result: Deferred<Unit>) {
        assertTrue("Command did not complete", result.isCompleted)
        assertFalse("Command failed or was cancelled", result.isCancelled)
        runBlocking { result.await() }
    }

    private fun assertFailedWith(
        result: Deferred<Unit>,
        expected: Throwable,
    ) {
        assertTrue("Command was left pending", result.isCompleted)
        val actual = runCatching { runBlocking { result.await() } }.exceptionOrNull()
        // Coroutine debug/stacktrace recovery may copy an exception across await().
        assertEquals(expected.javaClass, actual?.javaClass)
        assertEquals(expected.message, actual?.message)
        assertTrue(generateSequence(actual) { it.cause }.any { it === expected })
    }

    private class ManualExecutor : Executor {
        private val pending = ArrayDeque<Runnable>()

        val pendingCount: Int get() = pending.size

        override fun execute(command: Runnable) {
            pending.addLast(command)
        }

        fun runNext() {
            pending.removeFirst().run()
        }
    }
}
