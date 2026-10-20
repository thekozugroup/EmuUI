# Core support and compatibility checkpoint — 20 October 2026

Later update: [build-input recovery](BUILD_INPUT_RECOVERY_20261020.md) recovered the exact locked Android core/PSP bytes and published corresponding sources. It supersedes the missing-input/archive finding at the end of this checkpoint, without changing any runtime result or hash lock.

This checkpoint starts from the existing `v0.3.0-dev.20261010.2-DEBUG` source (`cffb0fb`), not the older `main` preview. No new system is advertised merely because it has a library filter or libretro module. The existing two-display DS layout, arm64-only release scope, free/ad-free behavior, restricted-core exclusions, save formats and core revision locks are preserved.

## Requested systems

| System | Existing native path | Firmware requirement | Verification for this cloud change |
| --- | --- | --- | --- |
| Game Boy | Gambatte, `b75225203ffea8b65124bb31acb598e91e7f22d9` | Direct boot; no external boot ROM required by the fixture | Original `.gb` fixture and host libretro checks; see the fixture report below. Current Android APK gameplay remains unrun. |
| Game Boy Color | Same Gambatte core; color-capable header and palette fixture | Direct boot; no external boot ROM required by the fixture | Original `.gbc` fixture and host libretro checks. Current Android APK gameplay remains unrun. |
| Game Boy Advance | mGBA, `c758314a639aa0066e7b65a8341448181b73c804` | Built-in high-level BIOS implementation can direct-boot the fixture; no vendor BIOS supplied | Original `.gba` fixture and host libretro checks. Current Android APK gameplay remains unrun. |
| Nintendo DS | melonDS DS, `33c48260402865ef77667487528efd5ca7ce1233`, with existing API23 patch and pinned melonDS dependency | Existing independently authored FreeBIOS/firmware fallback can run lawful supported homebrew. Some encrypted/commercial images require lawfully obtained native BIOS/firmware. DSi support is not claimed. | Historical scoped native DS results in [QA.md](QA.md) are retained as history. Original DS fixture source remains in `qa/homebrew/ds`; this new Android candidate has not run it. |
| NES, conservative additional top-display candidate | FCEUmm, `5cd4a43e16a7f3cd35628d481c347a0a98cfdfa2` | No BIOS needed for the existing mapper-0 test cartridge | Historical native NES and state-compatibility results exist. Current Android candidate remains unrun. |

For single-display systems, game pixels belong on the upper display and controls on the lower display. DS retains separate upper/lower game displays and lower stylus mapping. No additional core package or BIOS download was added in this change. Other inherited systems remain subject to their existing restrictions and independent compatibility tests; they are not newly certified here. In particular, SNES/Sega/arcade exclusions and the pre-existing 3DS UI limitation must stay out of store support claims.

## What this change fixes

Save files keep their established names, compressed state format and JSON metadata. No existing state is deleted, converted or assigned a guessed version. Raw, UTF-8 and compressed atomic writes now leave the file descriptor open until Android `AtomicFile.finishWrite` can sync and commit it. Gzip still closes its compressor and finishes its trailer. This fixes the prior close-before-sync ordering; it is not a guarantee against every storage failure or sudden power loss.

SRAM read/write errors and exhausted save-state read/write failures reach the caller; unreadable snapshots are not silently treated as absent. Manual save failures show an error, and Save-and-close keeps the game open for a retry. Unsupported/empty core snapshots cannot overwrite a valid manual slot; invalid indices are checked in release builds; zero-length slot files are not presented as available. Quick-load feedback reports an unsuccessful core restore accurately. Cancellation escapes retry/save handling and the game-loader wrappers rather than being retried or wrapped as a storage failure. Optional preview failure is logged separately after the actual state has committed.

A restored core preference pointing to a removed or unrelated core recovers to that system's valid default instead of failing a `first` lookup. Existing valid choices are retained, including DeSmuME selection and the previous pending-DSV migration rule. No save migration is introduced by this fallback.

State bytes and metadata remain two separately atomic files, as before. An interruption between those commits can still leave a mismatched pair; the earlier missing-metadata observation is not declared resolved. Cross-version state compatibility is core-specific. In-game battery saves and emulator snapshots must be tested separately, and save export should precede updates or uninstalling.

## Executed checks and pending Android checks

The original fixture generator, instructions, source pins and machine-readable host evidence are in [qa/homebrew/handheld](../qa/homebrew/handheld/README.md). Generated ROMs and host emulator binaries are local QA outputs, excluded from publication. All artwork and fixture instructions are original; no commercial ROM, proprietary BIOS or vendor boot logo was added.

Four `SaveRetryTest` JVM tests passed against the actual production retry helper using the cloud Gradle distribution's Kotlin compiler and JUnit: cancellation is not retried, final disk errors remain available, a transient failure stops at success, and fatal errors escape. This standalone execution does not compile or exercise Android code. Three additional `CoreSelectionPolicyTest` JVM tests are provided for the normal Gradle suite.

Eight real-filesystem instrumentation methods in `SavePersistenceTest` are provided but **not run in this cloud container**. They use a fresh cache-backed `ContextWrapper`, so they do not read/replace app saves or require a game: raw/gzip/UTF-8 round trips, short legacy file handling, per-core state/metadata manager recreation, SRAM recreation/empty-write preservation, exhausted state/SRAM write failures, invalid/empty state protection, empty-slot reporting and corrupt-state read preservation. After the Android toolchain is available, build the ordinary test runner and run:

```sh
adb shell am instrument -w -r \
  -e class com.swordfish.lemuroid.app.shared.saves.SavePersistenceTest \
  com.kozudigital.emuui.debug.test/androidx.test.runner.AndroidJUnitRunner
```

Loading content, Android input mapping, audible output/quality, lifecycle pause/resume, process-death recovery and persistence must still pass on the exact APK/core hashes. Host libretro output is supporting fixture/core evidence, not a substitute for those app checks or physical foldable testing.

## Licensing and corresponding source

The source locks and expected Android hashes remain in `qa/remaining-core-source-builds.json` and `qa/source-core-binaries.json`. Root/project license families are Gambatte GPLv2, mGBA MPL-2.0, melonDS DS GPLv3 family and FCEUmm GPLv2. Individual notices and third-party terms still apply; this table does not relicense them or assert legal clearance for a binary.

Primary source references: [Gambatte pinned source](https://github.com/libretro/gambatte-libretro/tree/b75225203ffea8b65124bb31acb598e91e7f22d9), [mGBA pinned source](https://github.com/libretro/mgba/tree/c758314a639aa0066e7b65a8341448181b73c804), [melonDS DS pinned source](https://github.com/JesseTG/melonds-ds/tree/33c48260402865ef77667487528efd5ca7ce1233), [FCEUmm pinned source](https://github.com/libretro/libretro-fceumm/tree/5cd4a43e16a7f3cd35628d481c347a0a98cfdfa2). The existing [full source checkpoint](FULL_SOURCE_CORE_CHECKPOINT.md), [source integration instructions](SOURCE_CORE_INTEGRATION.md), and [free release checkpoint](FREE_RELEASE_CHECKPOINT.md) describe patches, source-export obligations and prior evidence.

This fresh container does not contain the previously prepared hash-locked native-input/source archive. Do not silently replace missing release inputs with the older binary submodule or current upstream HEAD. Complete source/dependency/notices delivery and a clean source-export rebuild remain release gates. No source publication, release signing, Play upload or merge is performed by this checkpoint.
