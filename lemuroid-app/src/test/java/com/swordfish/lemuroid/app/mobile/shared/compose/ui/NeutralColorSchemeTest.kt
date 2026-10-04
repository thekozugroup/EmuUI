package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class NeutralColorSchemeTest {
    @Test fun lightFallbackHasAccessibleContentRolePairs() {
        assertContentContrast(NeutralLightColorScheme)
    }

    @Test fun darkFallbackHasAccessibleContentRolePairs() {
        assertContentContrast(NeutralDarkColorScheme)
    }

    @Test fun lightAndDarkSurfaceLuminanceActuallyDiffer() {
        assertTrue(NeutralLightColorScheme.background.luminance() > 0.9f)
        assertTrue(NeutralDarkColorScheme.background.luminance() < 0.01f)
    }

    @Test fun neutralSurfacesHaveNoStrongColorCast() {
        listOf(NeutralLightColorScheme, NeutralDarkColorScheme).forEach { scheme ->
            listOf(scheme.background, scheme.surface, scheme.surfaceContainer, scheme.surfaceContainerHighest).forEach {
                val channels = listOf(it.red, it.green, it.blue)
                assertTrue(channels.max() - channels.min() < 0.02f)
            }
        }
    }

    private fun assertContentContrast(scheme: ColorScheme) {
        val pairs =
            listOf(
                "primary" to (scheme.primary to scheme.onPrimary),
                "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
                "secondary" to (scheme.secondary to scheme.onSecondary),
                "secondaryContainer" to (scheme.secondaryContainer to scheme.onSecondaryContainer),
                "tertiary" to (scheme.tertiary to scheme.onTertiary),
                "tertiaryContainer" to (scheme.tertiaryContainer to scheme.onTertiaryContainer),
                "background" to (scheme.background to scheme.onBackground),
                "guidanceTextButton" to (scheme.background to scheme.primary),
                "guidanceSupportingText" to (scheme.background to scheme.onSurfaceVariant),
                "surfaceTextButton" to (scheme.surface to scheme.primary),
                "surface" to (scheme.surface to scheme.onSurface),
                "surfaceVariant" to (scheme.surfaceVariant to scheme.onSurfaceVariant),
                "surfaceContainer" to (scheme.surfaceContainer to scheme.onSurface),
                "surfaceContainerLow" to (scheme.surfaceContainerLow to scheme.onSurface),
                "surfaceContainerHigh" to (scheme.surfaceContainerHigh to scheme.onSurface),
                "surfaceContainerHighest" to (scheme.surfaceContainerHighest to scheme.onSurface),
                "surfaceContainerLowest" to (scheme.surfaceContainerLowest to scheme.onSurface),
                "inverseSurface" to (scheme.inverseSurface to scheme.inverseOnSurface),
                "error" to (scheme.error to scheme.onError),
                "errorContainer" to (scheme.errorContainer to scheme.onErrorContainer),
                "primaryFixed" to (scheme.primaryFixed to scheme.onPrimaryFixed),
                "primaryFixedDim" to (scheme.primaryFixedDim to scheme.onPrimaryFixedVariant),
                "secondaryFixed" to (scheme.secondaryFixed to scheme.onSecondaryFixed),
                "secondaryFixedDim" to (scheme.secondaryFixedDim to scheme.onSecondaryFixedVariant),
                "tertiaryFixed" to (scheme.tertiaryFixed to scheme.onTertiaryFixed),
                "tertiaryFixedDim" to (scheme.tertiaryFixedDim to scheme.onTertiaryFixedVariant),
            )
        pairs.forEach { (name, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$name text contrast was $ratio:1, expected at least 4.5:1", ratio >= 4.5f)
        }
    }

    private fun contrast(
        first: Color,
        second: Color,
    ): Float {
        val light = maxOf(first.luminance(), second.luminance())
        val dark = minOf(first.luminance(), second.luminance())
        return (light + 0.05f) / (dark + 0.05f)
    }
}
