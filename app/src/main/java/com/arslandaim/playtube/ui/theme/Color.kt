/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Branding (Red)
val YoutubeRed = Color(0xFFFF0000)
val DarkRed = Color(0xFFB90000)

// Extended Theme Palette Data Class
data class ThemePalette(
    val name: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val primaryContainer: Color = primary.copy(alpha = 0.20f),
    val onPrimaryContainer: Color = primary,
    val secondaryContainer: Color = secondary.copy(alpha = 0.20f),
    val onSecondaryContainer: Color = secondary,
    val tertiaryContainer: Color = tertiary.copy(alpha = 0.18f),
    val onTertiaryContainer: Color = tertiary
)

val AppThemePalettes = listOf(
    ThemePalette(
        name = "RED",
        displayName = "Classic Red",
        primary = Color(0xFFE53935),
        secondary = Color(0xFFD81B60),
        tertiary = Color(0xFFFF8A65),
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondaryContainer = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF3E001D)
    ),
    ThemePalette(
        name = "BLUE",
        displayName = "Ocean Blue",
        primary = Color(0xFF1E88E5),
        secondary = Color(0xFF00ACC1),
        tertiary = Color(0xFF5C6BC0),
        primaryContainer = Color(0xFFD7E3FF),
        onPrimaryContainer = Color(0xFF001B3E),
        secondaryContainer = Color(0xFFC2E8FF),
        onSecondaryContainer = Color(0xFF001E2B)
    ),
    ThemePalette(
        name = "GREEN",
        displayName = "Emerald Green",
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF00897B),
        tertiary = Color(0xFF7CB342),
        primaryContainer = Color(0xFFB7F397),
        onPrimaryContainer = Color(0xFF042100),
        secondaryContainer = Color(0xFFA7F0E4),
        onSecondaryContainer = Color(0xFF00201B)
    ),
    ThemePalette(
        name = "PURPLE",
        displayName = "Royal Purple",
        primary = Color(0xFF8E24AA),
        secondary = Color(0xFFD81B60),
        tertiary = Color(0xFF5C6BC0),
        primaryContainer = Color(0xFFF6D8FF),
        onPrimaryContainer = Color(0xFF320048),
        secondaryContainer = Color(0xFFFFD9E2),
        onSecondaryContainer = Color(0xFF3E001D)
    ),
    ThemePalette(
        name = "AMBER",
        displayName = "Sunset Amber",
        primary = Color(0xFFF57C00),
        secondary = Color(0xFFE53935),
        tertiary = Color(0xFFFBC02D),
        primaryContainer = Color(0xFFFFDCC1),
        onPrimaryContainer = Color(0xFF301400),
        secondaryContainer = Color(0xFFFFDAD6),
        onSecondaryContainer = Color(0xFF410002)
    ),
    ThemePalette(
        name = "CYAN",
        displayName = "Neon Cyan",
        primary = Color(0xFF00ACC1),
        secondary = Color(0xFF1E88E5),
        tertiary = Color(0xFF26A69A),
        primaryContainer = Color(0xFFC2E8FF),
        onPrimaryContainer = Color(0xFF001E2B),
        secondaryContainer = Color(0xFFD7E3FF),
        onSecondaryContainer = Color(0xFF001B3E)
    ),
    ThemePalette(
        name = "PINK",
        displayName = "Rose Pink",
        primary = Color(0xFFE91E63),
        secondary = Color(0xFF8E24AA),
        tertiary = Color(0xFFFF7043),
        primaryContainer = Color(0xFFFFD9E2),
        onPrimaryContainer = Color(0xFF3E001D),
        secondaryContainer = Color(0xFFF6D8FF),
        onSecondaryContainer = Color(0xFF320048)
    )
)

fun getThemePalette(colorName: String): ThemePalette {
    return AppThemePalettes.find { it.name.equals(colorName, ignoreCase = true) }
        ?: AppThemePalettes.first()
}

// Light Theme Surface Containers & Colors
val LightBackground = Color(0xFFF9F9FB)
val LightSurface = Color(0xFFF0F2F5)
val LightSurfaceVariant = Color(0xFFE4E7EC)
val LightOnSurface = Color(0xFF0F0F12)
val LightOnSurfaceVariant = Color(0xFF5A5C63)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF5F6F9)
val LightSurfaceContainer = Color(0xFFEEF0F4)
val LightSurfaceContainerHigh = Color(0xFFE6E8ED)
val LightSurfaceContainerHighest = Color(0xFFDCDEE3)

// Dark Theme Surface Containers & Colors (Deep Charcoal Dark)
val DarkBackground = Color(0xFF0F0F12)
val DarkSurface = Color(0xFF18181C)
val DarkSurfaceVariant = Color(0xFF24242A)
val DarkOnSurface = Color(0xFFF5F5F7)
val DarkOnSurfaceVariant = Color(0xFFA2A4AD)
val DarkSurfaceContainerLowest = Color(0xFF0A0A0C)
val DarkSurfaceContainerLow = Color(0xFF141418)
val DarkSurfaceContainer = Color(0xFF1C1C22)
val DarkSurfaceContainerHigh = Color(0xFF25252D)
val DarkSurfaceContainerHighest = Color(0xFF2E2E38)

// OLED Theme Surface Containers & Colors (Pure Pitch Black #000000)
val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF0C0C0C)
val OledSurfaceVariant = Color(0xFF161616)
val OledOnSurface = Color(0xFFFFFFFF)
val OledOnSurfaceVariant = Color(0xFFB0B0B0)
val OledSurfaceContainerLowest = Color(0xFF000000)
val OledSurfaceContainerLow = Color(0xFF080808)
val OledSurfaceContainer = Color(0xFF121212)
val OledSurfaceContainerHigh = Color(0xFF1C1C1C)
val OledSurfaceContainerHighest = Color(0xFF262626)

// Special & Feature Accent Colors
val IncognitoPurple = Color(0xFF7C4DFF)
val OnlineGreen = Color(0xFF00C853)
val OfflineGray = Color(0xFF757575)

// Standard Alpha for Glass & Elevation Overlays
const val GlassAlpha = 0.88f
const val ScrimAlpha = 0.50f
