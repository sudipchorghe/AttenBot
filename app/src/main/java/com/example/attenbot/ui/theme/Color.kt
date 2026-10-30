package com.example.attenbot.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Apple Settings Light Theme Tokens
val LightPageBackground = Color(0xFFF2F2F7)
val LightGroupedBackground = Color(0xFFFFFFFF)
val LightAccent = Color(0xFF0F6F2F)
val LightTextPrimary = Color(0xFF1C1C1E)
val LightTextSecondary = Color(0xFF6E6E73)
val LightDivider = Color(0xFFD1D1D6)

// Apple Settings Dark Theme Tokens
val DarkPageBackground = Color(0xFF101713)
val DarkGroupedBackground = Color(0xFF1B251F)
val DarkAccent = Color(0xFF9CE5BF)
val DarkTextPrimary = Color(0xFFEEF5F0)
val DarkTextSecondary = Color(0xFFA4B6AA)
val DarkDivider = Color(0xFF34433A)

// Status Color Tokens
val StatusGreen = Color(0xFF34C759)
val StatusOrange = Color(0xFFFF9500)
val StatusRed = Color(0xFFFF3B30)

@Immutable
data class AppleSettingsColors(
    val pageBackground: Color,
    val groupedBackground: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val statusGreen: Color = StatusGreen,
    val statusOrange: Color = StatusOrange,
    val statusRed: Color = StatusRed,
)

val LocalAppleSettingsColors = staticCompositionLocalOf {
    AppleSettingsColors(
        pageBackground = LightPageBackground,
        groupedBackground = LightGroupedBackground,
        accent = LightAccent,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        divider = LightDivider,
    )
}