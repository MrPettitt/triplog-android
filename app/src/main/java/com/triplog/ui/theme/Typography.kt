package com.triplog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun style(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.sp
)

/** Nothing below 12sp, body copy at 16sp, generous line heights for legibility. */
val TripLogTypography = Typography(
    displaySmall = style(FontWeight.Bold, 40, 46, -0.5),
    headlineLarge = style(FontWeight.Bold, 30, 38, -0.3),
    headlineMedium = style(FontWeight.SemiBold, 24, 32, -0.2),
    titleLarge = style(FontWeight.SemiBold, 20, 28),
    titleMedium = style(FontWeight.SemiBold, 16, 24, 0.1),
    titleSmall = style(FontWeight.Medium, 14, 20, 0.1),
    bodyLarge = style(FontWeight.Normal, 16, 24, 0.2),
    bodyMedium = style(FontWeight.Normal, 14, 20, 0.2),
    bodySmall = style(FontWeight.Normal, 13, 18, 0.2),
    labelLarge = style(FontWeight.SemiBold, 15, 20, 0.1),
    labelMedium = style(FontWeight.Medium, 13, 18, 0.3),
    labelSmall = style(FontWeight.Medium, 12, 16, 0.4)
)
