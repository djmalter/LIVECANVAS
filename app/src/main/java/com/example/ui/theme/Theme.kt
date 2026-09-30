package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = Color.White,
    secondary = TealAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color.White,
    tertiary = AmberWarning,
    onTertiary = Color.Black,
    background = NearBlackCharcoal,
    onBackground = TextPrimary,
    surface = DarkSlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSlateBorder,
    error = CoralDanger,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme( // Dark-first studio creator tool
    primary = ElectricIndigo,
    onPrimary = Color.White,
    primaryContainer = DeepViolet,
    onPrimaryContainer = Color.White,
    secondary = TealAccent,
    onSecondary = Color.Black,
    background = NearBlackCharcoal,
    onBackground = TextPrimary,
    surface = DarkSlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSlateSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkSlateBorder,
    error = CoralDanger,
    onError = Color.White
)

@Composable
fun LiveCanvasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = LiveCanvasTheme(darkTheme, dynamicColor, content)
