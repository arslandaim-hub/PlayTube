/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ModernPlaybackProgress(
    progress: Float,
    bufferedProgress: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.Red,
    bufferedColor: Color = Color.White.copy(alpha = 0.35f),
    backgroundColor: Color = Color.White.copy(alpha = 0.15f)
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
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
            },
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        
        Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            val centerY = size.height / 2

            // Background
            drawLine(
                color = backgroundColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 4.dp.toPx()
            )

            // Buffered
            drawLine(
                color = bufferedColor,
                start = Offset(0f, centerY),
                end = Offset(width * bufferedProgress.coerceIn(0f, 1f), centerY),
                strokeWidth = 4.dp.toPx()
            )

            // Progress
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(width * progress.coerceIn(0f, 1f), centerY),
                strokeWidth = 4.dp.toPx()
            )
        }

        // Thumb
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = with(density) { (width * progress).toDp() } - 6.dp)
                .size(12.dp)
                .background(activeColor, CircleShape)
        )
    }
}

@Composable
fun PersistentProgressBar(
    progress: () -> Float,
    bufferedProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val width = size.width
        
        // Background
        drawRect(
            color = Color.White.copy(alpha = 0.1f),
            size = size
        )
        
        // Buffered
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            size = size.copy(width = width * bufferedProgress().coerceIn(0f, 1f))
        )
        
        // Progress
        drawRect(
            color = Color.Red,
            size = size.copy(width = width * progress().coerceIn(0f, 1f))
        )
    }
}
