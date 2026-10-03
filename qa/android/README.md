# Android runtime QA

These tests are device tests, not static proofs. Their presence does not mean they have run.

## Scope

Default instrumentation suite contains **6 tests**:

- `MainActivitySmokeTest` (4): launch, filter preservation across actual `ActivityScenario.recreate()`, real folder-picker cancel twice preserving URI/read grants, forced portrait guidance and landscape recovery preserving filter
- `InjectedFoldLayoutTest` (2): off-center horizontal hinge with half-open/flat transitions, vertical hinge guidance and unfolded recovery

WindowManager events are injected with `window-testing`; this is **simulated fold geometry**, not validation of an OEM fold sensor or physical hinge. Tests use English accessibility labels and a disposable landscape emulator.

A separate opt-in `SeededLibraryStateTest` (1) checks non-default selected cartridge preservation after two original QA cartridges have been imported through the real app flow. It is not in the default suite; pass `-e qaGameTitle` with the second title. It fails rather than pretending to pass if the fixture is absent.

## Build and run

Build the app and test APK with the same variant, for example:

```
./gradlew :lemuroid-app:assembleFreeDynamicDebug :lemuroid-app:assembleFreeDynamicDebugAndroidTest
ANDROID_HOME=/path/to/sdk qa/android/run_smoke.sh \
  --apk /path/to/app.apk --test-apk /path/to/androidTest.apk --avd EmuUI_QA_API29
```

Or omit `--avd` and reuse a running emulator in the **same persistent shell/session**. On isolated cloud executors, another shell invocation may not see its emulator/ADB processes. The script waits for `sys.boot_completed=1`, installs without wiping data or granting runtime permissions, stages the original ROM, runs the six tests, and captures screenshots, UI XML, logcat and instrumentation output. It refuses physical-device serials. Build first and stop Gradle before starting a software emulator on memory-limited hosts.

The script does not accept SDK agreements or install SDK packages. Those are prerequisites requiring authorization. Software rendering can be very slow; the default boot deadline is 30 minutes. Passing startup/instrumentation is not a gameplay pass.

## Original-content import and native gameplay

1. Generate `qa/homebrew/build_qa_rom.py` into a local output folder; never commit the resulting `.nes` binary
2. Stage it in `/sdcard/EmuUI-QA`, open **Import games folder**, select that folder in Android's real document picker, and allow the app's read-only folder access
3. Wait for the app's actual indexing work to complete. Do not fabricate Room database rows or label a staged file as an imported game
4. Select the original cartridge and press **Play game**. Verify actual changing emulator frames, then D-pad position and A color changes
5. Save state, move, load state, and visually compare player position. Separately test Home/resume and quit/relaunch
6. Native `GameActivity` uses `:game` and handles configuration changes. A rotation is not proof of Activity recreation. Check `adb shell am help` for a supported recreation command; otherwise a disposable emulator's temporary **Don't keep activities** setting can exercise stop/destroy/restore, restoring its previous value afterward. Use the actual OS switch and verify `onDestroy`/`onCreate` events; writing the global setting alone can leave the activity merely stopped. Do not toggle the Developer Options master setting during an ADB-only session: Android Settings can reset the emulator's existing USB debugging connection. Label this path accurately: it differs from retained-ViewModel live recreation
7. Capture both screenshots and logcat; inspect for native/libretro errors, not only Java exceptions

For a non-default selection regression, generate a second local cartridge from the same original source with a distinct filename and one unused padding-byte difference; record its new hash. Import both normally and use the second title for `SeededLibraryStateTest`.

## Reporting

Report each stage as passed, failed, blocked, or not run. Keep these distinct:

- Source/unit tests
- Android compilation
- Installation and launch
- Real import and library state
- Injected fold geometry
- Actual core rendering, input, save/load, and Activity/session restoration
- Genuine foldable hardware and DS touchscreen/stylus behavior

Neither desktop JSNES cartridge preflight nor a Compose preview counts as Android core gameplay.
