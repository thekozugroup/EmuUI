> Billing update: [PLAY_ONLY_BILLING.md](PLAY_ONLY_BILLING.md) supersedes earlier mandatory backend/app-account proposals and gift-only lifetime scope. The current choice is Play-only monthly plus purchasable lifetime. Signing ownership restrictions still apply.

# Billing client preparation — not activated

Settled model: US$0.99/month, ad-free, with permanent full-access gifts. The client uses official `com.android.billingclient:billing:9.1.0`, the current release listed at https://developer.android.com/google/play/billing/release-notes . No Console product, account, price, credentials or paid service was created. Product identifiers `emuui_monthly` and `emuui_lifetime` remain tentative. The monthly base-plan identifier is deliberately unset and products remain unconfirmed.

## Implemented locally

- Separate SUBS and INAPP ownership queries on refresh/restore and billing update wake-ups. The access activity refreshes when resumed, including returning from an out-of-app code redemption. Client callbacks never grant access directly.
- A testable verification boundary requires authenticated backend reconciliation before access. Pending, suspended, unknown-product and blank-token client records cannot authorize access. No consume API exists. The backend contract requires durable acknowledgment of valid initial purchases; no acknowledgment is sent by this unconfigured client.
- Lifetime precedence; canceled subscriptions retain only their verified paid period. Grace, hold, pause, expiry and revocation are represented explicitly. A newer authoritative snapshot can revoke prior lifetime ownership after refund/revocation. Partial query failures do not fabricate missing purchases or replace verified state.
- In-memory verification leases use elapsed time, not the user's wall clock. Leases are cleared on app-owner change, reject late prior-session results, and are not persisted as a local paid flag. The 24-hour maximum is a defensive bound, not an approved offline-access promise. The backend must choose and enforce the final freshness/offline policy.
- Only a confirmed monthly auto-renewing base plan with a single P1M recurring phase can be offered. Prices come from Play ProductDetails; there is no hard-coded price fallback or invented lifetime price. Requery before checkout and reconfirm changed terms. Display renewal/cancellation disclosure before checkout.
- Permanent gift instructions, restore, and a Google Play subscription-management link. Gift ownership does not cancel renewal; an active renewal alongside lifetime ownership gets an explicit warning. There is no lifetime sales CTA pending the owner's product-price/configuration decision.
- Settings opens an access screen with a separate save export action. Export reads saves, states, previews and legacy states only, stages a ZIP privately, then writes to a user-chosen Storage Access Framework destination. No ROM/BIOS/credentials are included; originals are not deleted or converted. Export does not require an entitlement and asks users to close a running game first.

## Deliberately not activated

Production verifier is `UnconfiguredPurchaseVerifier`, product confirmation is false, monthly base plan is null, and no identity is invented. Checkout is disabled. Test fakes exist only in unit tests. No real purchase, live restore, gift redemption, acknowledgment, refund or backend call has been tested or performed.

The gameplay paywall is **not connected** to GameLauncher/BaseGameActivity. Existing gameplay is left unchanged until authenticated verification and the cross-process access handoff are implemented and tested. The tested access policy distinguishes starting a game from library/save/export/management access, but that is not a claim of a completed production paywall. Do not publish this as the subscription-enabled release.

## Minimal remaining decision and setup

Choose an existing owner-approved backend/auth project, or explicitly authorize setup of a selected service including its terms/costs and security configuration. Provide confirmed Console product/base-plan identifiers after authorized creation. No private credentials belong in chat/source/artifacts.

The production verifier must authenticate the app owner independently of client-supplied IDs; validate package `com.kozudigital.emuui`, product, purchase state, token ownership and linked tokens against the Play Developer API; store idempotent token records; acknowledge valid initial purchases reliably within Google's deadline; and reconcile RTDN/voided purchases/refunds, including when a restore query returns no client purchases. It must return fresh, account-bound authoritative snapshots and must not trust a client's boolean or cached JSON. Secrets remain server-side. Add request/session binding and appropriate anti-replay protection in the actual transport adapter. No endpoint or credential is fabricated here.

Then wire identity changes, foreground refresh, cross-process game startup and expiry-safe game boundaries to the verifier. Do not interrupt active save writes or remove save/export access. Define and test offline behavior, authenticated reviewer access, account deletion and retention, backend retry/acknowledgment, restore on another device, concurrent sessions, and Play-account/app-account mismatch behavior before activation.

A dedicated reusable reviewer identity with server-side permanent entitlement still needs separate account/security approval. It must expose the same production features, with no publisher Google credentials or universal APK bypass. See COMMERCIAL_RELEASE_DECISIONS.md.

## Privacy and evidence limitations

Adding Billing introduces Google SDK/transitive code (including Google data transport). Final Data safety/privacy review must include the exact shipped SDK behavior and the future backend's purchase-token/account/retention flows; previous no-billing privacy statements are superseded. The app does not log purchase tokens or create a billing token store. Network observation of actual live billing remains unrun. Save export transfers data to the user's chosen document provider, which may be local or cloud-hosted.

Official integration and verification references: https://developer.android.com/google/play/billing/integrate and https://developer.android.com/google/play/billing/security .

The resolved Billing9.1.0 POM declares the Android Software Development Kit License (https://developer.android.com/studio/terms.html). The official AAR/POM hashes are recorded in `play-billing-dependency.json`. No installer/legal-acceptance prompt was presented or accepted. Do not label this SDK as GPL; review the release's SDK terms/notices and compatibility with the frontend distribution obligations before commercial clearance.

Final validation: 220 app unit tests passed, including19 billing/export tests; two distinct billing-preparation UI tests passed on actual16KB emulator. Final release build and lint passed. Live Play/backend flows and production paywall activation remain unrun and blocked as described above. See CURRENT_RELEASE_STATUS.md.
