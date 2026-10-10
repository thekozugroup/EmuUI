package com.swordfish.lemuroid.app.mobile.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkAccentTest {
    @Test fun dominantColorTracksTheArtworkRatherThanItsBrightestPixel() {
        val red = 0xFFB84840.toInt()
        val blue = 0xFF4060D0.toInt()
        assertEquals(red, dominantArtworkColor(IntArray(100) { if (it < 70) red else blue }))
        assertEquals(blue, dominantArtworkColor(IntArray(100) { if (it < 70) blue else red }))
    }

    @Test fun BlackAndWhiteBordersDoNotDrownOutTheCoverColor() {
        val green = 0xFF48A868.toInt()
        assertEquals(green, dominantArtworkColor(IntArray(100) {
            when { it < 45 -> 0xFFFFFFFF.toInt(); it < 90 -> 0xFF000000.toInt(); else -> green }
        }))
    }

    @Test fun emptyOrMonochromeImagesUseNeutralFallback() {
        assertNull(dominantArtworkColor(intArrayOf()))
        assertNull(dominantArtworkColor(intArrayOf(0xFF999999.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt())))
    }

    @Test fun invisiblePixelsDoNotColorTheGlow() {
        assertNull(dominantArtworkColor(IntArray(64) { 0x00FF0040 }))
    }

    @Test fun nearbyShadesProduceTheirAverageInsteadOfAQuantizedColor() {
        assertEquals(0xFF4264C6.toInt(), dominantArtworkColor(intArrayOf(0xFF4060C4.toInt(), 0xFF4468C8.toInt())))
    }
}
