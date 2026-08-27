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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.arslandaim.playtube.ui.components.player.PersistentProgressBar
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.PlaybackSpeedSelectionSheet
import com.arslandaim.playtube.ui.components.PitchSelectionSheet
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
import com.arslandaim.playtube.ui.components.player.VideoPlayerView
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.flow.SharedFlow

@UnstableApi
@Composable
fun PlayerScreen(
    videoId: String,
    initialTitle: String? = null,
    initialThumbnail: String? = null,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val isSaved by viewModel.isSaved.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val currentQuality by viewModel.currentQuality.collectAsStateWithLifecycle()
    val displayQuality by viewModel.displayQuality.collectAsStateWithLifecycle()
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
    val subtitleFontSize by viewModel.subtitleFontSize.collectAsStateWithLifecycle()
    val subtitleBackgroundOpacity by viewModel.subtitleBackgroundOpacity.collectAsStateWithLifecycle()
    val playbackPitch by viewModel.playbackPitch.collectAsStateWithLifecycle()
    val showStatsForNerds by viewModel.showStatsForNerds.collectAsStateWithLifecycle()
    val playbackStats by viewModel.playbackStats.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val isFetchingComments by viewModel.isFetchingComments.collectAsStateWithLifecycle()
    val replies by viewModel.replies.collectAsStateWithLifecycle()
    val isFetchingReplies by viewModel.isFetchingReplies.collectAsStateWithLifecycle()
    val activeReplyParent by viewModel.activeReplyParent.collectAsStateWithLifecycle()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val playlistIndex by viewModel.playlistIndex.collectAsStateWithLifecycle()
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
            isSaved = isSaved,
            isSubscribed = isSubscribed,
            playbackSpeed = playbackSpeed,
            currentQuality = currentQuality,
            displayQuality = displayQuality,
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
            onAddToPlaylistClick = onAddToPlaylistClick,
            onRetry = { viewModel.currentVideoItem?.let { viewModel.loadVideo(it) } },
            isAutoplayEnabled = isAutoplayEnabled,
            onAutoplayChange = viewModel::setAutoplayEnabled,
            sleepTimerRemainingTime = sleepTimerRemainingTime,
            shouldCloseAppOnTimerFinish = shouldCloseAppOnTimerFinish,
            onStartSleepTimer = viewModel.sleepTimerManager::startTimer,
            onSetEndOfVideoSleepTimer = viewModel.sleepTimerManager::setEndOfVideo,
            onCancelSleepTimer = viewModel.sleepTimerManager::cancelTimer,
            onSetShouldCloseApp = viewModel.sleepTimerManager::setShouldCloseApp,
            playbackPitch = playbackPitch,
            onSetPlaybackPitch = viewModel::setPlaybackPitch,
            showStatsForNerds = showStatsForNerds,
            playbackStats = playbackStats,
            onToggleStats = viewModel::toggleStatsForNerds,
            subtitleFontSize = subtitleFontSize,
            subtitleBackgroundOpacity = subtitleBackgroundOpacity,
            comments = comments,
            isFetchingComments = isFetchingComments,
            onLoadComments = viewModel::loadComments,
            onLoadNextCommentsPage = viewModel::loadNextCommentsPage,
            replies = replies,
            isFetchingReplies = isFetchingReplies,
            activeReplyParent = activeReplyParent,
            onLoadReplies = viewModel::loadReplies,
            onLoadNextRepliesPage = viewModel::loadNextRepliesPage,
            onCloseReplies = viewModel::closeReplies,
            currentPlaylist = currentPlaylist,
            playlistIndex = playlistIndex,
            onPlaylistVideoClick = { video ->
                currentPlaylist?.let { viewModel.loadVideo(video, it.id, it.title) }
            }
        )
}

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
private fun PlayerContent(
    videoId: String,
    initialTitle: String?,
    initialThumbnail: String?,
    uiState: PlayerUiState,
    isFavorite: Boolean,
    isSaved: Boolean,
    isSubscribed: Boolean,
    playbackSpeed: Float,
    currentQuality: String?,
    displayQuality: String,
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
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onRetry: () -> Unit,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    sleepTimerRemainingTime: Int?,
    shouldCloseAppOnTimerFinish: Boolean,
    onStartSleepTimer: (Int) -> Unit,
    onSetEndOfVideoSleepTimer: () -> Unit,
    onCancelSleepTimer: () -> Unit,
    onSetShouldCloseApp: (Boolean) -> Unit,
    playbackPitch: Float,
    onSetPlaybackPitch: (Float) -> Unit,
    showStatsForNerds: Boolean,
    playbackStats: com.arslandaim.playtube.ui.screens.player.PlaybackStats,
    onToggleStats: () -> Unit,
    subtitleFontSize: Float,
    subtitleBackgroundOpacity: Float,
    comments: List<com.arslandaim.playtube.domain.model.CommentItem>,
    isFetchingComments: Boolean,
    onLoadComments: () -> Unit,
    onLoadNextCommentsPage: () -> Unit,
    replies: List<com.arslandaim.playtube.domain.model.CommentItem>,
    isFetchingReplies: Boolean,
    activeReplyParent: com.arslandaim.playtube.domain.model.CommentItem?,
    onLoadReplies: (com.arslandaim.playtube.domain.model.CommentItem) -> Unit,
    onLoadNextRepliesPage: () -> Unit,
    onCloseReplies: () -> Unit,
    currentPlaylist: com.arslandaim.playtube.domain.model.PlaylistDetails?,
    playlistIndex: Int,
    onPlaylistVideoClick: (VideoItem) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showPitchSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(videoId) {
        onLoadComments()
    }

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val view = LocalView.current

    LaunchedEffect(isLandscape) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        
        if (isLandscape) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

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
            
            // Restore system bars on dispose
            val window = activity?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
            }
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

    if (showPitchSheet) {
        PitchSelectionSheet(
            currentPitch = playbackPitch,
            onDismiss = { showPitchSheet = false },
            onPitchSelected = { pitch ->
                onSetPlaybackPitch(pitch)
                showPitchSheet = false
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

    if (showCommentsSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                showCommentsSheet = false
                onCloseReplies()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            scrimColor = Color.Black.copy(alpha = 0.25f)
        ) {
            CommentsSheet(
                comments = comments,
                isFetching = isFetchingComments,
                onLoadMore = onLoadNextCommentsPage,
                onDismiss = { 
                    showCommentsSheet = false
                    onCloseReplies()
                },
                activeReplyParent = activeReplyParent,
                replies = replies,
                isFetchingReplies = isFetchingReplies,
                onRepliesClick = onLoadReplies,
                onLoadMoreReplies = onLoadNextRepliesPage,
                onCloseReplies = onCloseReplies
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.quality)) },
                    supportingContent = { Text(displayQuality) },
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
                    headlineContent = { Text("Audio Pitch") },
                    supportingContent = { Text(if (playbackPitch == 1f) "Normal" else "${playbackPitch}x") },
                    leadingContent = { Icon(Icons.Default.MusicNote, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showPitchSheet = true
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
                ListItem(
                    headlineContent = { Text("Stats for Nerds") },
                    trailingContent = {
                        Switch(
                            checked = showStatsForNerds,
                            onCheckedChange = { onToggleStats() }
                        )
                    },
                    leadingContent = { Icon(Icons.Default.Info, null) },
                    modifier = Modifier.clickable { onToggleStats() }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Force edge-to-edge
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dynamic Ambient Mode (Modern Glow Effect)
            if (!isLandscape && (uiState is PlayerUiState.Success || initialThumbnail != null)) {
                val ambientThumbnail = when(uiState) {
                    is PlayerUiState.Success -> uiState.bundle.thumbnailUrl
                    is PlayerUiState.Upcoming -> uiState.thumbnailUrl
                    else -> initialThumbnail ?: VideoUtils.getBestThumbnailUrl(videoId)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp) // Glow only around the top player area
                        .graphicsLayer {
                            alpha = 0.45f
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    120f, 120f, android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                        .then(
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                Modifier.blur(100.dp)
                            } else Modifier
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(ambientThumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low
                    )
                    
                    // Gradient overlay to fade ambient glow into background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                }
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
                        .then(
                            if (isLandscape) Modifier.fillMaxHeight() 
                            else Modifier.statusBarsPadding().aspectRatio(16f / 9f)
                        )
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
                            val isLive = uiState.bundle.isLive
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
                                            fontSize = subtitleFontSize,
                                            backgroundOpacity = subtitleBackgroundOpacity,
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                        )
                                    }
                                }
                            }

                            // Vertical HUDs Left: Brightness, Right: Volume
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
                                    onToggleSubtitles = onToggleSubtitles,
                                    onShowSubtitleSettings = { showSubtitleSheet = true },
                                    onShowSettings = { showSettingsSheet = true },
                                    onBack = onBack
                                )
                            }

                            // Persistent Progress Bar (Always visible at the very bottom border)
                            // move it after the controls to ensure it stays on top of the darkened overlay background
                            PersistentProgressBar(
                                progress = {
                                    val dur = duration()
                                    if (isLive) 1f else if (dur > 0) currentPosition().toFloat() / dur else 0f
                                },
                                bufferedProgress = {
                                    val dur = duration()
                                    if (isLive) 1f else if (dur > 0) bufferedPosition().toFloat() / dur else 0f
                                },
                                isInteractive = controlsVisible && !isLive,
                                onSeek = { percentage ->
                                    val totalDuration = duration()
                                    if (totalDuration > 0) {
                                        onSeekTo((percentage * totalDuration).toLong())
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                            )

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

                            if (showStatsForNerds) {
                                StatsForNerdsOverlay(
                                    stats = playbackStats,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(16.dp)
                                )
                            }

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
                                            modifier = Modifier.size(52.dp),
                                            color = Color.White,
                                            strokeWidth = 3.dp
                                        )
                                        
                                        if (isRecovering) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                text = stringResource(R.string.waiting_for_connection),
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
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
                    visible = !isLandscape && uiState !is PlayerUiState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
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
                                    if (currentPlaylist != null && playlistIndex != -1) {
                                        PlaylistStack(
                                            playlist = currentPlaylist,
                                            currentIndex = playlistIndex,
                                            onVideoClick = onPlaylistVideoClick
                                        )
                                    }
                                }
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
                                        isSaved = isSaved,
                                        isDownloaded = downloadedIds.contains(videoId),
                                        comments = comments,
                                        commentCount = null, // Extractor doesn't always provide count easily
                                        onToggleSubscription = onToggleSubscription,
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onSaveClick = { 
                                            uiState.bundle.let { bundle ->
                                                onAddToPlaylistClick(
                                                    VideoItem(
                                                        id = videoId,
                                                        title = uiState.title,
                                                        thumbnailUrl = bundle.thumbnailUrl ?: "",
                                                        uploaderName = uiState.uploader,
                                                        uploaderUrl = bundle.uploaderUrl,
                                                        viewCount = bundle.viewCount,
                                                        uploadDate = bundle.uploadDate,
                                                        duration = duration() / 1000
                                                    )
                                                )
                                            }
                                        },
                                        onDownloadClick = { if (!downloadedIds.contains(videoId)) onDownloadClick(null) },
                                        onShareClick = onShareVideo,
                                        onChannelClick = onChannelClick,
                                        onCommentsClick = { showCommentsSheet = true }
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
                                    onAddToPlaylistClick = onAddToPlaylistClick,
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
                                        isSaved = isSaved,
                                        isDownloaded = false,
                                        comments = emptyList(),
                                        commentCount = null,
                                        onToggleSubscription = onToggleSubscription,
                                        onToggleFavorite = { onToggleFavorite(null) },
                                        onSaveClick = {
                                            onAddToPlaylistClick(
                                                VideoItem(
                                                    id = videoId,
                                                    title = uiState.title,
                                                    thumbnailUrl = uiState.thumbnailUrl ?: "",
                                                    uploaderName = uiState.uploader,
                                                    uploaderUrl = null,
                                                    viewCount = -1L,
                                                    uploadDate = uiState.scheduledTime,
                                                    duration = 0L
                                                )
                                            )
                                        },
                                        onDownloadClick = { },
                                        onShareClick = onShareVideo,
                                        onChannelClick = onChannelClick,
                                        onCommentsClick = { }
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

@UnstableApi
@Composable
private fun StatsForNerdsOverlay(
    stats: com.arslandaim.playtube.ui.screens.player.PlaybackStats,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 260.dp),
        color = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stats for Nerds", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), thickness = 0.5.dp, color = Color.White.copy(alpha = 0.2f))
            
            StatRow("Resolution", stats.resolution)
            StatRow("Format", stats.videoFormat ?: "Unknown")
            StatRow("Bitrate", "${stats.bitrate / 1000} kbps")
            StatRow("Dropped Frames", stats.droppedFrames.toString())
            StatRow("Bandwidth", "${stats.bandwidthEstimate / 1000000} Mbps")
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ManualSubtitleView(
    cues: List<Cue>,
    fontSize: Float,
    backgroundOpacity: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        cues.forEach { cue ->
            val text = cue.text
            if (text != null && text.isNotEmpty()) {
                Surface(
                    color = Color.Black.copy(alpha = backgroundOpacity),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = text.toString(),
                        color = Color.White,
                        fontSize = fontSize.sp,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = (fontSize * 1.3f).sp,
                            textAlign = TextAlign.Center,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = Color.Black.copy(alpha = 0.8f),
                                offset = androidx.compose.ui.geometry.Offset(1.5f, 1.5f),
                                blurRadius = 3f
                            )
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@UnstableApi
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
    onToggleSubtitles: () -> Unit,
    onShowSubtitleSettings: () -> Unit,
    onShowSettings: () -> Unit,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        // Center Controls Section (Modern Layout)
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipPrevious()
                },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            // Central Play/Pause with custom circular background
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPlayPause()
                },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(76.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSkipNext()
                },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Modern Bottom Bar Section (Timestamps only, Seekbar moved to border)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .displayCutoutPadding()
                .padding(bottom = 12.dp) // Sit closer to the bottom border
        ) {
            // Time Display & Live Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                            ),
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = VideoUtils.formatDuration(currentPosition() / 1000),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                        ),
                        color = Color.White
                    )
                    Text(
                        text = VideoUtils.formatDuration(duration() / 1000),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 4f)
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Top Action Pill glassmorphic design
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            if (isIncognito) {
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Incognito", 
                            color = Color.White, 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))

            // Unified Settings Pill
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasSubtitles) {
                        IconButton(
                            onClick = onToggleSubtitles,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (isCcEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = null,
                                tint = if (isCcEnabled) Color.Yellow else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    IconButton(onClick = onShowSettings, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}
