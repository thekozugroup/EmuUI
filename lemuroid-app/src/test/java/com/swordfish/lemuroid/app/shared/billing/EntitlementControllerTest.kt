package com.swordfish.lemuroid.app.shared.billing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EntitlementControllerTest {
    private class FakeStore : PurchaseStore {
        val queries = mutableListOf<ProductKind>()
        val acknowledgments = mutableListOf<String>()
        var acknowledgmentWorks = true
        var fail: ProductKind? = null
        var subscriptions = emptyList<StorePurchase>()
        var lifetime = emptyList<StorePurchase>()
        override suspend fun purchases(kind: ProductKind): StoreResult<List<StorePurchase>> {
            queries += kind
            return if (kind == fail) StoreResult.Unavailable else StoreResult.Success(
                if (kind == ProductKind.SUBSCRIPTION) subscriptions else lifetime)
        }
        override suspend fun acknowledge(purchase: StorePurchase): Boolean {
            acknowledgments += purchase.token
            return acknowledgmentWorks
        }
    }
    private class FakeVerifier : PurchaseVerifier {
        override val configured = true
        var valid = true
        val received = mutableListOf<StorePurchase>()
        override fun verifies(purchase: StorePurchase): Boolean { received += purchase; return valid }
    }
    private val store = FakeStore()
    private val verifier = FakeVerifier()
    private var now = 100L
    private val products = BillingProducts(monthlyBasePlan = "test-monthly", confirmed = true)
    private val controller = EntitlementController(store, verifier, products) { now }
    private fun canPlay() = controller.state.value.allows(AccessAction.START_GAME, now)
    private fun purchased(product: String, token: String = product, acknowledged: Boolean = false, renews: Boolean = false) =
        StorePurchase(product, token, PurchaseState.PURCHASED, acknowledged = acknowledged, autoRenewing = renews)

    @Test fun clientRecordDoesNotGrantWithoutSignatureVerification() = runBlocking {
        store.subscriptions = listOf(purchased(products.monthly)); verifier.valid = false
        controller.refresh(); assertFalse(canPlay()); assertTrue(store.acknowledgments.isEmpty())
    }
    @Test fun pendingAndSuspendedNeverGrantOrAcknowledge() = runBlocking {
        store.subscriptions = listOf(StorePurchase(products.monthly, "pending", PurchaseState.PENDING),
            StorePurchase(products.monthly, "suspended", PurchaseState.PURCHASED, true))
        controller.refresh(); assertTrue(verifier.received.isEmpty()); assertTrue(controller.state.value.pending)
        assertFalse(canPlay()); assertTrue(store.acknowledgments.isEmpty())
    }
    @Test fun restoreBothTypesIncludesOutOfAppLifetimeGiftWithoutOrderId() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime))
        controller.refresh(); assertEquals(listOf(ProductKind.SUBSCRIPTION, ProductKind.LIFETIME), store.queries)
        assertTrue(canPlay()); assertEquals(listOf(products.lifetime), store.acknowledgments)
    }
    @Test fun lifetimePlusRenewingSubscriptionShowsManagementWarning() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime))
        store.subscriptions = listOf(purchased(products.monthly, renews = true))
        controller.refresh(); assertTrue(canPlay()); assertTrue(controller.state.value.shouldManageSubscriptionAfterGift)
    }
    @Test fun canceledButStillOwnedSubscriptionRetainsAccess() = runBlocking {
        store.subscriptions = listOf(purchased(products.monthly, renews = false))
        controller.refresh(); assertTrue(canPlay())
    }
    @Test fun expiredOrRefundedMissingPurchaseClearsAccess() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime)); controller.refresh(); assertTrue(canPlay())
        store.lifetime = emptyList(); controller.refresh(); assertFalse(canPlay())
    }
    @Test fun partialQueryFailurePreservesOnlyUnexpiredSnapshot() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime)); controller.refresh()
        store.fail = ProductKind.LIFETIME; controller.refresh(); assertTrue(canPlay())
        now += AccessLease.MAX_LEASE_MILLIS; assertFalse(canPlay())
    }
    @Test fun pendingCompletesOnLaterRestore() = runBlocking {
        store.lifetime = listOf(StorePurchase(products.lifetime, "gift", PurchaseState.PENDING))
        controller.refresh(); assertFalse(canPlay())
        store.lifetime = listOf(purchased(products.lifetime, "gift")); controller.refresh()
        assertTrue(canPlay()); assertFalse(controller.state.value.pending); assertEquals(listOf("gift"), store.acknowledgments)
    }
    @Test fun acknowledgmentFailureRetriesAndDoesNotDeleteGrantedAccess() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime)); store.acknowledgmentWorks = false
        controller.refresh(); assertTrue(canPlay()); assertEquals(BillingStatus.UNAVAILABLE, controller.state.value.status)
        store.acknowledgmentWorks = true; controller.refresh()
        assertEquals(2, store.acknowledgments.size); assertEquals(BillingStatus.READY, controller.state.value.status)
    }
    @Test fun acknowledgedPurchaseIsNotAcknowledgedAgain() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime, acknowledged = true)); controller.refresh()
        assertTrue(canPlay()); assertTrue(store.acknowledgments.isEmpty())
    }
    @Test fun canceledCheckoutKeepsExistingAccess() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime)); controller.refresh(); controller.purchaseCanceled()
        assertTrue(canPlay()); assertEquals(BillingStatus.CANCELED, controller.state.value.status)
    }
    @Test fun expiryNeverBlocksLibrarySaveExportOrManagement() {
        AccessAction.values().filter { it != AccessAction.START_GAME }.forEach { assertTrue(BillingState().allows(it, now)) }
    }
    @Test fun unconfiguredVerifierCannotQueryOrGrant() = runBlocking {
        val blocked = EntitlementController(store, UnconfiguredPurchaseVerifier, products) { now }
        blocked.refresh(); assertFalse(blocked.state.value.allows(AccessAction.START_GAME, now)); assertTrue(store.queries.isEmpty())
    }
    @Test fun backwardsMonotonicClockDoesNotExtendAccess() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime)); controller.refresh(); now--; assertFalse(canPlay())
    }
    @Test fun unknownProductsAndEmptyTokensAreNotVerified() = runBlocking {
        store.lifetime = listOf(purchased("unknown"), purchased(products.lifetime, "")); controller.refresh()
        assertTrue(verifier.received.isEmpty())
    }
    @Test fun newControllerDoesNotTrustPersistedBooleanOrPreviousAccount() {
        val fresh = EntitlementController(store, verifier, products) { now }
        assertFalse(fresh.state.value.allows(AccessAction.START_GAME, now))
    }
    @Test fun onlyPlainAutoRenewingMonthlyPlanIsOffered() {
        assertTrue(isStandardMonthlyPlan(null, listOf("P1M"), listOf(1)))
        assertFalse(isStandardMonthlyPlan("trial", listOf("P1M"), listOf(1)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1Y"), listOf(1)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1M"), listOf(2)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1W", "P1M"), listOf(2, 1)))
    }
}
