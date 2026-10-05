# Remaining 18 cores — actionable corresponding-source audit

Research date: 2026-10-05. No replacement binary was integrated, no system removed,
no tool installed and no 16 KB image approval assumed. This is source research, not
legal clearance or a claim of binary reproducibility.

17 immutable source **candidates** resolved: 15 clean embedded hash leads, one Citra
base hash explicitly marked dirty, and one DOSBox version-tag candidate. WonderSwan
has no revision lead. All 18 existing binary provenance gates remain open: a matching
short hash does not establish patches, compiler flags, dependency linkage or other ABIs.

The evidence archive contains fetched license/build files at these exact revisions,
complete source-tree path inventories, and a machine-readable ledger. No ROM/BIOS/font
binaries were downloaded in this research. Root license identification is not a complete
per-file dependency audit; source-build link maps must establish what ships.

## Common action, independent of emulator approval

For each retained core: fetch a complete source checkout at the selected full commit;
resolve required nested gitlinks; keep vendored notices; rebuild using approved NDK27.3;
record command, options, patches, output hashes and actual link map; then package exact
corresponding sources and notices for the NEW binary. Alternatively obtain the original
builder's full provenance/source offer if retaining the old bytes. Only runtime testing
of the resulting replacement waits for the 16 KB image; source reconstruction does not.
Do not use generic current-upstream links or a license-family label as a substitute.

## NDK reconstruction template (UNRUN for these 18)

All 17 resolved trees expose an Android libretro ndk-build entry. The per-core jni path
below identifies it. Retain its Application.mk settings and source feature defaults,
except documented release adjustments. Use absolute paths and run from the source's
expected project directory; some upstream makefiles use relative paths. Citra's recipe
also calls a shader-generation target and PPSSPP requires separate FFmpeg reconstruction.

```sh
"$ANDROID_NDK_ROOT/ndk-build" \
 NDK_PROJECT_PATH="$PROJECT_DIR" \
 APP_BUILD_SCRIPT="$SOURCE/$JNI_PATH/Android.mk" \
 NDK_APPLICATION_MK="$SOURCE/$JNI_PATH/Application.mk" \
 APP_ABI=arm64-v8a APP_PLATFORM=android-24 \
 APP_LDFLAGS='-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384' \
 NDK_OUT="$OUTPUT/obj" NDK_LIBS_OUT="$OUTPUT/lib" -j6
```

This is a grounded starting recipe, **not a proven reproducible build**. NDK27.3,
CMake3.31.5/Ninja, Git, Python and macOS make are already available. Do not install a
missing generator/compiler silently. Preserve the core's language/STL settings; where
STL is static, its notices also apply. Keep original binaries and save data intact.

## citra

