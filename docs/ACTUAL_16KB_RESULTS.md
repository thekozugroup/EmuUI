# Actual Android 16 / 16 KB verification


Current commercial direction (2026-10-05): US$0.99/month, ad-free, with permanent full-access gifts through unique codes. See COMMERCIAL_RELEASE_DECISIONS.md and OWNER_SIGNING_HANDOFF.md. Earlier free-launch proposals are superseded.

The approved official image is installed and tested. **No current arm64 load or tested
NES runtime failure was reproduced.** Do not treat the earlier RELRO endpoint flags as
observed crashes. This report supersedes the earlier statements that 16 KB runtime was
unrun; it does not certify untested games, other ABIs or Play delivery.

## Environment and artifacts

- Approved package only: `system-images;android-36;google_apis_ps16k;arm64-v8a`, revision 7.
- Existing SDK/emulator 37.1.11; isolated workspace AVD `EmuUI_API36_16KB`, serial 5582.
- Actual `adb shell getconf PAGE_SIZE`: **16384**.
- Installed QA and minified release packages each report **pageSizeCompat=0**.
- No compatibility override, security change, physical-device action or extra SDK package.
- No new terms prompt appeared; no `sdkmanager --licenses` acceptance was run.
- Current unsigned AAB unchanged: SHA256
  `431ad0ec8cfbf556aa42b9d34932265997508dc3c53eaae97f6c37e07fd825a7`.
- Current runtime gameplay APK: `playBundleDebug`, package `com.kozudigital.emuui.debug`.
- Bundletool 1.18.0 generated a local universal APK from the production AAB. It used the
  existing November 2025 default debug key, created no key, and did not alter/sign the AAB.
  That APK is QA-only and is not a Play-upload signing artifact.

## Results

| Check | Result | Scope |
| --- | --- | --- |
| Current 20 arm64 cores | PASS | Each actual AAB library dlopen + libretro API/system metadata; no ROM/BIOS/init in this probe. |
| Current renderer + AndroidX graphics.path | PASS | Actual AAB libraries dlopen; standalone probe does not invoke JNI. App tests exercise renderer/JNI separately. |
| Source-built FCEUmm + melonDS DS candidates | PASS | Both dlopen/API metadata on 16 KB; neither substituted into app. Candidate gameplay/save compatibility unrun. |
| Launcher/UI | PASS: 17 distinct tests | Nine successful tests in initial invocation, then eight from corrected class packages. Two initial ClassNotFound invocation errors are retained; they are not product failures. |
| SAF import | PASS | Two original NES fixtures imported through actual picker/read grant. First automatic click occurred during startup scan; after scan finished, a real UI click opened picker. |
| Current FCEUmm native gameplay | PASS | Native frames/input, fold transitions, gates, cutout/system insets and menus. Original lawful NES fixture. |
| Current NES recreation | PASS | Isolated test: two Activity recreations, retained ViewModel/new native view, live frames and nonempty serialization. Not a full disk-save/DS test. |
| Production-bundle generated APK | PASS | Universal APK generation, `zipalign -c -P16`, installation, cold startup of minified release. |
| Release posture guidance | PASS smoke | Actual book-fold emulator shows crease-direction guidance and portrait guidance after rotation. Debug gameplay uses injected horizontal fold; no claim of physical hinge coverage or minified release gameplay. |
| Crash buffer | Empty | No entries during these checks; not a guarantee against untested crashes. |

The original RELRO endpoint criterion flagged 17 arm64 cores and AndroidX. All now pass
actual dlopen on this 16 KB kernel; current FCEUmm additionally passed gameplay and recreation.
Do not integrate replacement cores merely to make the conservative inventory flag disappear.
Existing ABI/library LOAD alignment and release-generated APK ZIP checks remain useful.

The two 32-bit melonDS LOAD alignments are not evidence of a 64-bit 16 KB runtime failure.
Android's Play 16 KB guidance addresses 64-bit devices; those 32-bit observations must not be
misreported as failed arm64 tests. Four zero-byte non-arm64 core placeholders remain a
separate packaging/support issue (Citra armv7/x86/x86_64; PPSSPP x86_64). No ABI or system
was dropped or silently filtered.

## Shortest remaining compliant path

1. Complete corresponding-source/notices obligations for the actual distributed cores,
   using `REMAINING_CORE_SOURCE_AUDIT.md`. Matching hashes are candidate leads, not proof.
   Citra dirty changes, WonderSwan revision, PPSSPP assets/static dependencies and exact
   ABI source mappings remain specific gaps. Free/ad-free does not waive these duties.
2. Resolve the four empty placeholder files through valid source-built binaries or an
   explicitly approved support/delivery design. Do not fabricate a library or silently
   drop a supported ABI/system. Re-test any replacement's state compatibility.
3. Implement the agreed subscription/gift model after commercial clearance and finalize privacy/Data safety/Drive configuration.
4. Owner signing/Play App Signing handoff, then a signed Play-track delivery check.

DS saves remain user-reported working on an unspecified device/build. Additional DS compiler
installation and physical testing are deferred, not sole blockers to submission. Full DS
16KB gameplay, other cores' game initialization, other ABIs, Play Feature Delivery, production
OAuth and minified release gameplay remain unrun. No public push or Play declaration made.

Official technical reference: https://developer.android.com/guide/practices/page-sizes
