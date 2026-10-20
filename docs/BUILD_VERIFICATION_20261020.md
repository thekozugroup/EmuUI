# Cloud build verification — 20 October 2026

This supersedes the tooling/build blockers in [the initial checkpoint](CLOUD_READINESS_20261020.md) and [input recovery report](BUILD_INPUT_RECOVERY_20261020.md). Work remains on `codex/play-readiness-20261020` in the isolated EmuUI cloud checkout. Project-Subzero, the laptop, remote branches, main, releases and Play Console were not changed.

## Approved tools and exact inputs

After the explicit 17:47 UTC user approval, the disclosed [Android SDK agreement](https://developer.android.com/studio/terms), SDK Manager ID `android-sdk-license`, was accepted for cloud EmuUI builds/tests. The selected package-manager text is dated 16 January 2019; the official page observed during disclosure is dated 28 April 2026. These texts are not asserted identical. No blanket acceptance of other SDK agreements occurred.

Installed build tools: Android Platform 36 revision 2, Build Tools 35.0.0, NDK r27d `27.3.13750724` and CMake 3.31.5. Platform-tools 37.0.1 was installed as a dependency. Runtime diagnostics use stable Android Emulator 37.2.12 and Android 30 Google APIs x86_64 image revision 16 under the same previously accepted license. JDK 17.0.17+10 and Gradle 8.11.1 are pinned local tools. No production signing identity was created or used.

The recovered sixteen core libraries and PSP ZIP still match the unchanged repository locks. Published corresponding-source archives and their qualifications are preserved with the recovery handoff; matching source availability is not a claim of byte-identical core reconstruction. The original DS test cartridge now builds reproducibly from its existing source: see [DS fixture provenance](DS_FIXTURE_BUILD_20261020.md).

A read-only hygiene scan found two credential-shaped value assignments in a historical PPSSPP generated configure log inside the official source archive. They were not printed or used. The distributable source handoff redacts only those values, with original/new archive and member hashes recorded; all 22,066 other regular PPSSPP file payloads are unchanged. Original archives remain preserved outside the handoff. The sanitized source/input scan covers 58,527 members and has zero unresolved findings within its stated scope. No native lock or functional source version was substituted.

## Implementation and compiled source

Compiled application source: `adb8678e6d6f773fd06937b007eea7286a32fd47`, following `20eadd1` (Android validation fixes and exact notices). The final handoff commit can additionally contain documentation and Play assets without changing these compiled application inputs.

This source preserves the October 10 OLED/two-display/haptics direction and core locks. It includes the earlier safe-area/loading guidance, accessible controls and save-slot states, local artwork fallback, save failure propagation, invalid snapshot protection and core-selection fallback. Final Android compilation also caught and corrected an invalid Compose test import. Save-slot retries now cancel/load on the main dispatcher and perform disk work in IO, preventing a cancelled older load from publishing over a new retry. Repository style checks pass after formatting existing Kotlin violations.

Eight inventoried upstream notice paths were absent because broad ignore rules hid nested `dist`, `bin` and `build` paths. Their exact bytes were restored from the verified official QA APK against the existing inventories: four substantive notices and four empty upstream placeholders. All 236 inventoried notices now match. The publication guard reads the Git index, requires those notices and verifies their hashes before allowing literal upstream `build/LICENSE` paths. Four alternate-index checks confirm that missing/changed notices, generated files and forbidden binaries are still rejected. The upstream RST title underline `=======` is a documented false positive of `git diff --check`; the locked notice was not edited.

## Completed build checks

| Check | Result |
| --- | --- |
| Final bundled debug aggregate | PASS: APK, ordinary instrumentation APK, unit tests, lint and app/metadata Gradle ktlint checks |
| App Android Gradle unit tests | 220 tests, 28 suites, no failures/errors/skips |
| Renderer Android Gradle unit tests | 3 tests, no failures/errors/skips |
| Debug lint | 0 errors/fatals; 46 warnings and 4 hints remain |
| Separate game-process test APK | PASS compilation/package; built with `-PemuuiGameTests=true` |
| Final debug APK package audit | PASS: all 16 locked core hashes, all 236 notices, 18 arm64 libraries, restricted-core exclusions and debug certificate verification |
| Native alignment | LOAD segments in all 18 ELF files satisfy static 16 KB checks; renderer RELRO boundary passes. AndroidX graphics-path retains one RELRO-boundary review flag. Compressed native ZIP entries mean `zipalign -P 16` is not direct-map or 16 KB runtime proof. |
| Prior supporting host tests | 183 Kotlin/JUnit subset tests; native geometry/input/shader and sanitizer checks; original GB/GBC/GBA host-core checks; Python fixture/header/archive checks. These overlap concerns with the Gradle suite and are not added into a single total. |

Final debug APK: `EmuUI-0.3.1-dev.20261020-free-bundle-debug.apk`, 64,362,163 bytes, SHA-256 `3f719e9ee0b83d47bf39f13f18c3e15337c66d40b07bec6541d6bed739c40b63`. It is development-signed, debuggable, arm64-only, version code 8 and package `com.kozudigital.emuui.debug`. Debug keys may differ from previous installations; do not uninstall an installation containing saves without a verified backup. The free flavor differs from the Play flavor: optional Drive sync and the packaged PSP resource ZIP must be checked in the Play bundle.

A universal local-test APK was also derived from this unsigned AAB using the existing cloud Android debug key: `EmuUI-0.3.1-dev.20261020-play-release-local-test.apk`, 49,506,550 bytes, SHA-256 `631663ab134ec9f7d8f5e0805f5afb3ef45945e86b094f9fdfc668b467735cb8`. Signature, ZIP alignment, all sixteen core hashes and the PSP asset hash pass. It is nondebuggable but development-signed, uses the release package name, and is not a production upgrade or Play-delivery result.

Default test APK SHA-256: `e2a2debb4252a7fe577e8921c82ce6f414fa731e28da1ef46e47d34b54f91906`. Game-process test APK SHA-256: `e41138a878abe1f061604506e9857757912b3e6980686d3bd70d7156179102f3`.

The first APK (`9b9a7ade…`) predates restored notices and is kept only as historical evidence. It is not the final candidate.

## Play build and runtime follow-up

The minified Play dynamic release aggregate passed in 11 minutes 28 seconds: bundle, release lint and release-variant unit tests. All 220 release-variant unit tests passed with no failures/errors/skips; they exercise compiled classes rather than an installed minified APK. Release lint has zero errors/fatals, 46 warnings and four hints.

`EmuUI-0.3.1-dev.20261020-play-dynamic-release-unsigned.aab` is 52,114,806 bytes, SHA-256 `eaba8193363d6023fb159a8d483806c8aff972e4bb5a8b405a0a25061aa4c1d3`. It is confirmed **unsigned**, nondebuggable, package `com.kozudigital.emuui`, version code 8, minimum API 23 and target API 36. Official bundletool 1.18.3 validation passes. Its seventeen modules are base plus the sixteen existing cores. All sixteen core hashes, all 236 notices and the 137-file PSP ZIP match the unchanged source locks. Bundle configuration requests `PAGE_ALIGNMENT_16K`; extracted native libraries still require actual 16 KB-device loading tests. Release renderer LOAD/RELRO checks pass; AndroidX graphics-path retains the same review flag. No production signing, signed-track installation or Play delivery is inferred.

The Play dependency's `google.jks` and `google.p12` are hash-matched public CA truststores: each contains seventy trusted certificates and zero private/secret key entries. They are not signing keys. Exact binary manifests confirm no Billing or advertising-ID permission; the privacy inventory still needs publisher/provider/SDK processing review.

Current runtime acceptance remains pending. The first isolated Android guest booted but restarted system services before EmuUI could launch; APK/test-APK installs returned package-service Broken pipe errors. Guest logs also show initial FBE policy errors. A controlled reboot and package-state checks did not restore stable package/storage services. The cloud has no `/dev/kvm`; Android's [emulator documentation](https://developer.android.com/studio/run/emulator-commandline) labels acceleration-disabled software emulation unsupported and slow. These are environment failures, not an observed EmuUI crash. The old AVD/logs are preserved; a second fresh isolated AVD is prepared for a CPU-free retry after builds.

No current Android gameplay, audible audio, physical haptics/folds/cutouts, cover L/R, 16 KB device loading or new DS state compatibility result is inferred from static/host tests. The original DS fixture is silent and cannot verify DS audio or battery SRAM. Save state bytes and metadata still commit separately; existing saves are not migrated by this work.

The Play pack contains source-derived icon/feature graphic, accurate development copy, privacy-policy draft and implemented-behavior Data safety inventory. Current native screenshot capture and final Play delivery/OAuth/privacy-publisher acceptance remain gates. No historical screenshot or mockup is substituted for this APK.
