package com.swordfish.lemuroid.app.shared.storage.cache

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class CacheCleanupPolicyTest {
    @Test
    fun cleanupAfterGameplayMustReplaceCancelledOrFailedPrerequisites() {
        // This checks our chosen policy, not a simulated WorkManager database implementation.
        // AndroidX preserves active-chain ordering but starts anew after cancelled/failed leaves.
        assertEquals(ExistingWorkPolicy.APPEND_OR_REPLACE, cacheCleanupWorkPolicy)
    }
}
