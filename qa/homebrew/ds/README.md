# Original Nintendo DS QA fixture

A small original homebrew program for testing native dual-screen rendering, DS touch input, controller state, pause/resume, and emulator save-state restoration. It is test software, not a commercial game. The original ARM9 C source, hand-authored bitmap glyphs, and build wrappers are dedicated to the public domain under [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/). No warranty is provided.

No commercial ROM, BIOS, firmware, boot logo, icon, or external game assets are included. The original header logo field contains 154 zero bytes plus a generated two-byte CRC adjustment; it contains no artwork. This gives the genuine conventional DS-format logo CRC16 0xCF56. A literal 156-byte zero field gives CRC16 0xE155; the earlier retail-shaped EUIQ fixture was rejected on that check. Correctly classified #### homebrew is exempt from the fixed-checksum check, so no proprietary logo or native BIOS is required. This fixture is intended for direct-boot homebrew emulation, not boot-ROM authentication on an unmodified retail console.

The fixture uses the conventional `####` homebrew game code. melonDS identifies a modern 0x4000-header image as homebrew using that code; a retail-shaped custom code would incorrectly send this original unencrypted program through secure-area detection. The reserved ARM9 startup area is left untouched. In the exact runtime core, correctly classified homebrew also bypasses the native-BIOS requirement for encrypted retail images; see its [console configuration checks](https://github.com/JesseTG/melonds-ds/blob/v1.2.0/src/libretro/config/console.cpp#L255-L264). The core's [load checks](https://github.com/JesseTG/melonds-ds/blob/v1.2.0/src/libretro/config/console.cpp#L255-L264) require native BIOS for an encrypted-looking image only when it is not classified as homebrew. melonDS DS v1.2.0 pins that upstream revision in its [dependency manifest](https://github.com/JesseTG/melonds-ds/blob/v1.2.0/cmake/FetchDependencies.cmake). See [melonDS header classification](https://github.com/JesseTG/melonDS/blob/f6692df/src/NDS_Header.h#L209-L212).

## Build for the pinned Android core (legacy runtime)

The distributed melonDS DS 1.2.0 core pins melonDS `f6692df` from 2024-11-07. That predates [Calico compatibility fixes](https://github.com/melonDS-emu/melonDS/commit/584508230f) added on 2024-11-17 (byte-wide IPCSYNC and CP15 Trace Process ID support). The [devkitPro maintainer describes the resulting white-screen behavior](https://github.com/devkitPro/nds-examples/issues/18). The first Calico fixture and official hbmenu 0.11 both loaded but stayed white in the API 29 Android test, so the legacy build keeps the original ARM9 test program while removing that runtime dependency.

Use the official [libnds 1.8.3 archive](https://pkg.devkitpro.org/packages/libnds-1.8.3-1-any.pkg.tar.zst), extracted separately from an existing modern SDK. Point `LIBNDS_LEGACY` at its `opt/devkitpro/libnds` directory:

```sh
DEVKITPRO=/path/to/devkitpro LIBNDS_LEGACY=/path/to/legacy/libnds \
  bash ./qa/homebrew/ds/build-legacy.sh /outside/repository/ds-legacy-output
```

This emits `EmuUI_DS_Legacy_QA.nds` and its manifest. It uses the same `main.c`, legacy devkitARM CRT, libnds 1.8.3, and `arm7-legacy.c`. The latter is a clearly marked adaptation of devkitPro's licensed pre-Calico ARM7 template; its original notice is retained. It serves touch/input and system FIFO without network/audio service dependencies. `legacy-toolchain-provenance.json` records exact official inputs and source hashes. No Calico symbols appear in either link map. Successful compilation and deterministic output remain host checks, not native gameplay evidence.

## Build with current Calico runtime

Install the free official [devkitPro Nintendo DS toolchain](https://devkitpro.org/wiki/Getting_Started). Set `DEVKITPRO` to its root and optionally `DEVKITARM` if it differs from `$DEVKITPRO/devkitARM`. Python 3 generates and verifies the artwork-free header field, both header checksums, ARM segment bounds/entry points, and hash manifest.

```sh
DEVKITPRO=/path/to/devkitpro ./qa/homebrew/ds/build.sh /outside/repository/ds-qa-output
```

The output directory must be outside the public repository. It contains `EmuUI_DS_Homebrew_QA.nds`, intermediate files, and `EmuUI_DS_Homebrew_QA.manifest.json`. ROMs, toolchains and compiled outputs remain local-only. The build enables `-Wall -Wextra -Werror`. The header generator has fourteen host-side regression tests: run `python3 -m unittest discover -s qa/homebrew/ds -p test_header.py`. Independent rebuilds must be checked for byte-identical ROMs after changing the header generator.

`toolchain-provenance.json` records the exact official packages and SHA-256 values used for the first build. The packages were downloaded from the devkitPro package repository and each hash checked against its repository database. Extraction was confined to the cloud QA workspace. These hashes pin the build inputs; they do not by themselves prove runtime correctness.

## Expected display and controls

- Upper screen: purple background, `EMUUI DS QA`, `TOP DISPLAY`, initial `STATE 000017`, and a live frame count
- Lower screen: teal grid, `BOTTOM TOUCH`, initial coordinates `X 128 Y 112`, `TOUCHES 0`, and a yellow crosshair
- A: increment state by 1 on each press
- B: increment state by 10 on each press
- X: reset state to 17
- Start: freeze/unfreeze the frame counter, allowing exact save/load comparisons
- D-pad: move the lower-screen crosshair by two DS pixels per held frame
- Touch: place the crosshair at the exact DS touch position, display X/Y, and increment `TOUCHES` once per contact. The crosshair is pink while pressed and yellow when released

Touch input remains active while the frame counter is frozen. The word `FROZEN` refers only to that counter, not to emulation pause.

## Suggested native emulator assertions

1. Import and launch the ROM. Verify both distinct screens and that the frame count advances.
2. Press A twice and B once. Verify state advances from 17 to 29. A long held press must still increment only once.
3. Use the D-pad and verify the crosshair position changes.
4. Touch the lower DS screen near its center and near opposite corners. Verify printed coordinates correspond to the touched positions and the contact count advances once per contact. A touch on the top display must not incorrectly map to the bottom display.
5. Freeze the counter with Start. Record the frame count, state, touch count and position. Save an emulator state, then mutate state with B and change the touch position. Load the saved state and verify all four recorded values are restored exactly.
6. Unfreeze and verify execution resumes. Repeat save/load and pause/resume once to expose stale state or repeated-action bugs.

Do not count the host-side design preview or successful compilation as a native DS emulator pass. Native Android results and screenshots belong in the main QA report.

## Linked software and provenance

The current-runtime build calls the unmodified official [libnds](https://github.com/devkitPro/libnds/tree/v2.0.2) API (zlib license) and links the unmodified [Calico 1.2.0](https://github.com/devkitPro/calico/tree/v1.2.0) runtime and its standard ARM7 service executable (Zope Public License 2.1). The C runtime is [newlib](https://sourceware.org/newlib/COPYING.NEWLIB), whose component files have their own permissive notices. GCC/libgcc retains its upstream license and [runtime-library exception](https://www.gnu.org/licenses/gcc-exception-3.1.html). These third-party components are not re-licensed as CC0. Retain their applicable notices if redistributing a compiled build.

The GNU compiler/binutils and [ndstool](https://github.com/devkitPro/ndstool) are build tools with their own upstream licenses; they are not copied into the public repository. The downloaded `default-arm7` package was not used: the build explicitly uses Calico's `ds7_maine.elf`.

Technical API references: [libnds input](https://github.com/devkitPro/libnds/blob/v2.0.2/include/nds/arm9/input.h), [libnds background API](https://github.com/devkitPro/libnds/blob/v2.0.2/include/nds/arm9/background.h), and [official ARM9 build template](https://github.com/devkitPro/nds-examples/blob/master/templates/arm9/Makefile). The ARM9 fixture implementation, graphics, and build wrappers were written for this test. The legacy ARM7 service is the separately attributed adaptation described above. Third-party startup/runtime sources retain their own licenses; they are not re-licensed as CC0.
