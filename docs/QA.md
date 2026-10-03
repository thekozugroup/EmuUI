# EmuUI verification record

## Version 0.1.1 candidate

Updated 3 October 2026 at 20:58 UTC. All eight Android 29 instrumentation tests passed against the native-tested APK identified below. Android 35 startup remains blocked, and an earlier-autosave metadata observation remains unresolved. Version 0.1.0 results below remain historical evidence and are not automatically attributed to 0.1.1.

- Candidate version: `0.1.1-DEBUG`, version code 2, package `com.thekozugroup.emuui.debug`
- Native-tested APK SHA256: `a4eae85730f87370e4ce802a9610fc0b547dd9b765f190ed06ce01b00660319d`
- PASS: aggregate build in 1 minute 18 seconds; all 87 JVM tests passed with zero failures, errors or skips; Kotlin style check passed; lint reports 0 errors, 34 warnings and 1 informational item
- PASS: debug APK signature verifies under v1 and v2
- BLOCKED: Android 35 produced no usable Activity after two cold launcher attempts. Warm return, import and native gameplay were not reached
- PASS: all 7 Android 29 UI tests in 152.892 seconds, plus the separate native game test in 80.367 seconds. Two real ActivityScenario recreations retained the ViewModel and recreated the Activity/native view; both full DS display regions were pixel-identical across all three screenshots
- PASS: one real WorkManager request reached RETRY, RETRY, then terminal FAILURE under the existing DNS failure in 270.1 seconds. Restoring the same SHA256-verified core bytes allowed a later request to succeed. This validates bounded failure handling and cache recovery, not successful guest network acquisition
- PASS: native DS rendering, A/B input, touch mapping and new autosave creation. The fresh saved tuple was state 28, frozen frame 1382, X63/Y143 and touch count 1; new autosave metadata was version 2
- OBSERVED: an earlier autosave load emitted IncompatibleStateException before fresh 0.1.1 state creation. Its old metadata could not be recovered from the preserved backup. This is an unresolved metadata-read observation, not demonstrated cross-version incompatibility. Fresh 0.1.1 state and recreation checks passed

Android 35 used software CPU emulation, SwiftShader, 4 vCPUs and 2 GB RAM, with notifications ungranted. The first process-start ANR sampled Android NetworkSecurityConfigProvider before Application.onCreate; the retry sampled runnable coroutine-class initialization in BackgroundWork; an automatic service restart sampled ART dex verification. No WorkManager lock wait or submission-queue deadlock was identified in these traces. Parallel system-process ANRs occurred. The traces establish blocked startup, not an exclusive app or environment cause. Details are in `qa/v0.1.1/runtime-status.json` and its referenced ANR logs.

The changes use supported lazy WorkManager configuration with ordered off-main scheduling and observation; bounded broadcast completion and aborted-launch recovery; bounded download retry/failure/cancellation; and complete staged ZIP validation, including entry sizes and CRCs, before replacing PSP assets. The candidate-specific runtime results above supplement the unit coverage; no Android 35 pass is claimed. No EmuUI ANR or fatal exception was observed in the Android 29 candidate run.

Test composition: 36 prior geometry/library/state checks, 15 queue and bounded-submission checks, 30 core-download/update checks, and 6 launch-recovery/cache-policy checks. The archive tests cover malformed, empty, truncated, traversal, wrong-size and wrong-CRC inputs while preserving an existing valid cache.

### Packaging verification and artifact scope

The source rebuild reconstructs the unchanged SQLite metadata database from seven small gzip parts. Compressed and uncompressed integrity pins match the original; the database inside the rebuilt APK is byte-identical. No archive parts or temporary files are packaged. Five Python archive checks and actual Gradle positive reconstruction, cache reuse, corrupt-input rejection, size-limit rejection and restoration checks passed. The conservative full rebuild also passed 87 JVM tests, app/metadata Kotlin style checks and lint with 0 errors, 34 warnings and 1 informational item.

That packaging rebuild produced APK SHA256 `9cdf95468de30b6846260a2544e3f57bfbc67f40bd7e4542e16c4bb3473cf2d2`. Some DEX/signature entries differ despite unchanged application source. Native Android claims apply only to `a4eae857…`, not to `9cdf9546…` or a future CI artifact. A CI APK is a standard rebuild from the tested source; it must be labeled accordingly and must not be presented as the exact emulator-tested binary.

CI uses a generated debug signing key. Its certificate can differ from the local APK or another CI run, so Android may reject an in-place update. Back up saves before considering an uninstall, which can erase app data. No production signing identity is provided.

