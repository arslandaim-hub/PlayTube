/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.arslandaim.playtube.domain.model.VideoItem
import kotlin.math.roundToInt

@Composable
fun PlayerOverlay(
    isExpanded: Boolean,
    currentVideo: VideoItem?,
    bottomBarHeight: androidx.compose.ui.unit.Dp = 0.dp,
    isIncognito: Boolean = false,
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onMaximize: () -> Unit,
    onMinimize: () -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val visibility by viewModel.miniPlayerManager.visibilityState.collectAsState()
    if (visibility == MiniPlayerVisibility.Hidden) return

    // Keep a local copy of the last non-null video to prevent flicker during transitions
    var lastVideo by remember { mutableStateOf<VideoItem?>(null) }
    LaunchedEffect(currentVideo) {
        if (currentVideo != null) {
            lastVideo = currentVideo
        }
    }

    val displayVideo = currentVideo ?: lastVideo
    if (displayVideo == null) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Dynamic calculation for mini-player position
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues(density).calculateBottomPadding()
    val miniPlayerHeight = 64.dp
    
    // Offset for dragging
    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }

    // Target offset and scale based on state
    // We want the mini-player to sit right above the bottom bar.
    // targetY is the top position of the mini-player.
    val targetY = if (isExpanded) 0f else screenHeight - with(density) { (navBarHeight + bottomBarHeight + miniPlayerHeight).toPx() }
    val targetScale = if (isExpanded) 1f else 0.98f

    val animatedY by animateFloatAsState(
        targetValue = targetY + offsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "PlayerY"
    )

    val animatedX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayerX"
    )

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayerScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // Ensure it's above everything
    ) {
        // SSOT Back Interception: Depend strictly on manager visibility to prevent bleed-through
        BackHandler(enabled = visibility == MiniPlayerVisibility.Expanded) {
            onMinimize()
        }

        if (isExpanded) {
            // Full Screen Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, animatedY.roundToInt()) }
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                    }
                    .pointerInput(isExpanded) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (isExpanded && dragAmount.y > 0) {
                                    offsetY += dragAmount.y
                                }
                            },
                            onDragEnd = {
                                if (offsetY > screenHeight * 0.2f) {
                                    onMinimize()
                                }
                                offsetY = 0f
                            },
                            onDragCancel = {
                                offsetY = 0f
                            }
                        )
                    }
            ) {
                PlayerScreen(
                    videoId = displayVideo.id,
                    initialTitle = displayVideo.title,
                    initialThumbnail = displayVideo.thumbnailUrl,
                    viewModel = viewModel,
                    onBack = onMinimize,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick
                )
            }
        } else {
            // Mini Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                // Require a more significant upward movement to maximize
                                if (dragAmount.y < -15) {
                                    onMaximize()
                                }
                            },
                            onDragEnd = {
                                if (offsetX > screenWidth * 0.3f || offsetX < -screenWidth * 0.3f) {
                                    onClose()
                                }
                                offsetX = 0f
                            },
                            onDragCancel = {
                                offsetX = 0f
                            }
                        )
                    }
            ) {
                MiniPlayerUI(
                    video = displayVideo,
                    player = viewModel.player,
                    isIncognito = isIncognito,
                    onMaximize = onMaximize,
                    onClose = onClose
                )
            }
        }
    }
}
