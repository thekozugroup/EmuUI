package com.swordfish.lemuroid.app.mobile.feature.emuui

import android.content.pm.ActivityInfo
import android.view.Surface
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsoleOrientationTest {
    @Test fun calibrationUsesObservedLandscapeOnEitherNaturalDisplayOrientation() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ConsoleOrientation.landscapeRequest(Surface.ROTATION_0))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ConsoleOrientation.landscapeRequest(Surface.ROTATION_90))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, ConsoleOrientation.landscapeRequest(Surface.ROTATION_180))
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, ConsoleOrientation.landscapeRequest(Surface.ROTATION_270))
    }
}