Candidate evidence is stored separately under `qa/v0.1.1`; final screenshots and report will identify this candidate's exact hash. The delivered 0.1.0 artifacts are unchanged. The local verification baseline is commit `f44f840cd9ab13ffd69888d2967dfe8f661c5029`, local tag `preview-0.1.0-tested`, source tree `7c7f072a121303f61b38e47156b81a2282494183`; these are not claims of a public GitHub tag. The final source hygiene scan passed 752 indexed entries. This record documents local checks; the public repository and its Actions runs provide the separately verifiable publication and CI status.

## Delivered version 0.1.0 record

Updated 3 October 2026 at 18:52 UTC. This is a development preview, with results scoped to the exact APK and environment below. PASS means an executed check met its stated assertions. PARTIAL, PENDING, BLOCKED and NOT RUN do not imply passed coverage.

Results are scoped to these artifacts. Earlier runtime checks must not be silently attributed to a newer binary.

- Latest accessibility-fix candidate: `c299d343d5791e0d4a660c15aabcf414a3d31462a7497c0a2b7ff4e926451414`. Aggregate build, 36 unit tests, lint and Kotlin style checks passed. Large-font layout passed on a fresh Android 29 run at 150% and 200%. A separate startup ANR blocked the Android 35 rerun; all eight final Android 29 instrumentation tests passed, including native game double recreation
- Android 35 initial observation build: `22be571992d78510942eecf24dc1dce0e4e97a72969116b43e0ad1697778cd41`. Native home rendered; 150% font clipping and a focus-input ANR were observed
- Android 29 DS runtime-tested build: `951c781847857a1cf7b60f783fd276d27dbc39cd838caf3c3f86610d8a1dc66e`
- Android 29 library and NES runtime-tested build: `8f8c5b1555576053ac3f895469abc118b7828ab74b969843650755fc6bda304b`

### Build and source checks

| Check | Result | Scope |
| --- | --- | --- |
| Native app and instrumentation APKs | PASS on latest candidate | `freeDynamicDebug`, package `com.thekozugroup.emuui.debug`, version `0.1.0-DEBUG` |
| Latest candidate unit tests | PASS | 36 JUnit tests, zero failures/errors/skips: 14 fold/touch geometry, 7 library selection/filter, 5 recreation checkpoint, 8 scan status and 2 Nintendo DS default settings. Geometry includes a 231-case hinge sweep |
| Lint and Kotlin style | PASS on latest candidate | 0 lint errors, 34 warnings, 1 informational item; `ktlintCheck` passed. This is not a warning-free build |
| Material 3 Expressive integration | COMPILED and native home observed | Official theme, motion scheme, animated button shapes and progress components; experimental `1.4.0-alpha18` pin documented in [MATERIAL3_EXPRESSIVE.md](MATERIAL3_EXPRESSIVE.md) |
| APK signature and notices | PASS on latest candidate | Debug APK verifies with v1 and v2 signatures; GPL-3.0, Apache-2.0 and NOTICE texts are included. No emulator-core `.so` libraries are bundled. Production signing is not configured |
| Publication asset scan | PASS | The latest scan checked 728 indexed entries; rerun after any further publication changes. Shared signing key, commercial-game promotional screenshots, ROMs and BIOS files excluded. This limited guard is not a complete secret or licensing audit |

Build environment: Linux x86_64, JDK 17, Gradle 8.10.2, Android Gradle Plugin 8.7.1, Kotlin 2.0.21, compile/target SDK 35 and Build Tools 34.0.0. See [README.md](../README.md) for build commands and [PROVENANCE.md](PROVENANCE.md) for source and licensing details.

### Observed emulator runtime results

Emulator 37.2.12 used software CPU emulation (`-accel off`) and SwiftShader at 1280 by 720 pixels, 240 dpi. `/dev/kvm` was unavailable. This environment cannot establish real-device performance, audio quality, thermals or OEM hinge behavior. Android 35 initial home/font observations use APK `22be571992d78510942eecf24dc1dce0e4e97a72969116b43e0ad1697778cd41`; the patched startup failures use `c299d343d5791e0d4a660c15aabcf414a3d31462a7497c0a2b7ff4e926451414`. These are separate from the Android 29 checks.

