package com.triplog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand
val Brand400 = Color(0xFF6C8CFF)
val Brand500 = Color(0xFF4F6FF0)
val Brand700 = Color(0xFF3550C8)
val BrandContainer = Color(0xFF1C2754)
val Amber400 = Color(0xFFFFB547)

// Surfaces (layered from darkest to lightest)
val Surface900 = Color(0xFF0B0F17)
val Surface800 = Color(0xFF111723)
val Surface700 = Color(0xFF171F2E)
val Surface600 = Color(0xFF1F2939)
val Surface500 = Color(0xFF2B374A)

// Text (all pass WCAG AA on Surface900)
val TextPrimary = Color(0xFFF4F6FA)
val TextSecondary = Color(0xFFB4BED0)
val TextTertiary = Color(0xFF8B96AB)

// Status
val GreenSync = Color(0xFF4ADE80)
val RedError = Color(0xFFFF6B6B)

val BrandGradient = Brush.linearGradient(listOf(Brand500, Brand700))

private val DarkColorScheme = darkColorScheme(
    primary = Brand400,
    onPrimary = Surface900,
    primaryContainer = BrandContainer,
    onPrimaryContainer = TextPrimary,
    secondary = Amber400,
    onSecondary = Surface900,
    secondaryContainer = Surface600,
    onSecondaryContainer = TextPrimary,
    background = Surface900,
    onBackground = TextPrimary,
    surface = Surface800,
    onSurface = TextPrimary,
    surfaceVariant = Surface700,
    onSurfaceVariant = TextSecondary,
    outline = Surface500,
    outlineVariant = Surface600,
    error = RedError,
    onError = Surface900,
)

@Composable
fun TripLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = TripLogTypography,
        content = content
    )
}
