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

// Theme Colors
data class ThemePalette(
    val name: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color
)

val AppThemePalettes = listOf(
    ThemePalette("RED", "Classic Red", Color(0xFFFF0000), Color(0xFFB90000)),
    ThemePalette("BLUE", "Ocean Blue", Color(0xFF1E88E5), Color(0xFF1565C0)),
    ThemePalette("GREEN", "Emerald Green", Color(0xFF2E7D32), Color(0xFF1B5E20)),
    ThemePalette("PURPLE", "Royal Purple", Color(0xFF8E24AA), Color(0xFF6A1B9A)),
    ThemePalette("AMBER", "Sunset Amber", Color(0xFFF57C00), Color(0xFFE65100)),
    ThemePalette("CYAN", "Neon Cyan", Color(0xFF00ACC1), Color(0xFF00838F)),
    ThemePalette("PINK", "Rose Pink", Color(0xFFE91E63), Color(0xFFC2185B))
)

fun getThemePalette(colorName: String): ThemePalette {
    return AppThemePalettes.find { it.name.equals(colorName, ignoreCase = true) }
        ?: AppThemePalettes.first()
}

// Light Theme Colors
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF9F9F9)
val LightOnSurface = Color(0xFF0F0F0F)
val LightOnSurfaceVariant = Color(0xFF606060)

// Dark Theme Colors (Deep Dark/OLED)
val DarkBackground = Color(0xFF0F0F0F)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFFAAAAAA)

// Special Colors
val IncognitoPurple = Color(0xFF673AB7)
val OnlineGreen = Color(0xFF2E7D32)

// Standard Alpha for Glass/Surface effects
const val GlassAlpha = 0.90f
const val ScrimAlpha = 0.4f
