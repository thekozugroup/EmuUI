# Cloud readiness checkpoint — 20 October 2026

**Later cloud build follow-up:** [approved tooling and candidate verification](BUILD_VERIFICATION_20261020.md) supersedes the earlier SDK-approval/build blockers below. Historical results retain their original scope.

**Later follow-up:** [Exact build-input recovery](BUILD_INPUT_RECOVERY_20261020.md) resolves the missing sixteen core inputs, PSP archive and source-archive availability findings below. All seventeen locked payloads now verify locally. SDK agreement approval/installation and Android verification remain pending; the initial handoff artifacts are preserved.

**Implemented and host-tested; Android build and release acceptance remain blocked.** This work is on `codex/play-readiness-20261020`, in a separate cloud checkout. GitHub `main` was `bcda1e6` (0.1.1); the established October 10 preview is tag `v0.3.0-dev.20261010.2-DEBUG` at `cffb0fb79f7e387ef0b429d2e05f35ee077a9a8b`. Only the dedicated working branch was advanced to that source. No main merge, remote push, Play write, release, laptop access or change to Project-Subzero was performed.

The source version is now `0.3.1-dev.20261020`, code 8. There is no APK/AAB for this source in this handoff, and no previous binary is represented as the new candidate.

## Implemented

- Preserved the OLED console, silver identity, Material 3 Expressive components, generous library icons, haptics preferences, shoulder-based filtering, independent DS display regions and open-horizontal-fold requirement. Current documented UI deliberately has no filter-chip row. Ordinary phones and cover displays retain guidance instead of unsupported gameplay.
- Save slots now expose loading, empty, failed-read and retry states; preview-image failure does not hide a usable slot. Guidance scrolls with safe insets and large text. Game loading stays in the upper pane, clear of the hinge. Control adjustments have spoken labels and the control mode exposes its state.
- Atomic write descriptors remain open until `AtomicFile.finishWrite`; gzip finishes its trailer without closing that descriptor early. Save errors reach callers, empty snapshots/invalid slots are rejected, and failed Save-and-close keeps the game available for retry. Existing file formats and user saves are preserved. State and metadata still commit separately; no crash-proof or cross-version DS guarantee is asserted.
- Stale restored core preferences recover to a valid system core while retaining valid choices and the DeSmuME migration default. Unknown shader IDs use the normal default shader. Invalid controller ports cannot index outside the four controller slots.
- Removed the remote `fakeimg.pl` artwork placeholder, using a local icon instead; blank cover titles are safe. Existing HTTPS thumbnail upgrade and cleartext denial remain. Optional Drive, microphone, backup and document-provider behavior are inventoried accurately.
- Added original GB/GBC/GBA homebrew fixture sources and repeatable host tests. The requested systems already had native core integration, so no menu-only support or unrelated core expansion was added. NES is the conservative existing additional top-display candidate. See [core compatibility and licenses](CORE_COMPATIBILITY.md).

## Executed verification

| Check | Result and exact scope |
| --- | --- |
| Kotlin/JUnit host subset | 183 tests, zero failures, 19 classes and 16 actual production source files; Kotlin 2.0.21/JDK 17, no Android/Compose stubs. Includes the four new retry/cancellation checks. This is not the Android Gradle unit suite. |
| Native renderer | 437 geometry/pointer/aspect/shader-source checks and native controller pulse/port tests pass. AddressSanitizer/UBSan checks also pass. |
| Actual shader compilation | Ten shader programs across all seven built-in chains compile/link using Mesa llvmpipe OpenGL ES 3.2; no physical GPU certification. |
| Lawful handheld core preflight | GB/GBC on pinned-source host Gambatte and GBA on pinned-source host mGBA pass load, rendering, input, nonzero audio samples, pause/resume, save-state visual restoration and SRAM disk reload. These are separate host binaries, not the shipped Android libraries. |
| Python fixtures/packaging | Fourteen DS header tests, eight handheld generator tests and five metadata archive tests pass. |
| Style/source hygiene | Changed Kotlin files checked with standalone ktlint 1.2.1; final whitespace and staged-publication scans recorded in the handoff evidence. One existing geometry compiler warning and one native unused-log-callback warning remain. |
| Store assets | Source-derived 512px icon, opaque 1024×500 feature graphic, accurate copy, checksums and alt text validate. Submission-completeness check correctly fails because current native screenshots are missing. |

New Android UI/save-persistence tests are provided but have not been compiled or run here. Current Android GB/GBC/GBA/DS/NES loading, controls, audible audio, background/resume, current DS saves, physical haptics/folds/cutouts, cover L/R, minified release and Play split delivery remain unverified. Historical results in [QA](QA.md) retain their original binary scope.

## Exact blockers and outcomes

| Action / target | Outcome | Reason / next requirement |
| --- | --- | --- |
| Install Android platform 36, build tools 35.0.0, NDK 27.3.13750724, CMake 3.31.5 and emulator tooling in this container | Pending authorization | No accepted SDK agreements or preinstalled Android tools exist. README instructs the owner to review/accept terms; an approval question is pending. No terms were accepted. |
| Build `assembleFreeDynamicDebug`, Android test APK, app unit tests and lint | Blocked, no outputs | After recovering JDK/compiler, proxy and writable Android-user paths, Gradle stops configuring `libretrodroid` with `LicenceNotAcceptedException` for NDK 27.3.13750724. |
| Build `bundlePlayDynamicRelease` | Blocked, no AAB | Same NDK agreement failure. Additionally all sixteen hash-locked `.release-native/arm64-v8a` core inputs and `.release-native/assets/core-assets/ppsspp.zip` are absent from this fresh container. Their earlier handoff archive is not tracked in Git. No unverified replacement was used. |
| Capture verified app screenshots and run the end-to-end Android matrix | Not run, dependent on tooling/build | No runnable Android device/emulator is connected; no `/dev/kvm` is available. No mockup, host-core screenshot or historical app screenshot was substituted. |
| Production signing, Play testing and publication | Not performed | No release signing identity is supplied or created. The task does not authorize merge/release/publication. |

Run `python3 qa/cloud_build_preflight.py --release` for a read-only inventory. Stage the earlier exact native inputs using `qa/stage_source_release_inputs.py` when available; finish a clean corresponding-source rebuild and notices delivery before distribution. Recovering tooling does not by itself resolve native-input/source obligations, physical device acceptance, production OAuth/Drive, publisher privacy URL/contact, Data safety or Play-delivered testing.

## Deliverables

[Play draft pack](../distribution/play/README.md) contains the icon, feature graphic, editable vectors, listing text, [privacy/Data safety inventory](../distribution/play/PRIVACY_DATA_SAFETY.md), policy draft and screenshot capture/provenance plan. It is not submission complete. No copyrighted ROM, proprietary BIOS, signing secret or generated fixture binary is committed.

The local handoff directory contains a Git source bundle, source ZIP, source patch, Play draft ZIP, selected logs/manifests and SHA-256 checksums. Its README identifies the final commit. Those source artifacts remain unbuilt Android review material.
