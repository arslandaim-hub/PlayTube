/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
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
import com.arslandaim.playtube.ui.components.InfiniteScrollEffect
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.PlaybackSpeedSelectionSheet
import com.arslandaim.playtube.ui.components.QualitySelectionSheet
import com.arslandaim.playtube.ui.components.SubtitleSelectionSheet
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.ui.components.rememberSyncShimmerTransition
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.VideoUtils
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ErrorOutline
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.provider.Settings
import android.content.res.Configuration
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
    val availableSubtitles by viewModel.availableSubtitles.collectAsStateWithLifecycle()
    val selectedSubtitleLanguage by viewModel.selectedSubtitleLanguage.collectAsStateWithLifecycle()
    val currentPosition by viewModel.currentPosition.collectAsStateWithLifecycle()
    val bufferedPosition by viewModel.bufferedPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isRecovering by viewModel.isRecovering.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isIncognito by viewModel.isIncognitoMode.collectAsStateWithLifecycle()
    val isAutoplayEnabled by viewModel.isAutoplayEnabled.collectAsStateWithLifecycle()
    val preferredQuality by viewModel.preferredQuality.collectAsStateWithLifecycle()
    val sleepTimerRemainingTime by viewModel.sleepTimerRemainingTime.collectAsStateWithLifecycle()
    val shouldCloseAppOnTimerFinish by viewModel.shouldCloseAppOnTimerFinish.collectAsStateWithLifecycle()
    val syncTransition = rememberSyncShimmerTransition()

    var activeCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    DisposableEffect(viewModel.player) {
        val listener = object : Player.Listener {
            @androidx.annotation.OptIn(UnstableApi::class)
            override fun onCues(cueGroup: CueGroup) {
                // Intercept and sanitize cues to prevent stacking (roll-up)
                activeCues = if (cueGroup.cues.isEmpty()) {
                    emptyList()
                } else {
                    // 1. Only take the most recent cue object
                    val lastCue = cueGroup.cues.last()
                    val originalText = lastCue.text?.toString() ?: ""
                    
                    if (originalText.isNotBlank()) {
                        // 2. Extract only the last line if multiple lines exist
                        val singleLineText = if (originalText.contains("\n")) {
                            originalText.substringAfterLast("\n").trim()
                        } else {
                            originalText
                        }
                        
                        // 3. Rebuild the cue with sanitized text
                        listOf(lastCue.buildUpon().setText(singleLineText).build())
                    } else {
                        emptyList()
                    }
                }
            }
        }
        viewModel.player.addListener(listener)
        onDispose { viewModel.player.removeListener(listener) }
    }

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    // Memoize subtitle list to prevent redundant recompositions when other states change
    val memoizedSubtitles = remember(availableSubtitles) { availableSubtitles }
    
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
            isPlaying = isPlaying,
            isIncognito = isIncognito,
            preferredQuality = preferredQuality,
            downloadedIds = downloadedIds,
            favoriteIds = favoriteIds,
            seekAmount = seekAmount,
            showSeekFeedback = showSeekFeedback,
            isSeekForward = isSeekForward,
            isCcEnabled = isCcEnabled,
            availableSubtitles = memoizedSubtitles,
            selectedSubtitleLanguage = selectedSubtitleLanguage,
            currentPosition = { currentPosition },
            bufferedPosition = { bufferedPosition },
            duration = { duration },
            downloadState = downloadState,
            player = viewModel.player,
            activeCues = activeCues,
            syncTransition = syncTransition,
            snackbarMessage = viewModel.snackbarMessage,
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onToggleSubscription = viewModel::toggleSubscription,
            onSetQuality = viewModel::setQuality,
            onSetPlaybackSpeed = viewModel::setPlaybackSpeed,
            onToggleSubtitles = viewModel::toggleSubtitles,
            onSetSubtitleLanguage = viewModel::setSubtitleLanguage,
            onPlayPause = viewModel::togglePlayPause,
            onSkipNext = viewModel::playNext,
            onSkipPrevious = viewModel::playPrevious,
            onDownloadConfirm = viewModel::download,
            onDownloadClick = { viewModel.prepareDownload(it) },
            onDismissDownload = viewModel::dismissDownloadDialog,
            onLoadMore = viewModel::loadNextRelatedPage,
            onSeekForward = viewModel::seekForward,
            onSeekBackward = viewModel::seekBackward,
            onSeekTo = viewModel::seekTo,
            onShareVideo = viewModel::shareVideo,
            onBack = onBack,
            onVideoClick = onVideoClick,
            onChannelClick = onChannelClick,
            onRetry = { viewModel.currentVideoItem?.let { viewModel.loadVideo(it) } },
            isAutoplayEnabled = isAutoplayEnabled,
            onAutoplayChange = viewModel::setAutoplayEnabled,
            sleepTimerRemainingTime = sleepTimerRemainingTime,
            shouldCloseAppOnTimerFinish = shouldCloseAppOnTimerFinish,
            onStartSleepTimer = viewModel.sleepTimerManager::startTimer,
            onSetEndOfVideoSleepTimer = viewModel.sleepTimerManager::setEndOfVideo,
            onCancelSleepTimer = viewModel.sleepTimerManager::cancelTimer,
            onSetShouldCloseApp = viewModel.sleepTimerManager::setShouldCloseApp
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
    isPlaying: Boolean,
    isIncognito: Boolean,
    preferredQuality: String,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    seekAmount: Int,
    showSeekFeedback: Boolean,
    isSeekForward: Boolean,
    isCcEnabled: Boolean,
    availableSubtitles: List<com.arslandaim.playtube.domain.model.SubtitleItem>,
    selectedSubtitleLanguage: String?,
    currentPosition: () -> Long,
    bufferedPosition: () -> Long,
    duration: () -> Long,
    downloadState: DownloadDialogState,
    player: Player,
    activeCues: List<Cue>,
    syncTransition: InfiniteTransition,
    snackbarMessage: SharedFlow<String>,
    onToggleFavorite: (VideoItem?) -> Unit,
    onToggleSubscription: () -> Unit,
    onSetQuality: (com.arslandaim.playtube.domain.model.StreamItem?) -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleSubtitles: () -> Unit,
    onSetSubtitleLanguage: (String?) -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
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
    onChannelClick: (String) -> Unit,
    onRetry: () -> Unit,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    sleepTimerRemainingTime: Int?,
    shouldCloseAppOnTimerFinish: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSetEndOfVideoSleepTimer: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetShouldCloseApp: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
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
        brightnessLevel = if ((layoutParams?.screenBrightness ?: -1f) < 0) {
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
    InfiniteScrollEffect(
        listState = listState,
        buffer = 5,
        enabled = uiState is PlayerUiState.Success && !isBuffering,
        onLoadMore = onLoadMore
    )

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
                preferredQuality = preferredQuality,
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { stream ->
                    onSetQuality(stream)
                    showQualityDialog = false
                }
            )
        }
    }

    if (showSubtitleSheet) {
        SubtitleSelectionSheet(
            subtitles = availableSubtitles,
            currentLanguage = selectedSubtitleLanguage,
            isCcEnabled = isCcEnabled,
            onDismiss = { showSubtitleSheet = false },
            onLanguageSelected = { lang ->
                onSetSubtitleLanguage(lang)
                showSubtitleSheet = false
            }
        )
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
                val currentLangName = selectedSubtitleLanguage?.let { 
                    java.util.Locale.forLanguageTag(it).displayLanguage.replaceFirstChar { c -> c.uppercase() } 
                } ?: stringResource(R.string.off)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.subtitles)) },
                    supportingContent = { Text(if (isCcEnabled) currentLangName else stringResource(R.string.off)) },
                    leadingContent = { Icon(Icons.Default.ClosedCaption, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showSubtitleSheet = true
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
                ListItem(
                    headlineContent = { Text(stringResource(R.string.sleep_timer)) },
                    supportingContent = {
                        Text(
                            when (sleepTimerRemainingTime) {
                                null -> stringResource(R.string.timer_off)
                                -1 -> stringResource(R.string.timer_end_of_video_active)
                                else -> stringResource(R.string.timer_minutes_remaining, sleepTimerRemainingTime)
                            }
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Timer, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showSleepTimerSheet = true
                    }
                )
            }
        }
    }

    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )

                val currentMinutes = if (sleepTimerRemainingTime != null && sleepTimerRemainingTime > 0) sleepTimerRemainingTime else 0
                var selectedMinutes by remember { mutableIntStateOf(currentMinutes) }
                val isEndOfVideo = sleepTimerRemainingTime == -1

                ListItem(
                    headlineContent = { Text("${stringResource(R.string.duration)}: ${if (isEndOfVideo) stringResource(R.string.timer_end_of_video) else if (selectedMinutes == 0) stringResource(R.string.off) else stringResource(R.string.timer_minutes_placeholder, selectedMinutes)}") },
                    trailingContent = {
                        if (!isEndOfVideo && selectedMinutes > 0) {
                            val timeFormat = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
                            Text(
                                text = stringResource(R.string.ends_at, timeFormat.format(System.currentTimeMillis() + selectedMinutes * 60000)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                )

                Slider(
                    value = if (isEndOfVideo) 0f else selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.roundToInt() },
                    valueRange = 0f..120f,
                    steps = 23, // 5 min gaps: (120/5)-1 = 23
                    modifier = Modifier.padding(horizontal = 24.dp),
                    enabled = !isEndOfVideo
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.timer_end_of_video)) },
                    trailingContent = {
                        Switch(
                            checked = isEndOfVideo,
                            onCheckedChange = { if (it) onSetEndOfVideoSleepTimer() else onCancelSleepTimer() }
                        )
                    }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.close_app_on_finish)) },
                    supportingContent = { Text(stringResource(R.string.close_app_desc)) },
                    trailingContent = {
                        Switch(
                            checked = shouldCloseAppOnTimerFinish,
                            onCheckedChange = onSetShouldCloseApp
                        )
                    }
                )

                Button(
                    onClick = {
                        if (!isEndOfVideo) {
                            if (selectedMinutes > 0) onStartSleepTimer(selectedMinutes)
                            else onCancelSleepTimer()
                        }
                        showSleepTimerSheet = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }
    }

    LaunchedEffect(brightnessOverlayVisible) {
        if (brightnessOverlayVisible) {
            delay(3000L)
            brightnessOverlayVisible = false
        }
    }

    LaunchedEffect(volumeOverlayVisible) {
        if (volumeOverlayVisible) {
            delay(3000L)
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
                        .data(VideoUtils.getLowResThumbnail(videoId))
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.15f
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    100f, 100f, android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                        .then(
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(80.dp)
                            } else Modifier
                        ),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Low
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
                        is PlayerUiState.Loading, is PlayerUiState.Error, is PlayerUiState.Upcoming -> {
                            // Show high-res placeholder during loading, error, or upcoming
                            ThumbnailImage(
                                videoId = videoId,
                                thumbnailUrl = when(uiState) {
                                    is PlayerUiState.Upcoming -> uiState.thumbnailUrl
                                    is PlayerUiState.Success -> uiState.bundle.thumbnailUrl
                                    else -> null
                                } ?: initialThumbnail ?: VideoUtils.getBestThumbnailUrl(videoId),
                                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.Ultra,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High
                            )

                            if (uiState is PlayerUiState.Upcoming) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Upcoming Content",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "This Premiere or Live Stream has not started yet.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Center
                                        )
                                        uiState.scheduledTime?.let { time ->
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(20.dp)
                                            ) {
                                                Text(
                                                    text = VideoUtils.formatUploadDate(time),
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (uiState is PlayerUiState.Error) {
                                val isNetworkError = uiState.error is PlayTubeError.Network
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyState(
                                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Playback Error",
                                        description = uiState.error.getMessage(),
                                        actionText = stringResource(R.string.retry),
                                        onActionClick = onRetry
                                    )
                                }
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

                                // Manual Subtitle Overlay
                                if (isCcEnabled && activeCues.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = if (controlsVisible) 64.dp else 24.dp),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        ManualSubtitleView(
                                            cues = activeCues,
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                        )
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
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                isRightSide = true,
                                modifier = Modifier.align(Alignment.CenterEnd)
                            )

                            // Persistent Progress Bar (Always visible at the very bottom)
                            val isLive = (uiState as? PlayerUiState.Success)?.bundle?.isLive == true
                            PersistentProgressBar(
                                progress = {
                                    val dur = duration()
                                    if (isLive) 1f else if (dur > 0) currentPosition().toFloat() / dur else 0f
                                },
                                bufferedProgress = {
                                    val dur = duration()
                                    if (isLive) 1f else if (dur > 0) bufferedPosition().toFloat() / dur else 0f
                                },
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
                                    isPlaying = isPlaying,
                                    currentPosition = currentPosition,
                                    duration = duration,
                                    isLive = isLive,
                                    isCcEnabled = isCcEnabled,
                                    isIncognito = isIncognito,
                                    hasSubtitles = (uiState as? PlayerUiState.Success)?.bundle?.subtitles?.isNotEmpty() == true,
                                    onPlayPause = onPlayPause,
                                    onSkipNext = onSkipNext,
                                    onSkipPrevious = onSkipPrevious,
                                    onSeekTo = onSeekTo,
                                    onToggleSubtitles = onToggleSubtitles,
                                    onShowSubtitleSettings = { showSubtitleSheet = true },
                                    onShowSettings = { showSettingsSheet = true },
                                    onBack = onBack
                                )
                            }

                            LaunchedEffect(controlsVisible, player.isPlaying) {
                                if (controlsVisible && player.isPlaying) {
                                    delay(3000L)
                                    controlsVisible = false
                                }
                            }

                            SeekGestureOverlay(
                                visible = showSeekFeedback,
                                amount = seekAmount,
                                isForward = isSeekForward
                            )

                            // Consolidated Player Loading UI
                            val showLoader = (uiState is PlayerUiState.Loading) || isBuffering || isRecovering
                            if (showLoader) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(if (isRecovering) Color.Black.copy(alpha = 0.5f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = Color.White,
                                            strokeWidth = 4.dp
                                        )
                                        if (isRecovering) {
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
                        }
                    }
                }

                // Metadata Area
                AnimatedVisibility(
                    visible = uiState !is PlayerUiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
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
                                    isAutoplayEnabled = isAutoplayEnabled,
                                    onAutoplayChange = onAutoplayChange,
                                    onVideoClick = onVideoClick,
                                    onChannelClick = onChannelClick,
                                    onFavoriteClick = { onToggleFavorite(it) },
                                    onDownloadClick = { onDownloadClick(it) }
                                )
                            }
                            is PlayerUiState.Upcoming -> {
                                item {
                                    UnifiedMetadataHub(
                                        title = uiState.title,
                                        viewCount = -1L,
                                        uploadDate = uiState.scheduledTime,
                                        description = null,
                                        uploaderName = uiState.uploader,
                                        uploaderThumbnailUrl = null,
                                        uploaderUrl = null,
                                        subscriberCount = null,
                                        isSubscribed = isSubscribed,
                                        isFavorite = isFavorite,
                                        isDownloaded = false,
                                        onToggleSubscription = onToggleSubscription,
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onDownloadClick = { },
                                        onShareClick = onShareVideo,
                                        onChannelClick = onChannelClick
                                    )
                                }
                            }
                            else -> {
                                // Handled by AnimatedVisibility
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
private fun ManualSubtitleView(
    cues: List<Cue>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cues.forEach { cue ->
            val text = cue.text
            if (text != null && text.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(10.dp),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = text.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.5f),
                                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                                blurRadius = 2f
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> playerView?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> playerView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                this.keepScreenOn = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                useController = false // We use our custom Compose controller
                
                // Disable native buffering indicator to prevent "Double Loader" bug
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)

                // Hide internal subtitle view to use our manual one
                subtitleView?.visibility = android.view.View.GONE
                playerView = this
            }
        },
        update = { view ->
            if (view.player != player) {
                view.player = player
            }
            view.subtitleView?.visibility = android.view.View.GONE
        },
        onRelease = { view ->
            view.player = null
            playerView = null
        },
        modifier = modifier
    )
}