Source: [libretro/citra](https://github.com/libretro/citra/tree/5263fae3344e5e9af43036e0e38bec2d10fb2407); revision `5263fae3344e5e9af43036e0e38bec2d10fb2407`.

License evidence: GPLv2 text; component exceptions. Android entry: `jni`.

Dependencies/assets: 31 pinned submodules: dynarmic, Boost, Crypto++, LibreSSL, FAAD2, fmt, Vulkan/shader components and others. The Android recipe explicitly links dynarmic, faad, ssl and cpufeatures. Desktop theme licenses are not evidence those themes are shipped.

Missing evidence / next action: Existing runtime explicitly says dirty. Obtain the exact patch set, complete selected dependency sources, shader-generation inputs and original build options. A clean-base rebuild would be a new candidate, not reconstruction of the shipped binary. Three non-arm64 files are empty; arm64-only recipe is explicit.

## desmume

Source: [libretro/desmume](https://github.com/libretro/desmume/tree/7f05a8d447b00acd9e0798aee97b4f72eb505ef9); revision `7f05a8d447b00acd9e0798aee97b4f72eb505ef9`.

License evidence: GPLv2 text; mixed utilities. Android entry: `desmume/src/frontend/libretro/jni`.

Dependencies/assets: Android recipe enables libfat and uses vendored libretro-common. AsmJit COPYING was collected; Cocoa font SIL OFL and Windows unrar restrictions belong to other frontends unless a link map proves inclusion. Do not blanket-apply their terms to Android.

Missing evidence / next action: Reconstruct the libretro-only source list and preserve libfat/libretro-common per-file notices; obtain original JIT choices. The recipe enables ARM JIT only for arm and x86 JIT only for x86, not arm64.

## dosbox_pure

Source: [schellingb/dosbox-pure](https://github.com/schellingb/dosbox-pure/tree/d743cfd9342c083ef6d3cecfc08c9af28a1481c8); revision `d743cfd9342c083ef6d3cecfc08c9af28a1481c8`.

License evidence: GPLv2-or-later in Android recipe. Android entry: `jni`.

Dependencies/assets: No git submodules in the resolved tag. DOSBox CPU/dynamic-recompiler sources and vendored libretro-common are in-tree. Preserve their headers and any embedded font/data notices, rather than assuming the root GPL labels every asset.

Missing evidence / next action: The binary provides only 1.0-preview3, not a commit. The tag is a candidate, not proof. Obtain builder commit/patch/config record or explicitly choose a source-built replacement at the recorded tag commit.

## fbneo

Source: [libretro/FBNeo](https://github.com/libretro/FBNeo/tree/56ab07bab7ba385d49e7da3324f68fc597a51e49); revision `56ab07bab7ba385d49e7da3324f68fc597a51e49`.

License evidence: Custom noncommercial, old MAME and mixed libraries. Android entry: `src/burner/libretro/jni`.

Dependencies/assets: src/license.txt is the operative full license; root LICENSE.md points to mutable upstream. It requires full verbatim license, public changes, no monetary profit and no donations for projects using the code. It lists old MAME, Final Burn, zlib, LZMA, libspng, dr_libs, Xbyak, scaler code and per-file exceptions. dr_libs and uGUI texts were collected.

Missing evidence / next action: Preserve full src/license.txt and all selected driver/library notices; audit Android source list for additional per-file restrictions. Establish that launch/business use satisfies noncommercial terms; no assumption that zero app price alone settles that question.

## gambatte

Source: [libretro/gambatte-libretro](https://github.com/libretro/gambatte-libretro/tree/b75225203ffea8b65124bb31acb598e91e7f22d9); revision `b75225203ffea8b65124bb31acb598e91e7f22d9`.

License evidence: GPLv2 text; vendored libretro files. Android entry: `libgambatte/libretro/jni`.

Dependencies/assets: Core plus libretro-common and palette/input code are vendored, with no git submodules at this commit. Root COPYING and Android build settings are available.

Missing evidence / next action: Archive full tracked core/wrapper sources and per-file notices; recover exact build flags and compare save serialization. Embedded hash does not identify builder patches or flags.

## genesis_plus_gx

Source: [libretro/Genesis-Plus-GX](https://github.com/libretro/Genesis-Plus-GX/tree/cecccacf767b1c8e86af3e315223b052a7f81b95); revision `cecccacf767b1c8e86af3e315223b052a7f81b95`.

License evidence: Custom noncommercial; LGPL and permissive components. Android entry: `libretro/jni`.

Dependencies/assets: Root license requires notices and, for modified distributions, complete source including used components. Android recipe enables CHD. Collected NTSC LGPL2.1, libchdr, minimp3, Tremor, LZMA, Zstd and FLAC texts. Wii-specific wiidrc is not automatically an Android dependency.

Missing evidence / next action: Retain linked CHD/audio/NTSC notices and their source; document use/noncommercial assessment and actual compile features. Resolve exact statically linked compression sources rather than only external ELF dependencies.

## handy

Source: [libretro/libretro-handy](https://github.com/libretro/libretro-handy/tree/fca239207e9c111da3e85d2faf0b1b9d7524e498); revision `fca239207e9c111da3e85d2faf0b1b9d7524e498`.

License evidence: zlib-style Handy license; wrapper per-file terms. Android entry: `jni`.

Dependencies/assets: lynx/license.txt expressly permits commercial use subject to origin/alteration/notice conditions. Android links platform zlib (-lz). Full core and wrapper notices remain relevant.

Missing evidence / next action: Root core is not blanket GPL. Preserve Handy notice and inspect compiled wrapper/header terms; provide source mapping/build flags for replacement verification and save compatibility.

## mame2003_plus

Source: [libretro/mame2003-plus-libretro](https://github.com/libretro/mame2003-plus-libretro/tree/870e8ba3fa4e6635e2eb9d85c939589498659c32); revision `870e8ba3fa4e6635e2eb9d85c939589498659c32`.

License evidence: MAME0.78 noncommercial and individual-file exceptions. Android entry: `jni`.

Dependencies/assets: LICENSE.md includes source availability, derivative-work naming/behavior restrictions, ROM distribution conditions and contributor acknowledgments. Android uses vendored libretro-common; numerous CPU/sound/driver files have their own terms.

Missing evidence / next action: Preserve full license and all compiled per-file notices, publish corresponding derivative source and verify noncommercial use. Identify any disallowed derivative changes in the pinned source; do not assume a known upstream core name alone proves this.

## mednafen_ngp

Source: [libretro/beetle-ngp-libretro](https://github.com/libretro/beetle-ngp-libretro/tree/139fe34c8dfc5585d6ee1793a7902bca79d544de); revision `139fe34c8dfc5585d6ee1793a7902bca79d544de`.

License evidence: GPLv2 text; Mednafen/NeoPop and per-file support code. Android entry: `jni`.

Dependencies/assets: Vendored Mednafen NGP/CPU/sound and libretro-common; root COPYING available. No git submodule entries. Per-file support-code terms still need to be carried into a complete source bundle.

Missing evidence / next action: Recover original options and full tracked sources; preserve CPU/audio/wrapper headers. Exact packaged ABI provenance remains missing despite the arm64 hash lead.

## mednafen_pce_fast

Source: [libretro/beetle-pce-fast-libretro](https://github.com/libretro/beetle-pce-fast-libretro/tree/d5c2b28ee6931ae43a4a79455937693ae8ccc8a1); revision `d5c2b28ee6931ae43a4a79455937693ae8ccc8a1`.

License evidence: GPLv2 text; codec/compression dependencies. Android entry: `jni`.

Dependencies/assets: Android recipe enables CHD. Collected libchdr, LZMA, Zstd and Tremor license texts; zlib1.2.11 is vendored. libretro-common and Mednafen CD/audio sources are part of build inputs.

Missing evidence / next action: Bundle exact vendored CHD/codec/compression sources and per-file notices, record selected feature flags and original compiler options. Do not treat platform NEEDED inventory as covering these static dependencies.

## mednafen_wswan

Source: [libretro/beetle-wswan-libretro](https://github.com/libretro/beetle-wswan-libretro); revision `UNRESOLVED`.

License evidence: Preliminary GPLv2 family; exact revision unresolved. Android entry: `not selected`.

Dependencies/assets: Runtime reports v0.9.35.1 only. The preliminary upstream project is libretro/beetle-wswan-libretro; no immutable source selected or current HEAD substituted.

Missing evidence / next action: Obtain the builder source commit, dirty patch set and build recipe for every ABI. If unavailable, choose an explicit reviewed replacement revision and test save compatibility; current source licenses cannot certify the old binary.

## mgba

Source: [libretro/mgba](https://github.com/libretro/mgba/tree/c758314a639aa0066e7b65a8341448181b73c804); revision `c758314a639aa0066e7b65a8341448181b73c804`.

License evidence: MPL2.0 plus component terms. Android entry: `libretro-build/jni`.

Dependencies/assets: Android sets MINIMAL_CORE=2 and DISABLE_THREADING. It includes in-tree zlib headers; collected MPL2.0, inih, libpng, RapidJSON and other notices. Qt/Discord/shader notices are not automatically linked in minimal libretro. Source tree also contains cinema/test content; do not package those as app games.

Missing evidence / next action: Map the exact minimal source list and MPL-covered files, publish corresponding source/modifications as required, and include actual selected dependency notices. No full source archive containing test-ROM data was downloaded here.

## mupen64plus_next_gles3

Source: [libretro/mupen64plus-libretro-nx](https://github.com/libretro/mupen64plus-libretro-nx/tree/222acbd3f98391458a047874d0372fe78e14fe94); revision `222acbd3f98391458a047874d0372fe78e14fe94`.

License evidence: GPLv2 root/core/GLideN64; mixed plugin terms. Android entry: `libretro/jni`.

Dependencies/assets: Corrects preliminary GPLv3-family label. GPLv2 is explicit in root LICENSE, core LICENSES and GLideN64/LICENSE. paraLLEl RSP provides LGPL/MIT texts, lightning LGPL, plugin dependencies and shader/scaler notices are separate. One gnulib gitlink is pinned. Android arm64 enables aarch64 dynarec, LLE, parallel RSP/RDP and threaded AL defaults; GLES3=1 selects -lGLESv3.

Missing evidence / next action: Lock exact GLES3/plugin options and all selected sources; account for LGPL static-link obligations where relevant, not merely copying a license. Recipe references aarch64-linux-android-strings; map to installed llvm-strings through a documented build adjustment if required. Resolve nested gnulib URL/source and original configs.

## pcsx_rearmed

Source: [libretro/pcsx_rearmed](https://github.com/libretro/pcsx_rearmed/tree/228c14e10e9a8fae0ead8adf30daad2cdd8655b9); revision `228c14e10e9a8fae0ead8adf30daad2cdd8655b9`.

License evidence: GPLv2 core; LGPL lightrec/lightning and compression terms. Android entry: `jni`.

Dependencies/assets: Two gitlinks: frontend/libpicofe and lightning/gnulib. Collected lightrec Library GPLv2, lightning LGPL texts, libchdr, LZMA24.05, zlib1.3.1, Zstd1.5.6. Android enables CHD and platform zlib/log. Actual dynarec selection determines which LGPL code is linked.

Missing evidence / next action: Record dynarec selection and link map; retain actual static-library source/notices and applicable relinking obligations. Obtain complete nested sources and original options. Do not bundle user PlayStation BIOS.

## ppsspp

Source: [hrydgard/ppsspp](https://github.com/hrydgard/ppsspp/tree/eab2cc85bca530d47873c2015457cfa754a0115d); revision `eab2cc85bca530d47873c2015457cfa754a0115d`.

License evidence: GPLv2-or-later project; PSPSDK BSD notice and many components. Android entry: `libretro/jni`.

Dependencies/assets: 22 pinned gitlinks include FFmpeg, libretro-common, shader compilers, compression, fonts and debugger. The Android libretro recipe consumes PREBUILT libavformat/libavcodec/libavutil/libswresample/libswscale archives; a top-level clone is not a source rebuild of those archives. Root LICENSE.TXT includes PSPSDK BSD and GPL. Separate runtime assets use tag1.15, not this core revision.

Missing evidence / next action: Recover/rebuild FFmpeg archive sources and configure flags (GPL/LGPL/nonfree choices), dependency closure and asset-license mapping. Runtime asset ZIP has18PGF fonts plus other assets; only debugger JS license comments are included as explicit license files. Establish font/image/shader source, authors and permitted redistribution separately. Do not claim core source license clears downloaded assets.

## prosystem

Source: [libretro/prosystem-libretro](https://github.com/libretro/prosystem-libretro/tree/acae250da8d98b8b9707cd499e2a0bf6d8500652); revision `acae250da8d98b8b9707cd499e2a0bf6d8500652`.

License evidence: GPLv2 core; zlib-style BupBoop. Android entry: `jni`.

Dependencies/assets: Root License.txt GPLv2; bupboop/License.txt is zlib-style. Coretone support and vendored libretro-common are in-tree; Android links platform zlib.

Missing evidence / next action: Retain GPL source and BupBoop/coretone/wrapper notices, record actual source list/options, and verify new binary serialization. Existing hash alone does not clear all ABIs.

## snes9x

Source: [libretro/snes9x](https://github.com/libretro/snes9x/tree/5a40cd5514e63e691e39141d64267798357a1424); revision `5a40cd5514e63e691e39141d64267798357a1424`.

License evidence: Custom noncommercial; explicit component exceptions. Android entry: `libretro/jni`.

Dependencies/assets: LICENSE prohibits commercial use and contains libretro contributors statement that commercial rights will not be granted. It identifies JMA GPL/LGPL, snes_ntsc LGPL and xBRZ GPLv3 with project-specific exceptions. Six submodules include desktop renderer/Windows libraries, not necessarily Android dependencies.

Missing evidence / next action: Preserve full LICENSE and compiled component notices/exceptions. Verify Android source selection and avoid treating all desktop submodules as shipped. A future monetized build cannot assume permission will be available; replacement would change compatibility/saves and requires a decision.

## stella

Source: [libretro/stella](https://github.com/libretro/stella/tree/749a21f653cf85fcedf4fe514ac8df1ad308be8e); revision `749a21f653cf85fcedf4fe514ac8df1ad308be8e`.

License evidence: GPLv2-or-later in Copyright.txt; support-library terms. Android entry: `src/os/libretro/jni`.

Dependencies/assets: Android recipe uses C++20, SOUND_SUPPORT and c++_static. Copyright.txt explicitly GPLv2-or-later; collected JSON MIT and httplib licenses. Desktop GUI/network dependencies may be excluded by libretro source list; test/roms is not app content.

Missing evidence / next action: Archive actual libretro core sources/notices, exclude unrelated test ROMs from app artifacts, recover original options and compare state compatibility. No third-party runtime tools are required merely to inspect source.

## Shared dependency and asset evidence

The first pass recorded 61 top-level submodule URL/revision pairs from available root
.gitmodules files; Mupen's nested gnulib gitlink is an additional record in its tree.
50 of those 61 had a root license retrieved at the pinned revision. The detailed JSON
lists the 11 not resolved by this bounded root-file search. This is NOT evidence that
those projects lack licenses. Some are platform-only; others carry per-file notices or
nonstandard license paths. Resolve actual Android linkage before deciding obligations.

PPSSPP's separate downloaded assets ZIP (tag1.15) contains debugger MIT notice comments,
not a comprehensive font/art/shader license bundle. Do not replace it with an unrelated
newer source snapshot. Existing runtime ZIP hashes and per-file inventory remain in the
prior evidence; source tag and assets tag are distinct provenance chains.

Free/ad-free distribution still invokes source and notice obligations. FBNeo additionally
prohibits donations for projects using its code; Genesis Plus GX and MAME derivative
conditions require source and notices; Snes9x has explicit noncommercial restrictions.
The parent's pricing/use decision remains separate from this audit. No permission requests
were sent to upstream authors and no public source publication occurred.

Prioritization: reconstruct the small no-submodule cores first (Gambatte, Handy, NGP,
ProSystem, Stella), then other vendored cores; isolate source/config reconstruction for
PPSSPP and N64. Citra's dirty diff and WonderSwan's unidentified revision require original
builder evidence or an explicitly selected replacement. These are evidence gaps, not
reasons to invent hashes or remove supported systems without authorization.

## Additional PPSSPP binary evidence

Read-only strings from the actual packaged arm64 PPSSPP core expose FFmpeg3.0.2,
five self-reported LGPL2.1-or-later library notices, and the configure command. It
records Windows NDK27.2.12479018, Android21, static libraries, network disabled, and
an explicit codec/demuxer list. Neither --enable-gpl nor --enable-nonfree appears
in that recovered command. This narrows the configuration gap but does not prove
source identity or absence of downstream patches. The exact command is preserved
in `ppsspp-ffmpeg-embedded-config.txt`.

The pinned FFmpeg repository now has its four Android build scripts and all four
GPL/LGPL license variants captured. Adapt host tool paths to the existing macOS
NDK27.3 in a separately recorded patch; do not install the historical Windows NDK.
Build all five static libraries from the selected source, preserve the final configure
output/link map and provide corresponding FFmpeg source and required LGPL materials.
