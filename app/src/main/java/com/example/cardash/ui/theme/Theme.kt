package com.example.cardash.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark automotive theme - optimized for driving in low light
private val DarkAutomotiveScheme = darkColorScheme(
    primary = DashBlue,
    primaryContainer = DashBlueDark,
    secondary = AccentOrange,
    tertiary = AccentCyan,
    background = DashDarkBackground,
    surface = DashDarkSurface,
    surfaceVariant = DashDarkSurfaceVariant,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGrey,
    error = Error
)

// Light theme for daytime driving
private val LightAutomotiveScheme = lightColorScheme(
    primary = DashBlue,
    primaryContainer = DashBlueLight,
    secondary = AccentOrange,
    tertiary = AccentCyan,
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Error
)

@Composable
fun CarDashTheme(
    darkTheme: Boolean = true, // Default to dark theme for automotive
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color for consistent automotive look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkAutomotiveScheme
        else -> LightAutomotiveScheme
    }
    
    // Apply automotive styling to status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DashDarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}