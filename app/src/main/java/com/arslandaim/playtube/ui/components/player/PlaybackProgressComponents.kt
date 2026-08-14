/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Interactive progress bar that sits at the bottom of the player.
 * Animates its height when [isInteractive] is true.
 */
@Composable
fun PersistentProgressBar(
    progress: () -> Float,
    bufferedProgress: () -> Float,
    isInteractive: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.Red,
    bufferedColor: Color = Color.White.copy(alpha = 0.3f),
    backgroundColor: Color = Color.White.copy(alpha = 0.15f)
) {
    val animatedHeight by animateDpAsState(
        targetValue = if (isInteractive) 6.dp else 2.dp,
        label = "ProgressBarHeight"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isInteractive) 32.dp else animatedHeight)
            .then(
                if (isInteractive) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                onSeek((offset.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                onSeek((change.position.x / size.width).coerceIn(0f, 1f))
                            }
                        }
                } else Modifier
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        val width = constraints.maxWidth.toFloat()
        
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
        ) {
            val centerY = size.height / 2
            val strokeWidth = animatedHeight.toPx()

            // Background
            drawLine(
                color = backgroundColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidth
            )

            // Buffered
            drawLine(
                color = bufferedColor,
                start = Offset(0f, centerY),
                end = Offset(width * bufferedProgress().coerceIn(0f, 1f), centerY),
                strokeWidth = strokeWidth
            )

            // Progress
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(width * progress().coerceIn(0f, 1f), centerY),
                strokeWidth = strokeWidth
            )
        }
    }
}
