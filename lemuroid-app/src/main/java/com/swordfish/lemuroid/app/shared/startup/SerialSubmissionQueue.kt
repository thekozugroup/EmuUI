package com.swordfish.lemuroid.app.shared.startup

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.ArrayDeque
import java.util.concurrent.Executor

/**
 * Runs accepted commands in submission order, even when [executor] has multiple worker threads.
 *
 * [executor] must dispatch asynchronously: an executor that runs inline cannot keep [submit]
 * nonblocking. Commands may block their worker, but never execute while holding the queue lock.
 * A command must finish its initialization/submission before returning; detached asynchronous work
 * is outside this queue's ordering guarantee. An empty command is a barrier for preceding commands.
 *
 * Cancelling the returned [Deferred] stops observation, not the accepted command. [onFailure] is a
 * best-effort, nonblocking reporter, called on the worker (or the submitter if dispatch is rejected).
 */
class SerialSubmissionQueue(
    private val executor: Executor,
    private val onFailure: (Throwable) -> Unit,
) {
    private val lock = Any()
    private val commands = ArrayDeque<Command>()
    private var draining = false

    fun submit(task: () -> Unit): Deferred<Unit> {
        val command = Command(task, CompletableDeferred())
        val needsDrain =
            synchronized(lock) {
                commands.addLast(command)
                if (draining) {
                    false
                } else {
                    draining = true
                    true
                }
            }

        if (needsDrain) {
            try {
                executor.execute(::drain)
            } catch (failure: Throwable) {
                // Include commands accepted while execute() was trying to dispatch this drain.
                // Reset atomically so a later submission can retry an executor that has recovered.
                val rejected =
                    synchronized(lock) {
                        commands.toList().also {
                            commands.clear()
                            draining = false
                        }
                    }
                rejected.forEach { fail(it, failure) }
            }
        }
        return command.completion
    }

    private fun drain() {
        while (true) {
            val command =
                synchronized(lock) {
                    if (commands.isEmpty()) {
                        draining = false
                        return
                    }
                    commands.removeFirst()
                }

            try {
                command.task()
            } catch (failure: Throwable) {
                fail(command, failure)
                continue
            }

            try {
                command.completion.complete(Unit)
            } catch (failure: Throwable) {
                // A caller's completion handler must not strand subsequent commands either.
                reportFailure(failure)
            }
        }
    }

    private fun fail(
        command: Command,
        failure: Throwable,
    ) {
        try {
            command.completion.completeExceptionally(failure)
        } catch (completionFailure: Throwable) {
            reportFailure(completionFailure)
        }
        reportFailure(failure)
    }

    private fun reportFailure(failure: Throwable) {
        try {
            onFailure(failure)
        } catch (_: Throwable) {
            // Reporting is best effort; a broken reporter cannot poison this queue.
        }
    }

    private data class Command(
        val task: () -> Unit,
        val completion: CompletableDeferred<Unit>,
    )
}
