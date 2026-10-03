package com.swordfish.lemuroid.app.shared.game

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GameLaunchRecoveryTest {
    @Test
    fun launchedGameLeavesReschedulingToNormalGameFinish() =
        runBlocking {
            val events = mutableListOf<String>()
            launchWithBackgroundRecovery({ events += "prepare" }, {
                events += "launch"
                true
            }, { events += "recover" })
            assertEquals(listOf("prepare", "launch"), events)
        }

    @Test
    fun destroyedOriginRecoversCancelledBackgroundWork() =
        runBlocking {
            val events = mutableListOf<String>()
            launchWithBackgroundRecovery({ events += "prepare" }, { false }, { events += "recover" })
            assertEquals(listOf("prepare", "recover"), events)
        }

    @Test
    fun partialPreparationFailureRecoversAndPropagates() =
        runBlocking {
            val failure = IllegalStateException("second cancellation failed")
            val events = mutableListOf<String>()
            val observed =
                runCatching {
                    launchWithBackgroundRecovery(
                        {
                            events += "prepare"
                            throw failure
                        },
                        {
                            events += "launch"
                            true
                        },
                        { events += "recover" },
                    )
                }.exceptionOrNull()
            assertSame(failure, observed)
            assertEquals(listOf("prepare", "recover"), events)
        }

    @Test
    fun startActivityFailureRecoversAndPropagates() =
        runBlocking {
            val failure = IllegalStateException("Activity unavailable")
            var recovered = 0
            val observed =
                runCatching {
                    launchWithBackgroundRecovery({}, { throw failure }, { recovered++ })
                }.exceptionOrNull()
            assertSame(failure, observed)
            assertEquals(1, recovered)
        }

    @Test
    fun coroutineCancellationRecoversAndIsNotSwallowed() =
        runBlocking {
            val cancellation = CancellationException("cancelled")
            var recovered = 0
            val observed =
                runCatching {
                    launchWithBackgroundRecovery({ throw cancellation }, { error("must not launch") }, { recovered++ })
                }.exceptionOrNull()
            assertSame(cancellation, observed)
            assertEquals(1, recovered)
        }
}
