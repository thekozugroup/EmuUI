# Source candidates and minified production gate — latest checkpoint

Three NEW arm64/API23 core candidates built successfully using already installed NDK 27.3, CMake 3.31.5 and macOS tools. No software, agreement, key or service was installed/created. Candidates are separate deliverables, **not integrated into the release AAB** and not claimed to reproduce the old binaries.

| Candidate | Exact source | Result / remaining distinction |
|---|---|---|
| WonderSwan | libretro/beetle-wswan-libretro 4b01295838ea89e3f1355bbe4cb5cf98aa6108cd | Builds; metadata v0.9.35.1. This does not identify the old binary revision. |
| PPSSPP | hrydgard/ppsspp eab2cc85bca530d47873c2015457cfa754a0115d | Builds with exceptions enabled, no RTTI; version explicitly recorded. All five FFmpeg static libraries rebuilt from hrydgard/ppsspp-ffmpeg 654c7da2ccffc7fd147abbe809aad7ba9e50b0a4. GPL/nonfree/version3 configure flags are zero. |
| Citra | libretro/citra 5263fae3344e5e9af43036e0e38bec2d10fb2407 + recorded patch | Maintained CMake build passes after Android GNU strerror_r and Android log-link fixes. Old dirty-tree delta remains unknown; this new candidate has explicit source identity. Runtime version UNKNOWN reflects archive checkout. |

PASS for each: all ELF LOAD segments align to 16384, RELRO end aligns to 16384, actual API36/16384-page dlopen and libretro metadata query. These probes do not initialize or play games. UNRUN: new-candidate gameplay, existing save/state compatibility, API23 runtime, extracted-archive clean rebuild and Play split delivery. No additional system removal. Practical next choice: qualify and adopt these source-built replacements, or obtain exact old corresponding source/build deltas from the original builder. Publishing source and notices must accompany whichever binaries ship; source candidate archives alone do not cure the current AAB's old provenance gaps.

Archives include dependency pins/hashes, license-file inventories, actual linker maps, patches, build recipe and binary hashes. A dependency being fetched does not mean it linked; maps identify linked components. Full license compatibility/notice closure for the actual adopted replacement remains a release gate. Preserve LGPL source/relinking obligations for static components as applicable; do not blanket relicense dependencies under the frontend GPL.

## PSP runtime assets

All 18 PGF files byte-match the pinned upstream PPSSPP tree. Upstream history before dc34bea8d4de's compatibility-name change identifies 16 Latin files as Ume Hy Gothic/Ume P Mincho and jpn0/kr0 as Source Han Sans. Added Ume's permissive license and Adobe's historical SIL OFL 1.1 copyright/license to packaged assets and the HTML notice resource. This is history-based provenance, not an assertion that Sony-named headers are proprietary fonts. Exact original TTF revision remains unidentified; no glyphs changed.

Sources: https://github.com/hrydgard/ppsspp/commit/dc34bea8d4de ; https://github.com/adobe-fonts/source-han-sans/blob/dbe6e6eca603ff6ddf96b027ad1337cb5a06da8c/LICENSE.txt ; upstream Ume license mirrored at https://sources.debian.org/src/fonts-horai-umefont/670-5/license.html/ . Font-specific licenses remain in force.

Runtime ZIP has 139 files. 74 match the PPSSPP v1.15 tree, 11 debugger files match pinned ppsspp-debugger 9776332f720c854ef26f325a0cf9e32c02115a9c. Two .git pointer files are packaging debris. Remaining 52 files (44 translations and 8 shader/config/image files) have not been pinned exactly. Keep this narrow asset-source gap open: resolve the historical archive's source or build and review an asset package from known source, preserving PSP behavior and notices. No silent deletion was made. Debugger JavaScript/shader delivery also requires the existing Play executable-content assessment, rather than assuming all ZIP data is inert.

## Minified release gate

Actual R8-minified production package tested on API36/16KB, signed only with the existing local debug key for emulator QA. Billing page shows two unavailable purchase actions with restore/export available. The harness invokes the actual production GameLauncher entry after inserting a synthetic game row with no ROM contents. This exercises the entitlement gate without substituting entitlement state. It removes only its own fixture row.

Initial full ExternalGameLauncherActivity startup remained waiting on startup work; therefore that full route is NOT passed. Direct gate testing exposed CancellationException being caught as a load error after the activity finished, causing BadTokenException. Fix rethrows lifecycle cancellation and avoids displaying a dialog on a closing/destroyed activity. Retest passes. Live products, purchase/refund/restore timing and normal external startup are still unrun. Source harness and exact results accompany the handoff.

User reports DS saves work; preserve that result. Independent current DS gameplay/disk-save restore and latest physical foldable/cutout checks remain unrun, distinct from prior automated DS/native and NES recreation results. No physical-device action or redundant toolchain setup was performed.
