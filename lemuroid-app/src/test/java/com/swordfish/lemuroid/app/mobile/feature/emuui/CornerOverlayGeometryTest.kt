package com.swordfish.lemuroid.app.mobile.feature.emuui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CornerOverlayGeometryTest {
    private val bounds = FoldRect(0, 28, 960, 400)

    private fun resolve(
        cutouts: List<FoldRect>,
        frame: FoldRect = bounds,
    ) = CornerOverlayGeometry.resolve(frame, 220, 48, 96, 48, 12, 8, cutouts)

    @Test fun ordinaryAndCenterCameraKeepTextAndUseTheStatusBarExactlyOnce() {
        val normal = resolve(emptyList())!!
        val center = resolve(listOf(FoldRect(440, 0, 520, 72)))!!
        assertEquals(normal, center)
        assertEquals(28, normal.left.top)
        assertEquals(28, normal.right.top)
        assertFalse(normal.compactBrand)
    }

    @Test fun crowdedCornerCollapsesTheBrandAndKeepsBothActionsAlongsideTheCamera() {
        val camera = FoldRect(70, 0, 150, 90)
        val placement = resolve(listOf(camera))!!
        assertTrue(placement.compactBrand)
        assertEquals(48, placement.left.width)
        assertEquals(28, placement.left.top)
        assertEquals(placement.left.top, placement.right.top)
        assertTrue(placement.left.right + 8 <= camera.left || placement.left.left >= camera.right + 8)
    }

    @Test fun aBroadNotchMovesOneSharedRowBelowItAndRemovingItRestoresTheBaseline() {
        val camera = FoldRect(0, 0, 960, 90)
        val placement = resolve(listOf(camera))!!
        assertEquals(98, placement.left.top)
        assertEquals(placement.left.top, placement.right.top)
        assertEquals(48, placement.left.height)
        assertEquals(48, placement.right.height)
        assertEquals(28, resolve(emptyList())!!.left.top)
    }

    @Test fun cameraAtTheToolsCornerMovesTheToolsWithoutRemovingTheBrandLabel() {
        val camera = FoldRect(850, 0, 910, 100)
        val placement = resolve(listOf(camera))!!
        assertFalse(placement.compactBrand)
        assertEquals(28, placement.right.top)
        assertTrue(placement.right.right <= camera.left - 8)
    }

    @Test fun aFullyBlockedOrUndersizedWindowHasNoTouchableRow() {
        assertNull(resolve(listOf(bounds)))
        assertNull(resolve(emptyList(), FoldRect(0, 28, 150, 60)))
    }

    @Test fun rotationAndMultipleCameraSweepKeepsTargetsAlignedAndClear() {
        for (width in listOf(400, 960, 1400)) {
            for (statusTop in listOf(0, 28, 72)) {
                val frame = FoldRect(0, statusTop, width, 400)
                for (x in 0 until width step 17) {
                    val cuts = listOf(FoldRect(x, 0, x + 40, 90), FoldRect(width - 80, 0, width, 40))
                    val row = resolve(cuts, frame) ?: continue
                    assertEquals(row.left.top, row.right.top)
                    assertEquals(row.left.height, row.right.height)
                    assertTrue(row.left.width >= 48 && row.right.width >= 48)
                    assertTrue(row.left.right + 8 <= row.right.left)
                    for (r in listOf(row.left, row.right)) {
                        assertTrue(r.top >= statusTop && r.left >= 12 && r.right <= width - 12)
                        assertFalse(
                            cuts.any {
                                val horizontal = r.left < it.right + 8 && r.right + 8 > it.left
                                val vertical = r.top < it.bottom + 8 && r.bottom + 8 > it.top
                                horizontal && vertical
                            },
                        )
                    }
                }
            }
        }
    }
}
