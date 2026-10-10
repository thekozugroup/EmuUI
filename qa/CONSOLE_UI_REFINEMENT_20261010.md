# DS-style console refinements

Development preview on `feature/console-ui-haptics-20261010`, following local UI/haptics commit `42798d9b314dddbd352c81928cbd16edadce48df`. Version code 7, version `0.3.0-dev.20261010.2`; API23 minimum, target36, arm64. The public source baseline remains QA commit `b07544a0b573deee56486e3268d26a9d32c760b7`.

## Behavior

- The upper launcher screen has a shared 16dp inset on all four edges. Its title/system plaque now floats at the same bottom margin as the lower corner controls. The top row adds the status-bar inset once; a real camera obstruction can increase clearance. Precise camera avoidance and logo-only collision behavior remain.
- The lower library has no header/count bar. Its shortcut dock is icon-only, floats 16dp above the panel bottom, and retains spoken labels, 48dp targets, haptics, and side-control focus/activation. The active filter is exposed as accessible state; L/R filtering and empty-filter recovery remain.
- Selection uses an outward, continuous 16dp gradient glow derived from the loaded cover. A bounded 32x32 local sample selects the dominant chromatic bucket, ignoring transparent pixels and black/white/gray borders. Missing or monochrome artwork uses the neutral palette. There is no new color-fetch request, dependency, permission, or image service. The outline is a faint 0.5dp edge; scale and selected semantics also indicate focus. The shelf reserves 20dp for the 16dp glow plus selected-card growth, including when scrolling to the final row.
- Settings, search, systems/results, dialogs and paused-game menus use consistent inset controls, compact console headers, 12dp inner cards/16dp outer panels and faint dividers. Dialog scrolling and enlarged-text support remain. The preceding control-haptics implementation is retained; shared header actions now use its interaction feedback too.

Horizontal-hinge gating, DS layout policy, neutral adaptive icon assets, manual saved previews, native input routing, save serialization/filenames, optional Drive and cover lookup are preserved. This round removes only the header and visible dock labels requested by the owner; it removes no emulator system or account feature. It does not redesign TV.

## Verification

| Check | Result and limit |
| --- | --- |
| Debug bundle and minified production bundle | PASS; production AAB unsigned |
| Unit suite | PASS: 211, zero failures/errors/skips |
| Lint | PASS: zero errors, 47 existing warnings, four hints |
| Final console UI suite | PASS: 19 on API36 arm64 at 2208x1840, including artwork color/fade/bottom-clearance, icon-only accessibility, controller navigation, haptics, keyboard, dialogs and cutouts |
| Layout/font variants | PASS: two checks each at 1840x1440, 2560x1840 and 2208x1840 with 1.5x text; density 2.625 |
| Native NES | PASS: FCEUmm fold/control/cutout/menu checks and two Activity recreations, in separate game-process test invocations |
| 16KB native and packaging | PASS: 18 libraries in each artifact, PT_LOAD alignment, PAGE_ALIGNMENT_16K and APK zipalign -P16 |
| Native comparison | 17 byte-identical; renderer differs only in build ID, every other ELF section identical; existing vendor graphics-path RELRO review flag retained |
| Notices | PASS: all 236 packaged files byte-identical |
| Preview signer/update | PASS: existing debug signer; update install preserves emulator data |
| Actual pixels | PASS: fresh UI, owned artwork-color fixtures and original native homebrew captures inspected |

FAILED final checks: none. Recovered attempts are described below. The runtime is the debug APK; minified production runtime and Play-delivered testing remain unrun.

The emulator uses actual 16384-byte pages. Fold and cutout metadata are synthetic; screenshots are actual Android renders. Gameplay uses the original CC0 NES fixture. Color-study images are test-owned flat-color PNGs, not game covers or gameplay. No commercial ROM, BIOS or artwork is included.

