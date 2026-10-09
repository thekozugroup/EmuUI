package com.swordfish.lemuroid.app.mobile.feature.emuui

import org.junit.Assert.*
import org.junit.Test

class ConsolePolishGeometryTest {
    @Test fun lowerImageGrowsWithoutTakingTheControlWingsOrChangingAspect() {
        for (width in listOf(1840, 2208, 2560)) {
            val upper = FoldGeometry.displayPanel(FoldRect(0, 0, width, 880), 0, 63)
            val before = FoldGeometry.displayPanel(FoldRect(567, 960, width - 567, 1816), 0, 63)
            val after = FoldGeometry.displayPanel(FoldRect(462, 960, width - 462, 1816), 0, 63)
            val old = FoldGeometry.independentDualScreen(upper, before)!!
            val result = FoldGeometry.balancedDualScreen(upper, after)!!
            assertTrue(result.lowerScreen.width + 0.01f >= minOf(old.lowerScreen.width, result.upperScreen.width * 0.94f))
            assertTrue(result.lowerScreen.width <= result.upperScreen.width * 0.9401f)
            assertEquals(4f / 3f, result.lowerScreen.width / result.lowerScreen.height, 0.0001f)
            assertEquals(width / 2f, (result.lowerScreen.left + result.lowerScreen.right) / 2f, 0.01f)
            assertTrue(result.lowerScreen.left >= after.bounds.left)
            assertTrue(result.lowerScreen.top >= after.bounds.top)
            assertTrue(result.lowerScreen.bottom <= after.bounds.bottom)
        }
    }

    @Test fun aCameraObstructionStillProtectsTheUpperNativePixels() {
        val upper = FoldGeometry.displayPanel(FoldRect(0, 0, 2208, 900), 0, 63)
        val lower = FoldGeometry.displayPanel(FoldRect(462, 960, 1746, 1816), 0, 63)
        val cutout = FoldRect(1020, 0, 1188, 96)
        val result = FoldGeometry.balancedDualScreen(upper, lower, listOf(cutout))!!
        assertTrue(result.upperScreen.top >= cutout.bottom)
        assertTrue(result.lowerScreen.width <= result.upperScreen.width * 0.9401f)
    }
}
