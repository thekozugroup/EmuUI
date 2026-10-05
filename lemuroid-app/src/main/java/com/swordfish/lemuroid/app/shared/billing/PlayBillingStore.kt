package com.swordfish.lemuroid.app.shared.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface PurchaseUpdate {
    data object Reconcile : PurchaseUpdate
    data object Canceled : PurchaseUpdate
    data object Unavailable : PurchaseUpdate
}

class PlayOffer internal constructor(
    val formattedPrice: String,
    val currencyCode: String,
    val amountMicros: Long,
    internal val token: String,
    internal val details: ProductDetails,
) {
    fun sameTerms(other: PlayOffer) = formattedPrice == other.formattedPrice &&
        currencyCode == other.currencyCode && amountMicros == other.amountMicros && token == other.token
}

/** Only the standard monthly, auto-renewing base plan is offered; no invented prices or trial terms. */
fun isStandardMonthlyPlan(offerId: String?, periods: List<String>, recurrenceModes: List<Int>): Boolean =
    offerId == null && periods == listOf("P1M") && recurrenceModes == listOf(1) // INFINITE_RECURRING

class PlayBillingStore(
    context: Context,
    private val products: BillingProducts,
    private val onUpdate: (PurchaseUpdate) -> Unit,
) : PurchaseStore {
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, _ ->
            // Purchase callbacks are wake-ups, never proof of entitlement.
            onUpdate(when (result.responseCode) {
                BillingClient.BillingResponseCode.OK, BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseUpdate.Reconcile
                BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseUpdate.Canceled
                else -> PurchaseUpdate.Unavailable
            })
        }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    override suspend fun purchases(kind: ProductKind): StoreResult<List<StorePurchase>> {
        if (!connect()) return StoreResult.Unavailable
        val type = if (kind == ProductKind.SUBSCRIPTION) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { result, purchases ->
                if (continuation.isActive) continuation.resume(
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) StoreResult.Unavailable else
                        StoreResult.Success(purchases.flatMap { purchase ->
                            purchase.products.map { product ->
                                StorePurchase(product, purchase.purchaseToken, when (purchase.purchaseState) {
                                    Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
                                    Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
                                    else -> PurchaseState.UNSPECIFIED
                                }, purchase.isSuspended, purchase.originalJson, purchase.signature, purchase.isAcknowledged, purchase.isAutoRenewing)
                            }
                        }),
                )
            }
        }
    }

    suspend fun monthlyOffer(): PlayOffer? {
        val basePlan = products.monthlyBasePlan ?: return null
        if (!products.confirmed || !connect()) return null
        if (client.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS).responseCode != BillingClient.BillingResponseCode.OK) return null
        val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(products.monthly)
                .setProductType(BillingClient.ProductType.SUBS).build(),
        )).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(query) { result, response ->
                val details = if (result.responseCode == BillingClient.BillingResponseCode.OK)
                    response.productDetailsList.singleOrNull { it.productId == products.monthly } else null
                val offer = details?.subscriptionOfferDetails?.singleOrNull {
                    it.basePlanId == basePlan && it.installmentPlanDetails == null && isStandardMonthlyPlan(it.offerId,
                        it.pricingPhases.pricingPhaseList.map { phase -> phase.billingPeriod },
                        it.pricingPhases.pricingPhaseList.map { phase -> phase.recurrenceMode })
                }
                val phase = offer?.pricingPhases?.pricingPhaseList?.singleOrNull()
                if (continuation.isActive) continuation.resume(
                    if (details != null && offer != null && phase != null && phase.priceAmountMicros > 0)
                        PlayOffer(phase.formattedPrice, phase.priceCurrencyCode, phase.priceAmountMicros, offer.offerToken, details)
                    else null,
                )
            }
        }
    }

    override suspend fun acknowledge(purchase: StorePurchase): Boolean {
        if (purchase.acknowledged) return true
        if (purchase.state != PurchaseState.PURCHASED || !connect()) return false
        return suspendCancellableCoroutine { continuation ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.token).build()) { result ->
                if (continuation.isActive) continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    suspend fun lifetimeOffer(): PlayOffer? {
        val option = products.lifetimePurchaseOption ?: return null
        if (!products.confirmed || !connect()) return null
        val query = QueryProductDetailsParams.newBuilder().setProductList(listOf(
            QueryProductDetailsParams.Product.newBuilder().setProductId(products.lifetime)
                .setProductType(BillingClient.ProductType.INAPP).build(),
        )).build()
        return suspendCancellableCoroutine { continuation ->
            client.queryProductDetailsAsync(query) { result, response ->
                val details = if (result.responseCode == BillingClient.BillingResponseCode.OK)
                    response.productDetailsList.singleOrNull { it.productId == products.lifetime } else null
                val offer = details?.oneTimePurchaseOfferDetailsList?.singleOrNull {
                    it.purchaseOptionId == option && it.offerId == null && it.rentalDetails == null && it.preorderDetails == null
                }
                if (continuation.isActive) continuation.resume(
                    if (details != null && offer != null && offer.priceAmountMicros > 0 && !offer.offerToken.isNullOrBlank())
                        PlayOffer(offer.formattedPrice, offer.priceCurrencyCode, offer.priceAmountMicros, requireNotNull(offer.offerToken), details)
                    else null,
                )
            }
        }
    }

    // Both offers are re-queried and changed prices reconfirmed by the UI.
    // No EmuUI account is needed: Play owns account selection and payment.
    fun launchPurchase(activity: Activity, offer: PlayOffer, verifierReady: Boolean): Boolean {
        if (!products.confirmed || !verifierReady || !client.isReady) return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(offer.details)
                .setOfferToken(offer.token).build(),
        )).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    fun close() = client.endConnection()
}
