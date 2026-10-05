package com.swordfish.lemuroid.app.mobile.feature.home

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseSystemAvailabilityTest {
    @Test fun restrictedSystemsAreNotOfferedButHistoricalRowsRemainReadable() {
        val omitted = setOf(SystemID.SNES, SystemID.SMS, SystemID.GENESIS, SystemID.SEGACD,
            SystemID.GG, SystemID.FBNEO, SystemID.MAME2003PLUS)
        omitted.forEach { id ->
            assertFalse(GameSystem.isAvailable(id.dbname))
            assertFalse(GameSystem.all().any { it.id == id })
            assertEquals(id, GameSystem.findById(id.dbname).id)
        }
        assertFalse(GameSystem.isAvailable("unknown-system"))
    }

    @Test fun uniqueExtensionsCannotReimportOmittedSystems() {
        listOf("smc", "sfc", "sms", "gen", "smd", "md", "gg").forEach {
            assertNull(GameSystem.findByUniqueFileExtension(it))
        }
        assertEquals(SystemID.NES, GameSystem.findByUniqueFileExtension("NES")?.id)
        assertEquals(SystemID.NDS, GameSystem.findByUniqueFileExtension("nds")?.id)
    }

    @Test fun dsCoresAndHistoricalSaveSyncNamesRemainIntact() {
        val ds = GameSystem.all().single { it.id == SystemID.NDS }
        assertEquals(setOf(CoreID.MELONDS, CoreID.DESMUME), ds.systemCoreConfigs.map { it.coreID }.toSet())
        assertTrue(GameSystem.isAvailable(SystemID.NDS.dbname))
        assertTrue(GameSystem.findSystemForCore(CoreID.SNES9X).any { it.id == SystemID.SNES })
        assertTrue(GameSystem.findSystemForCore(CoreID.GENESIS_PLUS_GX).any { it.id == SystemID.SEGACD })
    }
}
