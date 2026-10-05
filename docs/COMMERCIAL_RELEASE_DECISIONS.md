# Commercial release decisions — 2026-10-05

UPDATE: The user has now authorized replacement assessment or omission of the restricted SNES/Sega/arcade cores. See FIRST_PAID_RELEASE_SCOPE.md for the implemented scope and current validation; earlier approval-pending statements below are historical. ABI narrowing remains unapproved.

Settled user direction: US$0.99/month, ad-free, unique gift codes granting permanent full access. A free-to-download listing does not make paid access noncommercial. No products, prices, codes, billing credentials or agreements have been activated locally.

## Rights gate

The exact-source leads and license evidence in CORE_LICENSE_INVENTORY.md and REMAINING_CORE_SOURCE_AUDIT.md identify commercial restrictions for Snes9x, Genesis Plus GX, MAME2003+ and FBNeo. This candidate must not be commercially distributed assuming frontend GPL licensing overrides those restrictions.

| Core | Decision needed and impact |
| --- | --- |
| Snes9x | Obtain sufficient written permission or approve a commercially compatible SNES replacement. Its license expressly rejects commercial rights for libretro contributions; permission is not a dependable plan. Replacement requires save-state and performance QA. |
| Genesis Plus GX | Obtain permission or approve replacement research covering the actual Sega systems. A Master System/Game Gear core alone does not preserve Mega Drive/CD coverage. |
| MAME2003+ | Obtain permission or approve a modern commercially compatible arcade core. Modern MAME licensing does not relicense the old code; ROM-set and save compatibility differ. |
| FBNeo | Resolve its no-profit restrictions or approve replacement. Existing game and save-state compatibility cannot be assumed. |

No system or ABI removal is authorized or implemented. Options for the owner: rights acquisition where feasible; approve replacement development with explicit compatibility tradeoffs; or explicitly approve affected system exclusions. Modern MAME is a research lead, not an integrated Android replacement: https://www.mamedev.org/legal.html . Per-file obligations and trademarks still apply. No ROM/BIOS rights follow from a core license.

Complete corresponding-source and notice obligations remain independent blockers: Citra dirty changes, WonderSwan source revision, exact ABI provenance, and PPSSPP static dependencies/assets need closure. Root license inventories are not complete notices for every linked dependency.

## Billing design for review

Use a monthly subscription and a separate non-consumable lifetime product (tentative IDs `emuui_monthly`, `emuui_lifetime`; neither created). A verified lifetime purchase or valid subscription grants full access. Never consume the lifetime purchase. Verify and acknowledge purchases, restore INAPP and SUBS separately, and handle pending transactions, refunds, revocation, account changes and offline behavior. No client-only secret-code bypass.

Subscription promo codes provide trials, not permanent ownership. One-time product codes can grant a non-consumable entitlement. The lifetime product needs an active eligible BUY option with valid regional pricing; its price remains an owner decision. A hidden purchase CTA is not proof of a supported promo-only product configuration. Verify redemption before promising gift-only availability. Current limit: 500 non-subscription codes per app per quarter. Creating promotions requires review of Promo codes Terms of Service; no acceptance occurred.

Lifetime redemption does not cancel an existing subscription. Show a clear Manage/Cancel subscription route so a gift recipient can stop renewal; do not silently cancel or promise that it already stopped. Backend selection, security configuration and sandbox purchase/restore/redemption tests remain unimplemented.

Official references: https://developer.android.com/google/play/billing/promo and https://support.google.com/googleplay/android-developer/answer/6321495 .

## Empty native files: tested narrow fix failed

The pinned repository has four zero-byte placeholders: Citra armv7/x86/x86_64 and PPSSPP x86_64. Upstream deliberately replaced PPSSPP x86_64 with an empty file in commit fee2e82. These files are not executable cores.

An exact-path packaging exclusion preserved every real ELF and all modules, but `bundlePlayDynamicRelease` failed: bundletool requires all native feature modules to support the same ABI set. PPSSPP then lacked x86_64 relative to base. Citra would also be inconsistent. Evidence: `evidence/placeholder-packaging-build.txt` in the workspace. The experiment was reverted; no broken exclusion remains in the release branch.

Safe completion requires real compatible source builds for missing combinations, or an explicitly reviewed delivery/support redesign. Do not add fake ELF stubs merely to satisfy bundletool, restore a known noncompliant old binary, or silently narrow the app to arm64. No currently working ABI/system was removed. The unsigned review AAB still contains the four placeholders and remains blocked.

## Reusable reviewer access — proposed, not configured

A single-use family promo code is insufficient. Play requires reusable, continuously available, location-independent access to paid features, with clear English instructions. Source: https://support.google.com/googleplay/android-developer/answer/15748846?hl=en .

Proposed design: a dedicated app-level demo/review identity authenticated by the selected backend, with a durable server-side full-access entitlement. It must exercise the same production features and restrictions as an entitled customer, not a special compliant-only review experience. Credentials must be reusable, not expire during review, and not require publisher intervention/OTP; allow concurrent reviewer sessions and all review locations. Supply only that account's credentials through Console App access. Never share the publisher Google account, embed a universal bypass in the APK, or rely on a one-time code or expiring subscription.

Exact approvals needed: select and authorize the backend/auth service and its terms/costs; approve creation of one isolated demo identity and its permanent review entitlement; approve secure storage of its credentials and entry into Console App access; designate an owner for availability and rotation. No such account or credentials have been created. If Drive requires separate Google authentication, independently approve a dedicated test-only Google identity/data and workable reusable review instructions, or choose another policy-compliant access design. Do not grant access to publisher Drive data. Provide lawful original homebrew fixtures and import instructions so reviewers can exercise gameplay without commercial ROMs. Adding app-level accounts also requires account/deletion/privacy design review before implementation.
