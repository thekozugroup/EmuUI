package com.swordfish.lemuroid.app.shared.storage.cache

import androidx.work.ExistingWorkPolicy

// Gameplay cancels this unique chain. APPEND alone would make later cleanup inherit CANCELLED.
internal val cacheCleanupWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
