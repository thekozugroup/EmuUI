# Store listing assets

Everything here is generated from EmuUI's own UI. Rebuild it after UI changes:

```sh
# 1. Render the real Compose screens (Robolectric native graphics; skipped unless requested).
./gradlew :lemuroid-app:testFreeDynamicDebugUnitTest --tests '*StoreScreenshots*' -PemuuiScreenshots
#    Behind a rate-limited Maven Central, add -Drobolectric.dependency.repo.url=<mirror>.
# 2. Compose the listing images (Playwright Chromium; Inter, Inter Display and JetBrains Mono fonts).
NODE_PATH=$(npm root -g) node store/tools/compose.mjs
# 3. Before a Play upload, copy the screenshot sets into the fastlane layout.
store/tools/stage-fastlane.sh
```

## Contents

| Path | What | Size |
| --- | --- | --- |
| `renders/` | Raw renders: Pixel 11 Pro Fold inner screen (`fold-inner`, 2152 x 2076), book-style fold (`bookfold-inner`, 2184 x 1968), flip-style fold in portrait (`clamshell`), 16:9 tablet and 16:9 phone | as rendered |
| `play/phone/` | Google Play phone set: 6 images, 16:9, short caption beside real UI | 1920 x 1080 |
| `play/sevenInch/`, `play/tenInch/` | Google Play large-screen sets: 4 images each, 16:9, no added text | 1920 x 1080, 2560 x 1440 |
| `play/featureGraphic.png` | Feature graphic, no alpha | 1024 x 500 |
| `play/icon.png` | Store icon, full-bleed square | 512 x 512 |
| `play/tvBanner.png` | TV banner (replaces the old Lemuroid banner) | 1280 x 720 |
| `play/alt-text.md` | Alt text to paste into Play Console | |
| `galaxy/` | Samsung Galaxy Store: 6 screenshots including native foldable captures, header, icon and listing text | |
| `social/github-social-preview.png` | Repository social preview | 1280 x 640 |
| `app-store/` | Apple App Store copy only; see below | |

Listing text for Google Play and F-Droid is in `fastlane/metadata/android/en-US/` (`title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/3.txt`). The icon, feature graphic and TV banner are also copied there.

## Rules these assets follow

- **Real UI.** Every screen is a render of the app's composables. The in-game frame is original pixel art standing in for the player's game. The pause-menu image simulates the window manager's blur with an in-window blur.
- **No third-party content.** Game titles are invented, art is EmuUI's procedural placeholder, and console names appear only in the full description's supported-systems list, next to a no-affiliation statement. No ROM or download language.
- **No device frames or Pixel branding.** Play advises against device imagery, "Pixel" is a Google trademark, and Galaxy Store only allows official Samsung device art. Screens are shown as rounded panels instead.
- **Play limits.** Phone captions stay well under 20% of the image. Large-screen sets carry no added text, because Play can crop it on large-screen surfaces. Screenshots and the feature graphic are 24-bit PNG with no alpha. Title 29/30, short description 77/80, full description about 2,900/4,000 characters, release notes 389/500. No "free", "best", "#1" or "new" claims.
- **Galaxy Store limits.** At most 2:1, 320 to 3,840 px, so the near-square inner-screen captures go in as they are. Title, short description (28/40 bytes) and description are in bytes.
- **Plain language.** Copy says what the app does, literally.

## Apple App Store

`app-store/` holds name, subtitle, keywords, promotional text, description and URLs written to Apple's limits (name 29/30, subtitle 26/30, keywords 98/100 bytes, promotional text 152/170). There are **no App Store screenshots**, on purpose:

- This repository builds an Android app only. Apple requires screenshots of the iOS app in use (Guideline 2.3.3), and rejects Android imagery or platform names in iOS metadata (2.3.10). Android renders cannot be reused.
- An iOS version would need, from its own build: iPhone 6.9" (1320 x 2868) and 6.3" (1206 x 2622), iPad 13" (2064 x 2752) if it runs on iPad, and iPhone Duo inner (2853 x 2007) and outer (2034 x 1398) screens for the foldable badge. Apple says Duo screenshots become required in April 2027.
- Guideline 4.7 allows retro console and PC emulators. EmuUI offers no game downloads, so the 4.7.1 to 4.7.5 rules for offered software would not apply; say so in the review notes.
- A privacy policy URL is required and does not exist yet.

## Launch blockers outside this folder

- **Target SDK.** Since 31 August 2026, new Google Play apps and updates must target Android 16 (API 36); an extension runs to 1 November 2026. EmuUI targets API 35. Moving to 36 means compileSdk 36, a newer Android Gradle Plugin and new large-screen orientation rules. The manifest now declares `android:appCategory="game"`, which (unlike `android:isGame`) is what exempts games from Android 16's large-screen orientation override.
- **Release signing and a privacy policy** are not in this repository.
- **Play testing track.** Personal developer accounts created after 13 November 2023 must run a closed test with at least 12 testers for 14 consecutive days before production access.
- **Device verification.** This candidate has not run on a physical foldable; see `docs/QA.md`.
