# EmuUI

A landscape-first Android emulation handheld for foldables. Native Kotlin and Jetpack Compose, with native [Material 3 Expressive components](docs/MATERIAL3_EXPRESSIVE.md) and the emulation foundations of [Lemuroid](https://github.com/Swordfish90/Lemuroid).

The upper pane is the display. The lower pane is your library or controller. On devices reporting a horizontal folding feature, the interface leaves the hinge clear. Flat devices use a two-pane handheld layout. Portrait and vertical-fold postures require rotation.

## Development preview

This is an early, independently branded GPL-3.0 fork. Device support is subject to the [verification record](docs/QA.md). Physical foldable behavior and any particular phone model are not certified by a successful build alone. You must provide your own lawful games; ROMs and BIOS files are not included.

## Build

Install JDK 17 or a compatible Gradle-supported JDK, Android SDK Platform 35, and build-tools 34.0.0. Review and accept the Android SDK terms yourself. Then:

```sh
git clone --recurse-submodules https://github.com/thekozugroup/EmuUI.git
cd EmuUI
# Set ANDROID_HOME or local.properties to your installed SDK.
./gradlew :lemuroid-app:assembleFreeDynamicDebug
./gradlew :lemuroid-app:testFreeDynamicDebugUnitTest :lemuroid-app:lintFreeDynamicDebug
```

### Building a source ZIP

A source ZIP does not contain Git submodule contents. Before running Gradle, retrieve the pinned official core checkout into the empty `lemuroid-cores` directory:

```sh
git init lemuroid-cores
git -C lemuroid-cores remote add origin https://github.com/Swordfish90/LemuroidCores.git
git -C lemuroid-cores fetch --depth 1 origin fee2e824525daa22bcf318f96127fe43fa8a15ad
git -C lemuroid-cores checkout --detach FETCH_HEAD
```

This downloads upstream emulator binaries and their notices separately. See the component provenance and source links before redistributing them.

The dynamic debug APK downloads the selected upstream emulator core on demand. It uses a locally generated debug key; no signing keys are stored here. The package ID is `com.thekozugroup.emuui.debug`, so it can coexist with Lemuroid. Release signing is intentionally left to the distributor.

The unchanged library metadata is stored losslessly as small gzip pieces. Gradle reconstructs and hash-checks the ordinary SQLite asset automatically, offline. See [metadata packaging](lemuroid-metadata-libretro-db/README.md).

## CI preview downloads

Successful Android checks publish a standard GitHub Actions artifact named `emuui-preview-<source commit>`, containing the APK, SHA-256 checksum, GPL license and exact source link. GitHub sign-in may be required to download artifacts, and their retention is 90 days. This is a development artifact, not a formal GitHub Release or production-signed build.

CI rebuilds from the public source commit. Its APK is not claimed byte-identical to the locally tested build documented in [QA](docs/QA.md). Debug signing keys can differ between builds, so in-place upgrades are not guaranteed. Do not remove an installation containing saves without a verified backup.

## Licenses and provenance

See [COPYING](COPYING) and [provenance](docs/PROVENANCE.md). The upstream Lemuroid source and notices are preserved. Emulator cores retain their individual licenses and source obligations; the frontend's GPL license does not replace them. No association with Nintendo, Google or the Lemuroid maintainers is implied.