| Check | Result | Observed evidence |
| --- | --- | --- |
| Real folder import and recovery | PASS within scope | Storage Access Framework picker granted an empty folder, then a valid folder. The valid folder indexed two original NES fixtures and one original DS fixture. The empty folder showed generic welcome guidance; specific no-supported-files messaging and revoked-access recovery remain unverified |
| Final-candidate library regression | PASS on latest candidate | Real folder import produced populated entries on `c299d3…`. Long-title ellipsis, empty and populated library layouts, and 150%/200% font scale were rechecked. This is a manual affected-flow regression, separate from the seven earlier instrumented UI checks |
| Final-candidate UI instrumentation | PASS on latest candidate | All 7 tests passed in 178.744 seconds: launch, filter recreation, folder-picker cancellation with URI/read/write-grant preservation, portrait guidance and return, injected vertical hinge, off-centre horizontal hinge, and non-default imported selection recreation. The picker test was corrected to dismiss a remembered nested folder before this successful rerun; the app APK remained unchanged |
| Native NES rendering | PASS | Original EmuUI QA cartridge rendered through `fceumm`; animated frames were observed |
| Native NES touch controls | PASS | All four D-pad directions changed the visible player position. Holding the on-screen A button changed the player from green to red |
| Native NES manual save and load | PASS | The app saved a state, moved the player, then loaded the identical saved player bounds. A 669-byte slot state and metadata were recorded |
| Native NES force-stop and restore | PARTIAL | Home, saved-state verification, full-package force-stop, relaunch and Play restored matching player bounds with a changed process ID. The saved position was near the initial position, so this is qualified evidence. Native retained-ViewModel recreation is not yet established |
| Nintendo DS dual-screen rendering | PASS on 951c78… | APK `951c78…` rendered two distinct live displays with the legacy-toolchain original fixture, normal JIT and the exact pinned melonDS core. The initial capture shows top state 17/frame 207 and lower coordinates 128/112. Earlier white boots were isolated to newer fixture-runtime compatibility, not an app or JIT defect. Built-in firmware and FreeBIOS fallback succeeded; no external BIOS was downloaded |
| Nintendo DS controls and touch | PASS on 951c78… | A/B changed numerical state. Exact lower-screen touch mapping was observed with controls outside the touch display. The recorded saved target was state 29, frozen frame 1990, X63/Y143, touch count 1; mutation changed it to state 39, X204/Y61, touch count 2. On final `c299d3…`, dual-screen rendering, lower touch mapping and rejection of upper-screen touch were rechecked |
| Nintendo DS save and load | PASS on 951c78… | After mutation, loading restored the exact tuple: state 29, frozen frame 1990, X63/Y143, touch count 1 |
| Nintendo DS process restoration | PASS on 951c78… | Home produced an 81,913-byte autosave. Full-package force-stop and relaunch restored the complete saved tuple; process ID changed from 4131 to 4562. Both full DS display regions were pixel-identical to the saved baseline after load and process relaunch, while the mutation differed |
| Final Nintendo DS active-game resize | PASS on latest candidate | A real window resize from 1280 by 720 to 1440 by 1000 preserved state 29, frozen frame 2427, X63/Y143 and touch count 1 while both displays and controls repositioned. Touch at the resized display mapped to X192/Y40, and restoring the original dimensions preserved that tuple. This is configuration-resize coverage, not a physical hinge test |
| Native GameActivity recreation | PASS on latest candidate | One instrumentation test passed in 92.856 seconds, executing two real `ActivityScenario.recreate()` calls in the game process. Both cycles retained the ViewModel, created a new Activity and native view, and resumed frames with serialization available. Before and after both recreations, state 29, frozen frame 2427, X192/Y40 and touch count 2 were preserved. Exact top and bottom display regions were pixel-identical to the saved baseline |
| Core acquisition over guest network | BLOCKED | Guest DNS failed. Exact dynamic release `1.17.0` core bytes were downloaded separately and seeded locally, with host/device SHA256 equality verified. Native on-demand download success is not established |
| Android 35 first launch | OBSERVED | The native home rendered. Full Android 35 acceptance remains blocked by the later startup failures |
| Android 35 large text | FAILED in initial run | At 150% font scale, the hero and onboarding content clipped vertically. The `c299d3…` fix passed 150% and 200% font checks on Android 29; the Android 35 rerun was blocked by startup failures |
| Android 29 patched large text | PASS on latest candidate | At 150% and 200% font scale, the primary Add action stayed visible, the hero scrolled independently, and full onboarding cards plus Choose folder were reachable. No app ANR was reported during these final Android 29 font transitions. This does not establish an Android 35 pass |
| Labeled accessible controls | PASS within spot-check scope | On final `c299d3…`, the labeled control layout opened and its A button changed DS state 29 to 30 while the other frozen tuple fields stayed unchanged. This verifies the labeled control action, not TalkBack or full assistive-technology support |
| Android 35 responsiveness | OBSERVED ISSUE | During the font-scale transition the app recorded a focus-input ANR at 5010 ms with guest CPU around 89%. The sampled app main-thread stack was in `androidx.graphics.shapes.Morph` matching/`AngleMeasurer`, reached from Material 3 `LoadingIndicator.morphSequence` in `LibraryPane`. This is an observed hot path, not proof of an exclusive cause. The replacement indicator is compiled in `c299d3…`. The Android 35 retry hit the distinct startup failure below, so it could not complete the font-transition retest. That retry also increased the software emulator from 2 to 4 vCPUs while keeping 2 GB RAM, so it is not an isolated causal comparison. Real-device reproducibility is not established |
| Android 35 patched startup | BLOCKED | On `c299d3…` with 4 vCPUs and 2 GB RAM, two app launches failed startup. The sampled main thread was in `Room.databaseBuilder` through `WorkDatabase.create`, `WorkManager.initialize` and `LemuroidApplication.onCreate`. Parallel SystemUI, launcher and phone ANRs also occurred. The earlier indicator replacement did not remove all observed app timeouts. Logs are preserved; no final Android 35 suite pass is claimed |

