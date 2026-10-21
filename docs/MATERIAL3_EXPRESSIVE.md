# Material 3 Expressive integration

EmuUI uses the official AndroidX Material 3 Expressive APIs, rather than treating custom colors and rounded corners as an Expressive implementation.

## Deliberate version pin

`androidx.compose.material3:material3:1.4.0-alpha18` is pinned explicitly. This is an official Google Maven release from July 16, 2025. It is an **experimental alpha**, not the latest stable Material 3 release.

This version was selected to introduce actual Expressive APIs without a broad, unrelated Android toolchain migration. Its published Android AAR metadata and Maven dependencies match the existing build:

| Requirement | Published artifact | EmuUI build |
| --- | --- | --- |
| Minimum compile SDK | 35 | 35 |
| Minimum Android Gradle Plugin | 8.6.0 | 8.7.1 |
| Kotlin standard library | 2.0.21 | 2.0.21 |
| Compose runtime / UI | 1.8.2 | 1.8.2 |
| Compose foundation | 1.8.1 minimum | 1.8.2 resolved |

JDK 17, Gradle 8.10.2, target SDK 35, and the Kotlin Compose compiler plugin remain unchanged.

Do not replace the pin with Material 3 `1.4.0` stable and assume that Expressive APIs remain available. AndroidX removed its experimental Expressive APIs when the 1.4 line entered beta and continued them in the 1.5 alpha line. A future update should migrate deliberately, check the release notes, and rerun Android build, lint, unit, instrumentation, and physical-fold testing.

## APIs used in the app

- `MaterialExpressiveTheme` is the root theme in `LemuroidTheme.kt`, with `MotionScheme.expressive()`, the warm EmuUI palette (including explicit `surfaceContainer*` roles, so expressive components never fall back to baseline lilac tones), emphasized type styles and the increased shape steps (`largeIncreased`, `extraLargeIncreased`, `extraExtraLarge`)
- Home, upper pane: `SplitButtonLayout` with `SplitButtonDefaults.LeadingButton` (Play) and `TrailingButton` (game options). Narrow clamshell panes use the small split-button size and a shorter label. An empty library shows the expressive `Button` with `ButtonDefaults.shapes()`
- Home, lower pane: the library filters are a connected button group (`ToggleButton` with `ButtonGroupDefaults.connected*ButtonShapes()`), which replaces filter chips as the spec recommends. Each toggle keeps `selected` semantics for tests and assistive technology
- Library actions sit in a `VerticalFloatingToolbar` that floats over the shelf when the pane is tall enough, and fall back to inline icon buttons when it is not. Icon buttons use `IconButtonDefaults.shapes()` for the pressed-shape morph
- Cartridges, welcome steps and the guidance card are frosted surfaces (see below). Badges use static `MaterialShapes` (`Cookie9Sided`, `Sunny`, `Cookie4Sided`) on decorative elements only, never on text containers
- Selection colour and cartridge press scale use `MaterialTheme.motionScheme` effects and fast spatial springs
- Secondary screens use `ShortNavigationBar` on narrow windows and a collapsed `WideNavigationRail` on windows 600dp and wider, replacing the baseline navigation bar
- Settings groups are rounded `surfaceContainerLowest` cards with their title above; the in-game menu uses Android's segmented list style (large outer corners, small inner corners, 2dp gaps)
- A standard `CircularProgressIndicator` still handles initial library loading. The morphing `LoadingIndicator` stays removed: an API 35 ANR trace identified synchronous polygon-matching work on the main thread during startup. The static `MaterialShapes` above do not morph
- `LinearWavyProgressIndicator` displays active library-import progress

Experimental API opt-ins are scoped to the affected composables. Labels, content descriptions, touch targets, navigation routes and the physical hinge layout are preserved. Console preview placeholders are original procedural cartridge art; imported library entries may display their own cover art.

## Blur

Blur follows Android 17's system surfaces: a blur of what is behind, a neutral wash with a faint accent tint, no glass edge or refraction, and an opaque fallback.

- **Availability.** `LocalBlurAvailable` follows `WindowManager.isCrossWindowBlurEnabled` (API 31+) through its listener and is false on low-RAM devices. The system turns this off for Battery Saver, the "Reduce blur effects" accessibility setting, thermal limits, tunnel mode and unsupported GPUs, so one signal covers every case
- **In-window cards.** `FrostedSurface` records a `Backdrop` with a `GraphicsLayer` and redraws it through a 24dp `RenderEffect` blur, clipped to the card. The library toolbar blurs the cartridges passing under it; the "selected game" pill blurs the artwork under it. A surface must never sit inside its own backdrop source
- **Ambient colour.** The selected game's cover art, blurred at a quarter of the size and scaled up, tints the console background. Placeholder art uses a gradient field in the same colour instead, because blurring its dark screen glyph leaves a grey smudge
- **Windows.** Dialogs and the game-options sheet set `FLAG_BLUR_BEHIND` at 24dp with a 28% dim; the pause menu blurs the paused game at 34dp, matching Android 17's power menu. The emulator renders into a `SurfaceView`, which in-window `RenderEffect` cannot sample, so only window-level blur reaches it; the in-game Menu pills use a dark translucent tint instead
- **Fallback.** When blur is unavailable, frosted cards become opaque and window dims rise to 60% (75% for the pause menu), as SystemUI does. Text contrast never depends on the blur

## Shadows

`Modifier.softShadow` draws a two-layer shadow straight down: a tight contact layer (1dp offset, 3 to 4dp blur, about 5% ink) and a wide ambient layer (6 to 12dp offset, 18 to 32dp blur, 6 to 8% ink, tucked in by negative spread). The area under the shape is clipped out, so translucent cards never show a grey shadow through their body. Shadows render once in software at quarter resolution and are cached (4 MB LRU), so they look the same on every API level and in JVM renders. Levels: `Resting` for cards, `Raised` for the selected cartridge and preview card, `Floating` for the toolbar and pause panel. Material's own component shadows are turned off where these replace them.

## Verification

The integration must be compiled and the upgraded APK installed before its runtime behavior is considered verified. Existing instrumentation selectors for `EmuUI home`, `Import games folder`, `All games`, and `Favorites` are preserved. Test reduced motion, large font scale, denied notification permission, import cancellation, activity recreation, selection versus Play, and the fold / flat transitions against the upgraded revision.

## Official references

- [AndroidX Material 3 alpha18 release](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha18)
- [AndroidX 1.4 beta change removing experimental APIs](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01)
- [Published Google Maven POM](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18.pom)
- [Published Google Maven source archive](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18-sources.jar)
