> FREE RELEASE UPDATE: [FREE_RELEASE_CHECKPOINT.md](FREE_RELEASE_CHECKPOINT.md) supersedes all earlier monetization, purchase-gate and payment-profile requirements below.

# Settled price decision

US$0.99/month plus US$19.99 one-time lifetime access. The user delegated a reasonable lifetime price and the coordinating parent selected/communicated US$19.99. Official lifetime gift codes redeem the same non-consumable entitlement. Show only actual localized Play ProductDetails pricing in checkout; these US reference prices do not enable unconfigured products. Public product/base-plan/purchase-option IDs and licensing key remain pending.

# Play-only billing — current decision

Supersedes the backend/account requirements in earlier billing preparation notes. The owner requested Google Play only, one US$0.99/month subscription and one normally purchasable lifetime product. Lifetime price is US$19.99 one-time. No Supabase, app login, backend, service account, paid service or new signing key was created or is required by this implementation.

## Implemented

Official Play Billing 9.1.0 client; localized prices from ProductDetails; explicit monthly base plan and lifetime BUY option selection; changed-price reconfirmation; completed/pending handling; local RSA receipt signature and package/product/token/state binding; client acknowledgment after grant and retries on refresh; separate SUBS/INAPP restore on resume; non-consumable lifetime (no consume operation); Google Play subscription management; lifetime/subscription overlap warning; official Play promo redemption through restore; independent non-destructive save ZIP export.

The production Play GameLauncher checks ownership before preparing a new game, including external launcher actions. Failed/unconfigured checks open the purchase screen. Library, saves and export are not gated. Running games are not terminated when ownership changes. Debug builds retain gameplay for native QA; debug gameplay is not evidence of the production payment gate. No unsigned local boolean unlock or universal reviewer bypass exists. All local purchase data remains in memory, with no token logs or backups.

Configuration in PlayBillingConfiguration remains intentionally incomplete: actual product IDs, monthly base-plan ID, lifetime purchase-option ID, and this package's PUBLIC licensing RSA key from Console. These are configuration, not backend credentials/private signing keys. Confirm configuration only after both products/prices are approved and installed Play-track tests pass. Current unsigned AAB is REVIEW-BLOCKED, not launch-ready. Its release game gate denies access until configured.

## Client-only limits

Google explicitly documents client-only acknowledgment. A backend is recommended for stronger fraud resistance, reliable acknowledgment when the app is closed, and prompt server lifecycle notifications; it is optional infrastructure here, not a launch prerequisite.

A signature authenticates purchase data, not its present refund/revocation status. The app relies on queryPurchasesAsync current ownership; Play can cache results. There is no claimed exact server expiry or immediate offline revocation. The controller permits at most one hour of in-memory fallback after a successful query; new game-launch controllers query Play again, and no entitlement persists through process restart. Long-term offline gameplay is therefore not promised. Test offline and multi-Google-account behavior on the actual Play installation. Play selects the purchasing account; EmuUI does not read an account email or provide account switching. Restore requires the original purchasing Play account.

Canceled but still-owned subscriptions retain access; missing/suspended ownership does not. Complete successful empty queries remove prior access. Partial query failure cannot create new access. Pending records never grant or acknowledge. Unacknowledged purchases retry on app refresh/game launch; if the app cannot acknowledge within Google's window, Play may refund them. No RTDN, Voided Purchases API or background server processing is claimed.

Lifetime is one-time access, not a subscription conversion. Buying/redeeming it does not automatically cancel monthly renewal. UI states this before purchase and links to Play management. Gifts use official Console promo codes for the lifetime product; no custom key service. This does not imply unlimited promotional quantities or approval of a gift sale scheme.

## Remaining billing validation

Console/owner: actual products/base plan/purchase option; public licensing key; merchant/agreements/signing decisions; reviewer access instructions. Supply a practical repeatable review path through Play Console's app-access process without publisher credentials or a shipped universal bypass. Single-use gifts alone should not be assumed sufficient reviewer access. No separate app account is required simply because the app has a paywall.

Live Play-track tests remain necessary: monthly purchase, pending completion/cancel, lifetime, gift without order ID, acknowledgment/retry, same-account reinstall/second-device restore, multi-account selection, cancel/paid period, grace/hold/pause/expiry, refund/revoke, offline/reconnect, duplicate purchase handling and subscription-to-lifetime overlap. Also exercise the minified production game-launch gate and return from checkout to game selection. Unit fakes and a sideloaded debug APK cannot prove these.

## Official references checked 2026-10-05

- Client-only acknowledgment, pending processing and resume queries: https://developer.android.com/google/play/billing/integrate
- Backend security is recommended; client acknowledgment allowed; acknowledgment window and promo order-ID caveat: https://developer.android.com/google/play/billing/security
- Signed purchase data, canceled ownership and suspension semantics: https://developer.android.com/reference/com/android/billingclient/api/Purchase
- One-time BUY option/offer selection: https://developer.android.com/google/play/billing/one-time-product-multi-purchase-options-offers
- Play subscription management links: https://developer.android.com/google/play/billing/subscriptions
- Review access: https://support.google.com/googleplay/android-developer/answer/9859455

Privacy review must cover Play Billing data flows in addition to HTTPS cover queries, optional Drive sync and selected export providers. No EmuUI billing backend or app-account collection was added. Existing Drive OAuth is separate from payment identity.

## Local result

Production build and lint pass (0 errors, 45 warnings, 4 hints); 227 unit tests pass; two actual-16KB billing/export UI tests pass. Seven receipt-cryptography tests use ephemeral test-only RSA keys; no app-signing key was generated or configured. Screenshot shows disabled checkout because Console configuration is absent, so it is QA evidence rather than a finished paid listing screenshot. Live Play and minified gate tests remain unrun. See CURRENT_RELEASE_STATUS.md for artifact hash and recovered build-attempt details.
