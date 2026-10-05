# Shortest compliant submission path — follow-up


Current commercial direction (2026-10-05): US$0.99/month, ad-free, with permanent full-access gifts through unique codes. See COMMERCIAL_RELEASE_DECISIONS.md and OWNER_SIGNING_HANDOFF.md. Earlier free-launch proposals are superseded.

**Latest update:** see `ACTUAL_16KB_RESULTS.md`. The approved image is installed;
actual arm64 loading, scoped UI/NES tests and release startup now pass on 16,384-byte pages.
Earlier pending/unrun statements below describe the prior checkpoint.

The owner reports that DS saves worked on their device. Record this as user-reported
PASS with build/device unspecified; do not present it as independent testing of this
release candidate. Additional DS fixture compiler installation and physical QA are
deferred, rather than treated as mandatory submission blockers by themselves.

## Priority 1: obtain actual 16 KB evidence

Approve only `system-images;android-36;google_apis_ps16k;arm64-v8a`, revision 7, and a
separate workspace AVD. The installed emulator is 37.1.11; the image requires >=35.4.9.
Official package metadata:
https://dl.google.com/android/repository/sys-img/google_apis/sys-img2-3.xml
Official image:
https://dl.google.com/android/repository/sys-img/google_apis/arm64-v8a-ps16k-36_r07.zip
Download: 1,875,087,037 bytes; official SHA1 dda632745023567113d9954101709c6526a3c250.
License ID: android-sdk-arm-dbt-license, Android Software Development Kit License Agreement.
Exact package license is retained in `evidence/16kb-image-license.txt`; public terms:
https://developer.android.com/studio/terms
Install scope: existing SDK `system-images/android-36/google_apis_ps16k/arm64-v8a` only;
AVD/cache scope: this task workspace. Reserve approximately 8 GiB for image and AVD.
No security settings, physical devices, accounts, signing keys or SDK upgrades are required.
Nothing was installed or accepted during this follow-up.

Test with `getconf PAGE_SIZE` confirming 16384; run the native load probe against every
packaged arm64 library, then launcher and lawful NES gameplay/recreation. Preserve errors.
Do not rely on compatibility mode or change security settings to manufacture a pass.
The official guidance now explicitly describes the GNU_RELRO endpoint criterion. Existing
binaries violate it, but an observed runtime crash has not yet been demonstrated here.
https://developer.android.com/guide/practices/page-sizes

## Priority 2: source and notices even for free/ad-free distribution

Free distribution is not an exemption from GPL corresponding-source requirements.
FCEUmm's exact `Copying`, section 3, requires source or a qualifying source offer alongside
object-code distribution; a generic link to mutable upstream HEAD is insufficient.
https://github.com/libretro/libretro-fceumm/blob/5cd4a43e16a7f3cd35628d481c347a0a98cfdfa2/Copying

Use an explicit source-based rebuild ledger: full commit IDs, vendored/submodule sources,
patches, NDK/build options, output hashes, complete license texts and applicable notices.
Prepare downloadable corresponding source for the actual released frontend, renderer and
cores; publication requires the owner's release action. A source snapshot for one core
is not clearance of the other 19 or the current binary bundle.

Current isolated candidates (not substituted into the app):
- FCEUmm: full commit 5cd4a43e16a7f3cd35628d481c347a0a98cfdfa2, matching the existing
  arm64 binary's self-reported short hash. Built with installed NDK 27.3.13750724,
  android-24, arm64-v8a, both max/common-page-size=16384 flags. Build passed; both static
  LOAD and RELRO checks passed. Output SHA256
  700ea8151bada9185c776696bbc3e521bdf9da8b8dfebf59ccc4d8cf3db4dff4.
  Runtime/save compatibility is untested; existing binary provenance is not retroactively proved.
- melonDS DS v1.2.0: full commit 33c48260402865ef77667487528efd5ca7ce1233. Eleven normal
  build dependencies resolved to full commits in `melonds-resolved-source-lock.json`.
  Source build PASSED; LOAD and RELRO endpoint checks PASSED. New binary SHA256 `1e9b5c5cc165b7fb60049e3bde35613a27eb018a75edd782cbdd25648a8363dc`. Runtime/save compatibility remains untested. Source generates a 120,544-byte combined attribution file. Its linked components include
  melonDS, libretro-common, libslirp, GLM, fmt, pntr, yamc, span-lite, zlib and embedded
  graphics/free BIOS implementation sources; preserve all applicable notices. The official
  source manifest is https://github.com/JesseTG/melonds-ds/blob/v1.2.0/cmake/FetchDependencies.cmake.

No core has been removed, replaced or publicly pushed. Rebuilds at matching revisions
minimize change but still need save-state compatibility checks before integration.

## Priority 3: choose supported scope, not silent exclusions

Recommended: preserve arm64 DS and existing systems, rebuilding the pinned sources and
fixing only demonstrated compatibility issues. This is more work than a reduced launch,
but avoids silently losing the app's advertised core functions.

For a smaller release, the owner can approve arm64-only support. This excludes all 32-bit
phones and x86 devices. It eliminates the four empty non-arm64 placeholders and the two
32-bit melonDS LOAD failures, but does NOT fix arm64 RELRO issues or license obligations.
Per-module ABI exclusions require matching unavailable-system UI and Play delivery tests;
otherwise users can select a system whose core cannot be installed.

A NES-only source-built release is a narrower possible route, but removes DS and all other
systems and is not recommended without explicit scope approval. Removing the four
noncommercial families (Snes9x, Genesis Plus GX, MAME2003+, FBNeo) would remove current
SNES/Sega/arcade support. Do not remove these solely because the app is free; evaluate exact
license conditions and chosen commercial model. Ads, subscriptions or paid unlocks require
permission or compatible replacements for restrictive components. Replacement cores may
invalidate existing save states.

AndroidX graphics-path 1.0.1 is also flagged. The inspected official stable 1.1.0 AAR still
fails the same RELRO endpoint predicate on all four ABIs. A blind version bump is not a
verified fix; runtime validation or an audited source rebuild is required.
https://developer.android.com/jetpack/androidx/releases/graphics

## Deferred DS toolchain decision

The public EmuUI release exposes APK/source/QA/checksum assets, not a compiled DS fixture;
none was found in the supplied review pack. Existing original CC0 fixture sources remain
in `qa/homebrew/ds`; historical compiler provenance is Linux x86_64 and cannot run on this Mac.
If additional independent DS tests become necessary, approve a separately specified macOS
build environment: official devkitARM GCC/binutils/newlib, devkitarm rules/legacy CRT,
ndstool and libnds 1.8.3. Legacy fixture avoids the pinned core's old Calico compatibility
issue. Component licenses include GPL tool licenses, GCC runtime exception, newlib notices,
MPL CRT and zlib libnds/ARM7 notices. Do not treat original fixture CC0 as relicensing those.
Official macOS installer discovered (not downloaded/installed):
https://github.com/devkitPro/pacman/releases/download/v6.0.2/devkitpro-pacman-installer.pkg
65,370,714 bytes. It is a system installer, so NOT approved by the image-only request above.
Package repository listing returned HTTP403; a bounded isolated macOS toolchain package
set/size is not yet verified, and no URL guess or access bypass was attempted.

## Final account gates remain

Implement the agreed subscription/gift model after commercial clearance; finalize privacy/Data safety/Drive choices,
owner signing and Play App Signing handoff, then signed Play test delivery. No fabricated
attestations or publication while these are unresolved. Library upload remains blocked;
do not switch the already attempted upload to another route.
