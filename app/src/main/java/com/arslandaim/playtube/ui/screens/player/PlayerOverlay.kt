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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    // Dynamic calculation for mini-player position
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues(density).calculateBottomPadding()
    val miniPlayerWidth = 228.dp
    val miniPlayerHeight = 128.25.dp
    
    // Offset for dragging
    var offsetY by remember { mutableFloatStateOf(0f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    val miniPlayerWidthPx = with(density) { miniPlayerWidth.toPx() }
    val horizontalMarginPx = with(density) { 16.dp.toPx() }
    val defaultMiniX = screenWidth - miniPlayerWidthPx - horizontalMarginPx

    // Persistent horizontal position for the mini-player pill
    var persistentMiniPlayerOffsetX by rememberSaveable { mutableFloatStateOf(defaultMiniX) }

    val dismissalThresholdPx = with(density) { 60.dp.toPx() }

    // Visual feedback states: only trigger dismissal feedback in the middle 50% bottom area
    val isNearDismissalZone = remember(offsetY, persistentMiniPlayerOffsetX, offsetX) {
        if (isExpanded) false else {
            val currentAbsoluteX = persistentMiniPlayerOffsetX + offsetX
            val playerCenterAbsoluteX = currentAbsoluteX + (miniPlayerWidthPx / 2f)
            offsetY > dismissalThresholdPx && playerCenterAbsoluteX in (screenWidth * 0.25f)..(screenWidth * 0.75f)
        }
    }

    // Ensure mini player resets to corner when minimized
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            persistentMiniPlayerOffsetX = defaultMiniX
        }
    }

    // Target offset and scale based on state
    val bottomBarSpacing = 16.dp + bottomBarHeight
    
    val targetY = if (isExpanded) 0f else screenHeight - with(density) { (navBarHeight + bottomBarSpacing + miniPlayerHeight).toPx() }
    val targetX = if (isExpanded) 0f else persistentMiniPlayerOffsetX

    val animatedTargetY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "PlayerY",
        finishedListener = {
            if (it == targetY) {
                isMinimizing = false
            }
        }
    )

    val animatedTargetX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayerX"
    )

    val animatedScale by animateFloatAsState(
        targetValue = when {
            isExpanded -> 1f
            isNearDismissalZone -> 0.88f
            else -> 1f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PlayerScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isNearDismissalZone) 0.75f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "PlayerAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f) // Ensure it's above everything
    ) {
        // SSOT Back Interception: Maintain handler during minimize animation to prevent back-bleed
        // Use key to force re-registration when player state or navigation route changes, 
        // ensuring this handler is always the "most recent" in the dispatcher.
        key(isExpanded, isMinimizing, isLandscape, currentRoute) {
            BackHandler(enabled = isExpanded || isMinimizing) {
                if (isLandscape) {
                    (localContext as? android.app.Activity)?.requestedOrientation = 
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    safeMinimize()
                }
            }
        }

        if (isExpanded) {
            // Full Screen Player
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = animatedTargetY + offsetY
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    }
                    .pointerInput(isExpanded, isLandscape) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (isExpanded && dragAmount.y > 0) {
                                    offsetY += dragAmount.y
                                }
                            },
                            onDragEnd = {
                                if (offsetY > screenHeight * 0.2f) {
                                    if (isLandscape) {
                                        (localContext as? android.app.Activity)?.requestedOrientation = 
                                            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        safeMinimize()
                                    }
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
                    .graphicsLayer {
                        translationX = animatedTargetX + offsetX
                        translationY = animatedTargetY + offsetY
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
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
                                val isInMiddleZone = playerCenterAbsoluteX in (screenWidth * 0.25f)..(screenWidth * 0.75f)

                                val verticalThreshold = with(density) { 60.dp.toPx() }
                                val upThreshold = with(density) { 80.dp.toPx() }
                                
                                if (offsetY < -upThreshold) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMaximize()
                                } else if (offsetY > verticalThreshold && isInMiddleZone) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClose()
                                } else {
                                    persistentMiniPlayerOffsetX = (targetX + offsetX).coerceIn(
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