The seven final UI tests plus the separate native game-recreation test total eight passing Android tests on `c299d3…`. No EmuUI ANR or fatal exception was observed in those final Android 29 runs. The fold tests use synthetic WindowManager events on an ordinary emulator; they verify layout and library state reactions, not a physical hinge sensor, OEM extension, or an active game core surviving a physical fold.

### Original lawful test fixtures

- **NES EmuUI QA:** original CC0 source and local cartridge generator in `qa/homebrew`. A JSNES 2.1.0 preflight independently validated the fixture's rendering, animation, input and save-state behavior. This preflight is separate from the Android results above
- **Nintendo DS EmuUI DS QA:** original CC0 source in `qa/homebrew/ds`, with linked components retaining their respective licenses. Distinct displays, a frame timer, numerical button state and touch-coordinate markers are intended for stronger native assertions. Native dual-screen rendering, A/B input and touch mapping passed with the legacy-toolchain fixture; save/load and full-package force-stop restoration passed; actual configuration resize passed on the final candidate; dedicated GameActivity double destruction/recreation passed on the final candidate
- Generated ROMs, commercial games, BIOS files and user saves are not included in the published source

### Remaining acceptance checks

- Investigate and rerun both Android 35 app ANR scenarios, preferably with hardware acceleration or a real device; high software-emulation load does not prove the app is unaffected
- Verify read-only or invalid folders, permission revocation and recovery; distinguish an empty supported library from an import failure
- Test reduced motion, full TalkBack navigation and gameplay, safe insets and a physical external gamepad
- Verify active-game portrait/unsupported-posture guidance and physical foldable/OEM hinge behavior, including half-open, flat, reversed landscape, vertical hinge and insufficient-window states
- Characterize audio, sustained performance, thermals and long sessions on real hardware
- At the 0.1.0 snapshot, implementation source was provided as an archive and the public repository contained only its initial README. Later publication and CI status are tracked separately by the public repository

`ClickableViewAccessibility` warnings remain on native touch surfaces. The accessible control grid has labels and hold/release handling, but emulated game pixels and stylus input do not expose a semantic accessibility tree. Full TalkBack gameplay is not certified. No Pixel 11 Pro Fold or other particular physical device is certified.

### Evidence index

The delivered evidence archive contains a selected subset of the validation records below; generated files are not all included in the source repository.

Build records: `build-final-distribution.log` for the library/NES-tested APK, `build-release-candidate.log` for the DS-tested APK, `build-final-layout.log` for the initial Android 35 build, and `build-accessible-final.log` for the latest accessibility-fix candidate. JUnit XML and lint reports are under `lemuroid-app/build/test-results` and `lemuroid-app/build/reports` in a built checkout.

Final test logs: `qa/logs/api29-final-default-seven-retry.txt` and `qa/logs/api29-final-game-recreation.txt`. Final recreation pixel comparison: `qa/evidence/api29-final-recreation-comparison.json`. Runtime evidence bundle: `qa/evidence/api29-runtime-progress.json`, `qa/evidence/api29-final-runtime-progress.json`, `qa/evidence/api35-runtime-progress.json`, `qa/logs/api29-distribution-instrumentation.txt`, `qa/logs/api29-seeded-selection.txt`, core checksums, native home/import screenshots, `qa/logs/api35-app-anr-traces.txt`, `qa/logs/api35-patched-app-anr-traces-final.txt`, `qa/logs/api35-patched-logcat.txt`, the Android 35 normal and 150% font screenshots, and the NES input/save-load screenshots, and DS tuple images (`ds-state-mutated.png`, `ds-state-loaded-settled.png`, `ds-after-process-relaunch.png`) with `ds-process-restore.txt` and `ds-frame-comparison.json`. Generated logs and screenshots are validation artifacts rather than source code. Consult the delivered evidence bundle for these files; the Git repository need not include generated artifacts.
