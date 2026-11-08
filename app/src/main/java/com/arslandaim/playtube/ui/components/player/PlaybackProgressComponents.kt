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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.domain.model.SponsorSegment
import com.arslandaim.playtube.utils.VideoChapter

/**
 * Interactive progress bar that sits at the bottom of the player with horizontal margins,
 * rounded ends, start/end timestamps, and bottom elevation.
 */
@Composable
fun PersistentProgressBar(
    progress: () -> Float,
    bufferedProgress: () -> Float,
    isInteractive: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    chapters: List<VideoChapter> = emptyList(),
    sponsorSegments: List<SponsorSegment> = emptyList(),
    durationMs: Long = 0L,
    activeColor: Color = Color.Red,
    bufferedColor: Color = Color.White.copy(alpha = 0.3f),
    backgroundColor: Color = Color.White.copy(alpha = 0.15f)
) {
    val animatedHeight by animateDpAsState(
        targetValue = if (isInteractive) 6.dp else 2.dp,
        label = "ProgressBarHeight"
    )

    val haptic = LocalHapticFeedback.current

    val hasChapters = chapters.size >= 2 && durationMs > 0L
    var lastChapterIndex by remember { mutableIntStateOf(-1) }

    val segments = remember(chapters, durationMs) {
        if (!hasChapters) {
            listOf(0f to 1f)
        } else {
            val list = mutableListOf<Pair<Float, Float>>()
            for (i in chapters.indices) {
                val startFrac = (chapters[i].startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                val endFrac = if (i < chapters.lastIndex) {
                    (chapters[i + 1].startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                } else {
                    1f
                }
                if (endFrac > startFrac) {
                    list.add(startFrac to endFrac)
                }
            }
            if (list.isEmpty()) listOf(0f to 1f) else list
        }
    }

    val currentChapterTitle = remember(progress(), chapters, durationMs) {
        if (!hasChapters) null else {
            val currentMs = (progress() * durationMs).toLong()
            chapters.findLast { currentMs >= it.startMs }?.title
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 16.dp)
    ) {
        if (isInteractive && !currentChapterTitle.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "• $currentChapterTitle",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isInteractive) 40.dp else animatedHeight)
                .then(
                    if (isInteractive) {
                        Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                                }
                            }
                            .pointerInput(hasChapters, durationMs) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                                    if (hasChapters && durationMs > 0) {
                                        val currentMs = (frac * durationMs).toLong()
                                        val currentIdx = chapters.indexOfLast { currentMs >= it.startMs }
                                        if (currentIdx != -1 && currentIdx != lastChapterIndex) {
                                            lastChapterIndex = currentIdx
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    onSeek(frac)
                                }
                            }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            val width = constraints.maxWidth.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight)
            ) {
                val centerY = size.height / 2
                val strokeWidth = animatedHeight.toPx()
                val gapPx = if (hasChapters) 3.dp.toPx() else 0f
                val currentProgFrac = progress().coerceIn(0f, 1f)
                val currentBufFrac = bufferedProgress().coerceIn(0f, 1f)

                for ((index, seg) in segments.withIndex()) {
                    val segStartFrac = seg.first
                    val segEndFrac = seg.second

                    var segStartPx = segStartFrac * width
                    var segEndPx = segEndFrac * width

                    if (hasChapters) {
                        if (index > 0) segStartPx += gapPx / 2f
                        if (index < segments.lastIndex) segEndPx -= gapPx / 2f
                    }

                    if (segEndPx <= segStartPx) continue

                    // Background line
                    drawLine(
                        color = backgroundColor,
                        start = Offset(segStartPx, centerY),
                        end = Offset(segEndPx, centerY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    // Buffered line
                    val bufEndPx = (currentBufFrac * width).coerceIn(segStartPx, segEndPx)
                    if (bufEndPx > segStartPx) {
                        drawLine(
                            color = bufferedColor,
                            start = Offset(segStartPx, centerY),
                            end = Offset(bufEndPx, centerY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }

                    // Progress line
                    val progEndPx = (currentProgFrac * width).coerceIn(segStartPx, segEndPx)
                    if (progEndPx > segStartPx) {
                        drawLine(
                            color = activeColor,
                            start = Offset(segStartPx, centerY),
                            end = Offset(progEndPx, centerY),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // SponsorBlock Segment Markers
                if (durationMs > 0L) {
                    for (sponsor in sponsorSegments) {
                        val startFrac = (sponsor.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val endFrac = (sponsor.endMs.toFloat() / durationMs).coerceIn(0f, 1f)
                        val sPx = startFrac * width
                        val ePx = endFrac * width
                        if (ePx > sPx) {
                            val segmentColor = when (sponsor.category.lowercase()) {
                                "sponsor" -> Color(0xFFFFD700)
                                "intro", "outro" -> Color(0xFF00E5FF)
                                "selfpromo", "interaction" -> Color(0xFF00E676)
                                "music_offtopic" -> Color(0xFFFF4081)
                                else -> Color(0xFFFFA000)
                            }
                            drawLine(
                                color = segmentColor,
                                start = Offset(sPx, centerY),
                                end = Offset(ePx, centerY),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
