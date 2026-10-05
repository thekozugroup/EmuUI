package com.swordfish.lemuroid.app.shared.billing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EntitlementControllerTest {
    private class FakeStore : PurchaseStore {
        val queries = mutableListOf<ProductKind>()
        var fail: ProductKind? = null
        var subscriptions = emptyList<StorePurchase>()
        var lifetime = emptyList<StorePurchase>()
        override suspend fun purchases(kind: ProductKind): StoreResult<List<StorePurchase>> {
            queries += kind
            return if (kind == fail) StoreResult.Unavailable else StoreResult.Success(
                if (kind == ProductKind.SUBSCRIPTION) subscriptions else lifetime)
        }
    }
    private class FakeVerifier : PurchaseVerifier {
        override val configured = true
        var received = emptyList<StorePurchase>()
        var calls = 0
        var response: VerificationResult = VerificationResult.Rejected
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun reconcile(ownerId: String, purchased: List<StorePurchase>): VerificationResult {
            calls++
            received = purchased
            gate?.await()
            return response
        }
    }
    private val store = FakeStore()
    private val verifier = FakeVerifier()
    private var now = 100L
    private val products = BillingProducts(monthlyBasePlan = "test-monthly", confirmed = true)
    private val controller = EntitlementController(store, verifier, products) { now }.also { it.bindOwner("owner-A") }
    private fun access(lifetime: Boolean = false, status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
        remaining: Long = 500, renews: Boolean = true, owner: String = "owner-A", revision: Long = 1) =
        VerificationResult.Verified(VerifiedAccountAccess(owner, revision, lifetime, status, remaining, renews, 1000))
    private fun canPlay() = controller.state.value.allows(AccessAction.START_GAME, now)
    private fun purchased(product: String, token: String = "test-token") = StorePurchase(product, token, PurchaseState.PURCHASED)

    @Test fun purchasedClientRecordNeverGrantsAccessWithoutVerification() = runBlocking {
        store.subscriptions = listOf(purchased(products.monthly))
        verifier.response = VerificationResult.Unavailable
        controller.refresh()
        assertFalse(canPlay())
    }
    @Test fun pendingAndSuspendedRecordsAreNeverSubmittedAsPaid() = runBlocking {
        store.subscriptions = listOf(StorePurchase(products.monthly, "pending", PurchaseState.PENDING),
            StorePurchase(products.monthly, "suspended", PurchaseState.PURCHASED, true))
        controller.refresh()
        assertTrue(verifier.received.isEmpty())
        assertTrue(controller.state.value.pending)
        assertFalse(canPlay())
    }
    @Test fun restoreQueriesBothKindsAndIncludesOutOfAppLifetimePurchase() = runBlocking {
        store.lifetime = listOf(purchased(products.lifetime))
        verifier.response = access(lifetime = true, status = SubscriptionStatus.EXPIRED, remaining = 0, renews = false)
        controller.refresh()
        assertEquals(listOf(ProductKind.SUBSCRIPTION, ProductKind.LIFETIME), store.queries)
        assertEquals(products.lifetime, verifier.received.single().productId)
        assertTrue(canPlay())
    }
    @Test fun lifetimeWinsAndExistingRenewalNeedsExplicitManagement() = runBlocking {
        verifier.response = access(lifetime = true, remaining = 0)
        controller.refresh()
        assertTrue(canPlay())
        assertTrue(controller.state.value.shouldManageSubscriptionAfterGift)
    }
    @Test fun canceledSubscriptionRetainsOnlyThePaidPeriod() = runBlocking {
        verifier.response = access(status = SubscriptionStatus.CANCELED, renews = false)
        controller.refresh()
        assertTrue(canPlay())
        now += 500
        assertFalse(canPlay())
    }
    @Test fun graceGrantsAccessButHoldPauseExpiredAndRevokedDoNot() = runBlocking {
        for (status in SubscriptionStatus.values()) {
            verifier.response = access(status = status)
            controller.refresh()
            assertEquals(status in setOf(SubscriptionStatus.ACTIVE, SubscriptionStatus.GRACE_PERIOD, SubscriptionStatus.CANCELED), canPlay())
        }
    }
    @Test fun refundReconciliationRemovesPreviouslyVerifiedLifetime() = runBlocking {
        verifier.response = access(lifetime = true)
        controller.refresh()
        assertTrue(canPlay())
        verifier.response = access(status = SubscriptionStatus.REVOKED, remaining = 0, renews = false, revision = 2)
        controller.refresh()
        assertFalse(canPlay())
    }
    @Test fun emptyStoreStillReconcilesPreviouslyOwnedPurchases() = runBlocking {
        verifier.response = access(lifetime = true)
        controller.refresh()
        verifier.response = VerificationResult.Rejected
        controller.refresh()
        assertEquals(2, verifier.calls)
        assertFalse(canPlay())
    }
    @Test fun partialStoreFailureCannotReplaceAnAuthoritativeSnapshot() = runBlocking {
        verifier.response = access(lifetime = true)
        controller.refresh()
        store.fail = ProductKind.LIFETIME
        controller.refresh()
        assertEquals(1, verifier.calls)
        assertTrue(canPlay())
        now += 1000
        assertFalse(canPlay())
    }
    @Test fun accountChangeRejectsLateVerification() = runBlocking {
        verifier.response = access(lifetime = true)
        verifier.gate = CompletableDeferred()
        val job = launch { controller.refresh() }
        while (verifier.calls == 0) kotlinx.coroutines.yield()
        controller.bindOwner("owner-B")
        verifier.gate!!.complete(Unit)
        job.join()
        assertFalse(canPlay())
        assertNull(controller.state.value.lease)
    }
    @Test fun mismatchedOwnerAndRegressedRevisionAreRejected() = runBlocking {
        verifier.response = access(owner = "owner-B")
        controller.refresh()
        assertFalse(canPlay())
        verifier.response = access(revision = 4)
        controller.refresh()
        assertTrue(canPlay())
        verifier.response = access(revision = 3)
        controller.refresh()
        assertFalse(canPlay())
    }
    @Test fun canceledCheckoutDoesNotRevokeExistingEntitlement() = runBlocking {
        verifier.response = access()
        controller.refresh()
        controller.purchaseCanceled()
        assertTrue(canPlay())
        assertEquals(BillingStatus.CANCELED, controller.state.value.status)
    }
    @Test fun expiryNeverBlocksLibrarySaveAccessExportOrManagement() {
        AccessAction.values().filter { it != AccessAction.START_GAME }.forEach {
            assertTrue(BillingState().allows(it, now))
        }
    }
    @Test fun unconfiguredProductionVerifierNeverGrantsOrQueriesPurchases() = runBlocking {
        val blocked = EntitlementController(store, UnconfiguredPurchaseVerifier, products) { now }
        blocked.bindOwner("owner-A")
        blocked.refresh()
        assertFalse(blocked.state.value.allows(AccessAction.START_GAME, now))
        assertTrue(store.queries.isEmpty())
    }
    @Test fun changingClockBackwardsDoesNotExtendAccess() = runBlocking {
        verifier.response = access(lifetime = true)
        controller.refresh()
        now--
        assertFalse(canPlay())
    }
    @Test fun unknownProductsAndEmptyTokensAreNotSubmitted() = runBlocking {
        store.lifetime = listOf(purchased("unknown"), purchased(products.lifetime, ""))
        controller.refresh()
        assertTrue(verifier.received.isEmpty())
    }
    @Test fun onlyPlainAutoRenewingMonthlyBasePlanIsOffered() {
        assertTrue(isStandardMonthlyPlan(null, listOf("P1M"), listOf(1)))
        assertFalse(isStandardMonthlyPlan("trial", listOf("P1M"), listOf(1)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1Y"), listOf(1)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1M"), listOf(2)))
        assertFalse(isStandardMonthlyPlan(null, listOf("P1W", "P1M"), listOf(2, 1)))
    }
}
