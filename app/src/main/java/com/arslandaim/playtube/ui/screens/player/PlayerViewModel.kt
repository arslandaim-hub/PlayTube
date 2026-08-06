/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.content.Context
import android.content.Intent
import com.arslandaim.playtube.services.PlaybackService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.common.PlaybackException
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.usecase.AddToHistoryUseCase
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.IsFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.IsSubscribedUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.VideoUtils
import com.arslandaim.playtube.utils.ConnectivityObserver
import com.arslandaim.playtube.utils.PlayTubeError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    val downloadRepository: DownloadRepository,
    val libraryRepository: LibraryRepository,
    private val videoRepository: com.arslandaim.playtube.domain.repository.VideoRepository,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val updateWatchProgressUseCase: com.arslandaim.playtube.domain.usecase.UpdateWatchProgressUseCase,
    private val updateUserInterestsUseCase: com.arslandaim.playtube.domain.usecase.UpdateUserInterestsUseCase,
    private val preferencesManager: PreferencesManager,
    @Named("HttpDataSourceFactory") private val httpDataSourceFactory: DataSource.Factory,
    private val dataSourceFactory: DataSource.Factory,
    private val connectivityObserver: ConnectivityObserver,
    val miniPlayerManager: MiniPlayerManager,
    val sleepTimerManager: SleepTimerManager,
    val queueManager: QueueManager,
    val player: ExoPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _isCcEnabled = MutableStateFlow(false)
    val isCcEnabled: StateFlow<Boolean> = _isCcEnabled.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    val sleepTimerRemainingTime: StateFlow<Int?> = sleepTimerManager.remainingTime
    val shouldCloseAppOnTimerFinish: StateFlow<Boolean> = sleepTimerManager.shouldCloseApp

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    private val _selectedSubtitleLanguage = MutableStateFlow<String?>(null)
    val selectedSubtitleLanguage: StateFlow<String?> = _selectedSubtitleLanguage.asStateFlow()

    val availableSubtitles: StateFlow<List<com.arslandaim.playtube.domain.model.SubtitleItem>> = _uiState
        .map { state ->
            if (state is PlayerUiState.Success) state.bundle.subtitles else emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isIncognitoMode: StateFlow<Boolean> = preferencesManager.isIncognitoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Playback Progress
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _currentQuality = MutableStateFlow<String?>(null)
    val currentQuality: StateFlow<String?> = _currentQuality.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Seek Feedback States
    private val _seekAmount = MutableStateFlow(0)
    val seekAmount: StateFlow<Int> = _seekAmount.asStateFlow()

    private val _showSeekFeedback = MutableStateFlow(false)
    val showSeekFeedback: StateFlow<Boolean> = _showSeekFeedback.asStateFlow()

    private val _isSeekForward = MutableStateFlow(true)
    val isSeekForward: StateFlow<Boolean> = _isSeekForward.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Download Dialog States
    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    val downloadedVideoIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { list -> 
            list.filter { it.status == DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var currentBundle: StreamBundle? = null
    var currentVideoItem: VideoItem? = null
    private var currentVideoId: String? = null
    private var loadingJob: Job? = null
    private var progressJob: Job? = null
    private var nextRelatedPage: Page? = null
    private var isFetchingNextRelatedPage = false
    private var lastSavedPosition = 0L
    private var isStalledDueToNetwork = false
    private var lastFailedPosition = 0L
    private var retryCount = 0
    private var retryJob: Job? = null
    private var preloadingJob: Job? = null
    private var isPreloaded = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            android.util.Log.d("PlayerLifecycle", "onPlaybackStateChanged: $playbackState")
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            
            // For live streams, duration might be C.TIME_UNSET or dynamic
            val rawDuration = player.duration
            _duration.value = if (rawDuration == C.TIME_UNSET) 0L else rawDuration.coerceAtLeast(0L)
            
            if (playbackState == Player.STATE_READY) {
                startProgressUpdate()
                isStalledDueToNetwork = false
                _isRecovering.value = false
            } else {
                stopProgressUpdate()
            }

            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                saveWatchProgress()
            }

            // Trigger autoplay ONLY if enabled AND sleep timer is NOT active
            if (playbackState == Player.STATE_ENDED && _isAutoplayEnabled.value && !sleepTimerManager.isTimerActive()) {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                } else {
                    currentBundle?.relatedVideos?.firstOrNull()?.let { nextVideo ->
                        loadVideo(nextVideo)
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("PlayerLifecycle", "onPlayerError: ${error.message}", error)
            
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player.seekToDefaultPosition()
                player.prepare()
                player.play()
                return
            }

            // Handle Expired URL (HTTP 403)
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                val cause = error.cause
                if (cause is androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException) {
                    if (cause.responseCode == 403) {
                        android.util.Log.w("PlayerRecovery", "HTTP 403 detected, likely expired URL. Recovering...")
                        lastFailedPosition = player.currentPosition
                        recoverExpiredUrl()
                        return
                    }
                }
            }

            if (isNetworkError(error)) {
                if (isStalledDueToNetwork && error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    // Possible expired URL after network restoration
                    recoverExpiredUrl()
                } else {
                    lastFailedPosition = player.currentPosition
                    isStalledDueToNetwork = player.playWhenReady
                    _isRecovering.value = isStalledDueToNetwork
                    
                    if (_uiState.value !is PlayerUiState.Success) {
                        _uiState.value = PlayerUiState.Error(PlayTubeError.Network)
                    } else {
                        viewModelScope.launch {
                            _snackbarMessage.emit("Connection lost. Waiting to resume...")
                        }
                    }

                    // Start exponential backoff retry if we have a connection or it was a timeout
                    scheduleRetry()
                }
            } else {
                val errorMessage = error.message ?: "Playback error"
                if (_uiState.value is PlayerUiState.Success) {
                    viewModelScope.launch {
                        _snackbarMessage.emit("Playback Error: $errorMessage")
                    }
                } else {
                    _uiState.value = PlayerUiState.Error(PlayTubeError.Extraction(errorMessage))
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d("PlayerLifecycle", "onIsPlayingChanged: $isPlaying")
            if (isPlaying) {
                isStalledDueToNetwork = false
                _isRecovering.value = false
            } else {
                saveWatchProgress()
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPosition.value = newPosition.positionMs
        }

        override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
            val isCcActive = tracks.groups.any { (it.type == C.TRACK_TYPE_TEXT) && it.isSelected }
            
            // If the user wants CC enabled but it's not active (common on first load or track change), try to enable it
            if (_isCcEnabled.value && !isCcActive) {
                val hasTextTracks = tracks.groups.any { (it.type == C.TRACK_TYPE_TEXT) && it.isSupported }
                if (hasTextTracks) {
                    updateCcState(true)
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val videoId = mediaItem?.mediaId
            if (videoId != null && videoId != currentVideoId) {
                android.util.Log.d("PlayerLifecycle", "onMediaItemTransition to $videoId, reason: $reason")
                // Reason 0 is standard transition (auto or manual skip in queue)
                loadVideoMetadata(videoId)
            }
        }
    }

    init {
        // Ensure we don't have multiple listeners if ViewModel is recreated
        player.removeListener(playerListener)
        player.addListener(playerListener)
        
        syncWithPlayer()
        
        // Load persistent CC preference
        viewModelScope.launch {
            combine(
                preferencesManager.isSubtitlesEnabled,
                preferencesManager.preferredSubtitleLanguage
            ) { enabled, preferredLang ->
                _isCcEnabled.value = enabled
                _selectedSubtitleLanguage.value = preferredLang
                updateCcState(enabled, preferredLang)
            }.collect()
        }

        // Load persistent Autoplay preference
        viewModelScope.launch {
            preferencesManager.isAutoplayEnabled.collectLatest { enabled ->
                _isAutoplayEnabled.value = enabled
            }
        }

        // Observe network for auto-recovery
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityObserver.Status.Available && isStalledDueToNetwork) {
                    retryCount = 0
                    retryJob?.cancel()
                    retryPlayback()
                }
            }
        }

        // Observe background skip events
        viewModelScope.launch {
            queueManager.skipToNextEvent.collect {
                playNext()
            }
        }
        viewModelScope.launch {
            queueManager.skipToPreviousEvent.collect {
                playPrevious()
            }
        }
    }

    private fun syncWithPlayer() {
        if (player.mediaItemCount > 0) {
            val currentItem = player.currentMediaItem
            if (currentItem != null) {
                val videoId = currentItem.mediaId
                if (videoId.isNotBlank() && currentVideoId == null) {
                    android.util.Log.d("PlayerViewModel", "Syncing with active player for $videoId")
                    currentVideoId = videoId
                    
                    val metadata = currentItem.mediaMetadata
                    val videoItem = VideoItem(
                        id = videoId,
                        title = metadata.title?.toString() ?: "",
                        thumbnailUrl = metadata.artworkUri?.toString() ?: "",
                        uploaderName = metadata.artist?.toString() ?: "",
                        uploaderUrl = null,
                        viewCount = 0,
                        uploadDate = null,
                        rawUploadDate = null,
                        duration = player.duration / 1000,
                        watchProgress = if (player.duration > 0) player.currentPosition.toFloat() / player.duration else null
                    )
                    
                    currentVideoItem = videoItem
                    
                    // Create a placeholder bundle to restore UI state
                    val placeholderBundle = StreamBundle(
                        videoStreams = emptyList(),
                        audioStreams = emptyList(),
                        title = videoItem.title,
                        uploaderName = videoItem.uploaderName,
                        uploaderUrl = null,
                        uploaderThumbnailUrl = null,
                        description = null,
                        viewCount = 0,
                        uploadDate = null,
                        thumbnailUrl = videoItem.thumbnailUrl
                    )
                    currentBundle = placeholderBundle
                    _uiState.value = PlayerUiState.Success(
                        title = videoItem.title,
                        uploader = videoItem.uploaderName,
                        bundle = placeholderBundle
                    )
                    
                    // Update MiniPlayerManager state
                    if (player.isPlaying || player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY) {
                        miniPlayerManager.minimize(videoItem)
                    }

                    // Trigger a proper load to fetch full stream details and related videos
                    loadVideo(videoItem)
                }
            }
        }
    }

    private fun loadVideoMetadata(videoId: String) {
        if (videoId.isBlank() || videoId == currentVideoId) return
        
        loadingJob?.cancel()
        currentVideoId = videoId
        nextRelatedPage = null
        isPreloaded = false
        preloadingJob?.cancel()

        loadingJob = viewModelScope.launch {
            getVideoStreamsUseCase(videoId)
                .onSuccess { bundle ->
                    if (bundle.isUpcoming) {
                        _uiState.value = PlayerUiState.Upcoming(
                            title = bundle.title,
                            uploader = bundle.uploaderName,
                            scheduledTime = bundle.scheduledStartTime,
                            thumbnailUrl = bundle.thumbnailUrl
                        )
                        miniPlayerManager.updateMetadata(VideoItem(
                            id = videoId,
                            title = bundle.title,
                            thumbnailUrl = bundle.thumbnailUrl ?: "",
                            uploaderName = bundle.uploaderName,
                            uploaderUrl = bundle.uploaderUrl,
                            uploaderThumbnailUrl = bundle.uploaderThumbnailUrl,
                            viewCount = -1L,
                            uploadDate = bundle.uploadDate,
                            rawUploadDate = null,
                            duration = 0,
                            watchProgress = null
                        ))
                        return@onSuccess
                    }

                    currentBundle = bundle
                    currentVideoItem = VideoItem(
                        id = videoId,
                        title = bundle.title,
                        thumbnailUrl = bundle.thumbnailUrl ?: "",
                        uploaderName = bundle.uploaderName,
                        uploaderUrl = bundle.uploaderUrl,
                        uploaderThumbnailUrl = bundle.uploaderThumbnailUrl,
                        viewCount = bundle.viewCount,
                        uploadDate = bundle.uploadDate,
                        rawUploadDate = null,
                        duration = player.duration / 1000,
                        watchProgress = null
                    )
                    
                    nextRelatedPage = bundle.nextRelatedVideosPage
                    _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                    miniPlayerManager.updateMetadata(currentVideoItem)
                    
                    // Watch secondary info
                    val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl
                    uploaderId?.let { id ->
                        launch {
                            isSubscribedUseCase(id).collectLatest {
                                _isSubscribed.value = it
                            }
                        }
                    }
                }
                .onFailure { exception ->
                    _uiState.value = PlayerUiState.Error(PlayTubeError.fromThrowable(exception))
                }
        }
    }

    fun loadVideo(video: VideoItem) {
        val videoId = video.id
        if (videoId.isBlank()) return
        
        android.util.Log.d("PlayerViewModel", "loadVideo called for $videoId. Current: $currentVideoId, PlayerCount: ${player.mediaItemCount}")

        // 1. Check if already active and fully loaded
        val isSameVideo = currentVideoId == videoId && player.mediaItemCount > 0
        val isFullyLoaded = isSameVideo && _uiState.value is PlayerUiState.Success && 
                           !(uiState.value as PlayerUiState.Success).bundle.videoStreams.isEmpty()

        if (isFullyLoaded) {
            android.util.Log.d("PlayerViewModel", "Video $videoId already fully active, maximizing")
            miniPlayerManager.maximize()
            if (!player.isPlaying && player.playWhenReady) player.play()
            return
        }

        loadingJob?.cancel()
        currentVideoId = videoId
        currentVideoItem = video
        nextRelatedPage = null
        lastSavedPosition = 0L
        isStalledDueToNetwork = false
        _isRecovering.value = false
        isPreloaded = false
        preloadingJob?.cancel()
        
        // Reset retry state
        retryCount = 0
        retryJob?.cancel()

        // 2. Only touch player if it's NOT the same video already in it (e.g. from sync)
        if (!isSameVideo) {
            miniPlayerManager.onNewVideoSelected(video)
            
            // CRITICAL: Clean reset to prevent state leaks during transitions
            player.stop()
            player.clearMediaItems()
            
            // Use a placeholder MediaItem to keep the notification alive
            val placeholderMetadata = MediaMetadata.Builder()
                .setTitle(video.title)
                .setArtist(video.uploaderName)
                .setArtworkUri(video.thumbnailUrl.let { android.net.Uri.parse(it) })
                .build()
            
            val placeholderMediaItem = MediaItem.Builder()
                .setMediaId(videoId)
                .setMediaMetadata(placeholderMetadata)
                .setUri(android.net.Uri.EMPTY)
                .build()

            player.setMediaItem(placeholderMediaItem)
            // Prevent playback attempts on empty URI
            player.playWhenReady = false
        }
        
        // 3. Keep UI stable during load
        // Immediately update bundle with next video's basic info so the UI doesn't look empty/stale
        val placeholderBundle = StreamBundle(
            videoStreams = emptyList(),
            audioStreams = emptyList(),
            title = video.title,
            uploaderName = video.uploaderName,
            uploaderUrl = video.uploaderUrl,
            uploaderThumbnailUrl = video.uploaderThumbnailUrl,
            description = null,
            viewCount = video.viewCount,
            uploadDate = video.uploadDate,
            thumbnailUrl = video.thumbnailUrl
        )
        currentBundle = placeholderBundle
        _uiState.value = PlayerUiState.Success(
            title = video.title,
            uploader = video.uploaderName,
            bundle = placeholderBundle
        )
        _isBuffering.value = !isSameVideo
        
        // Start the PlaybackService ONLY if background play is enabled
        viewModelScope.launch {
            if (preferencesManager.isBackgroundPlayEnabled.first()) {
                context.startService(Intent(context, PlaybackService::class.java))
            }
        }
        
        loadingJob = viewModelScope.launch {
            // Watch favorite status (Do this early so it works for local too)
            launch {
                isFavoriteUseCase(videoId).collectLatest {
                    _isFavorite.value = it
                }
            }

            // 1. Check for local download first
            val downloadedVideo = withContext(Dispatchers.IO) {
                downloadRepository.getDownloadByVideoId(videoId)
            }
            
            if (downloadedVideo != null && downloadedVideo.status == DownloadStatus.COMPLETED) {
                val localFile = File(downloadedVideo.filePath)
                val exists = withContext(Dispatchers.IO) { localFile.exists() }
                if (exists) {
                    // Minimal bundle for UI, using local file for playback
                    val localBundle = StreamBundle(
                        videoStreams = emptyList(),
                        audioStreams = emptyList(),
                        title = downloadedVideo.title,
                        uploaderName = downloadedVideo.uploaderName,
                        uploaderUrl = null,
                        uploaderThumbnailUrl = null,
                        description = "Playing from local storage",
                        viewCount = 0,
                        uploadDate = null,
                        thumbnailUrl = downloadedVideo.thumbnailUrl
                    )
                    currentBundle = localBundle
                    _uiState.value = PlayerUiState.Success(downloadedVideo.title, downloadedVideo.uploaderName, localBundle)
                    
                    if (!isSameVideo) {
                        val metadata = MediaMetadata.Builder()
                            .setTitle(downloadedVideo.title)
                            .setArtist(downloadedVideo.uploaderName)
                            .setArtworkUri(downloadedVideo.thumbnailUrl.let { android.net.Uri.parse(it) })
                            .build()

                        val mediaItem = MediaItem.Builder()
                            .setUri(localFile.toURI().toString())
                            .setMediaId(videoId)
                            .setMediaMetadata(metadata)
                            .build()

                        player.setMediaItem(mediaItem)
                        player.prepare()
                        resumeFromHistory(videoId)
                        player.playWhenReady = true
                    }
                    
                    _currentQuality.value = "Local (${downloadedVideo.quality})"
                    return@launch
                }
            }

            // 2. Otherwise, fetch from internet
            getVideoStreamsUseCase(videoId)
                .onSuccess { bundle ->
                    // Phase 1: Update Metadata and State (CRITICAL: Do this BEFORE setMediaSource)
                    currentBundle = bundle
                    nextRelatedPage = bundle.nextRelatedVideosPage
                    _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                    
                    // Phase 2: Immediate Playback Start
                    // Now that currentBundle is set, setMediaSource will correctly inject subtitles
                    val initialStream = bundle.videoStreams.find { it.quality.contains("360") }
                        ?: bundle.videoStreams.find { it.quality.contains("480") }
                        ?: bundle.videoStreams.find { it.quality.contains("720") }
                        ?: bundle.videoStreams.firstOrNull()

                    initialStream?.let { stream ->
                        if (!isSameVideo || player.playbackState == Player.STATE_IDLE) {
                            val resumePos = if (bundle.isLive) 0 else getResumePosition(videoId)
                            setMediaSource(stream, resumePos, bundle.isLive)
                        } else {
                            _currentQuality.value = stream.quality
                        }
                    }

                    // Phase 3: Secondary info
                    // Watch subscription status
                    val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl
                    uploaderId?.let { id ->
                        launch {
                            isSubscribedUseCase(id).collectLatest {
                                _isSubscribed.value = it
                            }
                        }
                    }

                    // Add to history if enabled
                    launch {
                        if (preferencesManager.isHistoryEnabled.first()) {
                            addToHistoryUseCase(
                                HistoryEntity(
                                    videoId = videoId,
                                    title = bundle.title,
                                    thumbnailUrl = bundle.thumbnailUrl ?: "",
                                    uploaderName = bundle.uploaderName
                                )
                            )
                        }
                    }
                }
                .onFailure { exception ->
                    _uiState.value = PlayerUiState.Error(PlayTubeError.fromThrowable(exception))
                }
        }
    }

    private suspend fun getResumePosition(videoId: String): Long {
        val history = libraryRepository.getHistory().first()
        val item = history.find { it.videoId == videoId }
        return if (item != null && item.durationMs > 0) {
            // Don't resume if very close to end (e.g. 95%)
            if (item.progressMs > item.durationMs * 0.95) 0 else item.progressMs
        } else 0
    }

    private suspend fun resumeFromHistory(videoId: String) {
        val resumePos = getResumePosition(videoId)
        if (resumePos > 0) {
            player.seekTo(resumePos)
            lastSavedPosition = resumePos
        }
    }

    fun loadNextRelatedPage() {
        val currentId = currentVideoId
        val currentPage = nextRelatedPage
        if (isFetchingNextRelatedPage || currentPage == null || currentId == null) return

        isFetchingNextRelatedPage = true
        viewModelScope.launch {
            try {
                val result = videoRepository.fetchNextRelatedPage(currentId, currentPage)
                val currentState = _uiState.value
                if (currentState is PlayerUiState.Success) {
                    nextRelatedPage = result.nextPage
                    val updatedBundle = currentState.bundle.copy(
                        relatedVideos = currentState.bundle.relatedVideos + result.items,
                        nextRelatedVideosPage = result.nextPage
                    )
                    currentBundle = updatedBundle
                    _uiState.value = PlayerUiState.Success(currentState.title, currentState.uploader, updatedBundle)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isFetchingNextRelatedPage = false
        }
    }

    fun toggleFavorite(video: VideoItem? = null) {
        val targetVideo = video ?: currentVideoItem ?: return
        val videoId = targetVideo.id
        
        viewModelScope.launch {
            val isFav = libraryRepository.isFavorite(videoId).first()
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = videoId,
                    title = targetVideo.title,
                    thumbnailUrl = targetVideo.thumbnailUrl,
                    uploaderName = targetVideo.uploaderName
                )
            )
            _snackbarMessage.emit(if (isFav) "Removed from Favorites" else "Added to Favorites")
        }
    }

    fun toggleSubscription() {
        val bundle = currentBundle ?: return
        val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = uploaderId,
                    name = bundle.uploaderName,
                    thumbnailUrl = bundle.uploaderThumbnailUrl,
                    subscriberCount = bundle.uploaderSubscriberCount
                )
            )
        }
    }

    fun prepareDownload(video: VideoItem? = null) {
        val targetVideo = video ?: currentVideoItem ?: return
        
        // Instant response for the currently active video
        if (targetVideo.id == currentVideoId && currentBundle != null && !currentBundle!!.videoStreams.isEmpty()) {
            _downloadState.value = DownloadDialogState.ShowDialog(targetVideo, currentBundle!!)
            return
        }

        viewModelScope.launch {
            // Check Repository Cache for related videos
            val cachedBundle = videoRepository.getCachedStreamBundle(targetVideo.id)
            if (cachedBundle != null && !cachedBundle.videoStreams.isEmpty()) {
                _downloadState.value = DownloadDialogState.ShowDialog(targetVideo, cachedBundle)
                return@launch
            }

            _downloadState.value = DownloadDialogState.Loading(targetVideo)
            getVideoStreamsUseCase(targetVideo.id)
                .onSuccess { bundle ->
                    _downloadState.value = DownloadDialogState.ShowDialog(targetVideo, bundle)
                }
                .onFailure {
                    _downloadState.value = DownloadDialogState.Idle
                }
        }
    }

    fun download(video: VideoItem, bundle: StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean) {
        viewModelScope.launch {
            val audioUrl = if (isAdaptive) {
                val isWebm = format?.contains("webm", ignoreCase = true) == true
                val compatibleStreams = bundle.audioStreams.filter { audio ->
                    if (isWebm) {
                        audio.format.contains("webm", ignoreCase = true) || 
                        audio.format.contains("opus", ignoreCase = true)
                    } else {
                        audio.format.contains("m4a", ignoreCase = true) || 
                        audio.format.contains("aac", ignoreCase = true)
                    }
                }

                // Prefer ORIGINAL track with highest bitrate, then any other track with highest bitrate
                val bestAudio = compatibleStreams.filter { it.trackType == "ORIGINAL" }
                    .maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?: compatibleStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                
                bestAudio?.url
            } else null

            downloadVideoUseCase(
                videoId = video.id,
                url = url,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                uploaderName = video.uploaderName,
                quality = quality,
                format = format,
                audioUrl = audioUrl
            )
            _snackbarMessage.emit("Downloading started")
            _downloadState.value = DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadDialogState.Idle
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setMediaSource(stream: StreamItem, startPosition: Long = 0, isLive: Boolean = false) {
        val bundle = currentBundle
        
        // 1. Build MediaMetadata for the MediaItem
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle?.title ?: currentVideoItem?.title ?: "Unknown Title")
            .setArtist(bundle?.uploaderName ?: currentVideoItem?.uploaderName ?: "Unknown Channel")
            .setArtworkUri(bundle?.thumbnailUrl?.let { android.net.Uri.parse(it) } ?: currentVideoItem?.thumbnailUrl?.let { android.net.Uri.parse(it) })
            .build()

        // 2. Map available subtitles to native SubtitleConfigurations
        val subtitleConfigs = bundle?.let { createSubtitleConfigs(it) } ?: emptyList()

        // 3. Build the primary MediaItem with Metadata
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(currentVideoId ?: "")
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)
            
        val isManifest = stream.format == "m3u8" || stream.format == "mpd"

        if (isLive || isManifest) {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            mediaItemBuilder.setMimeType(mimeType)
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(10000) // 10s offset for stability
                    .setMaxPlaybackSpeed(1.1f) // Allow catching up to live edge
                    .setMinPlaybackSpeed(0.9f)
                    .build()
            )
        }

        val mediaItem = mediaItemBuilder.build()

        // 4. Set the media source using automatic routing via DefaultMediaSourceFactory
        // Use raw HttpDataSource for HLS/DASH to bypass cache issues and handle segments reliably
        val effectiveDataSourceFactory = if (isManifest) httpDataSourceFactory else dataSourceFactory
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(effectiveDataSourceFactory)
        
        // CRITICAL: Stop and clear again before setting the final media source to ensure a clean state
        player.stop()
        player.clearMediaItems()
        
        // Let the factory automatically choose HlsMediaSource, DashMediaSource, or ProgressiveMediaSource
        if (stream.isAdaptive) {
            val audioUrl = currentBundle?.bestAudioStreamUrl
            if (audioUrl != null) {
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                player.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
            }
        } else {
            player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        }
        
        // 5. Track parameters and preparation
        updateCcState(_isCcEnabled.value, _selectedSubtitleLanguage.value)

        player.prepare()
        if (startPosition > 0 && !isLive) {
            player.seekTo(startPosition)
            lastSavedPosition = startPosition
        }
        player.playWhenReady = true
        _currentQuality.value = stream.quality
    }

    fun toggleSubtitles() {
        setSubtitlesEnabled(!_isCcEnabled.value)
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _isCcEnabled.value = enabled
        updateCcState(enabled, _selectedSubtitleLanguage.value)
        
        // Persist preference
        viewModelScope.launch {
            preferencesManager.setSubtitlesEnabled(enabled)
        }
    }

    fun setAutoplayEnabled(enabled: Boolean) {
        _isAutoplayEnabled.value = enabled
        viewModelScope.launch {
            preferencesManager.setAutoplayEnabled(enabled)
        }
    }

    fun setSubtitleLanguage(languageTag: String?) {
        viewModelScope.launch {
            if (languageTag == null) {
                setSubtitlesEnabled(false)
            } else {
                _isCcEnabled.value = true
                _selectedSubtitleLanguage.value = languageTag
                updateCcState(true, languageTag)
                preferencesManager.setSubtitlesEnabled(true)
                preferencesManager.setPreferredSubtitleLanguage(languageTag)
            }
        }
    }

    fun updateCcState(enabled: Boolean, preferredLang: String? = null) {
        val parametersBuilder = player.trackSelectionParameters.buildUpon()
        
        if (enabled) {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            
            val language = preferredLang ?: _selectedSubtitleLanguage.value
            if (language != null) {
                // Set preferred language and clear any specific track overrides
                // to allow ExoPlayer's selection logic to pick the best match for this language.
                parametersBuilder.setPreferredTextLanguage(language)
                parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
            }
            
            // If still no active track after setting preference, or if we want to ensure ONE is selected
            val currentTracks = player.currentTracks
            val hasActiveTextTrack = currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
            
            if (!hasActiveTextTrack) {
                // Fallback: If no track matches the preferred language perfectly, 
                // find the first supported text track and enable it.
                for (group in currentTracks.groups) {
                    if (group.type == C.TRACK_TYPE_TEXT && group.isSupported) {
                        parametersBuilder.addOverride(
                            TrackSelectionOverride(group.mediaTrackGroup, 0)
                        )
                        break
                    }
                }
            }
        } else {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            parametersBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
        }
        
        player.trackSelectionParameters = parametersBuilder.build()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun createSubtitleConfigs(bundle: StreamBundle): List<MediaItem.SubtitleConfiguration> {
        return bundle.subtitles.filter { it.url.isNotBlank() }.map { subtitle ->
            val mimeType = when (subtitle.format.lowercase()) {
                "vtt" -> MimeTypes.TEXT_VTT
                "ttml" -> MimeTypes.APPLICATION_TTML
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                else -> MimeTypes.TEXT_VTT
            }
            
            val label = if (subtitle.isAutoGenerated) {
                "${java.util.Locale.forLanguageTag(subtitle.languageTag).displayLanguage} (Auto-generated)"
            } else {
                java.util.Locale.forLanguageTag(subtitle.languageTag).displayLanguage
            }

            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
                .setMimeType(mimeType)
                .setLanguage(subtitle.languageTag)
                .setLabel(label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition
                val rawDur = player.duration
                val dur = if (rawDur == C.TIME_UNSET) 0L else rawDur.coerceAtLeast(0L)
                
                _currentPosition.value = pos
                _duration.value = dur
                _bufferedPosition.value = player.bufferedPosition

                // Trigger preloading when 80% through or after 30s of playback (VOD only)
                val isLive = currentBundle?.isLive == true
                if (!isLive && !isPreloaded && dur > 0) {
                    val progress = pos.toFloat() / dur
                    if (progress > 0.8f || pos > 30000) {
                        preloadNextVideo()
                    }
                }

                // Debounced save (VOD only, every 2 seconds or significant jump)
                if (!isLive && abs(pos - lastSavedPosition) >= 2000) {
                    saveWatchProgress()
                }
                
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun saveWatchProgress() {
        val videoId = currentVideoId ?: return
        val position = player.currentPosition
        val duration = player.duration
        val bundle = currentBundle
        
        // Don't save progress for live streams
        if (bundle?.isLive == true || duration <= 0 || duration == C.TIME_UNSET) return
        
        lastSavedPosition = position
        val watchRatio = position.toFloat() / duration

        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesManager.isHistoryEnabled.first()) {
                updateWatchProgressUseCase(videoId, position, duration)
                
                // Reinforce interests based on actual watch time
                bundle?.let {
                    updateUserInterestsUseCase(it.title, 0.5f, watchRatio)
                    updateUserInterestsUseCase(it.uploaderName, 1.0f, watchRatio)
                }
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun setQuality(stream: StreamItem) {
        val currentPosition = player.currentPosition
        setMediaSource(stream, currentPosition, currentBundle?.isLive ?: false)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
        _currentPosition.value = position
        saveWatchProgress()
    }

    private var seekJob: Job? = null

    fun performSeek(forward: Boolean) {
        seekJob?.cancel()
        
        if (_isSeekForward.value != forward || !_showSeekFeedback.value) {
            _seekAmount.value = 10
        } else {
            _seekAmount.value += 10
        }
        
        _isSeekForward.value = forward
        _showSeekFeedback.value = true

        val seekTime = if (forward) 10000L else -10000L
        player.seekTo(player.currentPosition + seekTime)

        seekJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            _showSeekFeedback.value = false
            _seekAmount.value = 0
            saveWatchProgress()
        }
    }

    fun seekForward() {
        performSeek(true)
    }

    fun seekBackward() {
        performSeek(false)
    }

    fun shareVideo() {
        val videoId = currentVideoId ?: return
        val title = currentBundle?.title ?: "Video"
        val url = "https://www.youtube.com/watch?v=$videoId"
        
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "$title\n\n$url")
            type = "text/plain"
        }
        
        val shareIntent = Intent.createChooser(sendIntent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    fun minimize() {
        if (miniPlayerManager.isMinimized.value) return
        val bundle = currentBundle ?: return
        val videoId = currentVideoId ?: return
        val videoItem = VideoItem(
            id = videoId,
            title = bundle.title,
            thumbnailUrl = bundle.thumbnailUrl ?: "",
            uploaderName = bundle.uploaderName,
            uploaderUrl = bundle.uploaderUrl,
            viewCount = bundle.viewCount ?: 0,
            uploadDate = bundle.uploadDate,
            rawUploadDate = null,
            duration = player.duration / 1000, // In seconds
            watchProgress = if (player.duration > 0) player.currentPosition.toFloat() / player.duration else null
        )
        miniPlayerManager.minimize(videoItem)
    }

    fun playNext() {
        if (player.hasNextMediaItem()) {
            android.util.Log.d("PlayerViewModel", "Manual skip using pre-loaded next item")
            player.seekToNextMediaItem()
        } else {
            currentBundle?.relatedVideos?.firstOrNull()?.let { nextVideo ->
                loadVideo(nextVideo)
            }
        }
    }

    fun playPrevious() {
        // Simple logic: if we've watched more than 5s, restart. Else we'd need a history stack
        if (player.currentPosition > 5000) {
            player.seekTo(0)
        } else {
            // Future: could implement a 'recent sessions' stack in LibraryRepository
            _snackbarMessage.tryEmit("No previous video in session")
        }
    }

    private fun isNetworkError(error: PlaybackException): Boolean {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> true
            else -> error.cause is java.net.UnknownHostException || 
                    error.cause is java.net.ConnectException || 
                    error.cause is java.net.SocketTimeoutException
        }
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        if (retryCount >= 5) {
            android.util.Log.d("PlayerRecovery", "Max retries reached")
            return
        }

        val delayMs = (Math.pow(2.0, retryCount.toDouble()) * 1000).toLong()
        retryCount++
        
        android.util.Log.d("PlayerRecovery", "Scheduling retry #$retryCount in ${delayMs}ms")
        
        retryJob = viewModelScope.launch {
            kotlinx.coroutines.delay(delayMs)
            
            // Only retry if we are still in a recoverable state
            if (isStalledDueToNetwork) {
                retryPlayback()
            }
        }
    }

    private fun retryPlayback() {
        android.util.Log.d("PlayerRecovery", "Retrying playback at $lastFailedPosition")
        
        // Before retrying online, check if we have a download to switch to
        checkAndSwitchToLocalIfAvailable()

        player.seekTo(lastFailedPosition)
        player.prepare()
        player.play()
    }

    private fun checkAndSwitchToLocalIfAvailable() {
        val videoId = currentVideoId ?: return
        viewModelScope.launch {
            val download = withContext(Dispatchers.IO) {
                downloadRepository.getDownloadByVideoId(videoId)
            }
            if (download != null && download.status == DownloadStatus.COMPLETED) {
                val localFile = File(download.filePath)
                if (withContext(Dispatchers.IO) { localFile.exists() }) {
                    android.util.Log.d("PlayerRecovery", "Network lost, switching to local file for $videoId")
                    
                    val metadata = MediaMetadata.Builder()
                        .setTitle(download.title)
                        .setArtist(download.uploaderName)
                        .setArtworkUri(download.thumbnailUrl.let { android.net.Uri.parse(it) })
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(android.net.Uri.fromFile(localFile))
                        .setMediaId(videoId)
                        .setMediaMetadata(metadata)
                        .build()

                    player.setMediaItem(mediaItem)
                    _currentQuality.value = "Local (${download.quality})"
                    // The retryPlayback caller will call prepare() and seekTo()
                }
            }
        }
    }

    private fun recoverExpiredUrl() {
        val videoId = currentVideoId ?: return
        android.util.Log.d("PlayerRecovery", "URL might be expired, re-fetching stream for $videoId")
        _isRecovering.value = true
        
        viewModelScope.launch {
            getVideoStreamsUseCase(videoId, forceRefresh = true)
                .onSuccess { bundle ->
                    currentBundle = bundle
                    val initialStream = bundle.videoStreams.find { it.quality.contains("360") }
                        ?: bundle.videoStreams.find { it.quality.contains("480") }
                        ?: bundle.videoStreams.firstOrNull()
                    
                    initialStream?.let {
                        setMediaSource(it, lastFailedPosition, bundle.isLive)
                        isStalledDueToNetwork = false
                        _isRecovering.value = false
                    }
                }
                .onFailure {
                    _uiState.value = PlayerUiState.Error(PlayTubeError.Unknown("Failed to recover stream"))
                    _isRecovering.value = false
                }
        }
    }

    private fun preloadNextVideo() {
        val nextVideo = currentBundle?.relatedVideos?.firstOrNull() ?: return
        isPreloaded = true
        android.util.Log.d("PlayerPreload", "Starting preload for next video: ${nextVideo.id}")
        
        preloadingJob = viewModelScope.launch(Dispatchers.IO) {
            videoRepository.preloadStreamBundle(nextVideo.id)
            
            // Phase 2: Prepare the next MediaItem in the player queue for instant skip
            val nextBundleResult = getVideoStreamsUseCase(nextVideo.id)
            nextBundleResult.onSuccess { bundle ->
                withContext(Dispatchers.Main) {
                    prepareNextMediaItem(nextVideo, bundle)
                }
            }
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun prepareNextMediaItem(video: VideoItem, bundle: StreamBundle) {
        if (!_isAutoplayEnabled.value) return // Don't preload if autoplay is disabled
        
        val stream = bundle.videoStreams.find { it.quality.contains("360") }
            ?: bundle.videoStreams.find { it.quality.contains("480") }
            ?: bundle.videoStreams.firstOrNull() ?: return
            
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle.title)
            .setArtist(bundle.uploaderName)
            .setArtworkUri(bundle.thumbnailUrl?.let { android.net.Uri.parse(it) })
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(video.id)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(createSubtitleConfigs(bundle))

        if (stream.format == "m3u8" || stream.format == "mpd") {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            mediaItemBuilder.setMimeType(mimeType)
        }

        val mediaItem = mediaItemBuilder.build()

        // Construct source using the same logic as setMediaSource to ensure audio merging
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        val finalSource = if (stream.isAdaptive) {
            val audioUrl = bundle.bestAudioStreamUrl
            if (audioUrl != null) {
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                MergingMediaSource(videoSource, audioSource)
            } else {
                mediaSourceFactory.createMediaSource(mediaItem)
            }
        } else {
            mediaSourceFactory.createMediaSource(mediaItem)
        }

        // Add as the next item in the queue if it's not already there
        if (player.mediaItemCount <= 1) {
            android.util.Log.d("PlayerPreload", "Adding next MediaSource to player queue: ${video.id}")
            player.addMediaSource(1, finalSource)
        }
    }

    fun stopPlayback() {
        saveWatchProgress()
        loadingJob?.cancel()
        loadingJob = null
        player.pause()
        player.stop()
        player.clearMediaItems()
        currentVideoId = null
        currentBundle = null
        _uiState.value = PlayerUiState.Loading
        _isBuffering.value = false
    }

    override fun onCleared() {
        saveWatchProgress()
        super.onCleared()
        // DO NOT stop the player here if the activity is just recreating
        // but we handle cleanup in MainActivity.onStop/onDestroy
        player.removeListener(playerListener)
    }
}

sealed interface PlayerUiState {
    object Loading : PlayerUiState
    data class Success(val title: String, val uploader: String, val bundle: StreamBundle) : PlayerUiState
    data class Upcoming(val title: String, val uploader: String, val scheduledTime: String?, val thumbnailUrl: String?) : PlayerUiState
    data class Error(val error: PlayTubeError) : PlayerUiState
}
