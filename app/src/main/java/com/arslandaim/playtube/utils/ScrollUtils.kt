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
    // Threshold to prevent jitter (accumulated scroll delta before changing state)
    val scrollOffset = remember { mutableFloatStateOf(0f) }
    var isCurrentlyVisible by remember { mutableStateOf(true) }
    val hideThreshold = -50f // Require more scroll to hide
    val showThreshold = 30f  // Quick reappearance

    // Reset offset when connection is recreated or on specific events if needed
    return remember(onVisibilityChange) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput) return Offset.Zero

                val delta = available.y
                scrollOffset.floatValue += delta

                if (scrollOffset.floatValue < hideThreshold) {
                    if (isCurrentlyVisible) {
                        onVisibilityChange(false)
                        isCurrentlyVisible = false
                    }
                    scrollOffset.floatValue = 0f
                } else if (scrollOffset.floatValue > showThreshold) {
                    if (!isCurrentlyVisible) {
                        onVisibilityChange(true)
                        isCurrentlyVisible = true
                    }
                    scrollOffset.floatValue = 0f
                }

                // Reset offset if direction changes significantly but hasn't hit threshold
                if ((delta > 0 && scrollOffset.floatValue < 0) || (delta < 0 && scrollOffset.floatValue > 0)) {
                    scrollOffset.floatValue = 0f
                }

                return Offset.Zero
            }
        }
    }
}
