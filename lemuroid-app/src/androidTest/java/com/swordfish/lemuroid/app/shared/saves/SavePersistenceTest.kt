package com.swordfish.lemuroid.app.shared.saves

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swordfish.lemuroid.common.kotlin.readBytesUncompressedAtomic
import com.swordfish.lemuroid.common.kotlin.readTextAtomic
import com.swordfish.lemuroid.common.kotlin.writeBytesAtomic
import com.swordfish.lemuroid.common.kotlin.writeBytesCompressedAtomic
import com.swordfish.lemuroid.common.kotlin.writeTextAtomic
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/** Real Android filesystem checks in an isolated test directory; no ROM/core/user saves required. */
@RunWith(AndroidJUnit4::class)
class SavePersistenceTest {
    private lateinit var root: File
    private lateinit var directories: DirectoriesManager
    private lateinit var states: StatesManager
    private lateinit var saves: SavesManager
    private val game =
        Game(
            fileName = "EmuUI-Persistence-QA.gba",
            fileUri = "test://persistence/fixture",
            title = "Original persistence test data",
            systemId = SystemID.GBA.dbname,
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = 0,
        )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.cacheDir, "save-persistence-${System.nanoTime()}").apply { check(mkdirs()) }
        val isolatedContext =
            object : ContextWrapper(context) {
                override fun getExternalFilesDir(type: String?): File = root
            }
        directories = DirectoriesManager(isolatedContext)
        states = StatesManager(directories)
        saves = SavesManager(directories)
    }

    @After
    fun tearDown() {
        if (::root.isInitialized) check(root.deleteRecursively())
    }

    @Test
    fun rawCompressedAndUtf8AtomicWritesRoundTripAfterReplacement() {
        val bytes = ByteArray(65537) { (it * 37).toByte() }
        val file = File(root, "atomic-data")
        file.writeBytesAtomic(byteArrayOf(9, 8, 7))
        file.writeBytesAtomic(bytes)
        assertArrayEquals(bytes, file.readBytesUncompressedAtomic())
        file.writeBytesCompressedAtomic(bytes)
        assertArrayEquals(bytes, file.readBytesUncompressedAtomic())
        file.writeTextAtomic("EmuUI · 日本語 · 🎮")
        assertEquals("EmuUI · 日本語 · 🎮", file.readTextAtomic())
        assertFalse(File(root, "atomic-data.new").exists())
        assertFalse(File(root, "atomic-data.bak").exists())
    }

    @Test
    fun emptyAndSingleByteLegacyFilesDoNotLoseBytesOrFailDuringGzipDetection() {
        val file = File(root, "legacy")
        file.writeBytesAtomic(byteArrayOf())
        assertArrayEquals(byteArrayOf(), file.readBytesUncompressedAtomic())
        file.writeBytesAtomic(byteArrayOf(0x1f))
        assertArrayEquals(byteArrayOf(0x1f), file.readBytesUncompressedAtomic())
    }

    @Test
    fun allHandheldCoreStateSlotsAndMetadataSurviveNewManagerInstances() =
        runBlocking {
            for (core in listOf(CoreID.GAMBATTE, CoreID.MGBA, CoreID.MELONDS)) {
                val config = GameSystem.findSystemForCore(core).first().systemCoreConfigs.first { it.coreID == core }
                val snapshot = SaveState(byteArrayOf(1, 2, 3, 4), SaveState.Metadata(2, config.statesVersion))
                states.setSlotSave(game, snapshot, core, 0)
                states.setAutoSave(game, core, snapshot)
                val reloaded = StatesManager(directories)
                for (saved in listOf(reloaded.getSlotSave(game, core, 0), reloaded.getAutoSave(game, core))) {
                    checkNotNull(saved)
                    assertArrayEquals(snapshot.state, saved.state)
                    assertEquals(snapshot.metadata, saved.metadata)
                }
                assertTrue(reloaded.getSavedSlotsInfo(game, core)[0].exists)
                assertFalse(reloaded.getSavedSlotsInfo(game, core)[1].exists)
            }
        }

    @Test
    fun sramSurvivesManagerRecreationAndEmptyCoreDataDoesNotEraseIt() =
        runBlocking {
            val config = GameSystem.findById(SystemID.GBA.dbname).systemCoreConfigs.first()
            val batterySave = ByteArray(8192) { (it * 17).toByte() }
            saves.setSaveRAM(game, batterySave)
            saves.setSaveRAM(game, byteArrayOf())
            assertArrayEquals(batterySave, SavesManager(directories).getSaveRAM(game, config))
        }

    @Test
    fun exhaustedStateWriteFailureIsReportedInsteadOfSilentlySucceeding() =
        runBlocking {
            File(directories.getStatesDirectory(), CoreID.MGBA.coreName).writeText("blocked by ordinary file")
            val failure =
                runCatching {
                    states.setSlotSave(
                        game,
                        SaveState(byteArrayOf(1), SaveState.Metadata()),
                        CoreID.MGBA,
                        0,
                    )
                }
            assertTrue("The caller must know no save was written", failure.exceptionOrNull() is IOException)
        }

    @Test
    fun exhaustedSramWriteFailureIsReportedInsteadOfSilentlySucceeding() =
        runBlocking {
            File(root, "saves").writeText("blocked by ordinary file")
            val failure = runCatching { saves.setSaveRAM(game, byteArrayOf(1)) }
            assertTrue("The caller must know no battery save was written", failure.exceptionOrNull() is IOException)
        }

    @Test
    fun invalidOrEmptyStateDoesNotReplaceExistingSlot() =
        runBlocking {
            val snapshot = SaveState(byteArrayOf(7, 8, 9), SaveState.Metadata(0, 2))
            states.setSlotSave(game, snapshot, CoreID.MELONDS, 0)
            val empty =
                runCatching { states.setSlotSave(game, SaveState(byteArrayOf(), snapshot.metadata), CoreID.MELONDS, 0) }
            assertTrue(empty.exceptionOrNull() is IllegalArgumentException)
            for (slot in listOf(-1, StatesManager.MAX_STATES)) {
                val invalid = runCatching { states.setSlotSave(game, snapshot, CoreID.MELONDS, slot) }
                assertTrue(invalid.exceptionOrNull() is IllegalArgumentException)
            }
            assertArrayEquals(snapshot.state, states.getSlotSave(game, CoreID.MELONDS, 0)!!.state)
            File(directories.getStatesDirectory(), "melonds/${game.fileName}.slot2").writeBytes(byteArrayOf())
            assertFalse(states.getSavedSlotsInfo(game, CoreID.MELONDS)[1].exists)
        }

    @Test
    fun corruptStateReadIsReportedAndDoesNotReplaceTheOriginalBytes() =
        runBlocking {
            val file = File(directories.getStatesDirectory(), "mgba/${game.fileName}.slot1")
            check(file.parentFile!!.mkdirs())
            val corruptGzip = byteArrayOf(0x1f, 0x8b.toByte(), 0, 0, 0)
            file.writeBytes(corruptGzip)
            val failure = runCatching { states.getSlotSave(game, CoreID.MGBA, 0) }
            assertTrue("Corruption must not be interpreted as an absent save", failure.exceptionOrNull() is IOException)
            assertArrayEquals(corruptGzip, file.readBytes())
        }
}
