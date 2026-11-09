/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.arslandaim.playtube.R
import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.di.ApplicationScope
import com.arslandaim.playtube.domain.model.*
import com.arslandaim.playtube.domain.repository.*
import com.arslandaim.playtube.domain.usecase.*
import com.arslandaim.playtube.services.PlaybackService
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.schabi.newpipe.extractor.Page
import java.io.File
import javax.inject.Inject
import kotlin.math.abs

import androidx.media3.common.text.Cue

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val getPlaylistDetailsUseCase: GetPlaylistDetailsUseCase,
    private val downloadRepository: DownloadRepository,
    val libraryRepository: LibraryRepository,
    private val videoRepository: VideoRepository,
    private val addToHistoryUseCase: AddToHistoryUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val isFavoriteUseCase: IsFavoriteUseCase,
    private val isSavedUseCase: IsSavedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val updateWatchProgressUseCase: UpdateWatchProgressUseCase,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase,
    private val getSponsorSegmentsUseCase: GetSponsorSegmentsUseCase,
    private val preferencesManager: PreferencesManager,
    private val connectivityObserver: ConnectivityObserver,
    private val playbackManager: PlaybackManager,
    private val playbackPrefetcher: com.arslandaim.playtube.data.network.PlaybackPrefetcher,
    private val poTokenProvider: com.arslandaim.playtube.data.network.PoTokenProvider,
    private val okHttpClient: okhttp3.OkHttpClient,
    @ApplicationScope private val externalScope: CoroutineScope,
    val miniPlayerManager: MiniPlayerManager,
    val sleepTimerManager: SleepTimerManager,
    val queueManager: QueueManager
) : ViewModel() {

    val player: Player = playbackManager.player
    
    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val isBuffering: StateFlow<Boolean> = playbackManager.isBuffering
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPosition: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.duration
    val bufferedPosition: StateFlow<Long> = playbackManager.bufferedPosition
    val playbackStats: StateFlow<PlaybackStats> = playbackManager.playbackStats

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _isSubscribed = MutableStateFlow(false)
    val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    private val _currentPlaylist = MutableStateFlow<PlaylistDetails?>(null)
    val currentPlaylist: StateFlow<PlaylistDetails?> = _currentPlaylist.asStateFlow()

    private val _playlistIndex = MutableStateFlow(-1)
    val playlistIndex: StateFlow<Int> = _playlistIndex.asStateFlow()

    private val _isCcEnabled = MutableStateFlow(false)
    val isCcEnabled: StateFlow<Boolean> = _isCcEnabled.asStateFlow()

    private val _isAutoplayEnabled = MutableStateFlow(true)
    val isAutoplayEnabled: StateFlow<Boolean> = _isAutoplayEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    val sleepTimerRemainingTime: StateFlow<Int?> = sleepTimerManager.remainingTime
    val shouldCloseAppOnTimerFinish: StateFlow<Boolean> = sleepTimerManager.shouldCloseApp

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    private val _selectedSubtitleLanguage = MutableStateFlow<String?>(null)
    val selectedSubtitleLanguage: StateFlow<String?> = _selectedSubtitleLanguage.asStateFlow()

    private val _selectedSubtitle = MutableStateFlow<SubtitleItem?>(null)
    val selectedSubtitle: StateFlow<SubtitleItem?> = _selectedSubtitle.asStateFlow()

    val currentCues: StateFlow<List<Cue>> = playbackManager.currentCues

    val availableSubtitles: StateFlow<List<SubtitleItem>> = uiState.map { state ->
        if (state is PlayerUiState.Success) state.bundle.subtitles else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAudioStream = MutableStateFlow<StreamItem?>(null)
    val selectedAudioStream: StateFlow<StreamItem?> = _selectedAudioStream.asStateFlow()

    val availableAudioStreams: StateFlow<List<StreamItem>> = uiState.map { state ->
        if (state is PlayerUiState.Success) {
            val streams = state.bundle.audioStreams
            val distinctByTrack = streams
                .groupBy { "${it.languageTag ?: "und"}:${it.trackType ?: "ORIGINAL"}" }
                .values
                .mapNotNull { group ->
                    group.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                }
            if (distinctByTrack.size > 1) distinctByTrack else emptyList()
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val volumeBoostLevel: StateFlow<VolumeBoostLevel> = playbackManager.volumeBoostLevel
    val isAudioOnlyMode: StateFlow<Boolean> = playbackManager.isAudioOnlyMode

    val isIncognitoMode: StateFlow<Boolean> = preferencesManager.isIncognitoMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val preferredQuality: StateFlow<String> = preferencesManager.preferredQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")

    val subtitleFontSize: StateFlow<Float> = preferencesManager.subtitleFontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)

    val subtitleBackgroundOpacity: StateFlow<Float> = preferencesManager.subtitleBackgroundOpacity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.65f)

    val isPlayerGesturesEnabled: StateFlow<Boolean> = preferencesManager.isPlayerGesturesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val chapters: StateFlow<List<VideoChapter>> = uiState.map { state ->
        if (state is PlayerUiState.Success) {
            VideoChapterParser.parseChapters(state.bundle.description)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentQuality = MutableStateFlow<String?>(null)
    val currentQuality: StateFlow<String?> = _currentQuality.asStateFlow()

    val displayQuality: StateFlow<String> = combine(
        preferredQuality,
        currentQuality,
        playbackStats
    ) { preferred, current, stats ->
        if (preferred == "Auto") {
            val res = stats.resolution.split("x").lastOrNull()?.let { "${it}p" }
                ?: current?.filter { it.isDigit() }?.let { "${it}p" }
                ?: ""
            if (res.isNotEmpty()) "Auto ($res)" else "Auto"
        } else {
            preferred
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")

    private val _playbackSpeed = MutableStateFlow(1f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isSpeedBoosted = MutableStateFlow(false)
    val isSpeedBoosted: StateFlow<Boolean> = _isSpeedBoosted.asStateFlow()

    val sponsorSegments: StateFlow<List<com.arslandaim.playtube.domain.model.SponsorSegment>> = playbackManager.sponsorSegmentsFlow

    private val _playbackPitch = MutableStateFlow(1f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    private val _seekAmount = MutableStateFlow(0)
    val seekAmount: StateFlow<Int> = _seekAmount.asStateFlow()

    private val _showSeekFeedback = MutableStateFlow(false)
    val showSeekFeedback: StateFlow<Boolean> = _showSeekFeedback.asStateFlow()

    private val _isSeekForward = MutableStateFlow(true)
    val isSeekForward: StateFlow<Boolean> = _isSeekForward.asStateFlow()

    private val _showStatsForNerds = MutableStateFlow(false)
    val showStatsForNerds: StateFlow<Boolean> = _showStatsForNerds.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private val _comments = MutableStateFlow<List<CommentItem>>(emptyList())
    val comments: StateFlow<List<CommentItem>> = _comments.asStateFlow()

    private val _isFetchingComments = MutableStateFlow(false)
    val isFetchingComments: StateFlow<Boolean> = _isFetchingComments.asStateFlow()

    private val _replies = MutableStateFlow<List<CommentItem>>(emptyList())
    val replies: StateFlow<List<CommentItem>> = _replies.asStateFlow()

    private val _isFetchingReplies = MutableStateFlow(false)
    val isFetchingReplies: StateFlow<Boolean> = _isFetchingReplies.asStateFlow()

    private val _activeReplyParent = MutableStateFlow<CommentItem?>(null)
    val activeReplyParent: StateFlow<CommentItem?> = _activeReplyParent.asStateFlow()

    private var nextCommentsPage: Page? = null
    private var nextRepliesPage: Page? = null

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
    private var nextRelatedPage: Page? = null
    private var isFetchingNextRelatedPage = false
    private var lastSavedPosition = 0L
    private var hasUpdatedInterestsForCurrentVideo = false
    private var isStalledDueToNetwork = false
    private var lastFailedPosition = 0L
    private var lastPauseTimestamp = 0L
    private var retryCount = 0
    private var recoveryAttempts = 0
    private var retryJob: Job? = null
    private var preloadingJob: Job? = null
    private var isPreloaded = false
    private val sessionHistory = mutableListOf<String>()

    init {
        viewModelScope.launch {
            playbackManager.playbackError.collect { handlePlayerError(it) }
        }
        viewModelScope.launch {
            playbackManager.recoveryRequired.collect { pos ->
                lastFailedPosition = pos
                recoverExpiredUrl()
            }
        }
        viewModelScope.launch {
            playbackManager.mediaItemTransition.collect { videoId ->
                if (videoId != null && videoId != currentVideoId) {
                    // Pre-fill UI with basic metadata from player if available to avoid skeleton stall
                    val metadata = player.mediaMetadata
                    if (metadata.title != null) {
                        val placeholderVideo = VideoItem(
                            id = videoId,
                            title = metadata.title.toString(),
                            thumbnailUrl = metadata.artworkUri?.toString() ?: "",
                            uploaderName = metadata.artist?.toString() ?: "",
                            uploaderUrl = null,
                            uploaderThumbnailUrl = null,
                            viewCount = 0,
                            uploadDate = null,
                            rawUploadDate = null,
                            duration = player.duration / 1000,
                            isShort = false
                        )
                        updateUiWithPlaceholder(placeholderVideo)
                        miniPlayerManager.updateMetadata(placeholderVideo)
                    }
                    loadVideoMetadata(videoId)
                }
            }
        }
        viewModelScope.launch {
            playbackManager.playbackEnded.collect {
                saveWatchProgress()
                if (_repeatMode.value == RepeatMode.PLAY_NEXT && !sleepTimerManager.isTimerActive()) {
                    playNext()
                }
            }
        }
        viewModelScope.launch {
            playbackManager.isPlaying.collect { playing ->
                if (playing) {
                    isStalledDueToNetwork = false
                    _isRecovering.value = false
                    recoveryAttempts = 0
                } else {
                    saveWatchProgress()
                }
            }
        }
        viewModelScope.launch {
            playbackManager.currentPosition.collect { pos ->
                val dur = playbackManager.duration.value
                if (!isPreloaded && dur > 0) {
                    val progress = pos.toFloat() / dur
                    val remainingTime = dur - pos
                    if (progress > 0.9f || remainingTime < 60000) {
                        preloadNextVideo()
                    }
                }
                if (abs(pos - lastSavedPosition) >= 5000) {
                    saveWatchProgress()
                }
            }
        }

        // Load preferences
        viewModelScope.launch {
            preferencesManager.isSubtitlesEnabled.collect { enabled ->
                if (_isCcEnabled.value != enabled) {
                    _isCcEnabled.value = enabled
                    playbackManager.updateCcState(enabled, _selectedSubtitleLanguage.value, _selectedSubtitle.value)
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.preferredSubtitleLanguage.collect { lang ->
                val available = (uiState.value as? PlayerUiState.Success)?.bundle?.subtitles ?: emptyList()
                if (available.isEmpty() || lang == null || available.any { it.languageTag.equals(lang, ignoreCase = true) }) {
                    _selectedSubtitleLanguage.value = lang
                    playbackManager.updateCcState(_isCcEnabled.value, lang, _selectedSubtitle.value)
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.isAutoplayEnabled.collect { _isAutoplayEnabled.value = it }
        }
        viewModelScope.launch {
            preferencesManager.repeatMode.collect { savedMode ->
                val mode = try { RepeatMode.valueOf(savedMode) } catch (e: Exception) { RepeatMode.OFF }
                _repeatMode.value = mode
                playbackManager.player.repeatMode = if (mode == RepeatMode.REPEAT_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            }
        }

        // Network recovery
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityObserver.Status.Available && isStalledDueToNetwork) {
                    retryCount = 0
                    retryJob?.cancel()
                    retryPlayback()
                }
            }
        }

        // Remote events
        viewModelScope.launch { queueManager.skipToNextEvent.collect { playNext() } }
        viewModelScope.launch { queueManager.skipToPreviousEvent.collect { playPrevious() } }

        viewModelScope.launch {
            playbackManager.onSponsorSkipped.collect { segment ->
                _snackbarMessage.emit(context.getString(R.string.skipped_segment, segment.category))
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
            val download = withContext(Dispatchers.IO) { downloadRepository.getDownloadByVideoIdResilient(videoId) }
            if (download != null && download.status == DownloadStatus.COMPLETED) {
                val localFile = File(download.filePath)
                if (withContext(Dispatchers.IO) { localFile.exists() }) {
                    val localSubtitles = if (!download.subtitlePath.isNullOrBlank() && withContext(Dispatchers.IO) { File(download.subtitlePath).exists() }) {
                        val subFile = File(download.subtitlePath)
                        val lang = download.subtitleLanguage ?: "en"
                        val format = if (subFile.name.endsWith(".ttml", ignoreCase = true)) "ttml" else "vtt"
                        listOf(
                            SubtitleItem(
                                id = "$lang:offline",
                                url = android.net.Uri.fromFile(subFile).toString(),
                                languageTag = lang,
                                format = format,
                                isAutoGenerated = false,
                                name = "Offline Subtitles"
                            )
                        )
                    } else emptyList()

                    val localBundle = StreamBundle(
                        videoStreams = emptyList(),
                        audioStreams = emptyList(),
                        title = download.title,
                        uploaderName = download.uploaderName,
                        uploaderUrl = null,
                        uploaderThumbnailUrl = null,
                        description = "Playing from local storage",
                        viewCount = 0,
                        uploadDate = null,
                        thumbnailUrl = download.thumbnailUrl,
                        subtitles = localSubtitles
                    )
                    currentBundle = localBundle
                    currentVideoItem = VideoItem(id = videoId, title = download.title, thumbnailUrl = download.thumbnailUrl, uploaderName = download.uploaderName, uploaderUrl = null, uploaderThumbnailUrl = null, viewCount = 0, uploadDate = null, rawUploadDate = null, duration = playbackManager.duration.value / 1000, isShort = false)
                    _uiState.value = PlayerUiState.Success(download.title, download.uploaderName, localBundle)
                    if (localSubtitles.isNotEmpty()) {
                        val activeSub = localSubtitles.first()
                        _selectedSubtitle.value = activeSub
                        _selectedSubtitleLanguage.value = activeSub.languageTag
                        _isCcEnabled.value = true
                        playbackManager.attachLocalSubtitleTrack(activeSub, enableCc = true)
                    }
                    miniPlayerManager.updateMetadata(currentVideoItem)
                    _currentQuality.value = "Local (${download.quality})"
                    updatePlaylistIndex()
                    launch { isSavedUseCase(videoId).collectLatest { _isSaved.value = it } }
                    return@launch
                }
            }

            getVideoStreamsUseCase(videoId)
                .onSuccess { bundle ->
                    if (bundle.isUpcoming) {
                        _uiState.value = PlayerUiState.Upcoming(bundle.title, bundle.uploaderName, bundle.scheduledStartTime, bundle.thumbnailUrl)
                        miniPlayerManager.updateMetadata(VideoItem(id = videoId, title = bundle.title, thumbnailUrl = bundle.thumbnailUrl ?: "", uploaderName = bundle.uploaderName, uploaderUrl = bundle.uploaderUrl, uploaderThumbnailUrl = bundle.uploaderThumbnailUrl, viewCount = -1L, uploadDate = bundle.uploadDate, rawUploadDate = null, duration = 0, isShort = false))
                        return@onSuccess
                    }
                    val activeBundle = resolveInitialSubtitles(videoId, bundle)
                    currentBundle = activeBundle
                    currentVideoItem = VideoItem(id = videoId, title = activeBundle.title, thumbnailUrl = activeBundle.thumbnailUrl ?: "", uploaderName = activeBundle.uploaderName, uploaderUrl = activeBundle.uploaderUrl, uploaderThumbnailUrl = activeBundle.uploaderThumbnailUrl, viewCount = activeBundle.viewCount, uploadDate = activeBundle.uploadDate, rawUploadDate = null, duration = playbackManager.duration.value / 1000, isShort = false)
                    nextRelatedPage = activeBundle.nextRelatedVideosPage
                    _uiState.value = PlayerUiState.Success(activeBundle.title, activeBundle.uploaderName, activeBundle)
                    miniPlayerManager.updateMetadata(currentVideoItem)
                    syncAudioTrack(activeBundle)
                    updatePlaylistIndex()
                    
                    val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl
                    uploaderId?.let { id -> launch { isSubscribedUseCase(id).collectLatest { _isSubscribed.value = it } } }
                    launch { isSavedUseCase(videoId).collectLatest { _isSaved.value = it } }
                }
                .onFailure { _uiState.value = PlayerUiState.Error(PlayTubeError.fromThrowable(it)) }
        }
    }

    fun loadVideo(video: VideoItem, playlistId: String? = null, playlistTitle: String? = null) {
        val videoId = video.id
        if (videoId.isBlank()) return

        playbackPrefetcher.onVideoTapped(videoId)
        
        val isSameVideo = currentVideoId == videoId && playbackManager.player.mediaItemCount > 0
        if (isSameVideo && _uiState.value is PlayerUiState.Success && !(uiState.value as PlayerUiState.Success).bundle.videoStreams.isEmpty()) {
            miniPlayerManager.maximize()
            if (!playbackManager.isPlaying.value && playbackManager.player.playWhenReady) playbackManager.resume()
            return
        }

        // If we are navigating within the same playlist, don't clear the playlist state to avoid UI flicker
        val keepPlaylist = playlistId != null && playlistId == _currentPlaylist.value?.id
        resetPlaybackState(videoId, video, keepPlaylist)
        
        if (playlistId != null && !keepPlaylist) {
            loadPlaylist(playlistId, playlistTitle)
        } else if (keepPlaylist) {
            updatePlaylistIndex()
        }

        if (!isSameVideo) {
            miniPlayerManager.onNewVideoSelected(video)
            playbackManager.stop()
        }
        
        updateUiWithPlaceholder(video)
        
        // Restore Session Metadata (Speed, Pitch, Subtitles)
        viewModelScope.launch {
            var speed = preferencesManager.playbackSpeed.first()
            if (speed == 2.0f) {
                speed = 1.0f
                preferencesManager.setPlaybackSpeed(1.0f)
            }
            val pitch = preferencesManager.playbackPitch.first()
            _playbackSpeed.value = speed
            _playbackPitch.value = pitch
            playbackManager.setPlaybackSpeed(speed)
            playbackManager.setPitch(pitch)

            val subsEnabled = preferencesManager.isSubtitlesEnabled.first()
            val lang = preferencesManager.preferredSubtitleLanguage.first()
            _isCcEnabled.value = subsEnabled
            _selectedSubtitleLanguage.value = lang
            playbackManager.updateCcState(subsEnabled, lang)
        }
        
        viewModelScope.launch {
            if (preferencesManager.isBackgroundPlayEnabled.first()) context.startService(Intent(context, PlaybackService::class.java))
        }
        
        loadingJob = viewModelScope.launch {
            val downloadedVideo = withContext(Dispatchers.IO) { downloadRepository.getDownloadByVideoIdResilient(videoId) }
            if (downloadedVideo != null && downloadedVideo.status == DownloadStatus.COMPLETED) {
                val localFile = File(downloadedVideo.filePath)
                if (withContext(Dispatchers.IO) { localFile.exists() }) {
                    playLocal(videoId, downloadedVideo, localFile, isSameVideo)
                    return@launch
                }
            }

            // Phase 4: Fast-Path Stream Extraction (/player vs /next decoupling)
            getVideoStreamsUseCase(videoId).onSuccess { bundle ->
                com.arslandaim.playtube.utils.InstantPlaybackTracker.onStreamResolved(videoId)
                if (bundle.isUpcoming) {
                    _uiState.value = PlayerUiState.Upcoming(bundle.title, bundle.uploaderName, bundle.scheduledStartTime, bundle.thumbnailUrl)
                    miniPlayerManager.updateMetadata(
                        VideoItem(
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
                            isShort = false
                        )
                    )
                    return@onSuccess
                }

                currentBundle = bundle
                nextRelatedPage = bundle.nextRelatedVideosPage
                _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                syncAudioTrack(bundle)
                
                val preferred = preferredQuality.value
                playbackManager.setAutoQualityEnabled(preferred == "Auto")
                val stream = selectBestMatchingQuality(bundle.videoStreams, preferred)

                val resumePos = if (bundle.isLive) 0L else getResumePosition(videoId)
                stream?.let {
                    if (!isSameVideo || playbackManager.player.playbackState == Player.STATE_IDLE) {
                        playbackManager.play(videoId, bundle, it, resumePos)
                    }
                    _currentQuality.value = it.quality
                }

                // Resolve and attach local sanitized subtitles asynchronously so video playback never waits
                launch {
                    val activeBundle = resolveInitialSubtitles(videoId, bundle)
                    if (currentVideoId == videoId) {
                        currentBundle = activeBundle
                        val currentState = _uiState.value
                        if (currentState is PlayerUiState.Success) {
                            _uiState.value = currentState.copy(bundle = activeBundle)
                        }
                        val sub = _selectedSubtitle.value
                        if (_isCcEnabled.value && sub != null) {
                            playbackManager.attachLocalSubtitleTrack(sub, enableCc = true)
                        }
                    }
                }

                // Parallel non-blocking enrichment tasks
                launch { isFavoriteUseCase(videoId).collectLatest { _isFavorite.value = it } }
                launch { isSavedUseCase(videoId).collectLatest { _isSaved.value = it } }
                launch {
                    getSponsorSegmentsUseCase(videoId).onSuccess { segments ->
                        playbackManager.setSponsorSegments(segments)
                    }
                }
                val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl
                uploaderId?.let { id -> launch { isSubscribedUseCase(id).collectLatest { _isSubscribed.value = it } } }
                launch { 
                    if (preferencesManager.isHistoryEnabled.first()) {
                        addToHistoryUseCase(
                            HistoryEntity(
                                videoId = videoId, 
                                title = bundle.title, 
                                thumbnailUrl = bundle.thumbnailUrl ?: "", 
                                uploaderName = bundle.uploaderName,
                                progressMs = resumePos
                            )
                        ) 
                    } 
                }
            }.onFailure { _uiState.value = PlayerUiState.Error(PlayTubeError.fromThrowable(it)) }
        }
    }

    private fun playLocal(videoId: String, downloadedVideo: DownloadEntity, localFile: File, isSameVideo: Boolean) {
        val localSubtitles = if (!downloadedVideo.subtitlePath.isNullOrBlank() && File(downloadedVideo.subtitlePath).exists()) {
            val subFile = File(downloadedVideo.subtitlePath)
            val lang = downloadedVideo.subtitleLanguage ?: "en"
            val format = if (subFile.name.endsWith(".ttml", ignoreCase = true)) "ttml" else "vtt"
            listOf(
                SubtitleItem(
                    id = "$lang:offline",
                    url = android.net.Uri.fromFile(subFile).toString(),
                    languageTag = lang,
                    format = format,
                    isAutoGenerated = false,
                    name = "Offline Subtitles"
                )
            )
        } else emptyList()

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
            thumbnailUrl = downloadedVideo.thumbnailUrl,
            subtitles = localSubtitles
        )
        currentBundle = localBundle
        _uiState.value = PlayerUiState.Success(downloadedVideo.title, downloadedVideo.uploaderName, localBundle)

        if (localSubtitles.isNotEmpty()) {
            val activeSub = localSubtitles.first()
            _selectedSubtitle.value = activeSub
            _selectedSubtitleLanguage.value = activeSub.languageTag
            _isCcEnabled.value = true
        }

        if (!isSameVideo) {
            playbackManager.playLocal(
                videoId = videoId, 
                file = localFile, 
                title = downloadedVideo.title, 
                uploader = downloadedVideo.uploaderName, 
                thumbnail = downloadedVideo.thumbnailUrl,
                bundle = localBundle
            )
            viewModelScope.launch { playbackManager.player.seekTo(getResumePosition(videoId)) }
        }
        _currentQuality.value = "Local (${downloadedVideo.quality})"

        // Populate downloaded videos playlist stack if no active online playlist
        viewModelScope.launch {
            val completedDownloads = withContext(Dispatchers.IO) {
                downloadRepository.getAllDownloads().first().filter { it.status == DownloadStatus.COMPLETED }
            }
            if (completedDownloads.isNotEmpty() && (_currentPlaylist.value == null || _currentPlaylist.value?.id == "downloads")) {
                val downloadedVideos = completedDownloads.map { it.toVideoItem() }
                val downloadedPlaylist = PlaylistDetails(
                    id = "downloads",
                    title = context.getString(R.string.downloaded_videos),
                    uploaderName = context.getString(R.string.offline_downloads),
                    uploaderUrl = null,
                    thumbnailUrl = downloadedVideo.thumbnailUrl,
                    videos = downloadedVideos
                )
                _currentPlaylist.value = downloadedPlaylist
                updatePlaylistIndex()
            }
        }
    }

    private fun resetPlaybackState(videoId: String, video: VideoItem, keepPlaylist: Boolean = false) {
        _isFavorite.value = false
        _isSaved.value = false
        _isSubscribed.value = false
        _currentQuality.value = null
        _selectedAudioStream.value = null
        _selectedSubtitle.value = null
        _isCcEnabled.value = false
        _comments.value = emptyList()
        nextCommentsPage = null
        
        if (!keepPlaylist) {
            _currentPlaylist.value = null
            _playlistIndex.value = -1
        }

        playbackManager.setSponsorSegments(emptyList())
        lastPauseTimestamp = 0L
        if (!sessionHistory.contains(videoId)) {
            sessionHistory.add(videoId)
            if (sessionHistory.size > 10) sessionHistory.removeAt(0)
        }
        loadingJob?.cancel()
        currentVideoId = videoId
        currentVideoItem = video
        nextRelatedPage = null
        lastSavedPosition = 0L
        hasUpdatedInterestsForCurrentVideo = false
        isStalledDueToNetwork = false
        _isRecovering.value = false
        isPreloaded = false
        preloadingJob?.cancel()
        retryCount = 0
        recoveryAttempts = 0
        retryJob?.cancel()
    }

    private fun updateUiWithPlaceholder(video: VideoItem) {
        val placeholderBundle = StreamBundle(videoStreams = emptyList(), audioStreams = emptyList(), title = video.title, uploaderName = video.uploaderName, uploaderUrl = video.uploaderUrl, uploaderThumbnailUrl = video.uploaderThumbnailUrl, description = null, viewCount = video.viewCount, uploadDate = video.uploadDate, thumbnailUrl = video.thumbnailUrl)
        currentBundle = placeholderBundle
        _uiState.value = PlayerUiState.Success(video.title, video.uploaderName, placeholderBundle)
    }

    private suspend fun getResumePosition(videoId: String): Long {
        val item = libraryRepository.getHistory().first().find { it.videoId == videoId }
        val dur = item?.durationMs ?: 0L

        val historyProgress = if (item != null && dur > 0 && item.progressMs < (dur * 0.90) && (dur - item.progressMs) > 5000L) {
            item.progressMs
        } else {
            0L
        }

        val lastSessionId = preferencesManager.lastPlayedVideoId.first()
        if (lastSessionId == videoId) {
            val lastPos = preferencesManager.lastPlayedPosition.first()
            if (lastPos > 0) {
                if (dur > 0 && (lastPos >= (dur * 0.90) || (dur - lastPos) <= 5000L)) {
                    return 0L
                }
                return lastPos
            }
        }
        return historyProgress
    }

    fun toggleFavorite(video: VideoItem? = null) {
        val target = video ?: currentVideoItem ?: return
        viewModelScope.launch {
            val isFav = libraryRepository.isFavorite(target.id).first()
            toggleFavoriteUseCase(FavoriteEntity(videoId = target.id, title = target.title, thumbnailUrl = target.thumbnailUrl, uploaderName = target.uploaderName))
            _snackbarMessage.emit(if (isFav) context.getString(R.string.removed_from_favorites) else context.getString(R.string.added_to_favorites))
        }
    }

    fun toggleSubscription() {
        val bundle = currentBundle ?: return
        val uploaderId = VideoUtils.extractChannelId(bundle.uploaderUrl) ?: bundle.uploaderUrl ?: return
        viewModelScope.launch { toggleSubscriptionUseCase(SubscriptionEntity(channelId = uploaderId, name = bundle.uploaderName, thumbnailUrl = bundle.uploaderThumbnailUrl, subscriberCount = bundle.uploaderSubscriberCount)) }
    }

    private fun handlePlayerError(error: PlaybackException) {
        if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            playbackManager.player.seekToDefaultPosition()
            playbackManager.player.prepare()
            playbackManager.resume()
            return
        }
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS && (error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException)?.responseCode == 403) {
            lastFailedPosition = playbackManager.player.currentPosition
            recoverExpiredUrl()
            return
        }
        if (isNetworkError(error)) {
            lastFailedPosition = playbackManager.player.currentPosition
            isStalledDueToNetwork = playbackManager.player.playWhenReady
            _isRecovering.value = isStalledDueToNetwork
            if (_uiState.value !is PlayerUiState.Success) _uiState.value = PlayerUiState.Error(PlayTubeError.Network)
            else viewModelScope.launch { _snackbarMessage.emit(context.getString(R.string.connection_lost_waiting)) }
            scheduleRetry()
        } else {
            _uiState.value = PlayerUiState.Error(PlayTubeError.fromThrowable(error))
        }
    }

    private fun isNetworkError(error: PlaybackException) = when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT, PlaybackException.ERROR_CODE_IO_UNSPECIFIED, PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED, PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> true
        else -> error.cause is java.net.UnknownHostException || error.cause is java.net.ConnectException || error.cause is java.net.SocketTimeoutException
    }

    private fun scheduleRetry() {
        retryJob?.cancel()
        if (retryCount >= 5) return
        val delayMs = (Math.pow(2.0, retryCount.toDouble()) * 1000).toLong()
        retryCount++
        retryJob = viewModelScope.launch {
            delay(delayMs)
            if (isStalledDueToNetwork) retryPlayback()
        }
    }

    private fun retryPlayback() {
        checkAndSwitchToLocalIfAvailable()
        playbackManager.player.seekTo(lastFailedPosition)
        playbackManager.player.prepare()
        playbackManager.resume()
    }

    private fun checkAndSwitchToLocalIfAvailable() {
        val videoId = currentVideoId ?: return
        viewModelScope.launch {
            val download = withContext(Dispatchers.IO) { downloadRepository.getDownloadByVideoIdResilient(videoId) }
            if (download != null && download.status == DownloadStatus.COMPLETED) {
                val localFile = File(download.filePath)
                if (withContext(Dispatchers.IO) { localFile.exists() }) {
                    val metadata = MediaMetadata.Builder().setTitle(download.title).setArtist(download.uploaderName).setArtworkUri(download.thumbnailUrl.let { android.net.Uri.parse(it) }).build()
                    playbackManager.player.setMediaItem(androidx.media3.common.MediaItem.Builder().setUri(android.net.Uri.fromFile(localFile)).setMediaId(videoId).setMediaMetadata(metadata).build())
                    _currentQuality.value = "Local (${download.quality})"
                }
            }
        }
    }

    private fun recoverExpiredUrl() {
        val videoId = currentVideoId ?: return
        if (recoveryAttempts >= 2) {
            _isRecovering.value = false
            _uiState.value = PlayerUiState.Error(PlayTubeError.Unknown("Stream playback failed after multiple recovery attempts"))
            return
        }
        recoveryAttempts++
        val previousQuality = _currentQuality.value
        _isRecovering.value = true
        viewModelScope.launch {
            // Check if it finished downloading while paused
            val download = withContext(Dispatchers.IO) { downloadRepository.getDownloadByVideoIdResilient(videoId) }
            if (download != null && download.status == DownloadStatus.COMPLETED) {
                val localFile = File(download.filePath)
                if (withContext(Dispatchers.IO) { localFile.exists() }) {
                    playLocal(videoId, download, localFile, true)
                    _isRecovering.value = false
                    return@launch
                }
            }

            getVideoStreamsUseCase(videoId, forceRefresh = true).onSuccess { bundle ->
                currentBundle = bundle
                _uiState.value = PlayerUiState.Success(bundle.title, bundle.uploaderName, bundle)
                syncAudioTrack(bundle)
                
                val preferred = preferredQuality.value
                val primaryStream = selectBestMatchingQuality(bundle.videoStreams, preferred)
                val stream = if (recoveryAttempts > 1 && previousQuality != null && bundle.videoStreams.size > 1) {
                    bundle.videoStreams.firstOrNull { it.quality != previousQuality } ?: primaryStream
                } else {
                    primaryStream
                }

                if (stream != null) {
                    // Phase 3: Seamless Hot-Swap Recovery
                    playbackManager.hotSwapSource(videoId, bundle, stream, lastFailedPosition)
                    _currentQuality.value = stream.quality
                    isStalledDueToNetwork = false
                    _isRecovering.value = false
                } else {
                    _isRecovering.value = false
                    _uiState.value = PlayerUiState.Error(PlayTubeError.Unknown("No playable streams found"))
                }

                launch {
                    val activeBundle = resolveInitialSubtitles(videoId, bundle)
                    if (currentVideoId == videoId) {
                        currentBundle = activeBundle
                        val sub = _selectedSubtitle.value
                        if (_isCcEnabled.value && sub != null) {
                            playbackManager.attachLocalSubtitleTrack(sub, enableCc = true)
                        }
                    }
                }
            }.onFailure {
                _uiState.value = PlayerUiState.Error(PlayTubeError.Unknown("Failed to recover stream"))
                _isRecovering.value = false
            }
        }
    }

    private fun preloadNextVideo() {
        // Condition prevents preloading of next video when Autoplay is set to off.
        if (_repeatMode.value == RepeatMode.OFF || !_isAutoplayEnabled.value) return

        val next = getNextAutoplayVideo() ?: return
        isPreloaded = true
        preloadingJob = viewModelScope.launch(Dispatchers.IO) {
            val download = downloadRepository.getDownloadByVideoId(next.id)
            if (download != null && download.status == DownloadStatus.COMPLETED) {
                val localFile = File(download.filePath)
                if (localFile.exists()) {
                    withContext(Dispatchers.Main) {
                        if (_isAutoplayEnabled.value) playbackManager.prepareNextLocalSource(next, localFile)
                    }
                    return@launch
                }
            }

            videoRepository.preloadStreamBundle(next.id)
            getVideoStreamsUseCase(next.id).onSuccess { bundle -> withContext(Dispatchers.Main) { if (_isAutoplayEnabled.value) playbackManager.prepareNextSource(next, bundle) } }
        }
    }

    private fun getNextAutoplayVideo(): VideoItem? {
        val playlist = _currentPlaylist.value
        val index = _playlistIndex.value
        if (playlist != null && index != -1 && index < playlist.videos.size - 1) {
            return playlist.videos[index + 1]
        }

        val related = currentBundle?.relatedVideos ?: return null
        return related.find { it.id !in sessionHistory } ?: related.firstOrNull()
    }

    private fun saveWatchProgress() {
        val videoId = currentVideoId ?: return
        val pos = playbackManager.currentPosition.value
        val dur = playbackManager.duration.value
        val bundle = currentBundle
        if (bundle?.isLive == true || dur <= 0 || dur == C.TIME_UNSET) return
        lastSavedPosition = pos
        val ratio = (pos.toFloat() / dur).coerceIn(0f, 1f)
        val shouldRecordInterests = !hasUpdatedInterestsForCurrentVideo && (pos >= 10_000L || ratio >= 0.15f)
        if (shouldRecordInterests) {
            hasUpdatedInterestsForCurrentVideo = true
        }
        externalScope.launch {
            val isLocal = currentQuality.value?.contains("Local") == true
            preferencesManager.setLastPlayedSession(videoId, pos, isLocal)
            
            if (preferencesManager.isHistoryEnabled.first()) {
                updateWatchProgressUseCase(videoId, pos, dur)
                if (shouldRecordInterests && bundle != null) {
                    updateUserInterestsUseCase(bundle.title, 0.5f, ratio.coerceAtLeast(0.5f))
                    updateUserInterestsUseCase(bundle.uploaderName, 1.0f, ratio.coerceAtLeast(0.5f))
                }
            }
        }
    }

    fun togglePlayPause() {
        if (playbackManager.isPlaying.value) {
            playbackManager.pause()
        } else {
            playbackManager.resume()
        }
    }
    fun seekTo(pos: Long) { playbackManager.seekTo(pos); saveWatchProgress() }
    fun setPlaybackSpeed(speed: Float) { 
        _playbackSpeed.value = speed
        playbackManager.setPlaybackSpeed(speed)
        viewModelScope.launch { preferencesManager.setPlaybackSpeed(speed) }
    }
    fun setTemporarySpeedBoost(active: Boolean) {
        _isSpeedBoosted.value = active
        if (active) {
            playbackManager.setPlaybackSpeed(2.0f)
        } else {
            playbackManager.setPlaybackSpeed(_playbackSpeed.value)
        }
    }
    fun setPlaybackPitch(pitch: Float) { 
        _playbackPitch.value = pitch
        playbackManager.setPitch(pitch)
        viewModelScope.launch { preferencesManager.setPlaybackPitch(pitch) }
    }
    fun setVolumeBoostLevel(level: VolumeBoostLevel) {
        playbackManager.setVolumeBoostLevel(level)
    }
    fun setAudioOnlyMode(enabled: Boolean) {
        playbackManager.setAudioOnlyMode(enabled)
    }
    fun setQuality(stream: StreamItem?) {
        val videoId = currentVideoId ?: return
        val bundle = currentBundle ?: return

        viewModelScope.launch {
            if (stream == null) {
                // User selected "Auto"
                playbackManager.setAutoQualityEnabled(true)
                preferencesManager.setPreferredQuality("Auto")
                val autoStream = selectAutoQuality(bundle.videoStreams)
                autoStream?.let { 
                    playbackManager.switchQualitySeamlessly(videoId, bundle, it)
                    _currentQuality.value = it.quality
                }
            } else {
                playbackManager.setAutoQualityEnabled(false)
                preferencesManager.setPreferredQuality(stream.quality)
                playbackManager.switchQualitySeamlessly(videoId, bundle, stream)
                _currentQuality.value = stream.quality
            }
        }
    }

    fun toggleStatsForNerds() {
        _showStatsForNerds.value = !_showStatsForNerds.value
    }

    private fun selectAutoQuality(streams: List<StreamItem>): StreamItem? {
        if (streams.isEmpty()) return null

        val estimate = playbackManager.getBandwidthEstimate()
        val bufferedDuration = playbackManager.player.bufferedPosition - playbackManager.player.currentPosition

        val sortedStreams = streams.sortedBy {
            it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
        }

        val thresholds = Constants.QualityThresholds

        // If buffer is very low (less than 5s), stick to a lower quality even if bandwidth is high
        val maxAllowedQuality = if (bufferedDuration < 5000) thresholds.P480 else Long.MAX_VALUE

        return when {
            estimate >= thresholds.P2160 && thresholds.P2160 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("2160") || it.quality.contains("4K") }
            estimate >= thresholds.P1440 && thresholds.P1440 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("1440") || it.quality.contains("2K") }
            estimate >= thresholds.P1080 && thresholds.P1080 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("1080") }
            estimate >= thresholds.P720 && thresholds.P720 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("720") }
            estimate >= thresholds.P480 && thresholds.P480 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("480") }
            estimate >= thresholds.P360 && thresholds.P360 <= maxAllowedQuality -> sortedStreams.findLast { it.quality.contains("360") }
            else -> sortedStreams.firstOrNull()
        } ?: sortedStreams.findLast { it.quality.contains("480") } ?: sortedStreams.firstOrNull()
    }

    private fun selectBestMatchingQuality(streams: List<StreamItem>, preferred: String): StreamItem? {
        if (streams.isEmpty()) return null
        if (preferred == "Auto") return selectAutoQuality(streams)

        val targetHeight = preferred.filter { it.isDigit() }.toIntOrNull()
            ?: return selectAutoQuality(streams)

        val streamsWithHeight = streams.map { stream ->
            val height = stream.quality.filter { it.isDigit() }.toIntOrNull() ?: 0
            stream to height
        }

        // 1. Try exact height match (e.g. preferred "1080p", stream has "1080p60")
        val exactMatch = streamsWithHeight.find { it.second == targetHeight }
        if (exactMatch != null) return exactMatch.first

        // 2. Highest resolution <= targetHeight (e.g. preferred 1080p, max video quality 720p)
        val highestLowerOrEqual = streamsWithHeight
            .filter { it.second in 1..targetHeight }
            .maxByOrNull { it.second }
        if (highestLowerOrEqual != null) return highestLowerOrEqual.first

        // 3. Lowest resolution > targetHeight if all available streams exceed targetHeight
        val lowestHigher = streamsWithHeight
            .filter { it.second > targetHeight }
            .minByOrNull { it.second }
        if (lowestHigher != null) return lowestHigher.first

        return selectAutoQuality(streams)
    }

    private fun updateBundleWithSubtitle(preparedSub: SubtitleItem) {
        val bundle = currentBundle ?: return
        val existingSubtitles = bundle.subtitles.toMutableList()
        val index = if (preparedSub.id.isNotBlank()) {
            existingSubtitles.indexOfFirst { it.id == preparedSub.id }.takeIf { it != -1 }
                ?: existingSubtitles.indexOfFirst { it.languageTag.equals(preparedSub.languageTag, ignoreCase = true) }
        } else {
            existingSubtitles.indexOfFirst { it.languageTag.equals(preparedSub.languageTag, ignoreCase = true) }
        }
        if (index != -1) {
            existingSubtitles[index] = preparedSub
        } else {
            existingSubtitles.add(preparedSub)
        }
        val updatedBundle = bundle.copy(subtitles = existingSubtitles)
        currentBundle = updatedBundle
        val state = _uiState.value
        if (state is PlayerUiState.Success) {
            _uiState.value = state.copy(bundle = updatedBundle)
        }
    }

    private suspend fun prepareLocalSubtitleTrack(videoId: String, sub: SubtitleItem): SubtitleItem = withContext(Dispatchers.IO) {
        if (sub.url.startsWith("file:") || sub.url.startsWith("content:")) return@withContext sub

        val lang = sub.languageTag
        val safeId = sub.id.ifBlank { lang }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val cacheFile = File(context.cacheDir, "sub_v2_${videoId}_${safeId}.vtt")

        if (cacheFile.exists() && cacheFile.length() > 15) {
            return@withContext sub.copy(url = android.net.Uri.fromFile(cacheFile).toString(), format = "vtt")
        }

        val (normalizedUrl, _) = CaptionTrackResolver.normalizeSubtitleUrl(sub.url, sub.format)
        val candidateUrls = listOf(normalizedUrl, sub.url).filter { it.isNotBlank() }.distinct()
        val visitorData = poTokenProvider.getVisitorData()

        for (targetUrl in candidateUrls) {
            try {
                val requestBuilder = okhttp3.Request.Builder()
                    .url(targetUrl)
                    .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                    .header("Accept-Language", "en-US,en;q=0.9")

                if (targetUrl.contains("youtube.com") || targetUrl.contains("googlevideo.com")) {
                    requestBuilder.header("Cookie", Constants.YouTube.CONSENT_COOKIE)
                    if (visitorData.isNotBlank()) {
                        requestBuilder.header("X-Goog-Visitor-Id", visitorData)
                    }
                }

                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        val rawText = response.body.string()
                        val cleanWebVtt = if (rawText.isNotBlank()) {
                            SubtitleSanitizer.sanitizeWebVtt(rawText)
                        } else ""
                        if (cleanWebVtt.length > 15) {
                            cacheFile.writeText(cleanWebVtt, Charsets.UTF_8)
                            PTLog.d("PlayerViewModel", "Successfully fetched and sanitized subtitle track for $videoId ($lang)")
                            return@withContext sub.copy(url = android.net.Uri.fromFile(cacheFile).toString(), format = "vtt")
                        } else {
                            PTLog.w("PlayerViewModel", "Subtitle response had no parseable cues from $targetUrl for $videoId ($lang)")
                        }
                    } else {
                        PTLog.w("PlayerViewModel", "Subtitle HTTP request failed with code ${response.code} for $videoId ($lang)")
                    }
                }
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error fetching subtitle track from $targetUrl for $videoId ($lang)", e)
            }
        }

        sub
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _isCcEnabled.value = enabled
        if (!enabled) {
            playbackManager.updateCcState(false, _selectedSubtitleLanguage.value, _selectedSubtitle.value)
            viewModelScope.launch { preferencesManager.setSubtitlesEnabled(false) }
            return
        }

        viewModelScope.launch {
            preferencesManager.setSubtitlesEnabled(true)
            val available = (uiState.value as? PlayerUiState.Success)?.bundle?.subtitles
                ?: currentBundle?.subtitles
                ?: emptyList()
            if (available.isEmpty()) {
                playbackManager.updateCcState(false, _selectedSubtitleLanguage.value, null)
                return@launch
            }

            val preferredLang = _selectedSubtitleLanguage.value ?: preferencesManager.preferredSubtitleLanguage.first()
            val targetSub = _selectedSubtitle.value?.takeIf { sel ->
                available.any { it.id == sel.id || it.languageTag.equals(sel.languageTag, ignoreCase = true) }
            } ?: selectSubtitleTrack(available, preferredLang)

            if (targetSub != null) {
                _selectedSubtitle.value = targetSub
                _selectedSubtitleLanguage.value = targetSub.languageTag
                playbackManager.updateCcState(true, targetSub.languageTag, targetSub)

                val currentVid = currentVideoId ?: "temp"
                val preparedSub = prepareLocalSubtitleTrack(currentVid, targetSub)
                if (_isCcEnabled.value && currentVideoId == currentVid) {
                    _selectedSubtitle.value = preparedSub
                    updateBundleWithSubtitle(preparedSub)
                    playbackManager.attachLocalSubtitleTrack(preparedSub, enableCc = true)
                }
            } else {
                playbackManager.updateCcState(true, preferredLang, null)
            }
        }
    }

    fun setSubtitleLanguage(lang: String?) { 
        viewModelScope.launch { 
            if (lang == null) {
                _selectedSubtitle.value = null
                setSubtitlesEnabled(false)
            } else { 
                val available = (uiState.value as? PlayerUiState.Success)?.bundle?.subtitles
                    ?: currentBundle?.subtitles
                    ?: emptyList()
                val matching = available.find { it.id.equals(lang, ignoreCase = true) }
                    ?: available.find { it.languageTag.equals(lang, ignoreCase = true) && !it.isAutoGenerated }
                    ?: available.find { it.languageTag.equals(lang, ignoreCase = true) }
                    ?: available.find { it.languageTag.startsWith(lang, ignoreCase = true) }

                if (matching != null) {
                    _isCcEnabled.value = true
                    _selectedSubtitle.value = matching
                    _selectedSubtitleLanguage.value = matching.languageTag
                    playbackManager.updateCcState(true, matching.languageTag, matching)
                    preferencesManager.setSubtitlesEnabled(true)
                    preferencesManager.setPreferredSubtitleLanguage(matching.languageTag)

                    val currentVid = currentVideoId ?: "temp"
                    val preparedSub = prepareLocalSubtitleTrack(currentVid, matching)
                    if (currentVideoId == currentVid) {
                        _selectedSubtitle.value = preparedSub
                        updateBundleWithSubtitle(preparedSub)
                        playbackManager.attachLocalSubtitleTrack(preparedSub, enableCc = _isCcEnabled.value)
                    }
                } else {
                    _selectedSubtitleLanguage.value = lang
                    _isCcEnabled.value = false
                    playbackManager.updateCcState(false, lang, null)
                    preferencesManager.setSubtitlesEnabled(false)
                    preferencesManager.setPreferredSubtitleLanguage(lang)
                }
            } 
        } 
    }
    fun setAudioTrack(stream: StreamItem) {
        val videoId = currentVideoId ?: return
        val bundle = currentBundle ?: return
        _selectedAudioStream.value = stream
        playbackManager.switchAudioTrack(videoId, bundle, stream)
    }
    fun setAutoplayEnabled(enabled: Boolean) { _isAutoplayEnabled.value = enabled; viewModelScope.launch { preferencesManager.setAutoplayEnabled(enabled) } }
    
    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
        playbackManager.player.repeatMode = if (mode == RepeatMode.REPEAT_ONE) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        viewModelScope.launch { preferencesManager.setRepeatMode(mode.name) }

        // If repeat mode is off, video in the queue will not be played.
        // Clears out preloaded next item.
        if (mode == RepeatMode.OFF && _currentPlaylist.value == null) {
            while (playbackManager.player.mediaItemCount > 1) {
                playbackManager.player.removeMediaItem(1)
            }
        }

    }

    fun cycleRepeatMode() {
        val nextMode = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.PLAY_NEXT
            RepeatMode.PLAY_NEXT -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }
    
    private var pendingSeekTargetMs: Long? = null

    fun performSeek(forward: Boolean) {
        seekJob?.cancel()
        val currentPos = playbackManager.player.currentPosition
        if (_isSeekForward.value != forward || !_showSeekFeedback.value) {
            _seekAmount.value = 10
            pendingSeekTargetMs = currentPos
        } else {
            _seekAmount.value += 10
        }
        _isSeekForward.value = forward
        _showSeekFeedback.value = true

        val basePos = pendingSeekTargetMs ?: currentPos
        val targetPos = (basePos + if (forward) 10000L else -10000L).coerceAtLeast(0L)
        pendingSeekTargetMs = targetPos

        seekJob = viewModelScope.launch {
            delay(400)
            pendingSeekTargetMs?.let { pos ->
                playbackManager.seekTo(pos)
                saveWatchProgress()
            }
            _showSeekFeedback.value = false
            _seekAmount.value = 0
            pendingSeekTargetMs = null
        }
    }
    private var seekJob: Job? = null
    fun seekForward() = performSeek(true)
    fun seekBackward() = performSeek(false)
    fun toggleSubtitles() = setSubtitlesEnabled(!_isCcEnabled.value)

    val abLoopState: StateFlow<ABLoopState> = playbackManager.abLoopState

    fun setABLoopPointA() {
        playbackManager.setABLoopPointA(playbackManager.player.currentPosition)
    }

    fun setABLoopPointB() {
        playbackManager.setABLoopPointB(playbackManager.player.currentPosition)
    }

    fun clearABLoop() {
        playbackManager.clearABLoop()
    }

    private fun syncAudioTrack(bundle: StreamBundle) {
        val defaultAudio = bundle.audioStreams.firstOrNull { it.url == bundle.bestAudioStreamUrl }
            ?: bundle.audioStreams.firstOrNull()
        _selectedAudioStream.value = defaultAudio
    }

    private suspend fun resolveInitialSubtitles(videoId: String, bundle: StreamBundle): StreamBundle {
        val available = bundle.subtitles
        if (available.isEmpty()) {
            _selectedSubtitle.value = null
            _selectedSubtitleLanguage.value = null
            _isCcEnabled.value = false
            playbackManager.updateCcState(false, null, null)
            return bundle
        }

        val preferredLang = preferencesManager.preferredSubtitleLanguage.first()
        val isEnabled = preferencesManager.isSubtitlesEnabled.first()
        val matchedSubtitle = selectSubtitleTrack(available, preferredLang)

        if (matchedSubtitle != null && isEnabled) {
            _selectedSubtitle.value = matchedSubtitle
            _selectedSubtitleLanguage.value = matchedSubtitle.languageTag
            _isCcEnabled.value = true
            playbackManager.updateCcState(true, matchedSubtitle.languageTag, matchedSubtitle)

            val preparedSub = withTimeoutOrNull(1200L) {
                prepareLocalSubtitleTrack(videoId, matchedSubtitle)
            }

            if (preparedSub != null) {
                _selectedSubtitle.value = preparedSub
                playbackManager.updateCcState(true, preparedSub.languageTag, preparedSub)
                val updatedSubs = available.map {
                    if (it.id == matchedSubtitle.id && it.languageTag == matchedSubtitle.languageTag) preparedSub else it
                }
                return bundle.copy(subtitles = updatedSubs)
            } else {
                viewModelScope.launch {
                    val asyncPrepared = prepareLocalSubtitleTrack(videoId, matchedSubtitle)
                    if (currentVideoId == videoId && _isCcEnabled.value) {
                        _selectedSubtitle.value = asyncPrepared
                        updateBundleWithSubtitle(asyncPrepared)
                        playbackManager.attachLocalSubtitleTrack(asyncPrepared, enableCc = true)
                    }
                }
                return bundle
            }
        } else {
            _selectedSubtitle.value = matchedSubtitle
            _selectedSubtitleLanguage.value = matchedSubtitle?.languageTag ?: preferredLang
            _isCcEnabled.value = false
            playbackManager.updateCcState(
                enabled = false,
                preferredLang = _selectedSubtitleLanguage.value,
                selectedSubtitle = matchedSubtitle
            )
            return bundle
        }
    }

    fun shareVideo() {
        val videoId = currentVideoId ?: return
        val url = "https://www.youtube.com/watch?v=$videoId"
        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "${currentBundle?.title}\n\n$url"); type = "text/plain" }
        context.startActivity(Intent.createChooser(sendIntent, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }

    fun minimize() {
        if (miniPlayerManager.isMinimized.value) return
        val bundle = currentBundle ?: return
        miniPlayerManager.minimize(VideoItem(id = currentVideoId ?: "", title = bundle.title, thumbnailUrl = bundle.thumbnailUrl ?: "", uploaderName = bundle.uploaderName, uploaderUrl = bundle.uploaderUrl, viewCount = bundle.viewCount ?: 0, uploadDate = bundle.uploadDate, rawUploadDate = null, duration = playbackManager.duration.value / 1000, watchProgress = if (playbackManager.duration.value > 0) playbackManager.currentPosition.value.toFloat() / playbackManager.duration.value else null, isShort = false))
    }

    private fun loadPlaylist(playlistId: String, title: String? = null) {
        val playlistUrl = if (playlistId.startsWith("http")) playlistId else "https://www.youtube.com/playlist?list=$playlistId"
        
        viewModelScope.launch {
            if (playlistId.startsWith("local:")) {
                val id = playlistId.substringAfter("local:").toIntOrNull()
                if (id != null) {
                    libraryRepository.getLocalPlaylists().firstOrNull()?.find { it.id == id }?.let { playlist ->
                        libraryRepository.getVideosForLocalPlaylist(id).firstOrNull()?.let { videos ->
                            val details = PlaylistDetails(
                                id = "local:$id",
                                title = playlist.name,
                                uploaderName = "Local Playlist",
                                uploaderUrl = null,
                                thumbnailUrl = playlist.thumbnailUrl ?: videos.firstOrNull()?.thumbnailUrl ?: "",
                                videos = videos.map { it.toVideoItem() }
                            )
                            _currentPlaylist.value = details
                            updatePlaylistIndex()
                        }
                    }
                }
                return@launch
            }

            getPlaylistDetailsUseCase(playlistUrl)
                .onSuccess { details ->
                    _currentPlaylist.value = if (title != null) details.copy(title = title) else details
                    updatePlaylistIndex()
                }
        }
    }

    private fun LocalPlaylistVideoEntity.toVideoItem() = VideoItem(
        id = videoId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = null,
        uploaderThumbnailUrl = null,
        viewCount = 0,
        uploadDate = null,
        rawUploadDate = null,
        duration = duration,
        isShort = false
    )

    private fun updatePlaylistIndex() {
        val currentId = currentVideoId ?: return
        val playlist = _currentPlaylist.value ?: return
        val index = playlist.videos.indexOfFirst { it.id == currentId }
        _playlistIndex.value = index
    }

    fun playNext() {
        val playlist = _currentPlaylist.value
        val index = _playlistIndex.value
        
        if (playlist != null && index != -1) {
            if (index < playlist.videos.size - 1) {
                loadVideo(playlist.videos[index + 1], playlist.id, playlist.title)
            } else if (_repeatMode.value == RepeatMode.PLAY_NEXT && playlist.videos.isNotEmpty()) {
                loadVideo(playlist.videos[0], playlist.id, playlist.title)
            }
        } else if (playbackManager.player.hasNextMediaItem()) {
            playbackManager.player.seekToNextMediaItem()
        } else {
            getNextAutoplayVideo()?.let { loadVideo(it) }
        }
    }

    fun playPrevious() {
        val playlist = _currentPlaylist.value
        val index = _playlistIndex.value

        if (playbackManager.player.currentPosition > 5000) {
            playbackManager.seekTo(0)
        } else if (playlist != null && index != -1 && index > 0) {
            loadVideo(playlist.videos[index - 1], playlist.id, playlist.title)
        } else {
            _snackbarMessage.tryEmit(context.getString(R.string.no_previous_video))
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
                    val combinedRelated = (currentState.bundle.relatedVideos + result.items)
                        .filter { it.id.isNotBlank() && it.id != currentId }
                        .distinctBy { it.id }
                    val existingIds = currentState.bundle.relatedVideos.map { it.id }.toSet()
                    val nextUnseenSeed = result.items.firstOrNull { it.id !in existingIds }?.id
                    nextRelatedPage = nextUnseenSeed?.let {
                        Page(Constants.YouTube.VIDEO_URL_PREFIX + it, it)
                    } ?: result.nextPage
                    val updatedBundle = currentState.bundle.copy(
                        relatedVideos = combinedRelated,
                        nextRelatedVideosPage = nextRelatedPage
                    )
                    currentBundle = updatedBundle
                    _uiState.value = PlayerUiState.Success(currentState.title, currentState.uploader, updatedBundle)
                }
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error fetching next related page", e)
            }
            isFetchingNextRelatedPage = false
        }
    }

    fun stopPlayback() {
        saveWatchProgress()
        loadingJob?.cancel()
        retryJob?.cancel()
        preloadingJob?.cancel()
        playbackManager.stop()
        currentVideoId = null
        currentBundle = null
        isStalledDueToNetwork = false
        _isRecovering.value = false
        _uiState.value = PlayerUiState.Loading
        _comments.value = emptyList()
        nextCommentsPage = null
    }

    fun loadComments() {
        val videoId = currentVideoId ?: return
        if (_isFetchingComments.value) return

        viewModelScope.launch {
            _isFetchingComments.value = true
            try {
                val result = videoRepository.getComments(videoId)
                _comments.value = result.items
                nextCommentsPage = result.nextPage
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error loading comments", e)
            } finally {
                _isFetchingComments.value = false
            }
        }
    }

    fun loadNextCommentsPage() {
        val videoId = currentVideoId ?: return
        val page = nextCommentsPage ?: return
        if (_isFetchingComments.value) return

        viewModelScope.launch {
            _isFetchingComments.value = true
            try {
                val result = videoRepository.fetchNextCommentsPage(videoId, page)
                _comments.value = _comments.value + result.items
                nextCommentsPage = result.nextPage
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error loading next comments page", e)
            } finally {
                _isFetchingComments.value = false
            }
        }
    }

    fun loadReplies(parent: CommentItem) {
        val videoId = currentVideoId ?: return
        if (_isFetchingReplies.value) return

        viewModelScope.launch {
            _activeReplyParent.value = parent
            _replies.value = emptyList()
            nextRepliesPage = null
            _isFetchingReplies.value = true
            try {
                val result = videoRepository.getCommentReplies(videoId, parent)
                _replies.value = result.items
                nextRepliesPage = result.nextPage
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error loading replies", e)
            } finally {
                _isFetchingReplies.value = false
            }
        }
    }

    fun loadNextRepliesPage() {
        val videoId = currentVideoId ?: return
        val parent = _activeReplyParent.value ?: return
        val page = nextRepliesPage ?: return
        if (_isFetchingReplies.value) return

        viewModelScope.launch {
            _isFetchingReplies.value = true
            try {
                val result = videoRepository.fetchNextCommentRepliesPage(videoId, parent.commentId, page)
                _replies.value = _replies.value + result.items
                nextRepliesPage = result.nextPage
            } catch (e: Exception) {
                PTLog.e("PlayerViewModel", "Error loading next replies page", e)
            } finally {
                _isFetchingReplies.value = false
            }
        }
    }

    fun closeReplies() {
        _activeReplyParent.value = null
        _replies.value = emptyList()
        nextRepliesPage = null
    }

    fun prepareDownload(video: VideoItem? = null) {
        val target = video ?: currentVideoItem ?: return
        if (target.id == currentVideoId && currentBundle != null && !currentBundle!!.videoStreams.isEmpty()) {
            _downloadState.value = DownloadDialogState.ShowDialog(target, currentBundle!!)
            return
        }
        viewModelScope.launch {
            val cached = videoRepository.getCachedStreamBundle(target.id)
            if (cached != null && !cached.videoStreams.isEmpty()) {
                _downloadState.value = DownloadDialogState.ShowDialog(target, cached)
                return@launch
            }
            _downloadState.value = DownloadDialogState.Loading(target)
            getVideoStreamsUseCase(target.id).onSuccess { _downloadState.value = DownloadDialogState.ShowDialog(target, it) }.onFailure { _downloadState.value = DownloadDialogState.Idle }
        }
    }

    private fun selectSubtitleTrack(subtitles: List<SubtitleItem>, preferredLang: String?): SubtitleItem? {
        if (subtitles.isEmpty()) return null
        if (!preferredLang.isNullOrBlank()) {
            val match = subtitles.find { 
                it.languageTag.equals(preferredLang, ignoreCase = true) || 
                it.id == preferredLang ||
                it.languageTag.startsWith(preferredLang, ignoreCase = true) 
            }
            if (match != null) return match
        }
        return subtitles.find { it.languageTag.startsWith("en", ignoreCase = true) }
            ?: subtitles.find { !it.isAutoGenerated }
            ?: subtitles.firstOrNull()
    }

    fun download(video: VideoItem, bundle: StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean, downloadSubtitles: Boolean = false) {
        viewModelScope.launch {
            val audioUrl = if (isAdaptive) {
                val isWebm = format?.contains("webm", ignoreCase = true) == true
                val compatible = bundle.audioStreams.filter { if (isWebm) it.format.contains("webm", ignoreCase = true) || it.format.contains("opus", ignoreCase = true) else it.format.contains("m4a", ignoreCase = true) || it.format.contains("aac", ignoreCase = true) }
                (compatible.filter { it.trackType == "ORIGINAL" }.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 } ?: compatible.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 })?.url
            } else null

            val selectedSubtitle = if (downloadSubtitles) {
                selectSubtitleTrack(bundle.subtitles, selectedSubtitleLanguage.value ?: preferencesManager.preferredSubtitleLanguage.first())
            } else null

            downloadVideoUseCase(
                videoId = video.id,
                url = url,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                uploaderName = video.uploaderName,
                quality = quality,
                format = format,
                audioUrl = audioUrl,
                subtitleUrl = selectedSubtitle?.url,
                subtitleLanguage = selectedSubtitle?.languageTag
            )
            _snackbarMessage.emit(context.getString(R.string.downloading_started))
            _downloadState.value = DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() { _downloadState.value = DownloadDialogState.Idle }

    override fun onCleared() {
        saveWatchProgress()
        loadingJob?.cancel()
        retryJob?.cancel()
        preloadingJob?.cancel()
        _replies.value = emptyList()
        _activeReplyParent.value = null
        super.onCleared()
    }
}

enum class RepeatMode {
    OFF,
    REPEAT_ONE,
    PLAY_NEXT
}

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Success(val title: String, val uploader: String, val bundle: StreamBundle) : PlayerUiState
    data class Upcoming(val title: String, val uploader: String, val scheduledTime: String?, val thumbnailUrl: String?) : PlayerUiState
    data class Error(val error: PlayTubeError) : PlayerUiState
}
