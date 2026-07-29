/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.R
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.PlaybackSpeedSelectionSheet
import com.arslandaim.playtube.ui.components.QualitySelectionSheet
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.ui.components.rememberSyncShimmerTransition
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun PlayerScreen(
    videoId: String,
    initialTitle: String? = null,
    initialThumbnail: String? = null,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val currentQuality by viewModel.currentQuality.collectAsStateWithLifecycle()
    val isBuffering by viewModel.isBuffering.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by viewModel.libraryRepository.getFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val seekAmount by viewModel.seekAmount.collectAsStateWithLifecycle()
    val showSeekFeedback by viewModel.showSeekFeedback.collectAsStateWithLifecycle()
    val isSeekForward by viewModel.isSeekForward.collectAsStateWithLifecycle()
    val isCcEnabled by viewModel.isCcEnabled.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val bufferedPosition by viewModel.bufferedPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isRecovering by viewModel.isRecovering.collectAsStateWithLifecycle()
    val syncTransition = rememberSyncShimmerTransition()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    PlayerContent(
        videoId = videoId,
        initialTitle = initialTitle,
        initialThumbnail = initialThumbnail,
        uiState = uiState,
        isFavorite = isFavorite,
        isSubscribed = isSubscribed,
        playbackSpeed = playbackSpeed,
        currentQuality = currentQuality,
        isBuffering = isBuffering,
        isRecovering = isRecovering,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        seekAmount = seekAmount,
        showSeekFeedback = showSeekFeedback,
        isSeekForward = isSeekForward,
        isCcEnabled = isCcEnabled,
        currentPosition = currentPosition,
        bufferedPosition = bufferedPosition,
        duration = duration,
        downloadState = downloadState,
        player = viewModel.player,
        syncTransition = syncTransition,
        snackbarMessage = viewModel.snackbarMessage,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onToggleSubscription = viewModel::toggleSubscription,
        onSetQuality = viewModel::setQuality,
        onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
        onToggleSubtitles = viewModel::toggleSubtitles,
        onDownloadConfirm = viewModel::download,
        onDownloadClick = { viewModel.prepareDownload(it) },
        onDismissDownload = viewModel::dismissDownloadDialog,
        onLoadMore = viewModel::loadNextRelatedPage,
        onSeekForward = viewModel::seekForward,
        onSeekBackward = viewModel::seekBackward,
        onSeekTo = viewModel::seekTo,
        onShareVideo = viewModel::shareVideo,
        onBack = onBack, // Use the provided onBack lambda which is passed from PlayerOverlay as onMinimize
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerContent(
    videoId: String,
    initialTitle: String?,
    initialThumbnail: String?,
    uiState: PlayerUiState,
    isFavorite: Boolean,
    isSubscribed: Boolean,
    playbackSpeed: Float,
    currentQuality: String?,
    isBuffering: Boolean,
    isRecovering: Boolean,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    seekAmount: Int,
    showSeekFeedback: Boolean,
    isSeekForward: Boolean,
    isCcEnabled: Boolean,
    currentPosition: Long,
    bufferedPosition: Long,
    duration: Long,
    downloadState: DownloadDialogState,
    player: Player,
    syncTransition: androidx.compose.animation.core.InfiniteTransition,
    snackbarMessage: SharedFlow<String>,
    onToggleFavorite: (VideoItem?) -> Unit,
    onToggleSubscription: () -> Unit,
    onSetQuality: (com.arslandaim.playtube.domain.model.StreamItem) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleSubtitles: () -> Unit,
    onDownloadConfirm: (VideoItem, StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDownloadClick: (VideoItem?) -> Unit,
    onDismissDownload: () -> Unit,
    onLoadMore: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShareVideo: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Gesture states
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    var brightnessOverlayVisible by remember { mutableStateOf(false) }
    var volumeOverlayVisible by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Initialize brightnessLevel
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val layoutParams = activity?.window?.attributes
        brightnessLevel = if (layoutParams?.screenBrightness ?: -1f < 0) {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } else {
            layoutParams?.screenBrightness ?: 0.5f
        }
    }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val listState = rememberLazyListState()
    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemsCount = listState.layoutInfo.totalItemsCount
            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItemIndex >= totalItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Reset orientation on dispose
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (showQualityDialog) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            QualitySelectionSheet(
                videoStreams = it.bundle.videoStreams,
                currentQuality = currentQuality,
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { stream ->
                    onSetQuality(stream)
                    showQualityDialog = false
                }
            )
        }
    }

    if (showSpeedSheet) {
        PlaybackSpeedSelectionSheet(
            currentSpeed = playbackSpeed,
            onDismiss = { showSpeedSheet = false },
            onSpeedSelected = { speed ->
                onSetPlaybackSpeed(speed)
                showSpeedSheet = false
            }
        )
    }

    if (showDescriptionSheet) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            ModalBottomSheet(
                onDismissRequest = { showDescriptionSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.description),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it.bundle.description ?: stringResource(R.string.no_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.quality)) },
                    supportingContent = { Text(currentQuality ?: stringResource(R.string.auto)) },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showQualityDialog = true
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.playback_speed)) },
                    supportingContent = { Text(if (playbackSpeed == 1f) stringResource(R.string.normal_speed) else "${playbackSpeed}x") },
                    leadingContent = { Icon(Icons.Default.Speed, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showSpeedSheet = true
                    }
                )
            }
        }
    }

    LaunchedEffect(brightnessOverlayVisible) {
        if (brightnessOverlayVisible) {
            delay(3000)
            brightnessOverlayVisible = false
        }
    }

    LaunchedEffect(volumeOverlayVisible) {
        if (volumeOverlayVisible) {
            delay(3000)
            volumeOverlayVisible = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dynamic Blurred Background
            if (uiState is PlayerUiState.Success || initialThumbnail != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data((uiState as? PlayerUiState.Success)?.bundle?.thumbnailUrl ?: initialThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.15f } // Faded for text readability
                        .blur(80.dp), // Strong blur for immersive feel
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier.aspectRatio(16f / 9f))
                        .background(Color.Black)
                ) {
                    when (uiState) {
                        is PlayerUiState.Loading, is PlayerUiState.Error -> {
                            // Show high-res placeholder during loading or error
                            ThumbnailImage(
                                videoId = videoId,
                                thumbnailUrl = initialThumbnail ?: VideoUtils.getBestThumbnailUrl(videoId),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High
                            )
                            if (uiState is PlayerUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    color = Color.White
                                )
                            }
                        }
                        is PlayerUiState.Success -> {
                            VideoPlayerGestureDetector(
                                onDoubleTapLeft = onSeekBackward,
                                onDoubleTapRight = onSeekForward,
                                onSingleTap = { controlsVisible = !controlsVisible },
                                onSwipeDown = onBack,
                                onSwipeUp = {
                                    val activity = context as? Activity
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                },
                                onDragStart = {
                                    isDragging = true
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    volumeLevel = currentVolume.toFloat() / maxVolume
                                },
                                onVerticalSwipeLeft = { dragPercentage ->
                                    brightnessLevel = (brightnessLevel + dragPercentage).coerceIn(0f, 1f)
                                    val activity = context as? Activity
                                    val layoutParams = activity?.window?.attributes
                                    layoutParams?.screenBrightness = brightnessLevel
                                    activity?.window?.attributes = layoutParams
                                    
                                    brightnessOverlayVisible = true
                                    volumeOverlayVisible = false
                                },
                                onVerticalSwipeRight = { dragPercentage ->
                                    volumeLevel = (volumeLevel + dragPercentage).coerceIn(0f, 1f)
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val newVolume = (volumeLevel * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    
                                    volumeOverlayVisible = true
                                    brightnessOverlayVisible = false
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) {
                                VideoPlayerView(
                                    player = player,
                                    controlsVisible = controlsVisible,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isRecovering) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = Color.White)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(R.string.waiting_for_connection),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }

                            // Vertical HUDs (Left: Brightness, Right: Volume)
                            VerticalGestureHUD(
                                visible = brightnessOverlayVisible,
                                progress = brightnessLevel,
                                icon = Icons.Default.BrightnessLow,
                                isRightSide = false,
                                modifier = Modifier.align(Alignment.CenterStart)
                            )
                            
                            VerticalGestureHUD(
                                visible = volumeOverlayVisible,
                                progress = volumeLevel,
                                icon = Icons.Default.VolumeUp,
                                isRightSide = true,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )

                            // Persistent Progress Bar (Always visible at the very bottom)
                            PersistentProgressBar(
                                progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                            )

                            // Custom Controls Overlay
                            androidx.compose.animation.AnimatedVisibility(
                                visible = controlsVisible,
                                enter = androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.fadeOut()
                            ) {
                                PlayerControlsOverlay(
                                    isPlaying = player.isPlaying,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    isCcEnabled = isCcEnabled,
                                    hasSubtitles = uiState.bundle.subtitles.isNotEmpty(),
                                    onPlayPause = { if (player.isPlaying) player.pause() else player.play() },
                                    onSeekTo = onSeekTo,
                                    onToggleSubtitles = onToggleSubtitles,
                                    onShowSettings = { showSettingsSheet = true },
                                    onBack = onBack
                                )
                            }

                            LaunchedEffect(controlsVisible, player.isPlaying) {
                                if (controlsVisible && player.isPlaying) {
                                    delay(3000)
                                    controlsVisible = false
                                }
                            }

                            SeekGestureOverlay(
                                visible = showSeekFeedback,
                                amount = seekAmount,
                                isForward = isSeekForward
                            )

                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    color = Color.White,
                                    strokeWidth = 4.dp
                                )
                            }
                        }
                    }
                }

                // Metadata Area
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (uiState) {
                        is PlayerUiState.Loading -> {
                            item {
                                com.arslandaim.playtube.ui.components.PlayerMetadataSkeleton(syncTransition)
                            }
                            
                            items(3) {
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    com.arslandaim.playtube.ui.components.VideoCardSkeleton(syncTransition)
                                }
                            }
                        }
                        is PlayerUiState.Success -> {
                            item {
                                UnifiedMetadataHub(
                                    title = uiState.title,
                                    viewCount = uiState.bundle.viewCount,
                                    uploadDate = uiState.bundle.uploadDate,
                                    description = uiState.bundle.description,
                                    uploaderName = uiState.uploader,
                                    uploaderThumbnailUrl = uiState.bundle.uploaderThumbnailUrl,
                                    uploaderUrl = uiState.bundle.uploaderUrl,
                                    subscriberCount = uiState.bundle.uploaderSubscriberCount,
                                    isSubscribed = isSubscribed,
                                    isFavorite = isFavorite,
                                    isDownloaded = downloadedIds.contains(videoId),
                                    onToggleSubscription = onToggleSubscription,
                                    onToggleFavorite = { onToggleFavorite(null) },
                                    onDownloadClick = { if (!downloadedIds.contains(videoId)) onDownloadClick(null) },
                                    onShareClick = onShareVideo,
                                    onChannelClick = onChannelClick
                                )
                            }

                            relatedVideosSection(
                                relatedVideos = uiState.bundle.relatedVideos,
                                downloadedIds = downloadedIds,
                                favoriteIds = favoriteIds,
                                onVideoClick = onVideoClick,
                                onChannelClick = onChannelClick,
                                onFavoriteClick = { onToggleFavorite(it) },
                                onDownloadClick = { onDownloadClick(it) }
                            )

                            if (uiState.bundle.nextRelatedVideosPage != null) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                        is PlayerUiState.Error -> {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = stringResource(R.string.error_prefix, uiState.message),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { /* Handle Retry */ }) {
                                            Text(stringResource(R.string.retry))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Shared Download Dialog logic
            when (val currentDownloadState = downloadState) {
                DownloadDialogState.Idle -> {}
                is DownloadDialogState.Loading -> {
                    AlertDialog(
                        onDismissRequest = { onDismissDownload() },
                        confirmButton = {},
                        title = { Text(stringResource(R.string.loading)) },
                        text = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    )
                }
                is DownloadDialogState.ShowDialog -> {
                    DownloadSelectionSheet(
                        videoStreams = currentDownloadState.bundle.videoStreams,
                        audioStreams = currentDownloadState.bundle.audioStreams,
                        onDismiss = { onDismissDownload() },
                        onDownload = { stream ->
                            onDownloadConfirm(
                                currentDownloadState.video,
                                currentDownloadState.bundle,
                                stream.url,
                                stream.quality,
                                stream.format,
                                stream.isAdaptive
                            )
                        }
                    )
                }
            }
        }
    }
}



