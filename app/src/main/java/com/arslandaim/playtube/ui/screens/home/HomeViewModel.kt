/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.SearchHistoryDao
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.HistoryUtils.applyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val videoRepository: VideoRepository,
    private val getRecommendationsUseCase: com.arslandaim.playtube.domain.usecase.GetRecommendationsUseCase,
    private val markNotInterestedUseCase: com.arslandaim.playtube.domain.usecase.MarkNotInterestedUseCase,
    private val getTrendingVideosUseCase: com.arslandaim.playtube.domain.usecase.GetTrendingVideosUseCase
) : ViewModel() {

    private val _internalState = MutableStateFlow(HomeState())

    val uiState: StateFlow<HomeState> = combine(
        _internalState,
        libraryRepository.getHistory()
    ) { state, history ->
        state.copy(
            trendingVideos = state.trendingVideos.applyHistory(history)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Download Dialog States
    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private var trendingFetchJob: Job? = null

    init {
        loadTrending()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchTrending(isRefresh = true)
            _isRefreshing.value = false
        }
    }

    fun loadTrending() {
        trendingFetchJob?.cancel()
        trendingFetchJob = viewModelScope.launch {
            _internalState.update { it.copy(isTrendingLoading = true, error = null) }
            fetchTrending(isRefresh = false)
            _internalState.update { it.copy(isTrendingLoading = false) }
        }
    }

    private suspend fun fetchTrending(isRefresh: Boolean) {
        try {
            // First, try to get personalized recommendations
            val recommendations = getRecommendationsUseCase(forceRefresh = isRefresh).getOrDefault(emptyList())
            
            // Second, fetch actual trending from YouTube with pagination support
            val trendingResult = getTrendingVideosUseCase().getOrNull()
            val trendingVideos = trendingResult?.items ?: emptyList()
            
            // Strict deduplication: Remove trending videos that are already in recommendations
            // This ensures the "Personalized" notification feels truly unique.
            val filteredTrending = trendingVideos.filter { tv -> recommendations.none { it.id == tv.id } }

            val combinedVideos = if (recommendations.isNotEmpty()) {
                (recommendations + filteredTrending).distinctBy { it.id }
            } else {
                trendingVideos
            }

            _internalState.update { 
                it.copy(
                    trendingVideos = combinedVideos,
                    nextTrendingPage = trendingResult?.nextPage,
                    isPersonalized = isRefresh && recommendations.isNotEmpty()
                )
            }
            
        } catch (e: Exception) {
            _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
        }
    }

    fun loadNextTrendingPage() {
        val page = _internalState.value.nextTrendingPage
        if (_internalState.value.isLoadingMore) return

        viewModelScope.launch {
            _internalState.update { it.copy(isLoadingMore = true) }
            try {
                if (page != null) {
                    getTrendingVideosUseCase.fetchNextPage(page)
                        .onSuccess { result ->
                            _internalState.update { state ->
                                state.copy(
                                    trendingVideos = (state.trendingVideos + result.items).distinctBy { it.id },
                                    nextTrendingPage = result.nextPage,
                                    isLoadingMore = false
                                )
                            }
                        }
                        .onFailure {
                            _internalState.update { it.copy(isLoadingMore = false) }
                        }
                } else {
                    // Fallback to recommendations if no trending page is available
                    val result = getRecommendationsUseCase()
                    val newVideos = result.getOrDefault(emptyList())
                    
                    _internalState.update { state ->
                        state.copy(
                            trendingVideos = (state.trendingVideos + newVideos).distinctBy { it.id },
                            isLoadingMore = false
                        )
                    }
                }
            } catch (e: Exception) {
                _internalState.update { it.copy(isLoadingMore = false) }
            }
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
            // Optimistic Cache Check
            val cachedBundle = videoRepository.getCachedStreamBundle(video.id)
            if (cachedBundle != null && !cachedBundle.videoStreams.isEmpty()) {
                _downloadState.value = DownloadDialogState.ShowDialog(video, cachedBundle)
                return@launch
            }

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

    fun downloadPlaylist(playlistTitle: String, videos: List<VideoItem>) {
        viewModelScope.launch {
            videos.forEach { video ->
                prepareDownload(video)
                // We need a way to automatically select quality for bulk download
                // For now, this just opens the dialog for each video (not ideal)
            }
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadDialogState.Idle
    }

    fun onPersonalizedNotifyShown() {
        _internalState.update { it.copy(isPersonalized = false) }
    }

    fun markNotInterested(video: VideoItem) {
        viewModelScope.launch {
            markNotInterestedUseCase(video)
            _snackbarMessage.emit("Video hidden from recommendations")
            
            // Immediately remove from current UI state to feel responsive
            _internalState.update { state ->
                state.copy(
                    trendingVideos = state.trendingVideos.filter { it.id != video.id }
                )
            }
        }
    }
}

data class HomeState(
    val trendingVideos: List<VideoItem> = emptyList(),
    val nextTrendingPage: org.schabi.newpipe.extractor.Page? = null,
    val isTrendingLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isPersonalized: Boolean = false,
    val error: PlayTubeError? = null
)
