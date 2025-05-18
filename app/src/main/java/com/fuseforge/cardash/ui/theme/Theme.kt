package com.fuseforge.cardash.ui.theme

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

// Racing-inspired dark theme - optimized for driving interfaces
private val RacingDarkScheme = darkColorScheme(
    primary = ElectricBlue,
    primaryContainer = RacingBlue,
    onPrimaryContainer = TextWhite,
    secondary = PerformanceOrange,
    secondaryContainer = Color(0xFF703500), // Dark orange
    onSecondaryContainer = TextWhite,
    tertiary = TechCyan,
    tertiaryContainer = Color(0xFF004D40), // Dark teal
    onTertiaryContainer = TextWhite,
    background = DashDarkBackground,
    surface = DashDarkSurface,
    surfaceVariant = DashDarkSurfaceVariant,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextGrey,
    error = Error,
    errorContainer = Color(0xFF640000) // Dark red
)

// Light racing theme for daytime driving
private val RacingLightScheme = lightColorScheme(
    primary = RacingBlue,
    primaryContainer = TurboBlue,
    onPrimaryContainer = Color.White,
    secondary = PerformanceOrange,
    secondaryContainer = Color(0xFFFFD180), // Light orange
    onSecondaryContainer = Color(0xFF703500), // Dark orange
    tertiary = TechCyan,
    tertiaryContainer = Color(0xFFB2EBF2), // Light cyan
    onTertiaryContainer = Color(0xFF004D40), // Dark teal
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFE0E0E0),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color(0xFF666666),
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
        darkTheme -> RacingDarkScheme
        else -> RacingLightScheme
    }
    
    // Apply racing-inspired styling to status bar
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkCarbon.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}