@Composable
private fun PersistentProgressBar(
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
        
        // Buffered (Preloaded) line
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            size = size.copy(width = width * bufferedProgress().coerceIn(0f, 1f))
        )
        
        // Playback progress line
        drawRect(
            color = Color.Red,
            size = size.copy(width = width * progress().coerceIn(0f, 1f))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControlsOverlay(
    isPlaying: Boolean,
    currentPosition: () -> Long,
    duration: () -> Long,
    isLive: Boolean,
    isCcEnabled: Boolean,
    isIncognito: Boolean,
    hasSubtitles: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleSubtitles: () -> Unit,
    onShowSubtitleSettings: () -> Unit,
    onShowSettings: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
    ) {
        // Center Play/Pause with Skip Buttons
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Skip Previous
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Surface(
                onClick = onPlayPause,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Skip Next
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        // Bottom Seek Section (Modern & Minimal)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.Red, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = VideoUtils.formatDuration(currentPosition() / 1000),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = VideoUtils.formatDuration(duration() / 1000),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (!isLive) {
                val currentPos = currentPosition()
                val totalDuration = duration()
                Slider(
                    value = currentPos.toFloat(),
                    onValueChange = { onSeekTo(it.toLong()) },
                    valueRange = 0f..totalDuration.toFloat().coerceAtLeast(1f),
                    thumb = {
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                        }
                    },
                    track = { sliderState ->
                        SliderDefaults.Track(
                            sliderState = sliderState,
                            modifier = Modifier.height(3.dp),
                            thumbTrackGapSize = 0.dp,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.Red,
                                inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp) // Professional 48dp touch target
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
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

            if (isIncognito) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color(0xFF9C27B0).copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Incognito", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasSubtitles) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = onToggleSubtitles,
                                    onLongClick = onShowSubtitleSettings
                                ),
                            contentAlignment = Alignment.Center
                        ) {
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
