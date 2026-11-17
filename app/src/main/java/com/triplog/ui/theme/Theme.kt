package com.triplog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Colours that change with the light or dark theme. Screens use the named properties below
 * (Surface900, TextPrimary and so on), which read the palette of the current theme.
 */
@Immutable
class AppColors(
    val isDark: Boolean,
    val brand400: Color,
    val amber400: Color,
    val greenSync: Color,
    val redError: Color,
    // Surfaces, layered from the page background (900) to outlines (500)
    val surface900: Color,
    val surface800: Color,
    val surface700: Color,
    val surface600: Color,
    val surface500: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color
)

/** All text colours pass WCAG AA (4.5:1) on Surface900 and Surface700. */
val DarkAppColors = AppColors(
    isDark = true,
    brand400 = Color(0xFF6C8CFF),
    amber400 = Color(0xFFFFB547),
    greenSync = Color(0xFF4ADE80),
    redError = Color(0xFFFF6B6B),
    surface900 = Color(0xFF0B0F17),
    surface800 = Color(0xFF111723),
    surface700 = Color(0xFF171F2E),
    surface600 = Color(0xFF1F2939),
    surface500 = Color(0xFF2B374A),
    textPrimary = Color(0xFFF4F6FA),
    textSecondary = Color(0xFFB4BED0),
    textTertiary = Color(0xFF8B96AB)
)

val LightAppColors = AppColors(
    isDark = false,
    brand400 = Color(0xFF2F49B8),
    amber400 = Color(0xFF8A5300),
    greenSync = Color(0xFF1B7A43),
    redError = Color(0xFFC0262D),
    surface900 = Color(0xFFF6F8FC),
    surface800 = Color(0xFFFFFFFF),
    surface700 = Color(0xFFFFFFFF),
    surface600 = Color(0xFFE6EBF3),
    surface500 = Color(0xFFC9D1DF),
    textPrimary = Color(0xFF0F1623),
    textSecondary = Color(0xFF47536A),
    textTertiary = Color(0xFF5B677C)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// Brand colours that look the same in both themes (white text on Brand500 is about 5:1)
val Brand500 = Color(0xFF4863E8)
val Brand700 = Color(0xFF3550C8)
val BrandGradient = Brush.linearGradient(listOf(Brand500, Brand700))

val Brand400: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.brand400
val Amber400: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.amber400
val GreenSync: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.greenSync
val RedError: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.redError

val Surface900: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface900
val Surface800: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface800
val Surface700: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface700
val Surface600: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface600
val Surface500: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface500

val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
val TextTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textTertiary

private fun materialColors(c: AppColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.brand400,
        onPrimary = c.surface900,
        primaryContainer = Color(0xFF1C2754),
        onPrimaryContainer = c.textPrimary,
        secondary = c.amber400,
        onSecondary = c.surface900,
        secondaryContainer = c.surface600,
        onSecondaryContainer = c.textPrimary,
        background = c.surface900,
        onBackground = c.textPrimary,
        surface = c.surface800,
        onSurface = c.textPrimary,
        surfaceVariant = c.surface700,
        onSurfaceVariant = c.textSecondary,
        outline = c.surface500,
        outlineVariant = c.surface600,
        error = c.redError,
        onError = c.surface900
    )
} else {
    lightColorScheme(
        primary = c.brand400,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDCE3FF),
        onPrimaryContainer = c.textPrimary,
        secondary = c.amber400,
        onSecondary = Color.White,
        secondaryContainer = c.surface600,
        onSecondaryContainer = c.textPrimary,
        background = c.surface900,
        onBackground = c.textPrimary,
        surface = c.surface800,
        onSurface = c.textPrimary,
        surfaceVariant = c.surface600,
        onSurfaceVariant = c.textSecondary,
        outline = c.surface500,
        outlineVariant = c.surface600,
        error = c.redError,
        onError = Color.White
    )
}

@Composable
fun TripLogTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = materialColors(colors),
            typography = TripLogTypography,
            content = content
        )
    }
}
