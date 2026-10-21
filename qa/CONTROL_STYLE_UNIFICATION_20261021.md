# Shared console control styling — 21 October 2026

This local development candidate continues `codex/play-readiness-20261020` from recovered commit `14df922fcfa8d331328a81e6a9019a4387b8100b`. Its preview identity is version code 9, `0.3.1-dev.20261021`. Core download tags, native input locks and dependency versions are independent and unchanged.

## Recovery checkpoint

The read-only cloud recovery audit found the original six commits intact, with a clean worktree at `14df922`. The source-only delta from the October 10 console baseline `cffb0fb79f7e387ef0b429d2e05f35ee077a9a8b` contained 116 files. The indexed publication guard passed; a separate scan of all 128 new blobs found no credential payloads. The existing two-file `codex-ui-main-audit` stash was preserved without applying it.

Live remote inspection on 21 October found `https://github.com/thekozugroup/EmuUI.git` to be the public repository, with default branch `main` still at `bcda1e61370f7690045cd1121581838c2a56122d` (app version `0.1.1`). The remote did not contain `codex/play-readiness-20261020`. The Claude branch `claude/trusting-bell-mn43ri`, then at `eed2913135925e47c35883c7a4cbe392a68e73d6`, descended from that older main rather than the October 10 console branch. This explains why a checkout of main or that Claude branch did not contain the recovered work.

No remote ref, main, Claude branch, release or Play listing was changed. A later authorized source publication must name one reviewed commit and the dedicated Codex branch explicitly, without force, mirror, tags, stash, ignored build inputs or sibling workspaces. A feature-branch push would not change the default branch. The existing Android workflow's push trigger covers main only, so a Codex-branch push alone is not an automatic CI run.

## Implementation

`ConsoleControlFace`, `ConsoleControlLabel` and `ConsoleControlStyle` in `lemuroid-touchinput` share the established library styling: Material surface-container colors, a 1 dp outline, rounded key faces, bold label typography and the same pressed treatment. Launcher keys and ordinary radial gameplay use the shared face. Accessible gameplay keeps its native `TextButton` and spoken/custom actions, using the shared style and label. A displayed A key receives the primary-container accent; this follows its visible label rather than its Android key code. Pressed keys pair the pressed surface with `onSurface` for contrast.

Group, analog and simultaneous-input silhouettes remain, with matching Material colors. The separate gradient/glass treatment was removed from gameplay control faces and the wing backing. Native game pixels and the OLED console frame color are unchanged.

The shared components only paint: they add no pointer input, click handling, accessibility container or haptic collector. The pressed scale uses `drawWithContent`, preserving input and accessibility bounds. The original launcher `Surface`, radial PadKit handlers and accessible `TextButton` retain interaction ownership. Launcher 48 dp targets, accessible 64-by-48 dp minima, radial padding/anchors, per-console labels, DS microphone/lid assignments, held/released actions and haptic dispatch remain in their existing implementations. Fold/orientation geometry, core inputs, saves and controller preferences are unchanged.

## Completed source and host checks

- Independent source review passed: input mappings/actions match the recovered base, and 456 protected native/core/lock/provenance/haptics/geometry paths are unchanged. The shared drawing path has no pointer or semantics modifiers.
- Scoped standalone ktlint 1.2.1 passed all eight changed Kotlin files; the app Gradle ktlint aggregate also passed. `git diff --check` passed. The publication guard passed all 1,543 indexed entries; staged blobs contained no detected credential payloads or generated artifacts.
- The app's Android Gradle JVM suite passed 220 tests across 28 suites, with zero failures, errors or skips. The renderer's three unchanged JVM tests were reused up to date. These tests do not render Compose or dispatch Android touch events.
- Bundled debug APK compilation/package and ordinary instrumentation APK compilation/package passed. The sixteen locked core hashes and 229 inventoried core notice hashes match; all 18 native libraries and all 236 packaged license files are byte-identical to the October 20 debug candidate. APK signature and ZIP-alignment checks passed. Static alignment does not establish 16 KB-device execution.
- The debug aggregate completed successfully in 8 minutes 56 seconds. Debug lint reported zero errors/fatals, 46 warnings and four hints. The APK retains the existing cloud debug certificate; this does not establish compatibility with an installation signed elsewhere.

## Verification boundary

Native appearance and input acceptance require an Android runtime. At this checkpoint ADB reported no attached device and `/dev/kvm` was absent. The previous two software AVD attempts failed Android package/storage readiness; the fresh guest's Watchdog killed `system_server` before EmuUI installation. No new guest boot, watchdog bypass or screenshot substitution is used for this styling change.

The smallest later device check is an update install of this development-signed APK on a compatible foldable, preserving any existing saves and verifying the signing certificate before installation. Compare library and gameplay controls in light/dark modes; check GB/GBA/DS buttons, D-pad diagonals, held/released and simultaneous inputs, Start/Select/shoulders, accessible hold/release actions, 48 dp minimum targets, rotation/fold guidance and haptic preferences. Use the original lawful fixtures described in `qa/homebrew`; actual screenshots must come from this installed candidate. Physical haptics, native control appearance and gameplay remain unverified until those checks run.

Earlier APK/AABs and their source archives remain preserved separately in the October 20 handoff. They do not contain this later styling change.
