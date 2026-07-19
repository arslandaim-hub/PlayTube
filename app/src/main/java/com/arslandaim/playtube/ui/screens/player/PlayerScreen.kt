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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.LocalConfiguration
import androidx.media3.ui.AspectRatioFrameLayout

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
    val seekAmount by viewModel.seekAmount.collectAsState()
    val showSeekFeedback by viewModel.showSeekFeedback.collectAsState()
    val isSeekForward by viewModel.isSeekForward.collectAsState()
    val isCcEnabled by viewModel.isCcEnabled.collectAsState()
    val isMinimized by viewModel.miniPlayerManager.isMinimized.collectAsState()
    
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
        seekAmount = seekAmount,
        showSeekFeedback = showSeekFeedback,
        isSeekForward = isSeekForward,
        isCcEnabled = isCcEnabled,
        player = viewModel.player,
        snackbarMessage = viewModel.snackbarMessage,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleSubscription = viewModel::toggleSubscription,
        onSetQuality = viewModel::setQuality,
        onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
        onToggleSubtitles = viewModel::toggleSubtitles,
        onDownload = viewModel::download,
        onSeekForward = viewModel::seekForward,
        onSeekBackward = viewModel::seekBackward,
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
    seekAmount: Int,
    showSeekFeedback: Boolean,
    isSeekForward: Boolean,
    isCcEnabled: Boolean,
    player: Player,
    snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String>,
    onToggleFavorite: () -> Unit,
    onToggleSubscription: () -> Unit,
    onSetQuality: (com.arslandaim.playtube.domain.model.StreamItem) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleSubtitles: () -> Unit,
    onDownload: (String?, String?, String?, Boolean) -> Unit,
    onSeekForward: () -> Unit,
    onSeekBackward: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    val isDownloaded = downloadedIds.contains(videoId)
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

    DisposableEffect(Unit) {
        onDispose {
            // Reset orientation on dispose
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }


    if (showDownloadDialog) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            DownloadSelectionSheet(
                videoStreams = it.bundle.videoStreams,
                audioStreams = it.bundle.audioStreams,
                onDismiss = { showDownloadDialog = false },
                onDownload = { stream ->
                    onDownload(stream.url, stream.quality, stream.format, stream.isAdaptive)
                    showDownloadDialog = false
                }
            )
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
                                onSingleTap = { /* PlayerView handles its own controls usually */ },
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

                            // Top Player Controls (Settings & CC)
                            TopPlayerControls(
                                isCcEnabled = isCcEnabled,
                                hasSubtitles = uiState.bundle.subtitles.isNotEmpty(),
                                onToggleSubtitles = onToggleSubtitles,
                                onShowSettings = { showSettingsSheet = true }
                            )

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
                                                    onClick = onToggleFavorite,
                                                    active = isFavorite
                                                )
                                                PlayerActionItem(
                                                    icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                                    label = if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download),
                                                    onClick = { if (!isDownloaded) showDownloadDialog = true },
                                                    active = isDownloaded
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


                            items(uiState.bundle.relatedVideos) { relatedVideo ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    VideoItemRow(
                                        video = relatedVideo,
                                        isDownloaded = downloadedIds.contains(relatedVideo.id),
                                        onChannelClick = { onChannelClick(relatedVideo.uploaderUrl ?: "") },
                                        onClick = { onVideoClick(relatedVideo) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
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
        }
    }
}

@Composable
private fun TopPlayerControls(
    isCcEnabled: Boolean,
    hasSubtitles: Boolean,
    onToggleSubtitles: () -> Unit,
    onShowSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasSubtitles) {
            IconButton(
                onClick = onToggleSubtitles,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isCcEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                    contentDescription = stringResource(R.string.toggle_subtitles),
                    tint = if (isCcEnabled) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        IconButton(
            onClick = onShowSettings,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings)
            )
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
                this.keepScreenOn = true // Keep screen awake during playback
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = true
                setShowSubtitleButton(false)
                setShowPreviousButton(false)
                setShowNextButton(false)
                setShowFastForwardButton(false)
                setShowRewindButton(false)
                
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
                    setBottomPaddingFraction(0.1f)
                }

                // Hide settings and speed buttons via ID (Safe approach for different Media3 layouts)
                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_settings)?.visibility = android.view.View.GONE
                findViewById<android.view.View>(androidx.media3.ui.R.id.exo_playback_speed)?.visibility = android.view.View.GONE
            }
        },
        update = {
            // State Updates - Does NOT recreate the view
        },
        modifier = modifier
    )
}

@Composable
fun DownloadQualityDialog(
    videoStreams: List<StreamItem>,
    audioStreams: List<StreamItem>,
    onDismiss: () -> Unit,
    onDownload: (StreamItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Video", style = MaterialTheme.typography.labelLarge)
                videoStreams.forEach { stream ->
                    TextButton(onClick = { onDownload(stream) }) {
                        Text("${stream.quality} (${stream.format})")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Audio", style = MaterialTheme.typography.labelLarge)
                audioStreams.forEach { stream ->
                    TextButton(onClick = { onDownload(stream) }) {
                        Text("${stream.quality} (${stream.format})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
