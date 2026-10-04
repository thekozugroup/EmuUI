package com.swordfish.lemuroid.app.mobile.feature.game

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoldGameLifecycleOwnerTest {
    @Test
    fun invalidPosturePausesAndCannotResumeABackgroundActivity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val host = TestOwner()
            host.registry.currentState = Lifecycle.State.RESUMED
            val native = FoldGameLifecycleOwner(host)
            assertEquals(Lifecycle.State.STARTED, native.lifecycle.currentState)
            native.setConsoleAvailable(true)
            assertEquals(Lifecycle.State.RESUMED, native.lifecycle.currentState)
            repeat(3) {
                native.setConsoleAvailable(false)
                assertEquals(Lifecycle.State.STARTED, native.lifecycle.currentState)
                native.setConsoleAvailable(true)
                assertEquals(Lifecycle.State.RESUMED, native.lifecycle.currentState)
            }
            host.registry.currentState = Lifecycle.State.CREATED
            assertEquals(Lifecycle.State.CREATED, native.lifecycle.currentState)
            native.setConsoleAvailable(false)
            native.setConsoleAvailable(true)
            assertEquals(Lifecycle.State.CREATED, native.lifecycle.currentState)
            host.registry.currentState = Lifecycle.State.RESUMED
            assertEquals(Lifecycle.State.RESUMED, native.lifecycle.currentState)
            native.dispose()
            assertEquals(Lifecycle.State.DESTROYED, native.lifecycle.currentState)
        }
    }

    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
