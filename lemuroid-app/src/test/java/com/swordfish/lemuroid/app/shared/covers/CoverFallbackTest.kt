package com.swordfish.lemuroid.app.shared.covers

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverFallbackTest {
    @Test
    fun blankMetadataTitleStillHasLocalPlaceholder() {
        assertEquals("?", CoverUtils.fallbackTitle(""))
        assertEquals("?", CoverUtils.fallbackTitle("  "))
    }

    @Test
    fun placeholderKeepsTitleInitialsAndSkipsMetadataSuffix() {
        assertEquals("EUI", CoverUtils.fallbackTitle("EmuUI QA Adventure (USA)"))
        assertEquals("H", CoverUtils.fallbackTitle("homebrew"))
    }
}
