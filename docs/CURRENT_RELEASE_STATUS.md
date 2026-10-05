# Current release status — arm64 and billing preparation

This supersedes earlier four-ABI and pricing checkpoints. Branch remains `release/play-preparation-20261005`; original source e46ef62f2801556912cdf6d86caf0d5282483f6e. No public push, main promotion, Play write, account creation, new key or agreement acceptance.

Approved first-release scope: arm64-v8a only. Omit Snes9x, Genesis Plus GX, FBNeo and MAME2003+ after bounded replacement assessment. DS and all other retained arm64 cores remain. Preserve save files; no conversion/deletion. See FIRST_PAID_RELEASE_SCOPE.md for the exact unavailable systems and listing corrections.

Billing preparation implements official Play SDK client, pure entitlement reconciliation, monthly offer/renewal disclosure, lifetime precedence, restore, pending/revoked states, subscription management after gifts and save export independent of entitlement. Products remain unconfirmed, identity absent, verifier unconfigured and real checkout disabled. Gameplay paywall is not activated or wired across processes. See BILLING_CLIENT_PREPARATION.md; do not claim subscription launch is complete.

Next owner/backend decision: identify an existing approved backend/auth project or authorize a specific service's setup, terms/costs and secure credentials. Then implement authenticated Play verification/acknowledgment, production entitlement transport, reviewer identity and account/data lifecycle. Confirm actual Console product/base-plan IDs and choose the lifetime product price. Do not share publisher credentials or create a universal bypass.

Source/license blockers remain for other retained cores: Citra dirty-source delta, WonderSwan revision, exact binary/source provenance and PPSSPP assets/static dependency obligations. Additional system removal is not authorized. New Billing SDK POM declares Android SDK License; final notices/license compatibility and privacy review remain necessary. Public privacy/Data safety must describe covers, optional Drive, billing/backend and user-selected save-export providers accurately.

Signing remains an owner-controlled handoff as described in OWNER_SIGNING_HANDOFF.md. Full DS gameplay/disk-save restoration, physical foldable, Play split delivery, production OAuth and live purchases/refunds/gifts remain unverified. Prior actual16KB native and NES/UI evidence is retained, with scope explicitly distinguished from this frontend revision.

Library upload is still blocked by `Library prepare_uploads is not available`; no output Library IDs/versions were created or alternate transfer routes attempted.

## Final validation checkpoint

- PASS: final production bundle and release lint, 0 errors, 45 warnings and 4 hints.
- PASS: 220 app unit tests, no failures/errors/skips (19 billing/export tests added).
- PASS: two distinct instrumented billing-preparation tests on actual API36/16KB emulator: disabled checkout/restore UI and subscription-independent real save-document picker. Final screenshot inspected; system-bar contrast corrected. These are not live billing tests.
- PASS: bundletool validation, PAGE_ALIGNMENT_16K, unsigned AAB. Exactly18 arm64 native entries, zero empty files, sixteen retained cores plus renderer and AndroidX. All retained native bytes match the previously actual16KB-tested build. Bundled QA AAR contains exactly16 arm64 cores.
- AAB: 43739588 bytes; SHA256 `93ae73c9510bc1265111209970171fbab36b0ec45ed8243afe4715df150ecefa`.
- Retained failed attempts: initial daemon/file-read stalls and incomplete generated Dagger outputs; recovered by preserving the generated app build directory and regenerating. The first instrumentation build needed an uncached official AndroidX dependency. One contrast retest initially lacked its test package after snapshot restart; cold boot plus reinstalling both QA packages passed. These failures are not hidden or counted as passes.
- UNRUN: live purchase/restore/gift/redemption/acknowledgment/refund/backend, production gameplay paywall, new minified billing-screen runtime, physical/independent DS gameplay/save checks, Play-delivered splits and production OAuth. Unit verifier fakes do not substitute for server/Play testing. SAF destination writing is covered by ZIP unit tests plus real picker UI; no end-to-end cloud-provider export is claimed.

The isolated emulator is stopped. Existing debug signer reused, no uninstall. No new signing credentials, accounts, Console products, agreements or paid services created.
