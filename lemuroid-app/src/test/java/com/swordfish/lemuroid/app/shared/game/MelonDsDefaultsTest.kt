package com.swordfish.lemuroid.app.shared.game

import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MelonDsDefaultsTest {
    private val config =
        GameSystem.findById(
            SystemID.NDS.dbname,
        ).systemCoreConfigs.first { it.coreID == CoreID.MELONDS }

    @Test fun autoModePreservesBothTouchscreenAndGamepadStylus() {
        assertEquals("auto", config.defaultSettings.first { it.key == "melonds_touch_mode" }.value)
    }

    @Test fun biosFallbackDoesNotDisableUserSuppliedFirmware() {
        assertTrue(config.requiredBIOSFiles.isEmpty())
        assertFalse(config.defaultSettings.any { it.key == "melonds_sysfile_mode" && it.value == "builtin" })
        assertFalse(config.defaultSettings.any { it.key == "melonds_firmware_nds_path" })
    }
}
