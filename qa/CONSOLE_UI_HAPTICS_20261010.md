# Refined console UI and control haptics

Development branch: `feature/console-ui-haptics-20261010`, based on published QA commit `b07544a0b573deee56486e3268d26a9d32c760b7`. Version code 6, version `0.3.0-dev.20261010`. The selected direction is OLED black, neutral glass controls, and clearer library/settings. This is a development preview; the Play launch remains a separate gated task.

## Behavior

- The mobile console uses a neutral Material 3 palette, darker grouped panels, outlined glass controls, visible pressed states and clearer text. New preferences default to Dark with wallpaper colors off. Existing explicit theme, frame and wallpaper-color choices are retained.
- The library gains a current-filter heading/count and labelled Import, Search, Systems, Settings, Help and Setup dock actions. The existing shoulder filtering and side-control navigation remain. No hidden filter-button row is restored.
- Shared mobile settings, search/results, systems, game menus, save-slot previews and save export use the same styling. Save previews fit within an 88x66dp thumbnail instead of being cropped. Save serialization, filenames and native core code are unchanged. This work does not constitute a separate TV interface redesign.
- Control haptics use the existing None, Press and Press & release preference. Android view feedback respects the system haptic setting and hardware support. Real press/release interactions drive console-control feedback; cancelled and disabled interactions remain quiet. The gameplay adapter deduplicates held buttons and analog sectors, emitting one tick per input-edge batch rather than each held frame. Simulated tilt directions remain quiet. PadKit's separate direct-vibration path is disabled to avoid duplicate feedback. API23–26 uses the virtual-key fallback for the release effect.
- Preference observation uses the existing Harmony store and refreshes on resume, including gameplay's separate process. No new dependency or permission is introduced.
- Search opens without forcing the keyboard. A focused field uses Android panning; Done hides the keyboard and clears focus. Physical fold geometry uses system-bar, display-cutout and waterfall insets separately from IME height, so typing cannot falsely collapse the lower pane into the small-window guidance gate.

Horizontal-hinge console gating, DS layout policy, cutout/status-bar accounting and logo-only collision behavior, adaptive icon assets, manual preview handling and save formats remain in place.

## Verification

| Check | Result and limit |
| --- | --- |
| Debug bundle, test APK, minified production bundle | PASS; production AAB remains unsigned |
| Unit suite | PASS: 206 tests, zero failures/errors/skips |
| Lint | PASS: zero errors, 47 existing warnings, four hints |
| Console UI/haptics/navigation/keyboard/cutout suite | PASS: 18 tests on API36 arm64 at 2208x1840 |
| Short and wide layout regressions | PASS: two tests each at 1840x1440 and 2560x1840; all at density 2.625 |
| Native NES homebrew | PASS: FCEUmm gameplay/control/fold/cutout/menu checks and two activity recreations, in separate real game-process invocations |
| Runtime page size | Actual 16384 bytes; fold and cutout metadata are synthetic |
| Native inventory | All 18 libraries in each artifact are arm64 only and byte-identical to the respective October 9 baseline; all PT_LOAD checks pass |
| RELRO heuristic | Existing `libandroidx.graphics.path.so` end-boundary review flag remains; no blanket all-core runtime certification |
| Bundle validation / packaging | PASS: bundletool validates; AAB requests PAGE_ALIGNMENT_16K; QA APK zipalign with `-P 16` passes |
| Preview signing | PASS: existing debug signer, package `com.kozudigital.emuui.debug`, version code 6; update install preserves emulator data |
| Notices | 236 packaged license files byte-identical to the previous release |
| Pixels | Fresh actual Android UI and original homebrew gameplay captures reviewed; no generated game art |

The debug renderer and release renderer differ by build type, as in the baseline. Each artifact was compared against its own baseline; debug runtime results do not certify the minified release.

Recovered failures are retained in local evidence: the initial palette exceeded the existing neutral-color tolerance; search's combined safe-drawing/IME insets collapsed the lower pane; a route test assumed the appearance row was initially visible without scrolling. The palette and search behavior were corrected, and the route test now scrolls and asserts the intended screens before capturing. Lint also found an API27 release-effect constant without a fallback; that is fixed. Completed final checks have no unresolved failures.

UNRUN: physical haptic feel/system-OEM behavior; API23–26 runtime; physical foldable/reversed camera-half mapping and cutouts; native DS gameplay/touch and current DS disk-save compatibility; fresh full persistent-save matrix; other-core gameplay; production Drive sign-in/sync; minified production UI/gameplay and Play-delivered splits; cover-display interaction; TV-specific regression suite. Prior owner-reported DS save success is retained separately and is not relabelled as this round's independent verification.

## Privacy, licensing and launch boundary

No feature removal, monetization change, native library replacement or network change is introduced by this delta. The free/ad-free/no-IAP checkpoint and retained optional Drive behavior remain. See [FREE_RELEASE_CHECKPOINT.md](../docs/FREE_RELEASE_CHECKPOINT.md), [FULL_SOURCE_CORE_CHECKPOINT.md](../docs/FULL_SOURCE_CORE_CHECKPOINT.md) and the historical [license inventory](../docs/CORE_LICENSE_INVENTORY.md) for the earlier scope and source obligations.

Static network evidence remains: `LibretroDBMetadataProvider.kt:181` sends normalized system/game-title paths over HTTPS to thumbnails.libretro.com; `SecureThumbnailInterceptor.kt:10` upgrades the legacy thumbnail host's HTTP URLs; network_security_config.xml blocks cleartext. Optional Drive requests DRIVE_APPDATA in `ActivateGoogleDriveActivity.kt:62,86`; `SaveSyncManagerImpl.kt:278,288,317` uses appDataFolder. These are source findings, not a new packet trace or retention/deletion certification. Save export may use a user-selected document provider.

Remaining launch gates: owner signing/Play App Signing and account agreements; production package/signer OAuth configuration and real Drive tests; final privacy/Data safety/listing/reviewer access; Play split/internal testing and current DS/save/device QA; source/notices obligations and any unresolved legal review. The unsigned AAB is review material, not an upload-ready signed production release.

No Play Console write, main promotion, public release, physical-device action, data uninstall/clear, new key, credential, SDK agreement acceptance or paid-service action was performed in this round.

## Official implementation references

- [Android view haptic feedback and system settings](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)
- [Compose window-inset types](https://developer.android.com/develop/ui/compose/system/insets)
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

Final artifact hashes, source delta, screenshots and selected logs are supplied in the accompanying local handoff. They identify the exact binaries tested; failed attempts remain in the separate workspace evidence directory.
