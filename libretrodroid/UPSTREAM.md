# Pinned LibretroDroid presentation extension

This source module replaces EmuUI's prebuilt LibretroDroid 0.13.2 AAR. It retains
one core instance, GL surface, emulation thread, save API and lifecycle. It contains
source only, no upstream Git history or precompiled libraries.

## Provenance and licenses

- [LibretroDroid 0.13.2](https://github.com/Swordfish90/LibretroDroid/tree/0ebd299624bfd51a0a1336dd0a2c56fe7ddbc0e3), commit `0ebd299624bfd51a0a1336dd0a2c56fe7ddbc0e3`; `src/main`, ProGuard files, README and GPL-3.0-or-later LICENSE retained
- [Oboe](https://github.com/google/oboe/tree/b15f5e39c01a7ada306d959e5129620b145fb8b4), upstream submodule pin `b15f5e39c01a7ada306d959e5129620b145fb8b4`; required `src`, `include`, CMakeLists and Apache-2.0 LICENSE retained
- [libretro-common](https://github.com/libretro/libretro-common/tree/b0c348ea5543c4d7fb0bc479258aa6988b20c0c9), upstream submodule pin `b0c348ea5543c4d7fb0bc479258aa6988b20c0c9`; required headers and `vfs`, `string`, `encodings`, `file`, `time` sources retained with their individual copyright/license notices (this revision has no root license file)

Unused upstream samples, applications, build workflows and test assets were not
imported. `UPSTREAM.lock.json` records archive hashes and each imported original
file's SHA-256 so an update can distinguish upstream changes from this patch.

## Public API

- `GLRetroView.getCoreAspectRatioUpdates(): StateFlow<Float>` replays the latest
  native display ratio; `coreAspectRatio` reads the same cache without waiting
  for the GL thread. Zero means not yet loaded. Initial load and each rendered
  step publish only finite positive ratios; unchanged values are conflated.
  Positive finite core DAR is already display-oriented and is honored unchanged.
  Only an absent/invalid DAR's base-width/base-height fallback is corrected for
  frontend quarter-turn rotation. Native FIT uses this exact same effective ratio
- `RenderRegion(source: RectF, destination: RectF)` describes normalized top-left
  rectangles in the **unrotated** full source image and full GL view
- `GLRetroView.setRenderRegions(List<RenderRegion>)` validates and copies at most
  eight regions, queues updates on the existing GL thread, and can be called
  before surface creation. An empty list restores upstream viewport fitting and
  core rotation. Identical updates do not release an active pointer
- Region destinations are exact; the caller does native-aspect FIT independently
  for each region. With DS top/bottom plus zero core gap, source rectangles are
  `(0,0,1,.5)` and `(0,.5,1,1)` in Android RectF edge notation
- Touch input still enters in window coordinates, normalized to `[-1,1]`. The
  native inverse maps a matching destination to source UV coordinates `[0,1]`,
  then uses the existing libretro pointer conversion. Outside, UP and CANCEL
  release the pointer. The app must keep its lower-DS-only touch gate
- `GLRetroView.enqueueAccessibleTap(keyCode: Int, port: Int = 0)` queues a
  frame-aware joypad pulse on the existing emulation thread. It accepts input
  only while emulation is ready. The native layer rejects invalid ports and
  unknown keys; each valid key has at most 64 pending activations, with further
  activations coalesced at that bound
- `GLRetroView.cancelAccessibleTaps(keyCode: Int? = null, port: Int = 0)` cancels
  pulse-owned state for one key or, with null, the whole port. It never releases
  an ordinary held key. Whole-port cancellation also advances a generation that
  rejects obsolete queued taps. The current main-thread/single-view call flow
  orders cancellations before later taps or holds; pause, posture gating and
  shutdown clear pending pulses

## Accessible input timing

`inputpulse.h` keeps accessible pulse state separate from ordinary held-key
state. An accepted pulse is presented as down for two actual core runs, then
up for one core run before the next queued pulse. Only completion of `retro_run`
advances this state; wall-clock delays, input polls, GL draws and duplicate video
callbacks do not. This replaces an app-side timed down/up pair that could drain
entirely before a slow core ran and therefore lose an accessible activation.

The effective joypad state is the logical OR of the ordinary hold and the pulse
overlay. Pulse completion or cancellation cannot release a held button, and an
ordinary release cannot erase a still-active pulse. An ordinary hold can mask
the pulse's release interval and any distinct tap edges. The timing guarantees
that the pressed state is available for core observation, not that every game
will respond: games may poll differently or require a longer hold. No claim of
distinct repeated tap edges is made while the same key is ordinarily held.

The host input suite has seven C++ scenarios with 23 assert sites, executed 220
times, plus 12 Python source contracts. These are not 220 distinct tests. They
cover core-run timing, bounded queues, validation, hold overlap, cancellation,
paused-core guards and generation wiring. Executed Android input and save
acceptance belongs to the exact APK records in [QA.md](../docs/QA.md).

## Deliberately small native change

`renderregion.{h,cpp}` contains pure/testable region geometry, validation, pointer
mapping, sample bounds and ratio fallback. `Video` draws each region only in the
final shader pass; software textures and hardware framebuffers remain shared.
The upstream JNI/Kotlin bridge adds region updates and aspect reporting. The
upstream zero-aspect fallback is corrected to use base width/height. Viewport,
surface, aspect, rotation, region, renderer-size and shader mutations invalidate
presentation so unchanged core frames still redraw after layout changes. Source files
also gain explicit standard-library includes required by portable compilation.
Shader program/pass changes wait for the next real frame so duplicate redraws
keep the last valid chain; hardware shader reconfiguration preserves the core
framebuffer and rebuilds only intermediate shader passes and texture filtering.

Final-pass samples are clamped to source texel centers so ordinary bilinear
filtering cannot read the other DS screen. Intermediate CUT2/CUT3 passes still
process the original combined image, so their edge-classification metadata can
include pixels across the internal screen boundary. This is **not** full
per-screen multipass filter isolation; device rendering QA remains required.
The split path omits the immersive background to keep the hinge/control area
black, and clears the complete surface with scissor disabled. Default unsplit
shader sampling remains unclamped and the upstream viewport path is retained.

When EGL creates another surface/context for an already-loaded game, only its
presentation objects are rebuilt. The existing emulation instance continues;
ROM/SRAM loading and lifecycle-observer registration happen once. CPU-side
renderer ownership is released without deleting stale GL names in the new
context. Hardware source-buffer dimensions reuse the last accepted runtime
geometry when available. For software cores, the last image is not cached outside EGL: a core
that emits only duplicate/null frames after context loss stays black until its
next real frame. Hardware recovery relies on the core's existing context-reset
callback. Android forced-context-loss QA is required before claiming recovery
for a particular core; no reset or SRAM reload is used as a workaround.

The display-aspect distinction follows the positive nominal DAR in libretro
geometry. For example, melonDS DS's `BufferAspectRatio()` already accounts for
its rotated layouts; blindly inverting every explicit DAR would invert twice.
See [melonDS DS screen layout](https://github.com/JesseTG/melonds-ds/blob/f394adbacb5722ee97c1b37c8064da9a25818310/src/libretro/screenlayout.hpp#L153-L160)
and [RetroArch core DAR handling](https://github.com/libretro/RetroArch/blob/master/gfx/video_driver.c#L3020-L3044).

## Builds and checks

- Android toolchain pins: NDK `27.3.13750724`, CMake `3.31.5`, app JDK 17/Gradle.
  They are expected to be present on the selected CI runner; do not install a
  new toolchain as an implicit part of local verification
- Build: `./gradlew :libretrodroid:assembleDebug` (also built by the app dependency)
- AAR: `libretrodroid/build/outputs/aar/libretrodroid-debug.aar`
- Native renderer Debug builds use `-O2` while retaining debug symbols; this
  avoids an unoptimized preview path when replacing the former release AAR.
  Vendored Oboe retains its own upstream Debug optimization flags
- Host tests: `python3 libretrodroid/tests/run_host_tests.py`
- Optional installed-Mesa shader check: `python3 libretrodroid/tests/run_host_shader_tests.py`

The renderer retains upstream 16KB ELF link alignment and enables NDK flexible
page sizes. That makes no assertion about separately downloaded emulator cores.
Host tests are not Android JNI, GPU-driver, ABI or device lifecycle verification.
