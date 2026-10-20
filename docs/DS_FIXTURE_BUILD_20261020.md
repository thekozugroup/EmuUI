# Original Nintendo DS fixture build — 20 October 2026

The existing original legacy DS fixture built successfully in the cloud, outside Git and without the user's laptop. Generated ROM: `EmuUI_DS_Legacy_QA.nds`, 107,008 bytes, SHA256 `d6878d2da479aa4aa68d7338faa178f51dbc29c06d5507e9c358c1cbb3c1ea3b`. This is a new build of the existing QA source; it is not claimed byte-identical to a historical fixture or runtime-tested merely because it compiled.

## Toolchain recovery

Exact official `pkg.devkitpro.org` package downloads failed with a network tunnel 403 before any package bytes arrived. Those requests were stopped. The authorized alternative used the official [devkitPro SDK image](https://github.com/devkitPro/docker), extracting only its SDK files into the cloud workspace. No image commands, package manager, system-wide installation or new agreement acceptance occurred.

- Official `devkitpro/devkitarm` Linux amd64 image manifest: `sha256:15b79ce75822c289538d8153da5fa7aafe5e6adc32ad8a575a197beca0f0761b`
- Verified SDK layer: `sha256:3c5026e520aeec9e208399f6d0a62098d5ee4c13d8cca97cdb9adbc6bb56a1c0`, 215,133,357 bytes
- Installed image metadata: devkitARM r68-1; GCC 16.1.0-1; binutils 2.46.0-1; newlib 4.6.0.20260123-5; rules 1.6.0-4; CRT 1.2.6-1; ndstool 2.3.1-1; general-tools 1.4.4-1. These versions match the earlier toolchain inventory; the retrieved image layer is not asserted to equal the original package archives.
- libnds built from official [v1.8.3 source](https://github.com/devkitPro/libnds/tree/v1.8.3), commit `04ff07cd2ac0f3f0c13adc7d54fd2c5b850341d8`, with `make -j1 release` and the upstream `make include/nds/libversion.h` target. No tracked source patch. That tag's generated internal version string reads 1.8.0; its existing source was preserved.

The first fixture attempt failed because the release-only libnds target had not generated `nds/libversion.h`. Running the upstream header target recovered the build; the failed log is retained. The repository's unchanged `qa/homebrew/ds/build-legacy.sh`, `main.c`, adapted ARM7 service and artwork-free header generator then built successfully.

## Verified host checks

All 14 existing header unit tests passed. Generated-header validation passed for `####` homebrew classification, artwork-free logo CRC `0xCF56`, header CRC `0x33CE`, ARM segment bounds and entry points. Two builds into different output directories produced byte-identical ROMs. Neither link map references Calico.

Evidence is in the cloud handoff under `evidence/ds-fixture/`; local test content and original sources are under `test-content/ds-original/`. Full toolchain/image files remain only in `/workspace/tooling/devkitpro-ds-legacy/`. The manifest and provenance record input/source/tool/library hashes and recovered attempts.

## Runtime and licensing limits

The runtime agent received the fixture path and hash. No Android DS loading, controls, stylus, save/load or lifecycle result is established by these build checks. Use the distinct upper/lower displays and frozen `(state, frame, X, Y, touches)` tuple described in the [fixture README](../qa/homebrew/ds/README.md). The fixture is silent and has no guest battery-save logic; it cannot establish DS audio or SRAM persistence.

Original ARM9 code, graphics and wrappers are CC0; the adapted ARM7 code retains its zlib notice. Linked libnds, CRT, newlib and GCC runtime retain their separate notices/terms described in that README and the handoff NOTICE. No proprietary BIOS, firmware or vendor boot logo is included. Generated content stays outside the app and public source repository.
