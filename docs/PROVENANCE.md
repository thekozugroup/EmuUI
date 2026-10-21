# Source and licensing provenance

EmuUI is a modified distribution of Lemuroid, not an official Lemuroid release.

- Lemuroid upstream: https://github.com/Swordfish90/Lemuroid
- Reviewed source revision: `53752bf29bc3f95c50f6c38f70cd4a53450a7098`
- Upstream license: GNU General Public License version 3, reproduced in `COPYING`
- Original Lemuroid and Retrograde notices and source package names are retained
- EmuUI modifications (2026-10-03): landscape handheld library, fold-aware viewports and input regions, original EmuUI visual identity and documentation
- EmuUI modifications (2026-10-21): Material 3 Expressive redesign, blur and shadow primitives, clamshell-foldable orientation handling, store listing assets and copy

The first EmuUI commit uses a clean source snapshot. Upstream history, its shared debug signing key, and the three commercial-game promotional screenshots are deliberately excluded. No ROMs, BIOS files, user saves or private signing credentials are included.

## Emulator components

`lemuroid-cores` remains an upstream Git submodule pinned at `fee2e824525daa22bcf318f96127fe43fa8a15ad` from https://github.com/Swordfish90/LemuroidCores. It contains separately licensed emulator cores and build metadata. This pin must not be represented as transferring ownership or changing those component licenses. Core names, download tag `1.17.0`, and source references are retained. The dynamic download tag `1.17.0` resolves to commit `1194b7dcc6208f8dd62b55b66753fde7838dbc91`. The x86_64 FCEUmm, melonDS and DeSmuME binaries in that tag were downloaded independently and SHA-256 matched byte-for-byte to the pinned submodule binaries used for local QA. Consult each linked core's license and complete corresponding-source requirements before distributing a binary containing it. Some cores have noncommercial or other additional restrictions; the GPL label for the frontend must not be read as a single license covering every core.

LibretroDroid is maintained separately at https://github.com/Swordfish90/LibretroDroid. The app retains its upstream dependency `0.13.2` (tag commit `0ebd299624bfd51a0a1336dd0a2c56fe7ddbc0e3`) and attribution.

## Distribution

Provide corresponding source for the exact EmuUI APK revision, build instructions, modified source, dependency versions and applicable notices with every distribution. Development APKs are unsigned release outputs or locally generated debug-signed test builds. Production signing material belongs outside this repository. No Android SDK packages or proprietary platform images are redistributed here. Full GPL-3.0 and Apache-2.0 texts plus key component notices are packaged under APK assets/licenses; original embedded notices are retained.

Lawful QA content must have explicit permission or be original homebrew. Never publish commercial ROMs, BIOS files, or screenshots containing unlicensed game content.

## Store assets

Store screenshots under `store/` are renders of EmuUI's own Compose UI made by `StoreScreenshots` (a Robolectric unit test that is skipped unless requested). Every game shown is an invented title with EmuUI's original procedural placeholder art, and the in-game frame is original pixel art drawn in that test. No commercial game content, console logos or device imagery is used. Robolectric and its dependencies are test-only and are not distributed in any APK. Marketing text uses the Inter, Inter Display and JetBrains Mono fonts (SIL Open Font License) at composition time; the fonts themselves are not redistributed.
