package com.swordfish.lemuroid.lib.core

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.SystemID

/** Restored preferences can refer to removed or unrelated cores; keep valid user choices. */
object CoreSelectionPolicy {
    fun resolve(
        system: GameSystem,
        requestedCoreName: String?,
        hasPendingDesmumeSaves: Boolean,
    ): SystemCoreConfig {
        system.systemCoreConfigs.firstOrNull { it.coreID.coreName == requestedCoreName }?.let { return it }
        val defaultCore =
            if (system.id == SystemID.NDS) {
                if (hasPendingDesmumeSaves) CoreID.DESMUME else CoreID.MELONDS
            } else {
                system.systemCoreConfigs.first().coreID
            }
        return system.systemCoreConfigs.first { it.coreID == defaultCore }
    }
}
