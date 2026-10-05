# Latest checkpoint — source candidates and production gate

This section supersedes the older checkpoints below. See SOURCE_CANDIDATES_AND_GATE_QA.md for exact source pins, probe limits, asset gaps and failed attempts.

- PASS: final production bundle, lint (0 errors, 45 warnings, 4 hints), 227 unit tests (0 failures/errors/skips); bundletool validation, 16 KB APK alignment, exactly 18 arm64 native entries unchanged from the previously tested baseline. Both new font license files verified inside the unsigned bundle.
- PASS: final minified billing UI and directly invoked real GameLauncher entitlement gate on actual API36/16384-byte pages. Found and fixed a lifecycle cancellation/dialog crash. Full ExternalGameLauncherActivity startup was not passed because startup work remained pending. No purchase was simulated as a real Play transaction.
- PASS: separate source-built WonderSwan, PPSSPP plus all five FFmpeg static dependencies, and Citra candidates. All three pass ELF/RELRO alignment and actual 16 KB dlopen/metadata probes. They are not in this AAB. Exact old source gaps remain until candidate adoption or old-builder evidence.
- Font notices now cover Ume and Source Han Sans based on upstream history. Remaining runtime PSP asset provenance is specifically 52 files plus two .git pointer artifacts; source/notices for adopted dependencies still need final closure.
- Current unsigned AAB: 43801677 bytes, SHA256 `78b711c2205d2959b9732b0a643fee5e78f5fef04d3d2b684e401546a680a643`. Filename `EmuUI-0.2.1-source-audit-gate-fix-unsigned-REVIEW-BLOCKED.aab`.
- UNRUN: replacement-core gameplay/save interoperability, clean extracted-source rebuild, full external launch startup, live Play billing/refunds/restore, Play splits, production OAuth and independent current DS/physical foldable QA. User-reported DS save success is preserved and is not relabeled as independently tested here.
- Remaining decisions/configuration: lifetime price; Console product/base-plan/purchase-option IDs and public licensing key; owner signing/agreements; privacy/Drive choices and listing approval. No additional systems removed. No public upload/push, Console writes, new credentials, software installation or agreement acceptance.
- Final emulator stopped after screenshots. Library upload remains unavailable; no output IDs/versions and no alternate transfer.

## Historical checkpoints

# Current release status — arm64 and billing preparation

This supersedes earlier four-ABI and pricing checkpoints. Branch remains `release/play-preparation-20261005`; original source e46ef62f2801556912cdf6d86caf0d5282483f6e. No public push, main promotion, Play write, account creation, new key or agreement acceptance.

Approved first-release scope: arm64-v8a only. Omit Snes9x, Genesis Plus GX, FBNeo and MAME2003+ after bounded replacement assessment. DS and all other retained arm64 cores remain. Preserve save files; no conversion/deletion. See FIRST_PAID_RELEASE_SCOPE.md for the exact unavailable systems and listing corrections.

Billing now uses the Play account only, signed local receipt verification, client acknowledgment, monthly/lifetime checkout, restore, pending handling, subscription management and save export. Production Play game launches require verified ownership; running games and saves are not interrupted. Public Console configuration remains absent, so checkout is disabled and release launches fail closed. Debug native QA remains accessible. Current validation results are recorded below after the new build; prior checkpoint is historical.

Current billing decision: Google Play only, US$0.99/month plus a normally purchasable lifetime product (price pending). No backend or EmuUI login required. See PLAY_ONLY_BILLING.md for local signature verification, acknowledgment, production launch gate, configuration and test limits. Actual Console product/base-plan/purchase-option IDs and the public licensing key remain needed. Do not share publisher credentials or create a universal bypass.

Source/license blockers remain for other retained cores: Citra dirty-source delta, WonderSwan revision, exact binary/source provenance and PPSSPP assets/static dependency obligations. Additional system removal is not authorized. New Billing SDK POM declares Android SDK License; final notices/license compatibility and privacy review remain necessary. Public privacy/Data safety must describe covers, optional Drive, Play Billing and user-selected save-export providers accurately.

