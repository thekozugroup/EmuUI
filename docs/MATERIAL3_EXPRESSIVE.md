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

- `MaterialExpressiveTheme` is the root theme in `LemuroidTheme.kt`
- `MotionScheme.expressive()` supplies the official Expressive motion system with EmuUI's neutral fallback colors, typography, and shape palette
- The primary Play / Add games action uses the Expressive `Button` overload with `ButtonDefaults.shapes()`, including its animated pressed-shape behavior
- A standard `CircularProgressIndicator` handles initial library loading. The morphing `LoadingIndicator` was removed after an API 35 ANR trace identified synchronous polygon-matching work on the main thread during startup; rendering progress must stay lightweight.
- `LinearWavyProgressIndicator` displays active library-import progress
- Cartridge selection color transitions use `MaterialTheme.motionScheme.fastEffectsSpec()`

Experimental API opt-ins are scoped to the affected composables. Labels, content descriptions, touch targets, navigation routes, and the physical hinge layout remain unchanged. Console preview placeholders are original procedural cartridge art; imported library entries may display their own cover art. Upstream system icons retain their original attribution.

## Appearance and wallpaper colors (0.2.0 source revision)

Settings → Appearance → Theme offers **System**, **Light**, and **Dark**. System is the default and follows Android's current dark-mode configuration; an explicit Light or Dark choice overrides it. The choice is stored as a stable string, rather than a translated label or list index. Unknown/corrupt stored values fall back to System.

On Android 12 (API 31) and later, **Wallpaper colors** is on by default and uses AndroidX `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` to obtain the system's wallpaper-derived Material You palette. Turning it off uses the neutral fallback. Older Android versions always use the complete neutral light/dark schemes. Missing OEM dynamic-color resources also fall back safely. Wallpaper appearance depends on the device's Android implementation and wallpaper settings; the app does not infer a palette from game artwork.

All launcher surfaces, selection roles, settings, menus, dialogs, and posture guidance use Material color roles with their corresponding foreground roles. Broad shell/panel surfaces stay understated; selection and primary actions use the primary family. The neutral fallback explicitly sets every role in the pinned Material 3 API, including all surface-container and fixed-color roles, to avoid accidental default purple components. Error colors retain their semantic red. Native emulator pixels are never recolored. Accessibility controls have an opaque theme surface with matching text; radial gameplay controls retain their existing neutral overlay styling.

The implementation reuses the existing **Harmony 1.1.9 multi-process preferences** store used by the rest of EmuUI. Each `AppTheme` subscribes to preference changes and refreshes on Activity resume. This matters because the launcher and game/menu run in separate processes: ordinary Android `SharedPreferences` or `MODE_MULTI_PROCESS` are not used for this feature. Resume and configuration changes also invalidate dynamic-color calculation, allowing fresh wallpaper resources to be read. Launcher status/navigation-bar icon contrast follows the selected mode via `WindowInsetsControllerCompat`; gameplay's immersive and black-system-bar policy is preserved without showing or hiding bars.

The theme keeps `MaterialExpressiveTheme` and `MotionScheme.expressive()`; Material You color selection is complementary to these APIs. No new runtime dependency, permission, account, network request, or credential is needed.

Focused JVM coverage includes stable preference parsing, explicit and System mode resolution, neutral-surface saturation, and 4.5:1 minimum contrast for fallback text-role pairs. These tests are not a substitute for native verification: check actual theme selection and persistence, cold relaunch, main/`:game` propagation and return, system dark-mode changes, font scaling, dialogs, game controls, and system-bar icons. Dynamic wallpaper behavior requires a working Android 12+ device; API 29 fallback screenshots cannot establish Monet runtime support. See [QA.md](QA.md) for the executed, artifact-specific results.

Sources: [Android Compose Material 3 dynamic color and roles](https://developer.android.com/develop/ui/compose/designsystems/material3#dynamic_color_schemes), [Harmony multi-process preference implementation](https://github.com/pablobaxter/Harmony).

## Verification

The integration must be compiled and the upgraded APK installed before its runtime behavior is considered verified. Existing instrumentation selectors for `EmuUI home`, `Import games folder`, `All games`, and `Favorites` are preserved. Test reduced motion, large font scale, denied notification permission, import cancellation, activity recreation, selection versus Play, and the fold / flat transitions against the upgraded revision.

## Official references

- [AndroidX Material 3 alpha18 release](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha18)
- [AndroidX 1.4 beta change removing experimental APIs](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01)
- [Published Google Maven POM](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18.pom)
- [Published Google Maven source archive](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18-sources.jar)
