> Current integration supersedes the original PSP download/source gaps below: Play now bundles 137 exactly traced PSP asset files and never downloads them externally. Three source-built cores are integrated. See SOURCE_CORE_INTEGRATION.md and CURRENT_RELEASE_STATUS.md for final validation and remaining gates. US pricing is settled at $0.99/month or $19.99 lifetime; checkout uses localized Play pricing.

# Play release preparation — 5 October 2026


Current commercial direction (2026-10-05): US$0.99/month, ad-free, with permanent full-access gifts through unique codes. See COMMERCIAL_RELEASE_DECISIONS.md and OWNER_SIGNING_HANDOFF.md. Earlier free-launch proposals are superseded.

**Latest update:** see `ACTUAL_16KB_RESULTS.md`. The approved image is installed;
actual arm64 loading, scoped UI/NES tests and release startup now pass on 16,384-byte pages.
Earlier pending/unrun statements below describe the prior checkpoint.

Status: **not release ready**. This record concerns the isolated MacBookPro checkout,
not the prior Helios QA checkpoint. Base: `e46ef62f2801556912cdf6d86caf0d5282483f6e`;
branch: `release/play-preparation-20261005`. No Console writes or signing keys were created.

## Source changes

- Release application ID is `com.kozudigital.emuui`; debug ID appends `.debug`.
  Instrumentation target package/processes follow it. Upstream Java namespaces remain.
- Target/compile API 36; AGP 8.10.1, Gradle 8.11.1 (official SHA256 pinned), build tools 35.
- New thumbnail URLs use HTTPS. Shared Coil and Retrofit clients upgrade old persisted
  thumbnail URLs before connecting; cleartext is disabled. This preserves library rows
  and save data. HTTPS does **not** stop the provider receiving the game title.
- Renderer now passes both max-page-size and common-page-size 16384 linker flags, as recommended for NDK r27.
- Corrected notices to describe the modified in-tree renderer and flavor-specific delivery.
- Added reproducible ELF inventory and a regression test for old thumbnail URLs.

No layout, fold gating, input, icon, manual-preview or save format changes were made.
Historical `docs/QA.md` results retain their original package and binary identity.

## Build identity and data transition

This is a new Android package, not an in-place upgrade of `com.thekozugroup.emuui.debug`.
Android will not grant access to the old app's private saves, database or folder grants.
Keep the old app installed. Export/backup and restore must be verified before suggesting
any migration; reimporting games alone does not transfer private saves. Existing Drive
OAuth registrations must not be assumed to authorize the new package/signing certificate.
Release signing and Play App Signing ownership remain an explicit owner handoff.

## Policy evidence

