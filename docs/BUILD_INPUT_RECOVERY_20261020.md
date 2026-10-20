# Exact build-input recovery — 20 October 2026

This follow-up resolves the missing-native-input and unavailable-source-archive findings in [the initial cloud checkpoint](CLOUD_READINESS_20261020.md). Application code remains the implementation committed at `58d7763`; the earlier handoff files are preserved. Android build/runtime acceptance is still blocked by tooling agreements and installation.

## Recovered without changing a hash lock

The authorized GitHub connector found the official [October 10 prerelease](https://github.com/thekozugroup/EmuUI/releases/tag/v0.3.0-dev.20261010.2-DEBUG), despite the shell's GitHub API request returning HTTP 403. Its `EmuUI-QA-arm64-v8a.apk` is 68,939,553 bytes, SHA-256 `c746fddc7dc54040b38b47728074d15dcfb67f310b2d677aa28bfc5e10c63b15`, matching the release asset digest.

- All sixteen extracted core libraries exactly match `qa/source-core-binaries.json`. They are staged in ignored `.release-native/arm64-v8a/`. No older submodule binary or new upstream build was substituted.
- The APK's `assets/core-assets/ppsspp.zip` is 8,889,729 bytes, SHA-256 `3ce10f4c0043c23ae57f7f812b4719233885c2554dfe18cee26cdc7f1353f29b`, exactly matching `qa/source-core-assets.json`. It is staged in ignored `.release-native/assets/core-assets/`. All 137 payloads match both `qa/psp-assets-provenance.json` and the published corresponding-source archive.
- The exact payloads were repacked into a local `Source-built-release-native-inputs-v2.zip`, 36,932,088 bytes, SHA-256 `774adea33f1a1151b40cc64e7f5f443357cffb26d109797ae276e3843a405df8`. This is a newly constructed ZIP container, not a byte-identity claim for the unavailable original handoff ZIP. The existing `qa/stage_source_release_inputs.py` successfully validates all seventeen payloads from it.
- All sixteen core source archives, the PSP resource source archive, frontend source archive and nine rebuild/audit records were downloaded from the same official release. All 27 files match its published sizes and SHA-256 digests. All eighteen tar archives pass a read-only absolute/parent-path check. They were not rebuilt in this container; source availability does not certify source-to-binary reproducibility or legal completeness.
- All sixteen recovered core ELF files pass the existing static 16 KB LOAD-alignment check, with no RELRO-boundary heuristic flags. This is not a new Android load/gameplay test.

The recovery preflight verifies every native/asset lock. Only the JDK/compiler and pinned submodule are available among the build prerequisites; Android platform, build tools, NDK and CMake remain missing.

## Exact tooling approval needed

The failing package is **Android NDK r27d, version `27.3.13750724`**, SDK Manager ID `ndk;27.3.13750724`. Both app and renderer build files pin it. Gradle's APK and AAB attempts stopped with `LicenceNotAcceptedException` during `:libretrodroid` configuration. The official [NDK version/download record](https://github.com/android/ndk/wiki/Unsupported-Downloads#r27d) identifies this version; no upgrade is proposed.

The necessary cloud build installation is:

| SDK Manager package | Purpose | Required license ID |
| --- | --- | --- |
| `platforms;android-36` | Compile SDK | `android-sdk-license` |
| `build-tools;35.0.0` | Packaging/build tools | `android-sdk-license` |
| `ndk;27.3.13750724` | Build and strip the native renderer | `android-sdk-license` |
| `cmake;3.31.5` | Pinned native build system | `android-sdk-license` |

The official human-readable [Android SDK License Agreement](https://developer.android.com/studio/terms) currently shows 28 April 2026. The package-manager text attached to the selected NDK in the official [repository metadata](https://dl.google.com/android/repository/repository2-3.xml) is dated 16 January 2019 and names the same `android-sdk-license` ID. Those texts are not asserted identical. The exact displayed package-manager text is preserved with the recovery evidence for review; no acceptance marker has been written.

Approval must authorize accepting the disclosed SDK agreement and installing/using these tools **in the cloud solely for EmuUI builds/tests**. Platform-tools, the stable Android Emulator and a suitable system image would additionally be required for local Android instrumentation, with their actual presented terms reviewed before acceptance. No blanket acceptance of unrelated preview, TV, Google APIs or third-party SDK licenses is proposed. No purchase, Play Console agreement, production signing or publication is included.

An earlier `request_user_input_async` question asked whether the standard cloud Android tooling terms could be accepted. The UI acknowledged creation, but no response has arrived in this thread. It remains an unanswered user question; there is no separate pending sandbox/auto-review approval, and no automatic rejection caused this blocker. The SDK installer refused missing acceptance. This follow-up does not create a duplicate approval form or accept any agreement.

## Remaining acceptance and preserved outputs

No new APK, AAB, Android test result or current app screenshot was produced. Exact core recovery does not validate this candidate's DS state compatibility, audible audio, lifecycle behavior, haptics, physical folds, production OAuth or Play delivery. State bytes and metadata remain separate commits as documented previously.

The historical October 10 app screenshots and their published checksums were also recovered into a separately labeled local reference directory. They are not substituted for screenshots of this candidate. The completed Play draft pack and its missing-current-screenshot status remain unchanged.

Recovery archives, per-file provenance, source download ledger, official license metadata/text and updated preflight are supplied as a separate handoff supplement. Original source/assets/evidence archives and their checksums remain intact. No app code, core lock, signing material, Project-Subzero checkout, remote branch, release or Play listing was changed by recovery.
