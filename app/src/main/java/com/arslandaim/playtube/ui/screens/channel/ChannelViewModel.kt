/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.usecase.GetChannelDetailsUseCase
import com.arslandaim.playtube.domain.usecase.IsSubscribedUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getChannelDetailsUseCase: GetChannelDetailsUseCase,
    private val isSubscribedUseCase: IsSubscribedUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val libraryRepository: LibraryRepository,
    private val videoRepository: com.arslandaim.playtube.domain.repository.VideoRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<ChannelUiState>(ChannelUiState.Loading)
    
    val uiState: StateFlow<ChannelUiState> = combine(
        _internalUiState,
        libraryRepository.getHistory()
    ) { state, history ->
        if (state is ChannelUiState.Success) {
            val historyMap = history.associateBy({ it.videoId }, { if (it.durationMs > 0) it.progressMs.toFloat() / it.durationMs else null })
            val updatedVideos = state.details.videos.map { it.copy(watchProgress = historyMap[it.id]) }
            ChannelUiState.Success(state.details.copy(videos = updatedVideos))
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChannelUiState.Loading)

    private val _isSubscribed = MutableStateFlow<Boolean?>(null)
    val isSubscribed: StateFlow<Boolean?> = _isSubscribed.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private var currentDetails: ChannelDetails? = null
    private var currentChannelUrl: String? = null
    private var nextPage: Page? = null
    private var isFetchingNextPage = false

    private var subscriptionJob: Job? = null

    fun loadChannel(channelUrl: String) {
        if (currentChannelUrl == channelUrl && _internalUiState.value is ChannelUiState.Success) return
        currentChannelUrl = channelUrl
        nextPage = null

        viewModelScope.launch {
            _internalUiState.value = ChannelUiState.Loading
            _isSubscribed.value = null // Reset for new channel
            
            // 1. Try immediate ID extraction from URL
            val immediateId = VideoUtils.extractChannelId(channelUrl)
            if (immediateId != null) {
                observeSubscription(immediateId)
            }
            
            getChannelDetailsUseCase(channelUrl)
                .onSuccess { details ->
                    currentDetails = details
                    nextPage = details.nextVideosPage
                    _internalUiState.value = ChannelUiState.Success(details)
                    
                    // 2. Observe using the REAL canonical channel ID (handles cases where URL was a name/@handle)
                    if (details.id != immediateId) {
                        observeSubscription(details.id)
                    }
                }
                .onFailure { exception ->
                    _internalUiState.value = ChannelUiState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    fun loadNextPage() {
        val url = currentChannelUrl
        val page = nextPage
        if (isFetchingNextPage || page == null || url == null) return

        isFetchingNextPage = true
        viewModelScope.launch {
            try {
                val result = videoRepository.fetchNextChannelVideosPage(url, page)
                val currentState = _internalUiState.value
                if (currentState is ChannelUiState.Success) {
                    nextPage = result.nextPage
                    val updatedDetails = currentState.details.copy(
                        videos = currentState.details.videos + result.items,
                        nextVideosPage = result.nextPage
                    )
                    currentDetails = updatedDetails
                    _internalUiState.value = ChannelUiState.Success(updatedDetails)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isFetchingNextPage = false
        }
    }

    private fun observeSubscription(channelId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            isSubscribedUseCase(channelId).collectLatest {
                _isSubscribed.value = it
            }
        }
    }

    fun toggleSubscription() {
        val details = currentDetails ?: return
        viewModelScope.launch {
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = details.id,
                    name = details.name,
                    thumbnailUrl = details.avatarUrl,
                    subscriberCount = details.subscriberCount
                )
            )
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            val isFavorite = libraryRepository.isFavorite(video.id).first()
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName
                )
            )
            _snackbarMessage.emit(if (isFavorite) "Removed from Favorites" else "Added to Favorites")
        }
    }

    fun prepareDownload(video: VideoItem) {
        viewModelScope.launch {
            _downloadState.value = DownloadDialogState.Loading(video)
            getVideoStreamsUseCase(video.id)
                .onSuccess { bundle ->
                    _downloadState.value = DownloadDialogState.ShowDialog(video, bundle)
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

                compatibleStreams.filter { it.trackType == "ORIGINAL" }
                    .maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?.url ?: compatibleStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }?.url
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
}

sealed interface ChannelUiState {
    object Loading : ChannelUiState
    data class Success(val details: ChannelDetails) : ChannelUiState
    data class Error(val message: String) : ChannelUiState
}
