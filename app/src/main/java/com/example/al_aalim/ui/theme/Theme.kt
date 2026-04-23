package com.example.al_aalim.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Al-Aalim always uses a dark teal + gold scheme
private val AlAalimColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = GoldDark,
    onPrimaryContainer = Color.White,

    secondary = PrimaryTeal,
    onSecondary = Color.White,
    secondaryContainer = SecondaryTeal,
    onSecondaryContainer = Color.White,

    tertiary = GoldRich,
    onTertiary = Color(0xFF1A1A2E),

    background = Color(0xFF0A3D3F),
    onBackground = Color.White,

    surface = Color(0xFF0D5C5E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1A7A7C),
    onSurfaceVariant = Color(0xAAFFFFFF),

    error = AccentRed,
    onError = Color.White,

    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x22FFFFFF),
)

@Composable
fun AlAalimTheme(
    darkTheme: Boolean = true, // always dark teal
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = AlAalimColorScheme,
        typography = AlAalimTypography,
        content = content,
    )
}
