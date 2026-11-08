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
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun PlayTubeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColorEnabled: Boolean = false,
    appThemeColor: String = "RED",
    isOledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = getThemePalette(appThemeColor)

    val darkColors = darkColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryContainer,
        onSecondaryContainer = palette.onSecondaryContainer,
        tertiary = palette.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = palette.tertiaryContainer,
        onTertiaryContainer = palette.onTertiaryContainer,
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        surfaceContainerLowest = DarkSurfaceContainerLowest,
        surfaceContainerLow = DarkSurfaceContainerLow,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceContainerHigh,
        surfaceContainerHighest = DarkSurfaceContainerHighest,
        outline = palette.primary.copy(alpha = 0.40f),
        outlineVariant = palette.primary.copy(alpha = 0.20f)
    )

    val oledColors = darkColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryContainer,
        onSecondaryContainer = palette.onSecondaryContainer,
        tertiary = palette.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = palette.tertiaryContainer,
        onTertiaryContainer = palette.onTertiaryContainer,
        background = OledBackground,
        onBackground = OledOnSurface,
        surface = OledSurface,
        onSurface = OledOnSurface,
        surfaceVariant = OledSurfaceVariant,
        onSurfaceVariant = OledOnSurfaceVariant,
        surfaceContainerLowest = OledSurfaceContainerLowest,
        surfaceContainerLow = OledSurfaceContainerLow,
        surfaceContainer = OledSurfaceContainer,
        surfaceContainerHigh = OledSurfaceContainerHigh,
        surfaceContainerHighest = OledSurfaceContainerHighest,
        outline = palette.primary.copy(alpha = 0.45f),
        outlineVariant = palette.primary.copy(alpha = 0.25f)
    )

    val lightColors = lightColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
        primaryContainer = palette.primaryContainer,
        onPrimaryContainer = palette.onPrimaryContainer,
        secondary = palette.secondary,
        onSecondary = Color.White,
        secondaryContainer = palette.secondaryContainer,
        onSecondaryContainer = palette.onSecondaryContainer,
        tertiary = palette.tertiary,
        onTertiary = Color.White,
        tertiaryContainer = palette.tertiaryContainer,
        onTertiaryContainer = palette.onTertiaryContainer,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        surfaceContainerLowest = LightSurfaceContainerLowest,
        surfaceContainerLow = LightSurfaceContainerLow,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceContainerHigh,
        surfaceContainerHighest = LightSurfaceContainerHighest,
        outline = palette.primary.copy(alpha = 0.30f),
        outlineVariant = palette.primary.copy(alpha = 0.15f)
    )

    val colorScheme = when {
        isDynamicColorEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme || isOledTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isOledTheme -> oledColors
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
                val isLight = !darkTheme && !isOledTheme
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun settingsSwitchColors(): SwitchColors {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f
    val thumbColor = if (isDarkTheme) Color.White else Color.Black

    return SwitchDefaults.colors(
        checkedThumbColor = thumbColor,
        uncheckedThumbColor = thumbColor,
        checkedTrackColor = if (isDarkTheme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        checkedBorderColor = Color.Transparent,
        uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        checkedIconColor = if (isDarkTheme) Color.Black else Color.White,
        uncheckedIconColor = if (isDarkTheme) Color.Black else Color.White
    )
}
