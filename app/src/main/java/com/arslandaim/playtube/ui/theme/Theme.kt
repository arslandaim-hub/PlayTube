/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.theme

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

@Composable
fun PlayTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColorEnabled: Boolean = false,
    appThemeColor: String = "RED",
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(appThemeColor)

    val darkColors = darkColorScheme(
        primary = palette.primary,
        secondary = palette.secondary,
        tertiary = DarkOnSurfaceVariant,
        background = DarkBackground,
        surface = DarkBackground,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = DarkOnSurface,
        onBackground = DarkOnSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurface,
        onSurfaceVariant = DarkOnSurfaceVariant
    )

    val lightColors = lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        secondary = palette.secondary,
        onSecondary = Color.White,
        tertiary = LightOnSurfaceVariant,
        onTertiary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightBackground,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurface,
        onSurfaceVariant = LightOnSurfaceVariant
    )

    val colorScheme = when {
        isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColors
        else -> lightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
