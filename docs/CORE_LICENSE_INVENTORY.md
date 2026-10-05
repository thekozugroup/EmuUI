# Core license inventory — preliminary, not clearance

Binary repository: Swordfish90/LemuroidCores, exact pin
`fee2e824525daa22bcf318f96127fe43fa8a15ad`. All twenty Play modules are listed below.
The pin contains binaries, manifests, Gradle scripts and an update script; no root/core
license texts, corresponding source revision ledger or complete static dependency ledger
were found. `update_cores.ipy` fetches mutable buildbot `nightly/android/latest` URLs.
Thus the license families below are upstream project guidance, **not** verified licenses
for each exact binary. Every row remains blocked for exact-source/license clearance.

| Module | Upstream project license family | Commercial distribution decision |
| --- | --- | --- |
| desmume | GPLv2 | Source/dependencies/notices unresolved |
| dosbox_pure | GPLv2 | Source/dependencies/notices unresolved |
| fbneo | Custom noncommercial | Permission or removal/replacement required for monetization |
| fceumm | GPLv2 | Source/dependencies/notices unresolved |
| gambatte | GPLv2 | Source/dependencies/notices unresolved |
| genesis_plus_gx | Custom noncommercial, mixed components | Permission or removal/replacement required for monetization |
| handy | zlib | Source/dependencies/notices unresolved |
| mame2003_plus | MAME noncommercial | Permission or removal/replacement required for monetization |
| mednafen_ngp | GPLv2 | Source/dependencies/notices unresolved |
| mednafen_pce_fast | GPLv2 | Source/dependencies/notices unresolved |
| mednafen_wswan | GPLv2 | Source/dependencies/notices unresolved |
| melonds | GPLv3 family; updater maps melonDS DS to melonds | Exact implementation varies by ABI; source/dependencies unresolved |
| mgba | MPLv2 | Source/dependencies/notices unresolved |
| mupen64plus_next_gles3 | GPLv3 family, plugin dependencies | Source/dependencies/notices unresolved |
| pcsx_rearmed | GPLv2 | Source/dependencies/notices unresolved |
| ppsspp | GPLv2, third-party assets/components | Source/dependencies/assets/notices unresolved |
| prosystem | GPLv2 | Source/dependencies/notices unresolved |
| snes9x | Custom noncommercial | Permission or removal/replacement required for monetization |
| stella | GPLv2 | Source/dependencies/notices unresolved |
| citra | GPLv2 | Source/dependencies/notices unresolved; renderer already rejects 3DS |

Sources: [Libretro maintained license index](https://docs.libretro.com/development/licenses/),
[FBNeo maintained license guidance](https://docs.libretro.com/library/fbneo/#license-and-changelog),
[Genesis Plus GX license](https://github.com/libretro/Genesis-Plus-GX/blob/master/LICENSE.txt),
[MAME2003-Plus license](https://github.com/libretro/mame2003-plus-libretro/blob/master/LICENSE.md),
[Snes9x license](https://github.com/snes9xgit/snes9x/blob/master/LICENSE).

Ads, ad-removal purchases and subscriptions are commercial even if downloading the app
is free. An ad-free initial release does not itself satisfy copyright notices or source
obligations. Do not remove cores silently: removing Snes9x loses current SNES support;
Genesis Plus GX affects supported Sega systems; MAME2003+ and FBNeo affect arcade coverage.
Replacement cores can change compatibility and invalidate existing save states. Obtain a
decision on these tradeoffs and preserve original saves before any migration.

Frontend GPL-3.0 source and notices remain. Modified in-tree LibretroDroid is GPL-3.0-or-later;
Oboe is Apache-2.0; vendored libretro-common retains per-file notices. AndroidX/Compose,
Google Play libraries and Drive libraries also require a final resolved dependency/license
inventory. Google service SDK terms and OAuth setup are separate from core licensing.

To complete this ledger, obtain each exact source revision plus submodule/dependency pins,
patches, compiler/linker flags, license texts, notices and a reproducible rebuild or
verified provenance attestation linking that source to the distributed ABI hashes. Do not
substitute current upstream HEAD licenses for the missing exact-binary evidence.

## Observed arm64 runtime metadata

All twenty cores loaded and returned libretro API 1 on the isolated API36/4KB emulator.
These self-reported hashes are leads, not verified corresponding-source attestations;
Citra explicitly reports a dirty build. No ROM, BIOS or game initialization was used.

```text
Android API36 arm64 emulator, PAGE_SIZE=4096. dlopen/API metadata only, NOT gameplay or 16KB compatibility.
libcitra_libretro_android.so: exit=0 LOAD PASS api=1 name=Citra version=5263fae33-dirty
libdesmume_libretro_android.so: exit=0 LOAD PASS api=1 name=DeSmuME version=git 7f05a8d4
libdosbox_pure_libretro_android.so: exit=0 LOAD PASS api=1 name=DOSBox-pure version=1.0-preview3
libfbneo_libretro_android.so: exit=0 LOAD PASS api=1 name=FinalBurn Neo version=v1.0.0.03  56ab07bab
libfceumm_libretro_android.so: exit=0 LOAD PASS api=1 name=FCEUmm version=(SVN) 5cd4a43
libgambatte_libretro_android.so: exit=0 LOAD PASS api=1 name=Gambatte version=v0.5.0-netlink b752252
libgenesis_plus_gx_libretro_android.so: exit=0 LOAD PASS api=1 name=Genesis Plus GX version=v1.7.4 cecccacf
libhandy_libretro_android.so: exit=0 LOAD PASS api=1 name=Handy version=0.97 fca2392
libmame2003_plus_libretro_android.so: exit=0 LOAD PASS api=1 name=MAME 2003-Plus version= 870e8ba3
libmednafen_ngp_libretro_android.so: exit=0 LOAD PASS api=1 name=Beetle NeoPop version=v1.29.0.0 139fe34
libmednafen_pce_fast_libretro_android.so: exit=0 LOAD PASS api=1 name=Beetle PCE Fast version=v1.31.0.0 d5c2b28
libmednafen_wswan_libretro_android.so: exit=0 LOAD PASS api=1 name=Beetle WonderSwan version=v0.9.35.1
libmelonds_libretro_android.so: exit=0 LOAD PASS api=1 name=melonDS DS version=1.2.0
libmgba_libretro_android.so: exit=0 LOAD PASS api=1 name=mGBA version=0.11-dev c758314a6
libmupen64plus_next_gles3_libretro_android.so: exit=0 LOAD PASS api=1 name=Mupen64Plus-Next version=2.8-Vulkan 222acbd
libpcsx_rearmed_libretro_android.so: exit=0 LOAD PASS api=1 name=PCSX-ReARMed version=r25 228c14e1
libppsspp_libretro_android.so: exit=0 LOAD PASS api=1 name=PPSSPP version=v1.19.3-987-geab2cc85bc
libprosystem_libretro_android.so: exit=0 LOAD PASS api=1 name=ProSystem version=1.3e acae250
libsnes9x_libretro_android.so: exit=0 LOAD PASS api=1 name=Snes9x version=1.63 5a40cd55
libstella_libretro_android.so: exit=0 LOAD PASS api=1 name=Stella version=8.0_pre 749a21f65
```
