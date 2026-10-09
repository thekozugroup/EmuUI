# Fold polish staging — 2026-10-09

Local branch: `feature/fold-polish-20261009`, based on `216856990d7c01b6946c85f524dfef0904b0eaef`. Main and the previously staged release are preserved. This is a review candidate, not a launch-ready declaration.

## Changes

- DS lower gameplay screen has more room: control wings change from a maximum 216dp to 176dp. The lower image stays centered, preserves 4:3, and is capped at 94% of the upper image width. Small screens still constrain wings to one third of available width. Launcher library dimensions are unchanged.
- Console frames default to true black; Appearance offers OLED black, Graphite, and Match app theme. Content surfaces retain their readable theme colors. Preferences propagate through the existing cross-process preference mechanism.
- Library selection uses a damped spring and a small scale increase (0.96 to 1.04). The top filter pills are removed, and keyboard/controller navigation no longer focuses their invisible row. Existing shoulder-based filtering remains.
- Main, gameplay, and game menu hide status/navigation bars, reapplying on resume/focus. Android transient swipe escape remains enabled, and gameplay reserves mandatory bottom-gesture space.
- Console position can be calibrated while landscape with a horizontal fold. The user physically places the rear-camera half at the bottom, saves the observed rotation, and can reset it. The saved orientation is limited to the same manufacturer/model. This is manual calibration, not automatic camera-side detection or verified physical-device mapping.
- Adaptive/themed icon mark scale changes from 0.86 to 0.79, with a proportional legacy reduction. Native vector canvases and neutral glass treatment remain intact.

## Current verification

PASS: debug bundle/APK build; 206 unit tests (zero failures/skips); debug lint (zero errors, 47 warnings, 4 hints); corrected selection/background/immersive, settings persistence, and orientation tests; icon masks; fold lifecycle; synthetic corner/center/broad cutout regression. A read-only capability probe completed, reporting rear presentation UNSUPPORTED on this emulator. A successful probe is not a successful cover-screen feature test.

PASS: real original NES homebrew gameplay in FCEUmm on API36 arm64 with actual 16384-byte memory pages; native geometry, controls, fold pause/resume and game-menu roundtrip; isolated two-recreation test with retained ViewModel/new native views. Fold metadata is injected; these are actual Android/native-core screenshots, not physical-foldable evidence.

Initial failures: settings test incorrectly expected the library dock after settings was restored on recreation; corrected test passed. Running both native classes in one invocation let normal game-process shutdown interrupt the second class; each class then passed in its own invocation. A stale generated build directory contained `licenses 2.html`; it was moved aside intact and clean compilation succeeded.

Preview static check: all 19 packaged arm64 native libraries pass 16KB PT_LOAD checks; `libandroidx.graphics.path.so` has a RELRO end-boundary review flag. APK 16KB zip alignment passes. Do not equate static alignment or NES execution with runtime certification of every core.

UNRUN: DS-native gameplay/touch/save-state interoperability with the new geometry; physical rear-half mapping and reversed posture; actual cutout hardware; cover-display presentation/touch; production Drive sign-in/sync; Play-delivered splits; fresh persistent-save compatibility matrix and all-core gameplay. Prior staging evidence remains separate from this round.

## Cover-display decision

No invisible cover-screen shoulder buttons, placement editor, or multitouch/release state machine have been added. The intended display and actual device model remain unconfirmed. Android dual-screen presentation capability must be established on that device; a black panel is not a powered-off touch surface. The emulator reports UNSUPPORTED and simultaneous touch is unverified. The earlier phrase “upcoming Pixel 11 Pro Fold” does not establish tested hardware.

Android distinguishes rear-display transfer (which moves the activity) from dual-screen presentation. Implementation must use capability checks and handle session visibility/end, cancellation, all-pointer release, backgrounding, and accidental input; do not infer support from a model name. This feature remains pending the device/display clarification and supported hardware verification.

## Official references

- [Immersive mode and temporary swipe escape](https://developer.android.com/develop/ui/views/layout/immersive)
- [Fold-aware layout metadata](https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware)
- [FoldingFeature API](https://developer.android.com/reference/androidx/window/layout/FoldingFeature)
- [Rear display and dual-screen modes](https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/support-foldable-display-modes)

## Release boundary

Keep EmuUI free/ad-free with retained Drive sync and the previously approved core scope. Network/privacy/license facts from the October 5 staging handoff remain applicable; this UI change does not resolve OAuth, native-network declarations, signing, or account migration. No physical device, user-data removal, new key, new SDK agreement, paid service, account/security action, Play submission, main promotion, or public release was performed.
