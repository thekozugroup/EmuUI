# Native GameActivity recreation test

This is an optional, seeded, debug-only instrumentation test. It changes no production manifest, Activity, service, preference, or core default. Android 8.0 / API 26 or later is required. Use a disposable QA emulator containing an already imported lawful fixture and its installed core.

## Why a separate test APK

`GameActivity` is declared in the actual `com.thekozugroup.emuui.debug:game` process. An `ActivityScenario` in the normal application process cannot obtain or recreate that Activity. Android's supported `instrumentation android:targetProcesses` manifest attribute selects the process in which the instrumentation runs.

The `-PemuuiGameTests=true` build property selects `com.swordfish.lemuroid.app.mobile.feature.game.GameProcessTestRunner` and the exact `:game` target process in a dedicated AndroidTest APK. Without that property, the ordinary test APK selects `androidx.test.runner.AndroidJUnitRunner` and the default app process for library/MainActivity tests. AGP rewrites the instrumentation name during manifest processing, so adding a second source-manifest entry is not reliable here. Build and verify the two test APKs separately; they share a package name and installing one replaces the other. Selecting `*` alone would make the application's default process primary and would not make ActivityScenario cross-process.

Verified source behavior for the current dependencies: AndroidJUnitRunner 1.5.2 executes its test request only in the primary instrumentation process. MonitoringInstrumentation 1.6.1 treats the sole explicit target process as primary, or the first listed process when more than one is specified. No Espresso Remote dependency is necessary for this single-process game test. Espresso Remote is useful for cross-process view interactions, but does not turn an ActivityScenario object into a remote Activity reference.

References: [Android instrumentation manifest](https://developer.android.com/guide/topics/manifest/instrumentation-element), [InstrumentationInfo.targetProcesses](https://developer.android.com/reference/android/content/pm/InstrumentationInfo#targetProcesses), [Multiprocess Espresso](https://developer.android.com/training/testing/espresso/multiprocess), [runner 1.5.2 source artifact](https://dl.google.com/dl/android/maven2/androidx/test/runner/1.5.2/runner-1.5.2-sources.jar), [monitor 1.6.1 source artifact](https://dl.google.com/dl/android/maven2/androidx/test/monitor/1.6.1/monitor-1.6.1-sources.jar).

## Prepare and run

1. Build `:lemuroid-app:assembleFreeDynamicDebugAndroidTest -PemuuiGameTests=true` and preserve its output with a distinct filename. Build ordinary MainActivity tests without the property. Install the matching application APK and selected game AndroidTest APK on the QA emulator. The explicit target is the `.debug` application ID; this is intentionally not a release-APK test configuration.
2. Import `EmuUI_DS_Legacy_QA.nds` normally, download its melonDS core through the app, and verify it boots. The title or filename must be unique in Room. No synthetic database row is inserted.
3. Optionally prepare a distinctive frozen fixture state and leave the app normally so its existing autosave can be loaded. The test requests load-save without changing the user's autosave preference.
4. Verify the packaged game test manifest has exactly the custom game runner with targetProcesses `com.thekozugroup.emuui.debug:game`. Separately verify the ordinary build has the default runner with targetProcesses `com.thekozugroup.emuui.debug`. Do not run an APK whose runner and process do not match its purpose. `adb shell pm list instrumentation` confirms the installed runner component.
5. Select the game runner and class explicitly:

```sh
adb shell am instrument -w -r \
  -e class com.swordfish.lemuroid.app.mobile.feature.game.GameActivityRecreationTest \
  -e gameTitle EmuUI_DS_Legacy_QA \
  -e coreName MELONDS \
  com.thekozugroup.emuui.debug.test/com.swordfish.lemuroid.app.mobile.feature.game.GameProcessTestRunner
```

The test deliberately skips under the default runner, so ordinary all-tests runs do not execute a local ActivityScenario against the remote game Activity. Under the game runner, the actual hosting process is asserted, not assumed. Missing/ambiguous fixture rows and unusable cores fail with diagnostics rather than silently skipping.

## Assertions and limits

The test reads the existing Room entity, selects its actual system's core configuration, and launches the existing serializable Intent contract. Before recreation and after each of two `ActivityScenario.recreate()` calls it waits for `GameState.Ready`, three real `GLRetroView.FrameRendered` events, and a nonempty native savestate serialization. It checks `RESUMED`, a new Activity instance, the same retained ViewModel, a new GLRetroView, and the preserved game ID in the Intent.

The test saves real UiAutomation screenshots at all three checkpoints under the app's external `files/qa-recreation/` directory: `00-before.png`, `01-after-recreate.png`, and `02-after-recreate.png`. Pull these after the run and compare the frozen fixture's display regions; do not count native frames alone as evidence that the original tuple survived.

These assertions establish that real Activity recreation does not throw on Ready state, reuse destroyed native views, or stop native frame/state handling. They do not prove that all fixture values are restored pixel-exactly. Keep the separate visible fixture tuple checks (state, frozen frame number, touch position, touch count) and process-death tests in the native QA report.

The live production GameService is used throughout both recreation transitions. Only after all assertions, the test stops that service with the public Context API and waits for the main thread before closing the final scenario. Otherwise, the app's intentional `exitProcess(0)` on final GameActivity destruction would kill its co-located instrumentation before JUnit can report. The normal runner cleanup remains enabled. No Developer Options, ADB settings, hidden APIs, Activity replacements, or production test hooks are used.

A successful source build is not an executed recreation test. Record the actual instrumentation result and logs separately.
