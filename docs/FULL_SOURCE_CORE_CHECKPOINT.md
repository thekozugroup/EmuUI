> FREE RELEASE UPDATE: [FREE_RELEASE_CHECKPOINT.md](FREE_RELEASE_CHECKPOINT.md) supersedes all earlier monetization, purchase-gate and payment-profile requirements below.

# Full source-core candidate

This supersedes the earlier three-core checkpoint. All sixteen retained emulator cores are now source-built, hash-locked replacements in Play modules and bundled QA. The pinned upstream submodule remains untouched. Only the four previously approved restricted cores are omitted; no further system, save format or minimum-Android change is intended. Package remains com.kozudigital.emuui, arm64/API23 minimum, target36.

`qa/remaining-core-source-builds.json` records the thirteen new pins and build locations. Exact build commands, link maps, source locks, dependencies and modified source are in `remaining-core-source-candidates-v2/*-api23-sources.tar.gz`. Existing cleaned publication archives cover WonderSwan, PPSSPP/FFmpeg, Citra and PSP assets. Stage `Source-built-release-native-inputs-v2.zip` with `qa/stage_source_release_inputs.py`; sixteen binary hashes and the bundled PSP asset hash are checked. Never substitute earlier native-input archives for this revision.

## Targeted changes

- DeSmuME's slot1 initialization guard survived shutdown while its deleted pointers remained. A second init/deinit aborted in both old and initially rebuilt binaries. The patch resets both slot initialization guards and device pointers, and clears deleted slot1 entries. Two cycles now pass. Serialization code is unchanged; DS gameplay and save compatibility are still unverified independently.
- melonDS's direct-network enumeration used API24 getifaddrs. API23 uses the existing BSD-licensed Android implementation from the pinned Citra source (origin and hashes accompany the archive), with a platform-conditional include and static source addition. No Android-floor increase. Two initialization cycles pass; this is not a network-functionality or DS gameplay certification.
- Gambatte explicitly builds as C++14 to retain its upstream auto_ptr code. Mupen uses the installed LLVM strings utility and GLES3 build flag. PCSX's arm64 recipe selects Ari64/NEON; the x86 Lightrec path is not selected. Pinned libpicofe is included. Unused gnulib autotools submodule contents were not needed by these Android recipes.
- NDK27.3 builds use max/common page size16384; exported binaries are stripped with llvm-strip --strip-unneeded. No tools or licenses were newly installed/accepted.

## Evidence and limits

Production bundle/build, 227 unit tests and lint pass (0 errors,45 warnings,4 hints). Exact AAB/QA native-scope verification passes: eighteen arm64 libraries, sixteen locked cores, two unchanged renderer libraries. All eighteen exact AAB libraries pass actual API36/16384-page loading. All core ELF LOAD and RELRO boundaries pass; unchanged androidx.graphics.path has a conservative RELRO-end review flag but its actual16KB dlopen passes. Standalone loading is not rendering/gameplay certification.

All thirteen source exports pass recorded archive checksums, path-safety and private-key-marker scans. Exported source preserves upstream headers and standalone license files; 89 additional notice records accompany the app. Full clean rebuilds from exported archives remain UNRUN, as does an exhaustive legal review of every dependency/header-only license. The source package is prepared, not publicly published. Publish corresponding modified sources, dependencies, build instructions and applicable GPL/LGPL installation/relinking material before binary distribution. No blanket relicensing is asserted.

FCEUmm old/new compatibility passes all six fixture cases: save old/resume old and new; save new/resume new and old. Serialized states are13726bytes and batteryRAM8192bytes;60-frame image checksums match. The original CC0 NES fixture's battery variant changes only the iNES battery flag. This does not prove compatibility for all games. Existing user saves were not read, converted or deleted.

No lawful gameplay fixtures are available here for the other replaced systems. Their gameplay and prior-state compatibility remain UNRUN; preserve the owner's separate DS-save success report. Latest physical DS/foldable checks, Play-delivered splits, real purchase/restore/refund/gifts and production OAuth remain UNRUN. The existing 3DS UI limitation remains. Console work is handled separately by the cloud worker; no local Console interaction, credential, key, agreement or account change was made.

Pricing remains US$0.99/month and US$19.99 lifetime using localized Play prices. Production stays fail-closed until actual public Play product configuration is supplied. Signing, reviewer access, privacy/listing and live Play testing remain launch gates. This is not a launch-ready assertion.

Final app QA: twelve instrumented regressions PASS, plus real NES homebrew gameplay with two Activity recreations PASS. Fresh gameplay screenshots inspected; these are QA captures, not Play-delivery proof. APK16KB zip alignment passes.

Unsigned AAB: EmuUI-0.2.1-full-source-unsigned-REVIEW-BLOCKED.aab; 52442627 bytes; SHA256 `1c629f928ae3c7a432230145a9b2aed4058ec1cb03d2a8a29ec024a717be95d6`.

Full minified ExternalGameLauncherActivity startup PASS without reflection or ownership injection; unconfigured production purchase gate remains closed. Final Git publication scan passes; no binary, key, game fixture or private browser data is staged.
