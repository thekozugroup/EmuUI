package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference
import androidx.window.testing.layout.FoldingFeature as TestFoldingFeature

/**
 * Device smoke tests. They deliberately use the public UI and real storage picker.
 * Run on a disposable landscape emulator with English system locale. No ROM is
 * required, no database rows are fabricated, and no runtime permission is granted.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivitySmokeTest {
    @get:Rule(order = 0)
    val windowInfo = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device get() = UiDevice.getInstance(instrumentation)

    @Test
    fun launchShowsLibraryAndImportWithoutAutomaticallyStartingAGame() {
        waitForLibrary()
        compose.onNodeWithContentDescription("EmuUI home").assertIsDisplayed()
        compose.onNodeWithText("All games").assertIsSelected()
        compose.onNodeWithContentDescription("Import games folder").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            assertEquals(MainActivity::class.java, activity.javaClass)
        }
    }

    @Test
    fun selectedFilterSurvivesActivityRecreation() {
        waitForLibrary()
        compose.onNodeWithText("Favorites").performClick().assertIsSelected()
        val oldActivity = AtomicReference<MainActivity>()
        compose.activityRule.scenario.onActivity(oldActivity::set)

        compose.activityRule.scenario.recreate()

        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
        compose.activityRule.scenario.onActivity { assertNotSame(oldActivity.get(), it) }
        compose.onNodeWithText("All games").performClick().assertIsSelected()
    }

    @Test
    fun cancellingFolderPickerTwicePreservesDirectoryAndReadGrants() {
        waitForLibrary()
        val context = instrumentation.targetContext
        // Package visibility can hide a handler from the instrumented app even
        // though startActivity works. Resolve via the disposable emulator's shell;
        // still open/cancel the real picker through the app's normal UI below.
        val pickerPackage =
            device.executeShellCommand("cmd package resolve-activity --brief -a android.intent.action.OPEN_DOCUMENT_TREE")
                .lineSequence().map(String::trim)
                .lastOrNull { it.matches(Regex("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+")) }
                ?.substringBefore('/')
        checkNotNull(pickerPackage) { "The emulator must have a real Storage Access Framework picker" }
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val key = context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder)
        val originalFolder = preferences.getString(key, null)
        val originalGrants =
            context.contentResolver.persistedUriPermissions
                .map { Triple(it.uri.toString(), it.isReadPermission, it.isWritePermission) }.toSet()

        repeat(2) {
            compose.waitUntil(TIMEOUT) {
                runCatching {
                    compose.onNodeWithContentDescription("Import games folder").assertIsEnabled()
                    true
                }.getOrDefault(false)
            }
            compose.onNodeWithContentDescription("Import games folder").performClick()
            check(device.wait(Until.hasObject(By.pkg(pickerPackage).depth(0)), TIMEOUT)) {
                "Folder picker did not appear; inspect the captured hierarchy and logcat"
            }
            cancelPicker(pickerPackage)
            waitForLibrary()
            assertEquals("Cancel must preserve the selected folder", originalFolder, preferences.getString(key, null))
            assertEquals(
                "Cancel must not replace persisted read grants",
                originalGrants,
                context.contentResolver.persistedUriPermissions
                    .map { Triple(it.uri.toString(), it.isReadPermission, it.isWritePermission) }.toSet(),
            )
        }
    }

    @Test
    fun portraitGuidanceReturnsToLandscapeWithoutLosingFilter() {
        waitForLibrary()
        compose.onNodeWithText("Favorites").performClick()
        val originalRotation = device.displayRotation
        val systemRotationRequired = android.os.Build.VERSION.SDK_INT >= 36 &&
            compose.activity.resources.configuration.smallestScreenWidthDp >= 600
        try {
            if (systemRotationRequired) {
                // API 36 ignores Activity orientation requests on large screens.
                // Rotate the disposable emulator itself to exercise the real configuration path.
                instrumentation.uiAutomation.setRotation((originalRotation + 1) % 4)
            } else {
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
            }
            compose.waitUntil(TIMEOUT) {
                runCatching {
                    compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                }.getOrDefault(false)
            }
            // A new Activity/configuration correctly discards stale fold data. Publish
            // current-window metadata only after its composition has subscribed.
            compose.waitForIdle()
            publishOpenInnerDisplay()
            compose.waitUntil(TIMEOUT) {
                runCatching {
                    compose.onAllNodes(hasText("Turn to your happy place")).fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
            }
            compose.onNodeWithText(
                "Use EmuUI in landscape, with the top screen above the library.",
            ).assertIsDisplayed()
            compose.onAllNodesWithContentDescription("Import games folder").fetchSemanticsNodes().let {
                assertEquals("Portrait guidance must not leave hidden import controls interactive", 0, it.size)
            }
        } finally {
            if (systemRotationRequired) {
                instrumentation.uiAutomation.setRotation(originalRotation)
                instrumentation.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
            } else {
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }
        }
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }.getOrDefault(false)
        }
        waitForLibrary()
        compose.onNodeWithText("Favorites").assertIsSelected()
    }

    private fun cancelPicker(pickerPackage: String) {
        // SAF remembers its last directory. Back may navigate to a parent before cancelling.
        // Stop as soon as the picker leaves the foreground; never send Back to the library.
        val pickerRoot = By.pkg(pickerPackage).depth(0)
        val deadline = SystemClock.uptimeMillis() + TIMEOUT
        while (device.hasObject(pickerRoot) && SystemClock.uptimeMillis() < deadline) {
            device.pressBack()
            if (device.wait(Until.gone(pickerRoot), 3_000)) return
        }
        check(!device.hasObject(pickerRoot)) { "Folder picker did not cancel within the timeout" }
    }

    private fun waitForLibrary() {
        publishOpenInnerDisplay()
        compose.waitUntil(TIMEOUT) {
            runCatching {
                compose.onAllNodes(hasContentDescription("EmuUI home")).fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        compose.waitForIdle()
    }

    private companion object {
        const val TIMEOUT = 30_000L
    }

    private fun publishOpenInnerDisplay() {
        compose.waitForIdle()
        // Test-process-only WindowManager event. Production has no posture override.
        windowInfo.overrideWindowLayoutInfo(
            TestWindowLayoutInfo(
                listOf(
                    TestFoldingFeature(
                        activity = compose.activity,
                        state = FoldingFeature.State.FLAT,
                        orientation = FoldingFeature.Orientation.HORIZONTAL,
                    ),
                ),
            ),
        )
    }
}
