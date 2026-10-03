package com.swordfish.lemuroid.app.shared.startup

import android.content.BroadcastReceiver
import android.content.Context
import android.os.Looper
import android.os.Process
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.Executors

/** Keeps WorkManager construction and submission off the UI thread, in caller order. */
object BackgroundWork {
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val submissions =
        SerialSubmissionQueue(
            Executors.newSingleThreadExecutor { runnable ->
                Thread({
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    runnable.run()
                }, "emuui-work-submissions")
            },
            onFailure = { Timber.e(it, "Unable to submit background work") },
        )

    /** Completion means WorkManager persisted the command, not that the Worker finished. */
    fun submit(
        context: Context,
        command: WorkManager.() -> Operation,
    ): Deferred<Unit> {
        val appContext = context.applicationContext
        return submissions.submit {
            check(Looper.myLooper() != Looper.getMainLooper())
            command(WorkManager.getInstance(appContext)).result.get()
        }
    }

    /**
     * Flow construction is cheap, including inside ViewModel constructors. The barrier prevents
     * an initial empty query overtaking queued startup/import submissions. All initialization and
     * flow acquisition occurs on IO; waiting for the singleton lock never blocks the main thread.
     */
    fun workInfos(
        context: Context,
        uniqueName: String,
    ): Flow<List<WorkInfo>> {
        val appContext = context.applicationContext
        return flow {
            submissions.submit {}.await()
            emitAll(WorkManager.getInstance(appContext).getWorkInfosForUniqueWorkFlow(uniqueName))
        }.flowOn(Dispatchers.IO)
    }

    /** goAsync still has a broadcast deadline; a slow earlier command must not hold it forever. */
    fun finishBroadcastWhenSubmitted(
        pendingResult: BroadcastReceiver.PendingResult,
        submitted: Deferred<Unit>,
    ) {
        receiverScope.launch {
            finishSubmissionWithin(
                submitted = submitted,
                timeoutMillis = 8_000,
                onTimeout = {
                    // The accepted command remains queued even though the receiver must return.
                    Timber.w("Background work submission is still pending after the broadcast wait limit")
                },
                onFailure = { Timber.e(it, "Broadcast work submission failed") },
                finish = { pendingResult.finish() },
            )
        }
    }
}
