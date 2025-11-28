package com.cacto.app.ui.theme

/**
 * Theme
 * =====
 *
 * PURPOSE:
 * Defines the application's color scheme and theming. Provides Material 3 theme
 * configuration with custom Cacto color palette. Supports both light and dark modes.
 *
 * WHERE USED:
 * - Imported by: All UI composables (App, screens)
 * - Applied by: CactoTheme composable wrapper
 * - Used in: All Material 3 components throughout the app
 *
 * RELATIONSHIPS:
 * - Defines: Color palette (CactoGreen, CactoPink, etc.)
 * - Provides: Material 3 color schemes (light and dark)
 * - Wraps: MaterialTheme for app-wide theming
 *
 * USAGE IN THEMING:
 * - Wraps entire app UI in CactoTheme composable
 * - Provides consistent colors across all screens
 * - Automatically adapts to system dark mode preference
 * - Custom color palette reflects "desert night" aesthetic
 *
 * DESIGN PHILOSOPHY:
 * Custom color palette inspired by desert/cactus theme. Uses Material 3 design
 * system for consistency. Supports both light and dark modes. Color names are
 * descriptive and easy to reference. Theme automatically adapts to system preference.
 */

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cacto color palette - Desert night vibes
val CactoGreen = Color(0xFF4ECDC4)
val CactoGreenDark = Color(0xFF2D9A8F)
val CactoPink = Color(0xFFE94560)
val CactoPinkDark = Color(0xFFB83A4E)
val CactoPurple = Color(0xFF7B2CBF)
val CactoYellow = Color(0xFFF7D060)
val CactoOrange = Color(0xFFFF6B35)

// Background colors
val DarkBackground = Color(0xFF1A1A2E)
val DarkSurface = Color(0xFF16213E)
val DarkSurfaceVariant = Color(0xFF0F3460)

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9ECEF)

private val DarkColorScheme = darkColorScheme(
    primary = CactoGreen,
    onPrimary = DarkBackground,
    primaryContainer = CactoGreenDark,
    onPrimaryContainer = Color.White,
    secondary = CactoPink,
    onSecondary = Color.White,
    secondaryContainer = CactoPinkDark,
    onSecondaryContainer = Color.White,
    tertiary = CactoPurple,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFFF6B6B),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CactoGreenDark,
    onPrimary = Color.White,
    primaryContainer = CactoGreen.copy(alpha = 0.2f),
    onPrimaryContainer = CactoGreenDark,
    secondary = CactoPink,
    onSecondary = Color.White,
    secondaryContainer = CactoPink.copy(alpha = 0.2f),
    onSecondaryContainer = CactoPinkDark,
    tertiary = CactoPurple,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = DarkBackground,
    surface = LightSurface,
    onSurface = DarkBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF6C757D),
    error = Color(0xFFDC3545),
    onError = Color.White
)

@Composable
fun CactoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

