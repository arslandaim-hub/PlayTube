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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.media3.common.util.UnstableApi
import com.arslandaim.playtube.domain.model.VideoItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@UnstableApi
@Composable
fun PlayerOverlay(
    isExpanded: Boolean,
    currentVideo: VideoItem?,
    bottomBarHeight: androidx.compose.ui.unit.Dp = 0.dp,
    isIncognito: Boolean = false,
    viewModel: PlayerViewModel,
    navController: NavHostController? = null,
    onClose: () -> Unit,
    onMaximize: () -> Unit,
    onMinimize: () -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val visibility by viewModel.miniPlayerManager.visibilityState.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    
    // Track navigation state to force BackHandler re-registration
    val navBackStackEntry by navController?.currentBackStackEntryAsState() ?: remember { mutableStateOf(null) }
    val currentRoute = navBackStackEntry?.destination?.route

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

    var isMinimizing by remember { mutableStateOf(false) }
    val localContext = androidx.compose.ui.platform.LocalContext.current

    val safeMinimize = {
        if (!isMinimizing) {
            isMinimizing = true
            (localContext as? android.app.Activity)?.requestedOrientation = 
                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onMinimize()
        }
    }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Dynamic calculation for mini-player position
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues(density).calculateBottomPadding()
    val miniPlayerWidth = 200.dp
    val miniPlayerHeight = 112.5.dp
    
    // Offset for dragging
    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    val miniPlayerWidthPx = with(density) { miniPlayerWidth.toPx() }
    val horizontalMarginPx = with(density) { 16.dp.toPx() }
    val defaultMiniX = screenWidth - miniPlayerWidthPx - horizontalMarginPx

    // Persistent horizontal position for the mini-player pill
    var persistentMiniPlayerOffsetX by rememberSaveable { mutableFloatStateOf(defaultMiniX) }

    // Visual feedback states
    val isNearDismissalZone = remember(offsetY, persistentMiniPlayerOffsetX, offsetX) {
        if (isExpanded) false else {
            val currentAbsoluteX = persistentMiniPlayerOffsetX + offsetX
            val playerCenterAbsoluteX = currentAbsoluteX + (miniPlayerWidthPx / 2f)
            val middleZoneStart = screenWidth * 0.3f
            val middleZoneEnd = screenWidth * 0.7f
            offsetY > with(density) { 50.dp.toPx() } && playerCenterAbsoluteX in middleZoneStart..middleZoneEnd
        }
    }

    // Ensure mini player resets to corner when minimized
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            persistentMiniPlayerOffsetX = defaultMiniX
        }
    }

    // Target offset and scale based on state
    // We want the mini-player to sit above the new floating bottom bar.
    // The bottom bar has 20.dp bottom padding + 64.dp height + 8.dp gap.
    val bottomBarSpacing = if (bottomBarHeight > 0.dp) bottomBarHeight + 28.dp else 16.dp
    val targetY = if (isExpanded) 0f else screenHeight - with(density) { (navBarHeight + bottomBarSpacing + miniPlayerHeight).toPx() }
    
    // X is either 0 (full screen) or the persistent offset (mini)
    val targetX = if (isExpanded) 0f else persistentMiniPlayerOffsetX
    
    val targetScale = when {
        isExpanded -> 1f
        isNearDismissalZone -> 0.85f // Scale down to indicate dismissal
        else -> 1f
    }

    val animatedY by animateFloatAsState(
        targetValue = targetY + offsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "PlayerY",
        finishedListener = {
            if (it == targetY) {
                isMinimizing = false
            }
        }
    )

    val animatedX by animateFloatAsState(
        targetValue = targetX + offsetX,
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
        // SSOT Back Interception: Maintain handler during minimize animation to prevent back-bleed
        // Use key to force re-registration when player state or navigation route changes, 
        // ensuring this handler is always the "most recent" in the dispatcher.
        key(isExpanded, isMinimizing, currentRoute) {
            BackHandler(enabled = isExpanded || isMinimizing) {
                safeMinimize()
            }
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
                                    safeMinimize()
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
                    onBack = safeMinimize,
                    onVideoClick = onVideoClick,
                    onChannelClick = onChannelClick,
                    onAddToPlaylistClick = onAddToPlaylistClick
                )
            }
        } else {
            // Mini Player - Rectangle - Horizontally Movable
            Box(
                modifier = Modifier
                    .width(miniPlayerWidth)
                    .height(miniPlayerHeight)
                    .offset { IntOffset(animatedX.roundToInt(), animatedY.roundToInt()) }
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = if (isNearDismissalZone) 0.7f else 1f
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                
                                // Real-time horizontal clamping
                                val newOffsetX = offsetX + dragAmount.x
                                val absoluteX = targetX + newOffsetX
                                if (absoluteX in horizontalMarginPx..(screenWidth - miniPlayerWidthPx - horizontalMarginPx)) {
                                    offsetX = newOffsetX
                                }
                                
                                offsetY += dragAmount.y
                            },
                            onDragEnd = {
                                val currentAbsoluteX = targetX + offsetX
                                val playerCenterAbsoluteX = currentAbsoluteX + (miniPlayerWidthPx / 2f)
                                
                                val verticalThreshold = with(density) { 120.dp.toPx() }
                                
                                // Determine action
                                var actionTriggered = false
                                if (offsetY < -verticalThreshold) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onMaximize()
                                    actionTriggered = true
                                } else if (offsetY > verticalThreshold) {
                                    // Close only if swiped down in the middle 40% of the screen
                                    val middleZoneStart = screenWidth * 0.3f
                                    val middleZoneEnd = screenWidth * 0.7f
                                    if (playerCenterAbsoluteX in middleZoneStart..middleZoneEnd) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onClose()
                                        actionTriggered = true
                                    }
                                }
                                
                                // Commit horizontal position if no vertical action was taken
                                if (!actionTriggered) {
                                    val finalX = targetX + offsetX
                                    persistentMiniPlayerOffsetX = finalX.coerceIn(
                                        horizontalMarginPx, 
                                        screenWidth - miniPlayerWidthPx - horizontalMarginPx
                                    )
                                }
                                
                                offsetX = 0f
                                offsetY = 0f
                            },
                            onDragCancel = {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        )
                    }
            ) {
                MiniPlayerUI(
                    player = viewModel.player,
                    isPlaying = isPlaying,
                    isIncognito = isIncognito,
                    onMaximize = onMaximize,
                    onPlayPause = viewModel::togglePlayPause,
                    onClose = onClose
                )
            }
        }
    }
}
