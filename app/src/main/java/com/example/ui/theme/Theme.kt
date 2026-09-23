package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisSpaceBlack,
    primaryContainer = JarvisBlueDark,
    onPrimaryContainer = JarvisCyanBright,
    secondary = JarvisBlue,
    onSecondary = JarvisTextPrimary,
    secondaryContainer = JarvisSurfaceCard,
    onSecondaryContainer = JarvisCyan,
    tertiary = JarvisNeonTeal,
    onTertiary = JarvisSpaceBlack,
    background = JarvisSpaceBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisSurfaceDark,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisSurfaceElevated,
    onSurfaceVariant = JarvisTextSecondary,
    outline = JarvisBorderCyan,
    error = JarvisRed,
    onError = JarvisTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Jarvis is designed dark sci-fi HUD
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = JarvisSpaceBlack.toArgb()
                it.navigationBarColor = JarvisSpaceBlack.toArgb()
                WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(it, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
