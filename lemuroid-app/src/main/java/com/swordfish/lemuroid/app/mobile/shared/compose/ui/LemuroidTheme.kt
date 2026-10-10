package com.swordfish.lemuroid.app.mobile.shared.compose.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Complete neutral fallback roles; no implicit purple/colored Material defaults. */
internal val NeutralLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF505358),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE3E5E8),
        onPrimaryContainer = Color(0xFF191B1E),
        secondary = Color(0xFF565C61),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0E3E5),
        onSecondaryContainer = Color(0xFF1A1E21),
        tertiary = Color(0xFF5B5B60),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE4E2E7),
        onTertiaryContainer = Color(0xFF202024),
        background = Color(0xFFF5F5F4),
        onBackground = Color(0xFF1B1D20),
        surface = Color(0xFFFAFAF9),
        onSurface = Color(0xFF1B1D20),
        surfaceVariant = Color(0xFFE3E4E4),
        onSurfaceVariant = Color(0xFF45484B),
        surfaceDim = Color(0xFFDADADA),
        surfaceBright = Color(0xFFFAFAF9),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF4F4F3),
        surfaceContainer = Color(0xFFEEEEED),
        surfaceContainerHigh = Color(0xFFE8E8E7),
        surfaceContainerHighest = Color(0xFFE2E2E1),
        outline = Color(0xFF75787B),
        outlineVariant = Color(0xFFC5C7C8),
        inverseSurface = Color(0xFF303133),
        inverseOnSurface = Color(0xFFF1F1F0),
        inversePrimary = Color(0xFFE3E5E8),
        surfaceTint = Color(0xFF505358),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        scrim = Color.Black,
        primaryFixed = Color(0xFFE3E5E8),
        primaryFixedDim = Color(0xFFE3E5E8),
        onPrimaryFixed = Color(0xFF191B1E),
        onPrimaryFixedVariant = Color(0xFF383B40),
        secondaryFixed = Color(0xFFE0E3E5),
        secondaryFixedDim = Color(0xFFC3C7CB),
        onSecondaryFixed = Color(0xFF1A1E21),
        onSecondaryFixedVariant = Color(0xFF43494D),
        tertiaryFixed = Color(0xFFE4E2E7),
        tertiaryFixedDim = Color(0xFFC8C6CB),
        onTertiaryFixed = Color(0xFF202024),
        onTertiaryFixedVariant = Color(0xFF47464B),
    )

internal val NeutralDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFE3E5E8),
        onPrimary = Color(0xFF202122),
        primaryContainer = Color(0xFF383B40),
        onPrimaryContainer = Color(0xFFE3E5E8),
        secondary = Color(0xFFC3C7CB),
        onSecondary = Color(0xFF2D3236),
        secondaryContainer = Color(0xFF43494D),
        onSecondaryContainer = Color(0xFFE0E3E5),
        tertiary = Color(0xFFC8C6CB),
        onTertiary = Color(0xFF313035),
        tertiaryContainer = Color(0xFF47464B),
        onTertiaryContainer = Color(0xFFE4E2E7),
        background = Color(0xFF000000),
        onBackground = Color(0xFFE3E3E2),
        surface = Color(0xFF0D0E10),
        onSurface = Color(0xFFE3E3E2),
        surfaceVariant = Color(0xFF45484B),
        onSurfaceVariant = Color(0xFFC5C7C8),
        surfaceDim = Color(0xFF000000),
        surfaceBright = Color(0xFF38393A),
        surfaceContainerLowest = Color(0xFF060708),
        surfaceContainerLow = Color(0xFF1B1D20),
        surfaceContainer = Color(0xFF202122),
        surfaceContainerHigh = Color(0xFF292B2D),
        surfaceContainerHighest = Color(0xFF343638),
        outline = Color(0xFF8F9295),
        outlineVariant = Color(0xFF45484B),
        inverseSurface = Color(0xFFE3E3E2),
        inverseOnSurface = Color(0xFF303133),
        inversePrimary = Color(0xFF505358),
        surfaceTint = Color(0xFFE3E5E8),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        scrim = Color.Black,
        primaryFixed = Color(0xFFE3E5E8),
        primaryFixedDim = Color(0xFFE3E5E8),
        onPrimaryFixed = Color(0xFF191B1E),
        onPrimaryFixedVariant = Color(0xFF383B40),
        secondaryFixed = Color(0xFFE0E3E5),
        secondaryFixedDim = Color(0xFFC3C7CB),
        onSecondaryFixed = Color(0xFF1A1E21),
        onSecondaryFixedVariant = Color(0xFF43494D),
        tertiaryFixed = Color(0xFFE4E2E7),
        tertiaryFixedDim = Color(0xFFC8C6CB),
        onTertiaryFixed = Color(0xFF202024),
        onTertiaryFixedVariant = Color(0xFF47464B),
    )

/** Wallpaper colors come from Android's Material You resources, not a fixed accent. */
internal fun consoleColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean,
): ColorScheme {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        try {
            return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } catch (_: Resources.NotFoundException) {
            // Some OEM builds omit dynamic palette resources. Keep the neutral theme usable.
        }
    }
    return if (darkTheme) NeutralDarkColorScheme else NeutralLightColorScheme
}

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
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean? = null,
    updateSystemBarIcons: Boolean = false,
    content: @Composable () -> Unit,
) {
    val themeState by rememberThemePreferences()
    val preferences = themeState.preferences
    val resolvedDark = darkTheme ?: preferences.mode.isDark(isSystemInDarkTheme())
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val colorScheme =
        remember(
            context,
            configuration,
            themeState.resumeGeneration,
            resolvedDark,
            dynamicColor,
            preferences.dynamicColor,
        ) {
            consoleColorScheme(context, resolvedDark, dynamicColor ?: preferences.dynamicColor)
        }

    // Only launcher/ordinary chrome opts in. Native gameplay retains its black,
    // immersive system bars; setting a theme must never show or hide those bars.
    val view = LocalView.current
    if (updateSystemBarIcons && !view.isInEditMode) {
        SideEffect {
            context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !resolvedDark
                    isAppearanceLightNavigationBars = !resolvedDark
                }
            }
        }
    }

    MaterialExpressiveTheme(
        motionScheme = MotionScheme.expressive(),
        colorScheme = colorScheme,
        typography = ConsoleTypography,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(20.dp),
                large = RoundedCornerShape(28.dp),
                extraLarge = RoundedCornerShape(36.dp),
            ),
    ) {
        val haptics = rememberConsoleHaptics()
        CompositionLocalProvider(
            LocalThemePreferences provides preferences,
            LocalConsoleHaptics provides haptics,
            content = content,
        )
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
