/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Unified Material 3 Shape System for PlayTube.
 * Ensures consistent corner radii across video cards, buttons, chips, dialogs, and bottom sheets.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

// Semantic Shape Helpers
val CardShape = RoundedCornerShape(16.dp)
val CompactCardShape = RoundedCornerShape(12.dp)
val ChipShape = RoundedCornerShape(20.dp)
val ButtonShape = RoundedCornerShape(24.dp)
val SheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val ThumbnailShape = RoundedCornerShape(12.dp)
val SearchShape = RoundedCornerShape(28.dp)
val BadgeShape = RoundedCornerShape(6.dp)
val CircleShapeToken = CircleShape