All 18 native libraries are present in both artifacts, arm64 only, and pass PT_LOAD alignment. Seventeen are byte-identical to the preceding UI build; the rebuilt libretrodroid renderer differs only in `.note.gnu.build-id`, with every other ELF section identical. The existing `libandroidx.graphics.path.so` RELRO end-boundary review flag remains. Packaged notices are all 236 original files, byte-identical. The AAB requests PAGE_ALIGNMENT_16K and remains unsigned; the preview APK uses the existing matching debug signer.

UNRUN: physical haptic feel/OEM behavior, API23-26 runtime, physical foldable/camera-half mapping, native DS gameplay/touch/current disk-save compatibility, fresh full persistent-save matrix, other-core gameplay, production Drive sign-in/sync, minified production UI/gameplay, Play-delivered splits, TV-specific tests and older instrumentation capture/smoke/manual-DS tests that still refer to removed filter controls. Previous owner-reported DS save success is not relabelled as this round's verification.

Recovered attempts remain in local evidence. A duplicate import caused the first compilation failure and was corrected. Gradle then stalled on cloud-sync copies of generated class files; a fresh temporary build-output directory completed successfully without deleting those files. Four initial test assertions still expected removed header/parent labels and were updated to validate filter state and accessible icons. Pixel review rejected initial screenshots obscured by a System UI startup ANR; emulator diagnostics identify `com.android.systemui` failing startup, not an EmuUI ANR. The system dialog was closed, screenshot guards added, and clean captures repeated. Pixel review also replaced the initial faintly banded glow with a continuous gradient and reserved room below selected cards during scrolling. A color-fixture test raced the Room UI update; it now waits for the inserted row to appear. Three old numeric layout assertions were updated for the 20dp glow clearance; the 48dp minimum target remains.

## Privacy, licenses and Play boundary

This UI delta introduces no network destination or account flow. Existing static evidence: `LibretroDBMetadataProvider.kt` sends normalized system/game-title paths to thumbnails.libretro.com; `SecureThumbnailInterceptor.kt` upgrades legacy HTTP thumbnail links to HTTPS, and network_security_config.xml blocks cleartext. Optional Google Drive requests DRIVE_APPDATA and uses appDataFolder for saves. Save export uses the document provider the owner chooses. These are source findings; a new packet trace or deletion/retention certification was not performed.

The [free/ad-free checkpoint](../docs/FREE_RELEASE_CHECKPOINT.md), [core-source checkpoint](../docs/FULL_SOURCE_CORE_CHECKPOINT.md), [license inventory](../docs/CORE_LICENSE_INVENTORY.md) and [preceding UI/haptics QA](CONSOLE_UI_HAPTICS_20261010.md) retain the broader launch evidence and obligations.

The API36 manifest meets the current [Play target-API requirement](https://support.google.com/googleplay/android-developer/answer/11926878). Native packaging and runtime checks follow the [Android 16KB guidance](https://developer.android.com/guide/practices/page-sizes); these checks do not certify every core or device. The listing still needs an accurate [Data safety declaration](https://support.google.com/googleplay/android-developer/answer/10787469).

Launch gates remain: authorized upload signing/Play App Signing and account agreements; production OAuth/Drive testing; privacy/Data safety/listing/reviewer access; source/notices obligations; Play split/internal testing and current DS/save/device QA. The unsigned AAB is review material. No Play Console write, main promotion, public release, physical-device action, app-data uninstall/clear, new key/credential, SDK installation/terms acceptance or paid service was performed.

## Official implementation references

- [Android Compose drawing and drawing modifiers](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Coil AsyncImage and image callbacks](https://coil-kt.github.io/coil/compose/)
- [Android window insets](https://developer.android.com/develop/ui/compose/system/insets)
- [Android haptic feedback](https://developer.android.com/develop/ui/views/haptics/haptic-feedback)

Exact binary hashes, incremental and cumulative source patches, clean screenshots and selected logs accompany the local handoff.