@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerView(
    player: Player,
    controlsVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                this.keepScreenOn = true 
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = false // We use our custom Compose controller
                
                // Professional Subtitle Styling
                subtitleView?.apply {
                    // Force Canvas rendering for predictable padding behavior
                    setViewType(SubtitleView.VIEW_TYPE_CANVAS)
                    
                    // Explicitly align to bottom to fix the "subtitles at top" issue
                    val params = layoutParams as? android.widget.FrameLayout.LayoutParams
                    params?.gravity = android.view.Gravity.BOTTOM
                    layoutParams = params

                    setApplyEmbeddedStyles(false)
                    setStyle(
                        CaptionStyleCompat(
                            Color.White.toArgb(),
                            0x66000000.toInt(), // Reduced background opacity (40%)
                            Color.Transparent.toArgb(),
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE, // Better contrast
                            Color.Black.toArgb(),
                            android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
                        )
                    )
                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.05f)
                }
            }
        },
        update = { playerView ->
            playerView.subtitleView?.setBottomPaddingFraction(
                if (controlsVisible) 0.18f else 0.08f
            )
        },
        modifier = modifier
    )
}

@Composable
private fun PersistentProgressBar(
    progress: Float,
    bufferedProgress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f))
    ) {
        // Buffered (Preloaded) line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                .background(Color.White.copy(alpha = 0.25f))
        )
        // Playback progress line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(Color.Red)
        )
    }
}

@Composable
private fun PlayerControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isCcEnabled: Boolean,
    hasSubtitles: Boolean,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleSubtitles: () -> Unit,
    onShowSettings: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        // Center Play/Pause with Scale Animation
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(76.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        // Bottom Seek Section (Modern & Minimal)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = VideoUtils.formatDuration(currentPosition / 1000),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = VideoUtils.formatDuration(duration / 1000),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().height(32.dp)
            )
        }

        // Top Navigation & Actions Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasSubtitles) {
                        IconButton(onClick = onToggleSubtitles, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = if (isCcEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = null,
                                tint = if (isCcEnabled) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(onClick = onShowSettings, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
