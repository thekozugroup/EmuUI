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
- `MotionScheme.expressive()` supplies the official Expressive motion system while preserving EmuUI's original color, typography, and shape palette
- The primary Play / Add games action uses the Expressive `Button` overload with `ButtonDefaults.shapes()`, including its animated pressed-shape behavior
- A standard `CircularProgressIndicator` handles initial library loading. The morphing `LoadingIndicator` was removed after an API 35 ANR trace identified synchronous polygon-matching work on the main thread during startup; rendering progress must stay lightweight.
- `LinearWavyProgressIndicator` displays active library-import progress
- Cartridge selection color transitions use `MaterialTheme.motionScheme.fastEffectsSpec()`

Experimental API opt-ins are scoped to the affected composables. Labels, content descriptions, touch targets, navigation routes, and the physical hinge layout remain unchanged. Console preview placeholders are original procedural cartridge art; imported library entries may display their own cover art. Upstream system icons retain their original attribution.

## Verification

The integration must be compiled and the upgraded APK installed before its runtime behavior is considered verified. Existing instrumentation selectors for `EmuUI home`, `Import games folder`, `All games`, and `Favorites` are preserved. Test reduced motion, large font scale, denied notification permission, import cancellation, activity recreation, selection versus Play, and the fold / flat transitions against the upgraded revision.

## Official references

- [AndroidX Material 3 alpha18 release](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-alpha18)
- [AndroidX 1.4 beta change removing experimental APIs](https://developer.android.com/jetpack/androidx/releases/compose-material3#1.4.0-beta01)
- [Published Google Maven POM](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18.pom)
- [Published Google Maven source archive](https://dl.google.com/dl/android/maven2/androidx/compose/material3/material3-android/1.4.0-alpha18/material3-android-1.4.0-alpha18-sources.jar)
