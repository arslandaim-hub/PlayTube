/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Composable
fun rememberScrollVisibilityConnection(
    onVisibilityChange: (Boolean) -> Unit
): NestedScrollConnection {
    // Local state to keep track of visibility
    var isCurrentlyVisible by remember { mutableStateOf(true) }
    
    // Reset offset when connection is recreated or on specific events if needed
    return remember(onVisibilityChange) {
        var accumulatedDelta = 0f
        val hideThreshold = -50f // Require more scroll to hide
        val showThreshold = 30f  // Quick reappearance
        
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val delta = available.y
                accumulatedDelta += delta

                if (accumulatedDelta < hideThreshold) {
                    if (isCurrentlyVisible) {
                        onVisibilityChange(false)
                        isCurrentlyVisible = false
                    }
                    accumulatedDelta = 0f
                } else if (accumulatedDelta > showThreshold) {
                    if (!isCurrentlyVisible) {
                        onVisibilityChange(true)
                        isCurrentlyVisible = true
                    }
                    accumulatedDelta = 0f
                }

                // Reset offset if direction changes significantly but hasn't hit threshold
                if ((delta > 0 && accumulatedDelta < 0) || (delta < 0 && accumulatedDelta > 0)) {
                    accumulatedDelta = 0f
                }

                return Offset.Zero
            }
        }
    }
}
