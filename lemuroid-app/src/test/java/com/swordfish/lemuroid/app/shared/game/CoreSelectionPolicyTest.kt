package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.core.CoreSelectionPolicy
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Test

class CoreSelectionPolicyTest {
    private val ds = GameSystem.findById(SystemID.NDS.dbname)

    @Test
    fun unknownRestoredPreferenceChoosesMelonDsWithoutLosingLegacyMigrationChoice() {
        assertEquals(CoreID.MELONDS, CoreSelectionPolicy.resolve(ds, "removed-core", false).coreID)
        assertEquals(CoreID.DESMUME, CoreSelectionPolicy.resolve(ds, "removed-core", true).coreID)
    }

    @Test
    fun existingDsCoreSelectionIsKeptEvenWhenTheDefaultWouldChange() {
        assertEquals(CoreID.DESMUME, CoreSelectionPolicy.resolve(ds, CoreID.DESMUME.coreName, false).coreID)
        assertEquals(CoreID.MELONDS, CoreSelectionPolicy.resolve(ds, CoreID.MELONDS.coreName, true).coreID)
    }

    @Test
    fun allGameBoySystemsRecoverFromAnUnrelatedCorePreference() {
        for ((system, core) in listOf(
            SystemID.GB to CoreID.GAMBATTE,
            SystemID.GBC to CoreID.GAMBATTE,
            SystemID.GBA to CoreID.MGBA,
        )) {
            val definition = GameSystem.findById(system.dbname)
            assertEquals(core, CoreSelectionPolicy.resolve(definition, CoreID.PPSSPP.coreName, false).coreID)
            assertEquals(core, CoreSelectionPolicy.resolve(definition, null, false).coreID)
        }
    }
}
