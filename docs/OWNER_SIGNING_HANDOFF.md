> Billing update: [PLAY_ONLY_BILLING.md](PLAY_ONLY_BILLING.md) supersedes earlier mandatory backend/app-account proposals and gift-only lifetime scope. The current choice is Play-only monthly plus purchasable lifetime. Signing ownership restrictions still apply.

# Owner signing handoff — 2026-10-05

Target: Kōzu Digital developer 6549433501192319912, package `com.kozudigital.emuui`. Release remains unsigned. Scoped repository inspection found no release signing configuration or keystore; this does not establish whether the owner has an existing key elsewhere or an existing Console enrollment. Only existing debug-keystore metadata was inspected. No private key was read, created, exported or uploaded for this handoff.

## Concrete owner actions required before signing

1. Confirm the package's existing Play App Signing state and whether an upload key already exists. Provide its public certificate fingerprint and secure local location through an approved secret-handling channel, not the key or password in chat. Existing signing identity must be preserved.
2. For a genuinely new app, choose Google-managed app signing or an owner-supplied signing key if same-key distribution across stores is required. Current Play help describes automatic quantum-ready hybrid signing for new apps; review the actual Console choice before the first open-testing/production rollout. A Google-generated private app signing key cannot be downloaded later.
3. If no upload key exists, explicitly authorize creation of a separate upload key in owner-controlled storage outside the repo/artifact tree, with encrypted backup and designated custodians. Approve the storage location and signing configuration separately. Do not use the debug key. No key-generation command has been run.
4. Review any actual Play App Signing terms/security prompts in the authenticated Console and authorize the specific displayed action. This document does not accept terms; their authenticated text was not available locally. Existing-key transfer, if chosen, requires its own approved PEPK workflow.
5. After technical/commercial gates close, sign the exact reviewed AAB with the approved upload identity; verify certificate, package, version, digest and signature. Parent may then perform the authorized track upload. No release should be promoted solely because signing passes.
6. Register only public distribution signing fingerprints with API providers such as Google OAuth. Upload/debug fingerprints are not substitutes. Hybrid signing can require multiple distribution fingerprints; use the actual Console certificates. Test Drive sign-in on a Play-delivered build before claiming it works.

Never put keystores, passwords, private keys or credentials in source, Library uploads, evidence ZIPs or chat. This handoff deliberately contains no secret values. Debug signers differ between machines; no uninstall or private-data migration is authorized. Existing debug saves must be preserved.

Sources checked 2026-10-05: https://developer.android.com/studio/publish/app-signing and https://support.google.com/googleplay/android-developer/answer/9842756 .
