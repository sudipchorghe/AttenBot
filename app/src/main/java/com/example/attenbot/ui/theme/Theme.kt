package com.example.attenbot.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightAppleColors = AppleSettingsColors(
    pageBackground = LightPageBackground,
    groupedBackground = LightGroupedBackground,
    accent = LightAccent,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    divider = LightDivider
)

private val DarkAppleColors = AppleSettingsColors(
    pageBackground = DarkPageBackground,
    groupedBackground = DarkGroupedBackground,
    accent = DarkAccent,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    background = LightPageBackground,
    onBackground = LightTextPrimary,
    surface = LightGroupedBackground,
    onSurface = LightTextPrimary,
    surfaceContainer = LightGroupedBackground,
    onSurfaceVariant = LightTextSecondary,
    outlineVariant = LightDivider
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color.White,
    background = DarkPageBackground,
    onBackground = DarkTextPrimary,
    surface = DarkGroupedBackground,
    onSurface = DarkTextPrimary,
    surfaceContainer = DarkGroupedBackground,
    onSurfaceVariant = DarkTextSecondary,
    outlineVariant = DarkDivider
)

object AppleTheme {
    val colors: AppleSettingsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppleSettingsColors.current
}

@Composable
fun AttenBotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val appleColors = if (darkTheme) DarkAppleColors else LightAppleColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalAppleSettingsColors provides appleColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
