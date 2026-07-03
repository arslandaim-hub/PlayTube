/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.domain.usecase.AddToHistoryUseCase
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.IsFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.IsSubscribedUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import com.arslandaim.playtube.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    val downloadRepository: DownloadRepository,
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

    private val _currentQuality = MutableStateFlow<String?>(null)
    val currentQuality: StateFlow<String?> = _currentQuality.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val downloadedVideoIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { list -> 
            list.filter { it.status == DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private var currentBundle: StreamBundle? = null
    private var currentVideoId: String? = null
    private var loadingJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
        }
    }

    init {
        player.addListener(playerListener)
    }

    fun loadVideo(videoIdOrUrl: String) {
        if (videoIdOrUrl.isBlank()) return

        val videoId = VideoUtils.extractVideoId(videoIdOrUrl)

        // If it's the same video and it's already playing/buffering, don't reload
        if (currentVideoId == videoId && player.playbackState != Player.STATE_IDLE && player.playbackState != Player.STATE_ENDED) {
            _uiState.value = PlayerUiState.Success(
                currentBundle?.title ?: "",
                currentBundle?.uploaderName ?: "",
                currentBundle!!
            )
            player.playWhenReady = true // Ensure it keeps playing if it was paused/stopped somehow
            return
        }
        
        loadingJob?.cancel()
        currentVideoId = videoId
        
        // Reset player for new content
        player.stop()
        player.clearMediaItems()
        miniPlayerManager.maximize() // Ensure UI is maximized if we are loading a new video manually
        
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
                    
                    val mediaItem = MediaItem.fromUri(android.net.Uri.fromFile(localFile))
                    player.setMediaItem(mediaItem)
                    player.prepare()
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
                    _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                    
                    // Watch subscription status
                    bundle.uploaderUrl?.let { url ->
                        launch {
                            isSubscribedUseCase(url).collectLatest {
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
                        setMediaSource(stream)
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

    fun toggleFavorite() {
        val bundle = currentBundle ?: return
        val videoId = currentVideoId ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = videoId,
                    title = bundle.title,
                    thumbnailUrl = bundle.thumbnailUrl ?: "",
                    uploaderName = bundle.uploaderName
                )
            )
        }
    }

    fun toggleSubscription() {
        val bundle = currentBundle ?: return
        val uploaderUrl = bundle.uploaderUrl ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = uploaderUrl,
                    name = bundle.uploaderName,
                    thumbnailUrl = bundle.uploaderThumbnailUrl
                )
            )
        }
    }

    fun download(url: String?, quality: String?, format: String?, isAdaptive: Boolean = false) {
        val bundle = currentBundle ?: return
        val videoId = currentVideoId ?: return
        
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
                videoId = videoId,
                url = url,
                title = bundle.title,
                thumbnailUrl = bundle.thumbnailUrl ?: "",
                uploaderName = bundle.uploaderName,
                quality = quality,
                format = format,
                audioUrl = audioUrl
            )
            _snackbarMessage.emit("Downloading started")
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    private fun setMediaSource(stream: StreamItem, startPosition: Long = 0) {
        val videoSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(stream.url))
            
        val finalSource = if (stream.isAdaptive) {
            val audioUrl = currentBundle?.bestAudioStreamUrl
            if (audioUrl != null) {
                val audioSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(audioUrl))
                MergingMediaSource(videoSource, audioSource)
            } else {
                videoSource
            }
        } else {
            videoSource
        }
        
        player.setMediaSource(finalSource)
        player.prepare()
        if (startPosition > 0) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
        _currentQuality.value = stream.quality
    }

    fun setQuality(stream: StreamItem) {
        val currentPosition = player.currentPosition
        setMediaSource(stream, currentPosition)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.setPlaybackSpeed(speed)
    }

    fun seekForward() {
        player.seekTo(player.currentPosition + 10000)
    }

    fun seekBackward() {
        player.seekTo(player.currentPosition - 10000)
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
            duration = player.duration / 1000 // In seconds
        )
        miniPlayerManager.minimize(videoItem)
    }

    fun stopPlayback() {
        loadingJob?.cancel()
        loadingJob = null
        player.stop()
        player.clearMediaItems()
        currentVideoId = null
        currentBundle = null
        _uiState.value = PlayerUiState.Loading
    }

    override fun onCleared() {
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
