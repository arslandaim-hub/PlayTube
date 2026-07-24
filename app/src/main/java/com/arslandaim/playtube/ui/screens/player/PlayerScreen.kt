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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.PlaybackSpeedSelectionSheet
import com.arslandaim.playtube.ui.components.QualitySelectionSheet
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.VideoItemRow
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
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsState()
    val favorites by viewModel.libraryRepository.getFavorites().collectAsState(initial = emptyList())
    val seekAmount by viewModel.seekAmount.collectAsState()
    val showSeekFeedback by viewModel.showSeekFeedback.collectAsState()
    val isSeekForward by viewModel.isSeekForward.collectAsState()
    val isCcEnabled by viewModel.isCcEnabled.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val bufferedPosition by viewModel.bufferedPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

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
        onBack = onBack,
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        ),
                        startY = 0f,
                        endY = 500f
                    )
                )
        ) {
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
                            // Show placeholder during loading or error
                            AsyncImage(
                                model = initialThumbnail,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.Medium
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
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Persistent Progress Bar (Always visible at the very bottom)
                            PersistentProgressBar(
                                progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                                bufferedProgress = if (duration > 0) bufferedPosition.toFloat() / duration else 0f,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .height(2.dp)
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

                            GestureOverlay(
                                visible = brightnessOverlayVisible,
                                icon = Icons.Default.BrightnessLow,
                                text = "${(brightnessLevel * 100).toInt()}%"
                            )
                            
                            GestureOverlay(
                                visible = volumeOverlayVisible,
                                icon = Icons.Default.VolumeUp,
                                text = "${(volumeLevel * 100).toInt()}%"
                            )

                            LaunchedEffect(brightnessOverlayVisible, isDragging) {
                                if (brightnessOverlayVisible && !isDragging) {
                                    delay(1500)
                                    brightnessOverlayVisible = false
                                }
                            }

                            LaunchedEffect(volumeOverlayVisible, isDragging) {
                                if (volumeOverlayVisible && !isDragging) {
                                    delay(1500)
                                    volumeOverlayVisible = false
                                }
                            }

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
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = initialTitle ?: stringResource(R.string.loading),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        is PlayerUiState.Success -> {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = uiState.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${VideoUtils.formatViewCount(uiState.bundle.viewCount)} • ${VideoUtils.formatUploadDate(uiState.bundle.uploadDate)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = stringResource(R.string.more),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable { showDescriptionSheet = true }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Unified Channel & Action Bar
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { uiState.bundle.uploaderUrl?.let { onChannelClick(it) } },
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AsyncImage(
                                                    model = uiState.bundle.uploaderThumbnailUrl,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                    filterQuality = FilterQuality.Medium
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = uiState.uploader,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (uiState.bundle.uploaderSubscriberCount != null && uiState.bundle.uploaderSubscriberCount > 0) {
                                                        Text(
                                                            text = "${VideoUtils.formatNumber(uiState.bundle.uploaderSubscriberCount)} subscribers",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = onToggleSubscription,
                                                    colors = if (isSubscribed) {
                                                        ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    } else {
                                                        ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.onSurface,
                                                            contentColor = MaterialTheme.colorScheme.surface
                                                        )
                                                    },
                                                    shape = CircleShape,
                                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                                ) {
                                                    Text(
                                                        text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                PlayerActionItem(
                                                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    label = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
                                                    onClick = { onToggleFavorite(null) },
                                                    active = isFavorite
                                                )
                                                PlayerActionItem(
                                                    icon = if (downloadedIds.contains(videoId)) Icons.Default.CheckCircle else Icons.Default.Download,
                                                    label = if (downloadedIds.contains(videoId)) stringResource(R.string.downloaded) else stringResource(R.string.download),
                                                    onClick = { if (!downloadedIds.contains(videoId)) onDownloadClick(null) },
                                                    active = downloadedIds.contains(videoId)
                                                )
                                                PlayerActionItem(
                                                    icon = Icons.Default.Description,
                                                    label = stringResource(R.string.info),
                                                    onClick = { showDescriptionSheet = true }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.related_videos),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }


                            items(uiState.bundle.relatedVideos, key = { it.id }) { relatedVideo ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    VideoItemRow(
                                        video = relatedVideo,
                                        isDownloaded = downloadedIds.contains(relatedVideo.id),
                                        isFavorite = favoriteIds.contains(relatedVideo.id),
                                        onFavoriteClick = { onToggleFavorite(relatedVideo) },
                                        onDownloadClick = { onDownloadClick(relatedVideo) },
                                        onChannelClick = { onChannelClick(relatedVideo.uploaderUrl ?: "") },
                                        onClick = { onVideoClick(relatedVideo) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

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

@Composable
private fun PlayerActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}


@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerView(
    player: Player,
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
                    setApplyEmbeddedStyles(false)
                    setStyle(
                        CaptionStyleCompat(
                            Color.White.toArgb(),
                            Color.Transparent.toArgb(),
                            Color.Transparent.toArgb(),
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            Color.Black.toArgb(),
                            null
                        )
                    )
                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 0.8f)
                    setBottomPaddingFraction(0.15f) // Increased padding to avoid being covered by progress bar
                }
            }
        },
        update = {
            // State Updates
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
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        // Buffered (Preloaded) line
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(bufferedProgress.coerceIn(0f, 1f))
                .background(Color.White.copy(alpha = 0.4f))
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
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        // Center Controls
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.3f),
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp).fillMaxSize()
                )
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp) // Leave room for persistent progress bar at the very bottom
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${VideoUtils.formatDuration(currentPosition / 1000)} / ${VideoUtils.formatDuration(duration / 1000)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            Slider(
                value = currentPosition.toFloat(),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Red,
                    activeTrackColor = Color.Red,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }

        // Top Controls
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            
            Spacer(modifier = Modifier.weight(1f))

            Row(modifier = Modifier.background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(20.dp))) {
                if (hasSubtitles) {
                    IconButton(onClick = onToggleSubtitles) {
                        Icon(
                            imageVector = if (isCcEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                            contentDescription = null,
                            tint = if (isCcEnabled) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }

                IconButton(onClick = onShowSettings) {
                    Icon(Icons.Default.Settings, null, tint = Color.White)
                }
            }
        }
    }
}
