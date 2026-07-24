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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import java.io.File
import javax.inject.Inject
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
    private val preferencesManager: PreferencesManager,
    private val dataSourceFactory: DataSource.Factory,
    val miniPlayerManager: MiniPlayerManager,
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
    private var currentVideoItem: VideoItem? = null
    private var currentVideoId: String? = null
    private var loadingJob: Job? = null
    private var progressJob: Job? = null
    private var nextRelatedPage: Page? = null
    private var isFetchingNextRelatedPage = false
    private var lastSavedPosition = 0L

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            _duration.value = player.duration.coerceAtLeast(0L)
            
            if (playbackState == Player.STATE_READY) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }

            if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
                saveWatchProgress()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) {
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
    }

    init {
        // Ensure we don't have multiple listeners if ViewModel is recreated
        player.removeListener(playerListener)
        player.addListener(playerListener)
        
        // Load persistent CC preference
        viewModelScope.launch {
            preferencesManager.isSubtitlesEnabled.collectLatest { enabled ->
                _isCcEnabled.value = enabled
                updateCcState(enabled)
            }
        }
    }

    fun loadVideo(video: VideoItem) {
        val videoId = video.id
        if (videoId.isBlank()) return
        currentVideoItem = video

        // If it's the same video and it's already playing/buffering, don't reload
        // Exception: If the current state is Error, we should allow a retry
        if (currentVideoId == videoId && 
            _uiState.value !is PlayerUiState.Error &&
            player.playbackState != Player.STATE_IDLE && 
            player.playbackState != Player.STATE_ENDED) {

            _uiState.value = PlayerUiState.Success(
                currentBundle?.title ?: video.title,
                currentBundle?.uploaderName ?: video.uploaderName,
                currentBundle ?: StreamBundle(
                    videoStreams = emptyList(),
                    audioStreams = emptyList(),
                    title = video.title,
                    uploaderName = video.uploaderName,
                    uploaderUrl = video.uploaderUrl,
                    uploaderThumbnailUrl = null,
                    description = null,
                    viewCount = video.viewCount,
                    uploadDate = video.uploadDate,
                    thumbnailUrl = video.thumbnailUrl
                )
            )
            player.playWhenReady = true
            miniPlayerManager.maximize()
            return
        }
        
        loadingJob?.cancel()
        currentVideoId = videoId
        nextRelatedPage = null
        lastSavedPosition = 0L
        
        // Reset player for new content
        player.stop()
        player.clearMediaItems()
        miniPlayerManager.onNewVideoSelected(video)
        
        // Start the PlaybackService ONLY if background play is enabled
        viewModelScope.launch {
            if (preferencesManager.isBackgroundPlayEnabled.first()) {
                context.startService(Intent(context, PlaybackService::class.java))
            }
        }
        
        loadingJob = viewModelScope.launch {
            _uiState.value = PlayerUiState.Loading
            
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
            android.util.Log.d("PlayerViewModel", "Checking local playback for $videoId. Found in DB: ${downloadedVideo != null}, Status: ${downloadedVideo?.status}")
            
            if (downloadedVideo != null && downloadedVideo.status == DownloadStatus.COMPLETED) {
                val localFile = File(downloadedVideo.filePath)
                val exists = withContext(Dispatchers.IO) { localFile.exists() }
                if (exists) {
                    android.util.Log.d("PlayerViewModel", "Local file exists at ${downloadedVideo.filePath}. Playing offline.")
                    
                    val metadata = MediaMetadata.Builder()
                        .setTitle(downloadedVideo.title)
                        .setArtist(downloadedVideo.uploaderName)
                        .setArtworkUri(downloadedVideo.thumbnailUrl.let { android.net.Uri.parse(it) })
                        .build()

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
                    
                    val mediaItem = MediaItem.Builder()
                        .setUri(android.net.Uri.fromFile(localFile))
                        .setMediaId(videoId)
                        .setMediaMetadata(metadata)
                        .build()

                    player.setMediaItem(mediaItem)
                    player.prepare()

                    // Try to resume from history
                    resumeFromHistory(videoId)

                    player.playWhenReady = true
                    _currentQuality.value = "Local (${downloadedVideo.quality})"
                    return@launch
                } else {
                    android.util.Log.e("PlayerViewModel", "Local file NOT found at ${downloadedVideo.filePath}")
                }
            }

            // 2. Otherwise, fetch from internet
            getVideoStreamsUseCase(videoId)
                .onSuccess { bundle ->
                    currentBundle = bundle
                    nextRelatedPage = bundle.nextRelatedVideosPage
                    _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                    
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

                    // Pick the best video stream for playback (Default to 360p for fast loading)
                    val initialStream = bundle.videoStreams.find { it.quality.contains("360") }
                        ?: bundle.videoStreams.find { it.quality.contains("480") }
                        ?: bundle.videoStreams.find { it.quality.contains("720") }
                        ?: bundle.videoStreams.firstOrNull() // Lowest quality if sorted ascending

                    initialStream?.let { stream ->
                        // Try to resume from history before setting media source
                        val resumePos = getResumePosition(videoId)
                        setMediaSource(stream, resumePos)
                    }
                }
                .onFailure { exception ->
                    val errorMessage = if (exception is java.net.UnknownHostException || exception is java.io.IOException) {
                        "No internet connection"
                    } else {
                        exception.message ?: "Unknown error"
                    }
                    _uiState.value = PlayerUiState.Error(errorMessage)
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
        viewModelScope.launch {
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
    private fun setMediaSource(stream: StreamItem, startPosition: Long = 0) {
        val bundle = currentBundle
        
        // 1. Build MediaMetadata for the MediaItem
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle?.title ?: currentVideoItem?.title ?: "Unknown Title")
            .setArtist(bundle?.uploaderName ?: currentVideoItem?.uploaderName ?: "Unknown Channel")
            .setArtworkUri(bundle?.thumbnailUrl?.let { android.net.Uri.parse(it) } ?: currentVideoItem?.thumbnailUrl?.let { android.net.Uri.parse(it) })
            .build()

        // 2. Map available subtitles to native SubtitleConfigurations
        val subtitleConfigs = currentBundle?.subtitles?.filter { it.url.isNotBlank() }?.map { subtitle ->
            val mimeType = when (subtitle.format.lowercase()) {
                "vtt" -> MimeTypes.TEXT_VTT
                "ttml" -> MimeTypes.APPLICATION_TTML
                "srt" -> MimeTypes.APPLICATION_SUBRIP
                else -> MimeTypes.TEXT_VTT
            }
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.url))
                .setMimeType(mimeType)
                .setLanguage(subtitle.languageTag)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        } ?: emptyList()

        // 3. Build the primary MediaItem with Metadata
        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(currentVideoId ?: "")
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        // 4. Set the media source using DefaultMediaSourceFactory logic.
        // For adaptive, we still need to merge with audio, but we MUST use the factory 
        // to ensure SubtitleConfigurations are processed.
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        
        if (stream.isAdaptive) {
            val audioUrl = currentBundle?.bestAudioStreamUrl
            if (audioUrl != null) {
                // The correct way: Use the factory to create the video source (it handles subtitles)
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                // Audio is a separate simple source
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                
                player.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
            }
        } else {
            player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        }
        
        // 4. Track parameters and preparation
        updateCcState(_isCcEnabled.value)

        player.prepare()
        if (startPosition > 0) {
            player.seekTo(startPosition)
            lastSavedPosition = startPosition
        }
        player.playWhenReady = true
        _currentQuality.value = stream.quality
    }

    fun toggleSubtitles() {
        val newState = !_isCcEnabled.value
        _isCcEnabled.value = newState
        updateCcState(newState)
        
        // Persist preference
        viewModelScope.launch {
            preferencesManager.setSubtitlesEnabled(newState)
        }
    }

    fun updateCcState(enabled: Boolean) {
        val parametersBuilder = player.trackSelectionParameters.buildUpon()
        
        if (enabled) {
            parametersBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            
            // Force selection of the first available text track if none is selected
            val currentTracks = player.currentTracks
            val hasActiveTextTrack = currentTracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }
            
            if (!hasActiveTextTrack) {
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

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition
                val dur = player.duration.coerceAtLeast(0L)
                _currentPosition.value = pos
                _duration.value = dur
                _bufferedPosition.value = player.bufferedPosition
                
                // Debounced save (every 15 seconds or significant jump)
                if (abs(pos - lastSavedPosition) >= 15000) {
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
        
        if (duration <= 0) return
        
        lastSavedPosition = position
        viewModelScope.launch(Dispatchers.IO) {
            if (preferencesManager.isHistoryEnabled.first()) {
                libraryRepository.updateWatchProgress(videoId, position, duration)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun setQuality(stream: StreamItem) {
        val currentPosition = player.currentPosition
        setMediaSource(stream, currentPosition)
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
    data class Error(val message: String) : PlayerUiState
}
