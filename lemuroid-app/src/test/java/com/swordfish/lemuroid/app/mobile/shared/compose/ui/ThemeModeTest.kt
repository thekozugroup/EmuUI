package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun missingOrUnknownPreferenceFollowsSystem() {
        listOf(null, "", "unknown", "DARK", "2").forEach {
            assertEquals(ThemeMode.SYSTEM, ThemeMode.fromPreference(it))
        }
    }

    @Test fun storedValuesAreStableAndRoundTrip() {
        assertEquals(listOf("system", "light", "dark"), ThemeMode.entries.map { it.preferenceValue })
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromPreference(it.preferenceValue)) }
    }

    @Test fun systemModeFollowsBothDeviceModes() {
        assertFalse(ThemeMode.SYSTEM.isDark(false))
        assertTrue(ThemeMode.SYSTEM.isDark(true))
    }

    @Test fun explicitLightOverridesBothDeviceModes() {
        assertFalse(ThemeMode.LIGHT.isDark(false))
        assertFalse(ThemeMode.LIGHT.isDark(true))
    }

    @Test fun explicitDarkOverridesBothDeviceModes() {
        assertTrue(ThemeMode.DARK.isDark(false))
        assertTrue(ThemeMode.DARK.isDark(true))
    }
}
