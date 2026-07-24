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
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
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
    private val searchRepository: SearchRepository,
    private val libraryRepository: LibraryRepository,
    private val videoRepository: VideoRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase
) : ViewModel() {

    private val _internalState = MutableStateFlow(HomeState())

    val uiState: StateFlow<HomeState> = combine(
        _internalState,
        libraryRepository.getHistory()
    ) { state, history ->
        val historyMap = history.associateBy({ it.videoId }, { if (it.durationMs > 0) it.progressMs.toFloat() / it.durationMs else null })
        
        state.copy(
            trendingVideos = state.trendingVideos.map { it.copy(watchProgress = historyMap[it.id]) },
            subscriptionVideos = state.subscriptionVideos.map { it.copy(watchProgress = historyMap[it.id]) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

    private val _selectedTab = MutableStateFlow(0) // 0: For You, 1: Subscriptions
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // Download Dialog States
    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private val categoryCache = mutableMapOf<String, List<VideoItem>>()
    private val nextPageCache = mutableMapOf<String, Page?>()
    private var trendingFetchJob: Job? = null
    private var isFetchingNextPage = false

    init {
        loadTrending()
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
        if (index == 0 && _internalState.value.trendingVideos.isEmpty()) {
            loadTrending()
        } else if (index == 1 && _internalState.value.subscriptionVideos.isEmpty()) {
            loadSubscriptionsFeed()
        }
    }

    fun onCategorySelected(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        
        // Instant UI update if cached
        categoryCache[category]?.let { cachedVideos ->
            _internalState.update { 
                it.copy(
                    trendingVideos = cachedVideos,
                    nextTrendingPage = nextPageCache[category],
                    isTrendingLoading = false,
                    isPersonalized = category == "All" && it.isPersonalized,
                    error = null
                )
            }
            return 
        }

        loadTrending()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            if (_selectedTab.value == 0) {
                // Clear cache on manual refresh to get fresh data
                categoryCache.clear()
                nextPageCache.clear()
                fetchTrending(isRefresh = true)
            } else {
                fetchSubscriptionsFeed()
            }
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
            val history = searchHistoryDao.getAllSearchHistory().first()
            val category = _selectedCategory.value
            
            // For "All", we use a mix but keep a primary topic for pagination tokens if possible
            // Here we pick "trending" or the latest history topic as the pagination driver
            val primaryTopic = if (category == "All") {
                if (history.isNotEmpty()) history.first().query else "trending"
            } else category

            val trendingVideosResult = searchRepository.search(primaryTopic)
            val trendingItems = trendingVideosResult.items.filterIsInstance<SearchItem.Video>().map { it.video }
            
            val finalVideos = if (category == "All" && history.size > 1) {
                // Supplement with other topics if it's the personalized "All" tab
                coroutineScope {
                    val otherTopics = history.drop(1).take(2).map { it.query } + listOf("music", "gaming")
                    val deferredResults = otherTopics.distinct().map { topic ->
                        async { 
                            try {
                                searchRepository.search(topic).items
                                    .filterIsInstance<SearchItem.Video>()
                                    .map { it.video }
                                    .take(10)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    val extra = deferredResults.awaitAll().flatten()
                    (trendingItems + extra).distinctBy { it.id }.shuffled()
                }
            } else {
                trendingItems
            }
            
            // Update cache
            categoryCache[category] = finalVideos
            nextPageCache[category] = trendingVideosResult.nextPage

            _internalState.update { 
                it.copy(
                    trendingVideos = finalVideos,
                    nextTrendingPage = trendingVideosResult.nextPage,
                    isPersonalized = isRefresh && category == "All" && history.isNotEmpty()
                )
            }
            
        } catch (e: Exception) {
            val errorMessage = if (e is java.net.UnknownHostException || e is java.io.IOException) {
                "No internet connection"
            } else {
                e.message ?: "Unknown error"
            }
            _internalState.update { it.copy(error = errorMessage) }
        }
    }

    fun loadNextTrendingPage() {
        val category = _selectedCategory.value
        val page = _internalState.value.nextTrendingPage
        if (isFetchingNextPage || page == null) return

        isFetchingNextPage = true
        viewModelScope.launch {
            try {
                val history = if (category == "All") searchHistoryDao.getAllSearchHistory().first() else emptyList()
                val primaryTopic = if (category == "All") {
                    if (history.isNotEmpty()) history.first().query else "trending"
                } else category

                val result = searchRepository.fetchNextPage(primaryTopic, com.arslandaim.playtube.domain.model.SearchSort.RELEVANCE, page)
                val newVideos = result.items.filterIsInstance<SearchItem.Video>().map { it.video }
                val updatedVideos = _internalState.value.trendingVideos + newVideos
                categoryCache[category] = updatedVideos
                nextPageCache[category] = result.nextPage
                
                _internalState.update { 
                    it.copy(
                        trendingVideos = updatedVideos,
                        nextTrendingPage = result.nextPage
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isFetchingNextPage = false
        }
    }

    fun loadSubscriptionsFeed() {
        viewModelScope.launch {
            _internalState.update { it.copy(isSubscriptionsLoading = true, error = null) }
            fetchSubscriptionsFeed()
            _internalState.update { it.copy(isSubscriptionsLoading = false) }
        }
    }

    private suspend fun fetchSubscriptionsFeed() {
        try {
            val subscriptions = libraryRepository.getSubscriptions().first()
            if (subscriptions.isEmpty()) {
                _internalState.update { it.copy(subscriptionVideos = emptyList()) }
                return
            }

            val allVideos = mutableListOf<VideoItem>()
            // Increase fetch count to 25 channels for a richer feed
            coroutineScope {
                val deferredVideos = subscriptions.take(25).map { sub ->
                    async {
                        try {
                            videoRepository.getChannelDetails(sub.channelId).videos
                        } catch (e: Exception) {
                            emptyList<VideoItem>()
                        }
                    }
                }
                allVideos.addAll(deferredVideos.awaitAll().flatten())
            }
            
            // Sort by rawUploadDate (newest first)
            val sortedVideos = allVideos
                .distinctBy { it.id }
                .sortedByDescending { it.rawUploadDate ?: 0L }
                .take(60)

            _internalState.update { it.copy(subscriptionVideos = sortedVideos) }
        } catch (e: Exception) {
            val errorMessage = if (e is java.net.UnknownHostException || e is java.io.IOException) {
                "No internet connection"
            } else {
                e.message ?: "Unknown error"
            }
            _internalState.update { it.copy(error = errorMessage) }
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
}

data class HomeState(
    val trendingVideos: List<VideoItem> = emptyList(),
    val nextTrendingPage: Page? = null,
    val subscriptionVideos: List<VideoItem> = emptyList(),
    val isTrendingLoading: Boolean = false,
    val isSubscriptionsLoading: Boolean = false,
    val isPersonalized: Boolean = false,
    val error: String? = null
)
