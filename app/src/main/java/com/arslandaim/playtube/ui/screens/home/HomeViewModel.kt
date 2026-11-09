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
import com.arslandaim.playtube.domain.model.SubtitleItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.data.local.BlacklistType
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.AiContentDetector
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.VideoUtils
import com.arslandaim.playtube.utils.HistoryUtils.applyHistory
import com.arslandaim.playtube.utils.HistoryUtils.getContinuePlaying
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val searchRepository: SearchRepository,
    private val preferencesManager: PreferencesManager,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val videoRepository: VideoRepository,
    private val getRecommendationsUseCase: com.arslandaim.playtube.domain.usecase.GetRecommendationsUseCase,
    private val markNotInterestedUseCase: com.arslandaim.playtube.domain.usecase.MarkNotInterestedUseCase,
    private val getTrendingVideosUseCase: com.arslandaim.playtube.domain.usecase.GetTrendingVideosUseCase,
    private val playbackPrefetcher: com.arslandaim.playtube.data.network.PlaybackPrefetcher
) : ViewModel() {

    private val _internalState = MutableStateFlow(HomeState())

    val uiState: StateFlow<HomeState> = combine(
        _internalState,
        libraryRepository.getHistory()
    ) { state, history ->
        val continuePlaying = getContinuePlaying(history)
        if (continuePlaying.isNotEmpty()) {
            playbackPrefetcher.prefetchViewport(continuePlaying.map { it.id })
        }
        state.copy(
            trendingVideos = state.trendingVideos.applyHistory(history),
            continuePlayingVideos = continuePlaying
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

    private fun getCategorySearchQuery(category: String): String {
        return when (category.lowercase()) {
            "music" -> "Trending Music"
            "gaming" -> "Gaming"
            "tech" -> "Technology News Reviews"
            "news" -> "Top News Headlines"
            "learning" -> "Education Learning Tutorials"
            else -> category
        }
    }

    private fun getCategoryCacheKey(category: String): String {
        return when {
            category.equals("All", ignoreCase = true) -> "home_all"
            category.equals("Trending", ignoreCase = true) -> "home_trending_only"
            else -> "home_category_${category.lowercase()}"
        }
    }

    init {
        // Phase 1: Load cached feed immediately if "All" is selected
        viewModelScope.launch {
            libraryRepository.getCachedFeed("home_all").collect { cache ->
                val currentCategory = _internalState.value.selectedCategory
                if (cache != null && _internalState.value.trendingVideos.isEmpty() && currentCategory.equals("All", ignoreCase = true)) {
                    val cleanCached = cache.videos.filter { !VideoUtils.isShort(it) }
                    _internalState.update { it.copy(trendingVideos = cleanCached, isTrendingLoading = false) }
                    playbackPrefetcher.prefetchViewport(cleanCached.map { it.id })
                }
            }
        }
        loadTrending()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val category = _internalState.value.selectedCategory
            when {
                category.equals("All", ignoreCase = true) -> fetchTrending(isRefresh = true)
                category.equals("Trending", ignoreCase = true) -> fetchPureTrending()
                else -> fetchCategoryVideos(category)
            }
            _isRefreshing.value = false
        }
    }

    fun selectCategory(category: String) {
        if (_internalState.value.selectedCategory.equals(category, ignoreCase = true) && !_internalState.value.isTrendingLoading && _internalState.value.trendingVideos.isNotEmpty()) return

        trendingFetchJob?.cancel()
        trendingFetchJob = viewModelScope.launch {
            val cacheKey = getCategoryCacheKey(category)
            val cachedFeed = libraryRepository.getCachedFeed(cacheKey).firstOrNull()
            val cachedVideos = cachedFeed?.videos ?: emptyList()

            _internalState.update { 
                it.copy(
                    selectedCategory = category,
                    isTrendingLoading = cachedVideos.isEmpty(),
                    trendingVideos = cachedVideos,
                    error = null
                )
            }

            if (cachedVideos.isNotEmpty()) {
                playbackPrefetcher.prefetchViewport(cachedVideos.map { it.id })
            }

            try {
                when {
                    category.equals("All", ignoreCase = true) -> fetchTrending(isRefresh = false)
                    category.equals("Trending", ignoreCase = true) -> fetchPureTrending()
                    else -> fetchCategoryVideos(category)
                }
                if (coroutineContext.isActive) {
                    _internalState.update { it.copy(isTrendingLoading = false) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e), isTrendingLoading = false) }
                }
            }
        }
    }

    private suspend fun fetchPureTrending() {
        try {
            val trendingResult = getTrendingVideosUseCase().getOrNull()
            val trendingVideos = trendingResult?.items ?: emptyList()

            val blacklistedIds = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
            val isBlockAiEnabled = preferencesManager.isBlockAiContentEnabled.first()

            val cleanVideos = trendingVideos.filter { video ->
                val isBlacklisted = video.id in blacklistedIds || 
                                    video.uploaderName in blacklistedIds || 
                                    (video.uploaderUrl != null && video.uploaderUrl in blacklistedIds)
                val isAi = isBlockAiEnabled && AiContentDetector.isAiContent(video)
                val isShort = VideoUtils.isShort(video)
                !isBlacklisted && !isAi && !isShort
            }

            _internalState.update { 
                it.copy(
                    trendingVideos = cleanVideos,
                    nextTrendingPage = trendingResult?.nextPage,
                    isPersonalized = false
                )
            }

            playbackPrefetcher.prefetchViewport(cleanVideos.map { it.id })
            libraryRepository.updateCachedFeed(getCategoryCacheKey("Trending"), cleanVideos)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
        }
    }

    private suspend fun fetchCategoryVideos(category: String) {
        try {
            val query = getCategorySearchQuery(category)
            val searchResult = searchRepository.search(query)
            val categoryVideos = searchResult.items
                .filterIsInstance<SearchItem.Video>()
                .map { it.video }

            val blacklistedIds = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
            val isBlockAiEnabled = preferencesManager.isBlockAiContentEnabled.first()

            val cleanVideos = categoryVideos.filter { video ->
                val isBlacklisted = video.id in blacklistedIds || 
                                    video.uploaderName in blacklistedIds || 
                                    (video.uploaderUrl != null && video.uploaderUrl in blacklistedIds)
                val isAi = isBlockAiEnabled && AiContentDetector.isAiContent(video)
                val isShort = VideoUtils.isShort(video)
                !isBlacklisted && !isAi && !isShort
            }

            _internalState.update { 
                it.copy(
                    trendingVideos = cleanVideos,
                    nextTrendingPage = searchResult.nextPage
                )
            }

            // Phase 3: Proactive Top-Items Viewport Prefetching for Category
            playbackPrefetcher.prefetchViewport(cleanVideos.map { it.id })

            // Phase 1: Silent Cache Update for Category
            libraryRepository.updateCachedFeed(getCategoryCacheKey(category), cleanVideos)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
        }
    }

    fun loadTrending() {
        trendingFetchJob?.cancel()
        trendingFetchJob = viewModelScope.launch {
            _internalState.update { it.copy(isTrendingLoading = true, error = null) }
            try {
                fetchTrending(isRefresh = false)
                if (coroutineContext.isActive) {
                    _internalState.update { it.copy(isTrendingLoading = false) }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (coroutineContext.isActive) {
                    _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e), isTrendingLoading = false) }
                }
            }
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

            val blacklistedIds = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
            val isBlockAiEnabled = preferencesManager.isBlockAiContentEnabled.first()

            val cleanVideos = combinedVideos.filter { video ->
                val isBlacklisted = video.id in blacklistedIds || 
                                    video.uploaderName in blacklistedIds || 
                                    (video.uploaderUrl != null && video.uploaderUrl in blacklistedIds)
                val isAi = isBlockAiEnabled && AiContentDetector.isAiContent(video)
                val isShort = VideoUtils.isShort(video)
                !isBlacklisted && !isAi && !isShort
            }

            _internalState.update { 
                it.copy(
                    trendingVideos = cleanVideos,
                    nextTrendingPage = trendingResult?.nextPage,
                    isPersonalized = isRefresh && recommendations.isNotEmpty()
                )
            }

            // Phase 3: Proactive Top-Items Viewport Prefetching
            playbackPrefetcher.prefetchViewport(cleanVideos.map { it.id })

            // Phase 1: Silent Cache Update
            libraryRepository.updateCachedFeed(getCategoryCacheKey("All"), cleanVideos)
            
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            _internalState.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
        }
    }

    fun loadNextTrendingPage() {
        val category = _internalState.value.selectedCategory
        val isAll = category.equals("All", ignoreCase = true)
        val isTrending = category.equals("Trending", ignoreCase = true)
        val isAllOrTrending = isAll || isTrending
        val page = _internalState.value.nextTrendingPage
        if (_internalState.value.isLoadingMore) return

        viewModelScope.launch {
            _internalState.update { it.copy(isLoadingMore = true) }
            try {
                if (isAllOrTrending) {
                    if (page != null) {
                        getTrendingVideosUseCase.fetchNextPage(page)
                            .onSuccess { result ->
                                val blacklistedIds = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
                                val isBlockAiEnabled = preferencesManager.isBlockAiContentEnabled.first()
                                val cleanItems = result.items.filter { video ->
                                    val isBlacklisted = video.id in blacklistedIds || 
                                                        video.uploaderName in blacklistedIds || 
                                                        (video.uploaderUrl != null && video.uploaderUrl in blacklistedIds)
                                    val isAi = isBlockAiEnabled && AiContentDetector.isAiContent(video)
                                    val isShort = VideoUtils.isShort(video)
                                    !isBlacklisted && !isAi && !isShort
                                }
                                _internalState.update { state ->
                                    state.copy(
                                        trendingVideos = (state.trendingVideos + cleanItems).distinctBy { it.id },
                                        nextTrendingPage = result.nextPage,
                                        isLoadingMore = false
                                    )
                                }
                                playbackPrefetcher.prefetchViewport(cleanItems.map { it.id })
                                // Phase 1: Silent Cache Update
                                libraryRepository.updateCachedFeed(getCategoryCacheKey(category), _internalState.value.trendingVideos)
                            }
                            .onFailure {
                                _internalState.update { it.copy(isLoadingMore = false) }
                            }
                    } else if (isAll) {
                        // Fallback to recommendations only on "All" tab if no trending page is available
                        val result = getRecommendationsUseCase()
                        val newVideos = result.getOrDefault(emptyList())
                        
                        _internalState.update { state ->
                            state.copy(
                                trendingVideos = (state.trendingVideos + newVideos).distinctBy { it.id },
                                isLoadingMore = false
                            )
                        }
                        playbackPrefetcher.prefetchViewport(newVideos.map { it.id })
                    } else {
                        _internalState.update { it.copy(isLoadingMore = false) }
                    }
                } else {
                    // Category pagination
                    if (page != null) {
                        val query = getCategorySearchQuery(category)
                        val searchResult = searchRepository.fetchNextPage(query = query, sort = SearchSort.RELEVANCE, page = page)
                        val categoryVideos = searchResult.items
                            .filterIsInstance<SearchItem.Video>()
                            .map { it.video }

                        val blacklistedIds = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
                        val isBlockAiEnabled = preferencesManager.isBlockAiContentEnabled.first()

                        val cleanItems = categoryVideos.filter { video ->
                            val isBlacklisted = video.id in blacklistedIds || 
                                                video.uploaderName in blacklistedIds || 
                                                (video.uploaderUrl != null && video.uploaderUrl in blacklistedIds)
                            val isAi = isBlockAiEnabled && AiContentDetector.isAiContent(video)
                            val isShort = VideoUtils.isShort(video)
                            !isBlacklisted && !isAi && !isShort
                        }

                        _internalState.update { state ->
                            state.copy(
                                trendingVideos = (state.trendingVideos + cleanItems).distinctBy { it.id },
                                nextTrendingPage = searchResult.nextPage,
                                isLoadingMore = false
                            )
                        }
                        playbackPrefetcher.prefetchViewport(cleanItems.map { it.id })
                        libraryRepository.updateCachedFeed(getCategoryCacheKey(category), _internalState.value.trendingVideos)
                    } else {
                        _internalState.update { it.copy(isLoadingMore = false) }
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
            _snackbarMessage.emit(if (isFavorite) "Removed from Liked Videos" else "Added to Liked Videos")
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

    private fun selectSubtitleTrack(subtitles: List<SubtitleItem>, preferredLang: String?): SubtitleItem? {
        if (subtitles.isEmpty()) return null
        if (!preferredLang.isNullOrBlank()) {
            val match = subtitles.find { 
                it.languageTag.equals(preferredLang, ignoreCase = true) || 
                it.languageTag.startsWith(preferredLang, ignoreCase = true) 
            }
            if (match != null) return match
        }
        return subtitles.find { it.languageTag.startsWith("en", ignoreCase = true) }
            ?: subtitles.find { !it.isAutoGenerated }
            ?: subtitles.first()
    }

    fun download(video: VideoItem, bundle: StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean, downloadSubtitles: Boolean = false) {
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

            val preferredLang = preferencesManager.preferredSubtitleLanguage.first()
            val selectedSubtitle = if (downloadSubtitles) selectSubtitleTrack(bundle.subtitles, preferredLang) else null

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

    fun blockChannel(video: VideoItem) {
        viewModelScope.launch {
            val channelKey = video.uploaderUrl ?: video.uploaderName
            libraryRepository.addToBlacklist(channelKey, BlacklistType.CHANNEL)
            if (video.uploaderName != channelKey) {
                libraryRepository.addToBlacklist(video.uploaderName, BlacklistType.CHANNEL)
            }
            _snackbarMessage.emit("Channel '${video.uploaderName}' blocked")

            _internalState.update { state ->
                state.copy(
                    trendingVideos = state.trendingVideos.filter { 
                        it.uploaderName != video.uploaderName && it.uploaderUrl != video.uploaderUrl 
                    }
                )
            }
        }
    }
}

data class HomeState(
    val selectedCategory: String = "All",
    val trendingVideos: List<VideoItem> = emptyList(),
    val continuePlayingVideos: List<VideoItem> = emptyList(),
    val nextTrendingPage: org.schabi.newpipe.extractor.Page? = null,
    val isTrendingLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isPersonalized: Boolean = false,
    val error: PlayTubeError? = null
)
