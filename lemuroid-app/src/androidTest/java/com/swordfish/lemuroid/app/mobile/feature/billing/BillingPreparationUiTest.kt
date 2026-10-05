package com.swordfish.lemuroid.app.mobile.feature.billing

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingPreparationUiTest {
    @get:Rule val compose = createAndroidComposeRule<BillingActivity>()

    @Test fun unconfiguredCheckoutIsDisabledAndRestoreRemainsAvailable() {
        compose.onNodeWithText("Subscribe monthly").assertIsNotEnabled()
        compose.onNodeWithText("Restore purchases").assertIsEnabled().performClick()
        compose.onNodeWithText("Purchases are not available yet.").assertExists()
        compose.onNodeWithText("Subscribe monthly").assertIsNotEnabled()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = File(instrumentation.targetContext.getExternalFilesDir(null), "billing-preparation.png")
        assertTrue(UiDevice.getInstance(instrumentation).takeScreenshot(screenshot))
    }

    @Test fun saveExportIsAvailableWithoutEntitlementAndOpensRealDocumentPicker() {
        compose.onNodeWithText("Export saves and previews").performScrollTo().assertIsEnabled().performClick()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        assertTrue(device.wait(Until.hasObject(By.pkg("com.google.android.documentsui")), 10000) ||
            device.wait(Until.hasObject(By.pkg("com.android.documentsui")), 10000))
        device.pressBack()
    }
}