- [Target API policy](https://support.google.com/googleplay/android-developer/answer/11926878):
  API 36 is required for new phone/tablet submissions from 31 August 2026.
- [AGP 8.10 compatibility](https://developer.android.com/build/releases/agp-8-10-0-release-notes):
  supports API 36, requires Gradle 8.11.1 and build tools 35.
- [Executable delivery policy](https://support.google.com/googleplay/android-developer/answer/16559646):
  native executable downloads must use Google Play. `playDynamic` uses SplitInstallManager;
  `freeDynamic` downloads `.so` from GitHub and is unsuitable for Play distribution.
  This finding does not require removing every core: Play Feature Delivery already exists.
- [16 KB guidance](https://developer.android.com/guide/practices/page-sizes): verify every
  packaged native library, APK alignment and actual 16 KB runtime. AGP/renderer alignment
  cannot certify independently prebuilt cores.
- [Android 16 behavior](https://developer.android.com/about/versions/16/behavior-changes-16):
  rerun large-window, orientation, cutout and lifecycle acceptance after targeting API 36.
- [Google publisher placement policy](https://support.google.com/publisherpolicies/answer/11112688):
  Google ads are prohibited on low-content and alerts/navigation/behavioral screens.
  Rotate/open-foldable-only guidance is not an acceptable AdMob placement. No ad SDK added.

## Privacy and network evidence

This is static source evidence, not a packet capture or completed Data safety declaration.

| Behavior | Evidence and release implications |
| --- | --- |
| Covers | `LibretroDBMetadataProvider.computeCoverUrl` includes recognized title and system in a request to `thumbnails.libretro.com`. Provider receives the path and connection metadata; HTTPS protects transit only. No global opt-out established. Retention/sharing claims remain unresolved. |
| Play native delivery | `lemuroid-app-ext-play/.../CoreUpdaterImpl.kt` requests Play split modules. Verify on a Play test track with the final signed build. |
| Free native delivery | `lemuroid-app-ext-free/.../CoreUpdaterImpl.kt` uses GitHub; do not submit this variant. |
| PSP assets | `PPSSPPAssetsManager.kt` downloads `1.15/assets/ppsspp.zip` from GitHub even in Play builds. Runtime tag 1.15 resolves to 29566a989ea367bd95e44965c6c5d6fc55156174. Its archive was inspected: 139 files, no ELF/DEX/JAR/native executable candidates; shaders and debugger JS remain. Artwork/font/dependency licenses remain unresolved. The pinned checkout contains a different archive (247 files), so its evidence must not be substituted. |
| Drive saves | Play flavor requests `DRIVE_APPDATA`; Google account email is displayed. `SaveSyncManagerImpl` syncs saves, states and state previews to appDataFolder. New package/signing OAuth setup and deletion behavior need testing; do not claim all data stays on device. |
| Backup | Main manifest sets `allowBackup=true`. Account for platform backup independently of Drive sync. |
| Microphone | Manifest requests RECORD_AUDIO; confirm runtime opt-in and local-only handling for each enabled core before declaring it. |
| Legacy paths | TV fallback artwork uses `fakeimg.pl`; inherited TV classes are present without a TV launcher. Reachability is unverified. |
| Permissions | INTERNET, legacy READ_EXTERNAL_STORAGE, VIBRATE, TV EPG, POST_NOTIFICATIONS, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE_DATA_SYNC and RECORD_AUDIO appear in source. Final merged manifest also includes WAKE_LOCK, RECEIVE_BOOT_COMPLETED, FOREGROUND_SERVICE, READ_EPG_DATA and the app-specific dynamic receiver permission; see bundle-manifest.txt. |

The supplied privacy draft is a working document, not an approved public policy. Final
privacy URL/contact, collection/retention, optional sync and monetization choices must
match the released binary. No production OAuth credentials or services were configured.

## Native and license gates

See `CORE_LICENSE_INVENTORY.md`. The core pin identifies binary bytes, but its update
script downloads mutable nightly/latest builds without corresponding source commit locks.
Exact source, dependency and license clearance is incomplete for **every** core.

Complete static inventory found 80 entries across 20 modules and four ABIs: 76 ELF files
and four empty placeholders (Citra armv7/x86/x86_64, PPSSPP x86_64). Bundled symlinks
duplicate those 80 entries. All 20 arm64 LOAD segments meet the static 16 KB alignment
check. Seventeen arm64 libraries have non-16-KB RELRO endpoints; this is a runtime-review
flag, not proof that Android loading fails. The 32-bit melonDS files have 4 KB LOAD
alignment. Do not certify an AAB from these observations alone. Empty placeholders need
an explicit ABI exclusion/support decision before shipping; no binary or ABI was removed.

All observed ELF NEEDED entries refer to Android platform libraries (EGL/GLES, OpenSLES,
android, libc, dl, log, m, stdc++, z). This does not inventory statically linked dependencies.

## Monetization design, deferred

The settled direction is US$0.99/month, ad-free, with permanent gift entitlements.
Billing and backend configuration are not implemented in this candidate; commercial core
rights and product configuration remain unresolved. See COMMERCIAL_RELEASE_DECISIONS.md.

If a future non-consumable is approved: use Play Billing product details for localized
price, verify PURCHASED tokens before entitlement, acknowledge without consuming, query
purchases on reconnect/foreground/reinstall and process out-of-app promo redemptions.
Preserve a verified entitlement during temporary offline failures; do not turn ads on
mid-game. Handle pending/cancelled/refunded purchases and account changes explicitly.
Secure backend verification, retention and consent/account setup require an approved design.
Sandbox tests must cover purchase, pending, cancellation, reinstall/restore, offline,
revocation and promo redemption; no local secret-code bypass.

[Official promo codes](https://developer.android.com/google/play/billing/promo) can grant
the same one-time product at no cost. Unique one-use codes have a 500-per-app-per-quarter
limit across one-time products. Gifts are free entitlements, not paid revenue. Subscription
promo codes grant trials, not permanent free subscriptions, and auto-renewal/payment terms
apply. No codes issued. See [Billing integration](https://developer.android.com/google/play/billing/integrate).

## Earlier build checkpoint (superseded by ACTUAL_16KB_RESULTS.md)

Final unsigned `playDynamicRelease` AAB: **191,301,721 bytes**,
SHA256 `431ad0ec8cfbf556aa42b9d34932265997508dc3c53eaae97f6c37e07fd825a7`.
Package `com.kozudigital.emuui`, versionCode 4, versionName 0.2.1, target API 36.
Official bundletool 1.18.0 validation passed; bundle config requests PAGE_ALIGNMENT_16K.
No signing entries are present. This is a blocked review candidate, not a submission-ready build.

| Check | Result and limits |
| --- | --- |
| JVM | PASS: 198 app + 3 renderer tests, no failures/errors/skips. |
| Release lint | PASS: 0 errors, 53 warnings retained. |
| Host tests | PASS: renderer 433 checks; input pulse suite; metadata 5; DS header 14. Host EGL runtime blocked by missing Linux libEGL.so.1 on macOS. |
| Final native package | 88 native entries: 80 core entries, 4 renderer, 4 AndroidX graphics.path. Four empty core placeholders and two 32-bit melonDS LOAD-alignment failures remain. All actual 64-bit LOAD segments meet 16 KB static requirements. 35 RELRO endpoint review flags remain; renderer endpoints now align on all four ABIs. These flags alone do not prove runtime failure. |
| Native parser | PASS: all 76 actual core ELF LOAD alignments cross-checked against official llvm-readelf. |
| Core loading | PASS: all 20 arm64 cores dlopen and expose libretro API 1 on API 36 / 4 KB emulator. This does not initialize games or certify other ABIs/page sizes. |
| Launcher/UI | PASS: 17 distinct checks across final suite and targeted SAF rerun. Initial orientation failure was an Android 16 large-window test assumption, corrected by system rotation; initial picker preflight skip was package visibility, corrected by shell resolution followed by real picker assertions. |
| Import/navigation | PASS: actual Android SAF import of two original lawful NES fixtures; populated launcher selection, settings and controls. No fabricated library rows or grants. |
| NES gameplay | PASS: FCEUmm native frames, input, fold gates/transitions, cutout inset cases and menus. Folding features/insets are injected on an emulator, not a physical hinge. |
| Recreation | PASS when run alone: two Activity recreations, retained ViewModel, new native view, live frames and nonempty serialization. Combined native+recreation run terminated the instrumentation process before recreation results; no crash-buffer entry. Retained as a failed combined harness run, not silently discarded. |
| Screenshots | Fresh real API 36 captures include launcher, icons, cutouts, lawful NES gameplay and recreation. Debug playBundle flavor uses bundled cores; these are QA/listing candidates, not captures of Play-delivered signed release. Some launcher captures include OS taskbar or active import indication. Review before publishing. |
| Unrun | DS gameplay and disk save/preview restore (DS build toolchain absent), actual 16 KB runtime, physical foldable, cross-package save migration, Drive/OAuth, Play split delivery and minified release runtime. |

The debug APK passed `zipalign -c -P 16`; Play-generated APK alignment remains unverified.
The existing debug key was reused. No release keys, OAuth configuration, billing or advertising
were created. NDK 27.3.13750724 and CMake 3.31.5 were installed only after explicit approval.
Tests use a separate workspace AVD; no physical device or existing app data was modified.

## Earlier launch checklist (current gates: COMMERCIAL_RELEASE_DECISIONS.md)

- [ ] Commercial core license/source clearance for the agreed subscription/gift model.
- [ ] Final supported core/ABI set and empty-placeholder handling; corresponding-source bundle.
- [x] Unsigned release AAB and final manifest produced and bundletool validated.
- [ ] Resolve packaged native placeholders/alignment and complete release APK + 16 KB runtime validation.
- [ ] Unit/lint plus real emulator tests, lawful NES/DS gameplay, save/load/recreation and cutouts.
- [ ] Actual 16 KB runtime and physical horizontal-hinge foldable acceptance.
- [ ] Fresh final-build real UI/gameplay screenshots; supplied promotional pack is not fresh QA.
- [ ] Public privacy policy, Data safety, Drive/OAuth decision and release notices.
- [ ] Owner-approved upload signing/Play App Signing; Console track and review requirements.

Do not interpret a checked-in test, historical pass, build success or asset screenshot as
proof that an unexecuted gate passed.