Signing remains an owner-controlled handoff as described in OWNER_SIGNING_HANDOFF.md. Full DS gameplay/disk-save restoration, physical foldable, Play split delivery, production OAuth and live purchases/refunds/gifts remain unverified. Prior actual16KB native and NES/UI evidence is retained, with scope explicitly distinguished from this frontend revision.

Library upload is still blocked by `Library prepare_uploads is not available`; no output Library IDs/versions were created or alternate transfer routes attempted.

## Current Play-only validation

- PASS: production bundle, release lint (0 errors, 45 warnings, 4 hints), 227 app unit tests (0 failures/errors/skips).
- PASS: two billing UI instrumentation tests on API36 with actual 16384-byte pages: both unconfigured checkout buttons disabled, restore available, save export opens real DocumentsUI. Screenshot inspected. These do not perform real purchases.
- PASS: bundletool validation and PAGE_ALIGNMENT_16K. Exactly 18 nonempty arm64 native entries, including both DS cores; 16-core bundled QA AAR. All retained native bytes exactly match the previously runtime-tested baseline. AAB remains unsigned.
- Current AAB: 43795576 bytes, SHA256 `465019d8029745bdd53108ce44d5b27d9f07e457c526366402d38832d9d78259`.
- FIXED/RECOVERED: initial nullable offer-token compilation error. Two subsequent builds stalled in native file reads of numbered duplicate generated XML/class files. Affected files/directories were moved to preserved workspace backups and regenerated; final build passed in 1m35s. No source/user-data deletion. A first native-scope command used an incorrect QA AAR path; corrected path passed.
- UNRUN: live Play purchase/acknowledgment/restore/gift/refund/lifecycle, multi-account/offline tests, minified production launch gate, Play-delivered splits, production OAuth, independent DS gameplay/disk-save and physical-device QA. Prior native/NES evidence is unchanged; no new gameplay pass is claimed for this frontend revision.
- Emulator stopped after screenshot capture. Debug packages updated with `install -r`; no uninstall, new keys, accounts, agreements or services.

## Previous validation checkpoint (ff32e4e; before Play-only revision)

- PASS: final production bundle and release lint, 0 errors, 45 warnings and 4 hints.
- PASS: 220 app unit tests, no failures/errors/skips (19 billing/export tests added).
- PASS: two distinct instrumented billing-preparation tests on actual API36/16KB emulator: disabled checkout/restore UI and subscription-independent real save-document picker. Final screenshot inspected; system-bar contrast corrected. These are not live billing tests.
- PASS: bundletool validation, PAGE_ALIGNMENT_16K, unsigned AAB. Exactly18 arm64 native entries, zero empty files, sixteen retained cores plus renderer and AndroidX. All retained native bytes match the previously actual16KB-tested build. Bundled QA AAR contains exactly16 arm64 cores.
- AAB: 43739588 bytes; SHA256 `93ae73c9510bc1265111209970171fbab36b0ec45ed8243afe4715df150ecefa`.
- Retained failed attempts: initial daemon/file-read stalls and incomplete generated Dagger outputs; recovered by preserving the generated app build directory and regenerating. The first instrumentation build needed an uncached official AndroidX dependency. One contrast retest initially lacked its test package after snapshot restart; cold boot plus reinstalling both QA packages passed. These failures are not hidden or counted as passes.
- UNRUN: live purchase/restore/gift/redemption/acknowledgment/refund/backend, production gameplay paywall, new minified billing-screen runtime, physical/independent DS gameplay/save checks, Play-delivered splits and production OAuth. Unit verifier fakes do not substitute for server/Play testing. SAF destination writing is covered by ZIP unit tests plus real picker UI; no end-to-end cloud-provider export is claimed.

The isolated emulator is stopped. Existing debug signer reused, no uninstall. No new signing credentials, accounts, Console products, agreements or paid services created.
