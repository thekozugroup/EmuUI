# WorkManager startup diagnosis and candidate fix

## Observed failure

The API 35 native trace `api35-patched-app-anr-traces-final.txt` samples the app main thread in `Room.databaseBuilder -> WorkDatabase.create -> WorkManagerImpl.initialize -> WorkManagerInitializer.create -> AppInitializer.initializeComponent -> LemuroidApplication.onCreate` (the first relevant stack begins around line 1904; a second sample also remains inside initialization). The software emulator was heavily stressed, but this is still a synchronous application-owned startup path and warrants a code correction. The delivered, tested 0.1.0 preview is preserved at tag `preview-0.1.0-tested`; these changes are a separate candidate.

## Changes

- `LemuroidApplication` implements the supported `Configuration.Provider` on-demand API. Configuration stays equivalent to the old default initializer. The manifest already removes the App Startup provider, and `MainProcessInitializer` no longer declares `WorkManagerInitializer` as a dependency.
- Startup still requests automatic save sync and core update. App-owned enqueue/cancel commands enter one process-owned FIFO queue on a background-priority executor. The queue waits for each WorkManager `Operation.result` before advancing. This is submission persistence, not worker execution completion.
- All app-owned `WorkManager.getInstance(Context)` calls are confined to `BackgroundWork`: commands run off-main; WorkInfo flows create/acquire their manager on IO. A queue barrier prevents initial observation from overtaking already-submitted startup commands.
- Unique names, worker inputs, constraints and delays are unchanged. The cache cleanup chain now uses `APPEND_OR_REPLACE`: gameplay intentionally cancels that chain, and plain `APPEND` would make recovery inherit CANCELLED. Active cleanup still appends; cancelled/failed prerequisites are replaced. Other enqueue/cancel policies are unchanged. Accepted commands survive cancellation of an observer or Activity coroutine. Failures are logged and returned through the completion handle; one failed command does not strand the rest.
- Folder selection retains the submission state in a ViewModel across Activity recreation and finishes only when the scan has been persisted. A process-restored pending selection safely requests another scan rather than dropping the import. Scan submission failure is visible. Cancellation still finishes without requesting a scan.
- Cancellation broadcasts use `goAsync()` with an eight-second bounded wait and `finish()` in `finally`. The accepted command remains queued after that observation deadline. Before game launch, all background-work cancellation commands are accepted, then awaited off-main; ordinary preparation errors are reported to the user and prevent launch, while coroutine cancellation is rethrown. If preparation cancels background work but no Activity is launched (including destroyed origins), a finally-path resubmits normal background scheduling after all accepted cancellations. Library indexing waits for its follow-on core-update submission before the worker returns.

This avoids a partial fix where a background initializer races an eager main-thread `getInstance()` in a ViewModel. It does not change emulation, ROM storage, save formats, core configuration, or WorkManager versions.

## Evidence and tests

`SerialSubmissionQueueTest` has 10 tests covering deferred/nonblocking submission, FIFO execution with a four-thread executor, held initialization, barriers, reentrant submission, exceptional completion, failure-reporter errors, cancelled observers, and executor rejection/recovery. `BoundedSubmissionTest` adds five tests for success, failure, timeout without command cancellation, rethrown coroutine cancellation, and reporter failure, with exactly-once finishing. `GameLaunchRecoveryTest` adds five tests for successful launch, destroyed origin, partial cancellation failure, Activity-start failure, and coroutine cancellation. A cache-policy regression verifies `APPEND_OR_REPLACE` is selected for recovery. All twenty-one passed using the standalone Kotlin compiler/JUnit runner with JVM assertions enabled. Coroutine-error assertions allow supported stacktrace-recovery copies while requiring the original exception in the cause chain. These are queue/observer tests, not native Android integration tests.

Focused Kotlin formatting and whitespace checks are run before the coordinated Android build. Native cold-start, real SAF import/cancel/recreate, scan cancellation, gameplay and restore results must be recorded for the new candidate. Do not carry over the 0.1.0 runtime pass to changed startup code.

## Remaining limits

On-demand initialization is synchronous in whichever caller first asks for WorkManager. This change routes application-owned UI/initialization calls off-main, but framework-owned paths such as WorkManager's JobService may still initialize it when the OS starts the process for a job. Other Application/Dagger/Room startup work and emulator-wide resource starvation can also cause delays. There is no claim that all possible API 35 ANRs are eliminated before native validation.

Submission commands are in memory until WorkManager persists them. This is why import Activities wait for persistence and receivers retain their asynchronous result. Process termination during that window is not equivalent to a durable scheduled job; restored import state resubmits safely. After a receiver timeout, completion of its accepted command is not guaranteed if Android terminates the process before WorkManager persists it. A running worker is not guaranteed to stop immediately merely because its cancellation operation completed.

## Supported API references

- [WorkManager on-demand initialization](https://developer.android.com/develop/background-work/background-tasks/persistent/configuration/custom-configuration)
- [Configuration.Provider](https://developer.android.com/reference/androidx/work/Configuration.Provider)
- [WorkManager 2.9.0 source artifact](https://dl.google.com/dl/android/maven2/androidx/work/work-runtime/2.9.0/work-runtime-2.9.0-sources.jar)

The exact 2.9.0 source shows `WorkManagerImpl.getInstance(Context)` checking `Configuration.Provider` and synchronizing initialization. No manual `initialize()` race or unsupported hidden API is introduced.

- [BroadcastReceiver.goAsync lifetime and deadlines](https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync())
