package com.swordfish.lemuroid.app.mobile.feature.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Framework drawable renders and an actual emulator launcher capture, not browser mockups. */
@SdkSuppress(minSdkVersion = 33)
class LauncherIconRegressionTest {
    @Test
    fun nativeAdaptiveMasksLegacyAndThemedMark() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val baseline = InstrumentationRegistry.getArguments().getString("baseline") == "true"
        val output = File(context.getExternalFilesDir(null), "local-notch-qa").apply { mkdirs() }
        val icon = context.packageManager.getApplicationIcon(context.applicationInfo)
        val adaptive = icon as? AdaptiveIconDrawable
        if (!baseline) assertTrue("Launcher must receive an adaptive icon", adaptive != null)
        val size = 216
        for (shape in listOf("circle", "squircle", "rounded-square")) {
            val image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            val mask = Path()
            when (shape) {
                "circle" -> mask.addCircle(108f, 108f, 108f, Path.Direction.CW)
                "squircle" -> {
                    mask.moveTo(108f, 0f)
                    mask.cubicTo(202f, 0f, 216f, 14f, 216f, 108f)
                    mask.cubicTo(216f, 202f, 202f, 216f, 108f, 216f)
                    mask.cubicTo(14f, 216f, 0f, 202f, 0f, 108f)
                    mask.cubicTo(0f, 14f, 14f, 0f, 108f, 0f)
                    mask.close()
                }
                else -> mask.addRoundRect(0f, 0f, 216f, 216f, 48f, 48f, Path.Direction.CW)
            }
            canvas.clipPath(mask)
            icon.setBounds(0, 0, size, size)
            if (adaptive != null) {
                // Retain the framework's foreground/background overscan bounds for each OEM mask.
                adaptive.background.draw(canvas)
                adaptive.foreground.draw(canvas)
            } else {
                icon.draw(canvas)
            }
            if (!baseline) {
                val edge = image.getPixel(108, 2)
                assertTrue("Masked edge must be full bleed", Color.alpha(edge) == 255)
                assertTrue("No white backing plate", Color.red(edge) < 180)
            }
            output.resolve("icon-$shape.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
        adaptive?.monochrome?.let { mark ->
            val image = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(image)
            canvas.drawColor(Color.rgb(220, 227, 233))
            mark.setBounds(-54, -54, size + 54, size + 54)
            mark.setTint(Color.rgb(38, 47, 56))
            mark.draw(canvas)
            output.resolve("icon-themed.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
        }
        if (!baseline) assertTrue("Explicit themed mark required", adaptive?.monochrome != null)
        val legacyId = context.resources.getIdentifier("emuui_launcher", "drawable", context.packageName)
        val legacy = context.getDrawable(legacyId)!!
        val legacyImage = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        legacy.setBounds(0, 0, size, size)
        legacy.draw(Canvas(legacyImage))
        output.resolve("icon-legacy.png").outputStream().use {
            legacyImage.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        legacyImage.recycle()
        val device = UiDevice.getInstance(instrumentation)
        device.pressHome()
        device.swipe(device.displayWidth / 2, device.displayHeight - 80, device.displayWidth / 2, 120, 30)
        assertTrue(
            "EmuUI must appear in the real launcher",
            device.wait(Until.hasObject(By.textContains("EmuUI")), 15_000),
        )
        device.takeScreenshot(output.resolve("actual-launcher.png"))
    }
}
