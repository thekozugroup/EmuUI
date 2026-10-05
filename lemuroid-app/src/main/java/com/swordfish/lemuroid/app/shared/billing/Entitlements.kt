package com.swordfish.lemuroid.app.shared.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Tentative identifiers only. Console products/base plan and backend are not configured.
data class BillingProducts(
    val monthly: String = "emuui_monthly",
    val lifetime: String = "emuui_lifetime",
    val monthlyBasePlan: String? = null,
    val confirmed: Boolean = false,
)

data class BillingIdentity(val ownerId: String, val obfuscatedAccountId: String)

enum class ProductKind { SUBSCRIPTION, LIFETIME }
enum class PurchaseState { PURCHASED, PENDING, UNSPECIFIED }

// Never log or persist this object: tokens must go only to the approved verifier.
class StorePurchase(
    val productId: String,
    val token: String,
    val state: PurchaseState,
    val suspended: Boolean = false,
)

sealed interface StoreResult<out T> {
    data class Success<T>(val value: T) : StoreResult<T>
    data object Unavailable : StoreResult<Nothing>
}

interface PurchaseStore {
    suspend fun purchases(kind: ProductKind): StoreResult<List<StorePurchase>>
    // Deliberately no consume API: lifetime ownership is non-consumable.
}

enum class SubscriptionStatus { NONE, ACTIVE, GRACE_PERIOD, CANCELED, ON_HOLD, PAUSED, EXPIRED, REVOKED }

/** Trusted backend output, not a Play client purchase or a locally stored boolean.
 * Backend must verify package/product/token ownership with Google, reconcile previous
 * purchases (including refunds), and durably acknowledge valid initial purchases.
 * Durations are computed from backend time, never from the device wall clock.
 */
data class VerifiedAccountAccess(
    val ownerId: String,
    val revision: Long,
    val lifetime: Boolean,
    val subscription: SubscriptionStatus,
    val subscriptionRemainingMillis: Long,
    val autoRenewing: Boolean,
    val recheckAfterMillis: Long,
)

sealed interface VerificationResult {
    data class Verified(val access: VerifiedAccountAccess) : VerificationResult
    data object Rejected : VerificationResult
    data object Unavailable : VerificationResult
}

interface PurchaseVerifier {
    val configured: Boolean
    // Complete account reconciliation, even when purchased is empty. Pending client
    // records never authorize access. This call must be authenticated to ownerId.
    suspend fun reconcile(ownerId: String, purchased: List<StorePurchase>): VerificationResult
}

object UnconfiguredPurchaseVerifier : PurchaseVerifier {
    override val configured = false
    override suspend fun reconcile(ownerId: String, purchased: List<StorePurchase>) = VerificationResult.Unavailable
}

enum class BillingStatus { NOT_CONFIGURED, CHECKING, READY, PENDING, UNAVAILABLE, CANCELED }
enum class AccessAction { START_GAME, VIEW_LIBRARY, VIEW_SAVES, EXPORT_SAVES, MANAGE_SUBSCRIPTION }

data class AccessLease(val access: VerifiedAccountAccess, val checkedAtElapsed: Long) {
    fun grantsAccess(nowElapsed: Long): Boolean {
        val elapsed = nowElapsed - checkedAtElapsed
        // A backwards clock/reboot never extends a lease. No lease is persisted yet.
        if (elapsed < 0 || elapsed >= access.recheckAfterMillis.coerceAtMost(MAX_LEASE_MILLIS)) return false
        if (access.lifetime) return true
        return access.subscription in setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.CANCELED) &&
            elapsed < access.subscriptionRemainingMillis
    }

    companion object {
        // Upper safety bound, not a promised offline-access duration. Backend chooses a shorter lease.
        const val MAX_LEASE_MILLIS = 24L * 60 * 60 * 1000
    }
}

data class BillingState(
    val status: BillingStatus = BillingStatus.NOT_CONFIGURED,
    val lease: AccessLease? = null,
    val pending: Boolean = false,
) {
    fun allows(action: AccessAction, nowElapsed: Long): Boolean =
        action != AccessAction.START_GAME || lease?.grantsAccess(nowElapsed) == true

    val shouldManageSubscriptionAfterGift: Boolean
        get() = lease?.access?.let { it.lifetime && it.autoRenewing } == true
}

class EntitlementController(
    private val store: PurchaseStore,
    private val verifier: PurchaseVerifier,
    private val products: BillingProducts,
    private val elapsedRealtime: () -> Long,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(BillingState())
    val state = mutableState.asStateFlow()
    private var ownerId: String? = null
    private var generation = 0L

    // Called on the owning UI scope. Reject late results from the previous login session.
    fun bindOwner(owner: String?) {
        if (ownerId == owner) return
        ownerId = owner
        generation++
        mutableState.value = BillingState()
    }

    fun purchaseCanceled() {
        mutableState.value = mutableState.value.copy(status = BillingStatus.CANCELED)
    }

    fun purchaseUnavailable() {
        mutableState.value = mutableState.value.copy(status = BillingStatus.UNAVAILABLE)
    }

    suspend fun refresh() = mutex.withLock {
        val owner = ownerId
        val requestGeneration = generation
        if (!products.confirmed || !verifier.configured || owner == null) {
            mutableState.value = BillingState()
            return@withLock
        }
        mutableState.value = mutableState.value.copy(status = BillingStatus.CHECKING)
        try {
            // Query separately: out-of-app lifetime gift redemption must restore with SUBS too.
            val subscriptions = store.purchases(ProductKind.SUBSCRIPTION)
            val lifetime = store.purchases(ProductKind.LIFETIME)
            if (requestGeneration != generation) return@withLock
            if (subscriptions !is StoreResult.Success || lifetime !is StoreResult.Success) {
                purchaseUnavailable() // preserve only an existing, unexpired verified lease
                return@withLock
            }
            val relevant = subscriptions.value.filter { it.productId == products.monthly } +
                lifetime.value.filter { it.productId == products.lifetime }
            val pending = relevant.any { it.state == PurchaseState.PENDING }
            val purchased = relevant.filter { it.state == PurchaseState.PURCHASED && !it.suspended && it.token.isNotBlank() }
            val result = verifier.reconcile(owner, purchased.distinctBy { it.token })
            if (requestGeneration != generation) return@withLock
            when (result) {
                is VerificationResult.Verified -> {
                    val access = result.access
                    val previous = mutableState.value.lease?.access
                    if (access.ownerId != owner || access.revision < 0 || access.recheckAfterMillis <= 0 ||
                        (previous != null && access.revision < previous.revision)) {
                        mutableState.value = BillingState(BillingStatus.UNAVAILABLE)
                    } else {
                        mutableState.value = BillingState(
                            if (pending) BillingStatus.PENDING else BillingStatus.READY,
                            AccessLease(access, elapsedRealtime()), pending,
                        )
                    }
                }
                VerificationResult.Rejected -> mutableState.value = BillingState(if (pending) BillingStatus.PENDING else BillingStatus.READY, pending = pending)
                VerificationResult.Unavailable -> mutableState.value = mutableState.value.copy(status = BillingStatus.UNAVAILABLE, pending = pending)
            }
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (_: Exception) {
            if (requestGeneration == generation) purchaseUnavailable()
        }
    }
}
