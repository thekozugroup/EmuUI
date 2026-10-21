package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** EmuUI's warm hardware palette deliberately stays independent of wallpaper colors. */
object ConsoleColors {
    val Ink = Color(0xFF252D2B)
    val Shell = Color(0xFFF2F0E6)
    val Paper = Color(0xFFFCFBF5)
    val Muted = Color(0xFF647069)
    val Lime = Color(0xFFD0EB82)
    val Moss = Color(0xFF486B35)
    val Lilac = Color(0xFFDAD6EF)
    val Peach = Color(0xFFF0CCAD)
    val Screen = Color(0xFF202D29)
    val Outline = Color(0xFFD6D9CC)
}

private val LightColorScheme =
    lightColorScheme(
        primary = ConsoleColors.Moss,
        onPrimary = Color.White,
        primaryContainer = ConsoleColors.Lime,
        onPrimaryContainer = ConsoleColors.Ink,
        secondary = Color(0xFF615A79),
        onSecondary = Color.White,
        secondaryContainer = ConsoleColors.Lilac,
        onSecondaryContainer = Color(0xFF29243E),
        tertiary = Color(0xFF895131),
        tertiaryContainer = ConsoleColors.Peach,
        onTertiaryContainer = Color(0xFF382215),
        background = ConsoleColors.Shell,
        onBackground = ConsoleColors.Ink,
        surface = ConsoleColors.Paper,
        onSurface = ConsoleColors.Ink,
        surfaceVariant = Color(0xFFE8EADF),
        onSurfaceVariant = ConsoleColors.Muted,
        outline = Color(0xFF777F74),
        outlineVariant = ConsoleColors.Outline,
        surfaceTint = ConsoleColors.Moss,
        // Expressive components read these roles; keep them on the warm shell, not baseline lilac.
        surfaceBright = ConsoleColors.Paper,
        surfaceDim = Color(0xFFDCDACF),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF7F6EE),
        surfaceContainer = Color(0xFFF0EFE6),
        surfaceContainerHigh = Color(0xFFEAE9DF),
        surfaceContainerHighest = Color(0xFFE4E3D8),
        inverseSurface = ConsoleColors.Ink,
        inverseOnSurface = ConsoleColors.Paper,
        inversePrimary = ConsoleColors.Lime,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = ConsoleColors.Lime,
        onPrimary = ConsoleColors.Ink,
        primaryContainer = Color(0xFF384F2D),
        onPrimaryContainer = ConsoleColors.Lime,
        secondary = ConsoleColors.Lilac,
        secondaryContainer = Color(0xFF464052),
        background = Color(0xFF19201D),
        onBackground = ConsoleColors.Paper,
        surface = ConsoleColors.Screen,
        onSurface = ConsoleColors.Paper,
        surfaceVariant = Color(0xFF354139),
        onSurfaceVariant = Color(0xFFC0CABD),
        outline = Color(0xFF899584),
        surfaceContainerLowest = Color(0xFF141A17),
        surfaceContainerLow = Color(0xFF1D2521),
        surfaceContainer = Color(0xFF222B27),
        surfaceContainerHigh = Color(0xFF2B3530),
        surfaceContainerHighest = Color(0xFF35403A),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ConsoleTypography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 40.sp,
                letterSpacing = (-1.2).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.8).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.6).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.4).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 1.sp,
            ),
        // Expressive emphasis: heavier weight and tighter tracking on the same size steps.
        headlineLargeEmphasized =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                lineHeight = 34.sp,
                letterSpacing = (-1.0).sp,
            ),
        titleLargeEmphasized =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 26.sp,
                letterSpacing = (-0.5).sp,
            ),
        titleMediumEmphasized =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.1).sp,
            ),
        labelLargeEmphasized =
            TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(),
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = ConsoleTypography,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                largeIncreased = RoundedCornerShape(32.dp),
                extraLarge = RoundedCornerShape(36.dp),
                extraLargeIncreased = RoundedCornerShape(40.dp),
                extraExtraLarge = RoundedCornerShape(48.dp),
            ),
    ) {
        CompositionLocalProvider(LocalBlurAvailable provides rememberBlurAvailable(), content = content)
    }
}
