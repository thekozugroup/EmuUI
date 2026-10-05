package com.swordfish.lemuroid.app.shared.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Fill only from this app's Console configuration. These defaults cannot enable checkout.
data class BillingProducts(
    val monthly: String = "emuui_monthly",
    val lifetime: String = "emuui_lifetime",
    val monthlyBasePlan: String? = null,
    val lifetimePurchaseOption: String? = null,
    val confirmed: Boolean = false,
)
enum class ProductKind { SUBSCRIPTION, LIFETIME }
enum class PurchaseState { PURCHASED, PENDING, UNSPECIFIED }

// Sensitive purchase data stays in memory; never log or back it up.
class StorePurchase(
    val productId: String,
    val token: String,
    val state: PurchaseState,
    val suspended: Boolean = false,
    val originalJson: String = "",
    val signature: String = "",
    val acknowledged: Boolean = false,
    val autoRenewing: Boolean = false,
)
sealed interface StoreResult<out T> {
    data class Success<T>(val value: T) : StoreResult<T>
    data object Unavailable : StoreResult<Nothing>
}
interface PurchaseStore {
    suspend fun purchases(kind: ProductKind): StoreResult<List<StorePurchase>>
    suspend fun acknowledge(purchase: StorePurchase): Boolean
    // Lifetime is non-consumable. Deliberately no consume operation.
}
interface PurchaseVerifier {
    val configured: Boolean
    fun verifies(purchase: StorePurchase): Boolean
}
object UnconfiguredPurchaseVerifier : PurchaseVerifier {
    override val configured = false
    override fun verifies(purchase: StorePurchase) = false
}
enum class BillingStatus { NOT_CONFIGURED, CHECKING, READY, PENDING, UNAVAILABLE, CANCELED }
enum class AccessAction { START_GAME, VIEW_LIBRARY, VIEW_SAVES, EXPORT_SAVES, MANAGE_SUBSCRIPTION }
data class VerifiedPlayAccess(val lifetime: Boolean, val subscription: Boolean, val autoRenewing: Boolean)
data class AccessLease(val access: VerifiedPlayAccess, val checkedAtElapsed: Long) {
    fun grantsAccess(nowElapsed: Long): Boolean =
        nowElapsed - checkedAtElapsed in 0 until MAX_LEASE_MILLIS && (access.lifetime || access.subscription)
    companion object {
        // In-memory fallback only. Play may itself serve cached ownership; this is not
        // a server expiry or a guarantee of revocation within this interval.
        const val MAX_LEASE_MILLIS = 60L * 60 * 1000
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
    fun purchaseCanceled() { mutableState.value = mutableState.value.copy(status = BillingStatus.CANCELED) }
    fun purchaseUnavailable() { mutableState.value = mutableState.value.copy(status = BillingStatus.UNAVAILABLE) }
    suspend fun refresh() = mutex.withLock {
        if (!products.confirmed || !verifier.configured) {
            mutableState.value = BillingState()
            return@withLock
        }
        mutableState.value = mutableState.value.copy(status = BillingStatus.CHECKING)
        try {
            val subscriptions = store.purchases(ProductKind.SUBSCRIPTION)
            val lifetime = store.purchases(ProductKind.LIFETIME)
            if (subscriptions !is StoreResult.Success || lifetime !is StoreResult.Success) {
                purchaseUnavailable()
                return@withLock
            }
            val relevant = subscriptions.value.filter { it.productId == products.monthly } +
                lifetime.value.filter { it.productId == products.lifetime }
            val pending = relevant.any { it.state == PurchaseState.PENDING }
            val candidates = relevant.filter { it.state == PurchaseState.PURCHASED && !it.suspended && it.token.isNotBlank() }
            val verified = candidates.filter(verifier::verifies).distinctBy { it.token }
            val access = VerifiedPlayAccess(verified.any { it.productId == products.lifetime },
                verified.any { it.productId == products.monthly },
                verified.any { it.productId == products.monthly && it.autoRenewing })
            // A complete successful query replaces previous ownership, including empty
            // results after expiry/refund/account changes. Play chooses the owning account.
            mutableState.value = BillingState(if (pending) BillingStatus.PENDING else BillingStatus.READY,
                AccessLease(access, elapsedRealtime()), pending)
            var acknowledgmentFailed = false
            for (purchase in verified.filterNot { it.acknowledged }) {
                // Grant first, then acknowledge. Failed acknowledgment retries on refresh.
                if (!store.acknowledge(purchase)) acknowledgmentFailed = true
            }
            if (acknowledgmentFailed || verified.size < candidates.distinctBy { it.token }.size) purchaseUnavailable()
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (_: Exception) { purchaseUnavailable() }
    }
}
