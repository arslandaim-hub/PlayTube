/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.data.local.SearchHistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.SearchVideosUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.VideoUtils
import com.arslandaim.playtube.utils.HistoryUtils.applyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchVideosUseCase: SearchVideosUseCase,
    private val searchRepository: SearchRepository,
    private val libraryRepository: LibraryRepository,
    private val preferencesManager: PreferencesManager,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val videoRepository: com.arslandaim.playtube.domain.repository.VideoRepository
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSort = MutableStateFlow(SearchSort.RELEVANCE)
    val searchSort: StateFlow<SearchSort> = _searchSort.asStateFlow()

    private val _isSortingNewest = MutableStateFlow(false)
    val isSortingNewest: StateFlow<Boolean> = _isSortingNewest.asStateFlow()

    val isGridView: StateFlow<Boolean> = preferencesManager.isSearchGridView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val isFetchingNextPage = MutableStateFlow(false)

    val uiState: StateFlow<SearchUiState> = combine(
        _internalUiState,
        _searchSort,
        libraryRepository.getHistory(),
        libraryRepository.getSubscriptions(),
        isFetchingNextPage
    ) { state, sort, history, subs, isLoadingMore ->
        if (state is SearchUiState.Success) {
            // Step 1: Extract videos and apply history
            val videos = state.items.filterIsInstance<SearchItem.Video>().map { it.video }.applyHistory(history)
            val videoMap = videos.associateBy { it.id }

            // Step 2: Reconstruct items with updated progress and subscription status
            val updatedItems = state.items
                .distinctBy { it.uniqueKey }
                .map { item ->
                    when (item) {
                        is SearchItem.Video -> {
                            SearchItem.Video(videoMap[item.video.id] ?: item.video)
                        }
                        is SearchItem.Channel -> {
                            val channelId = VideoUtils.extractChannelId(item.id) ?: item.id
                            val isSubscribed = subs.any { it.channelId.contains(channelId) || channelId.contains(it.channelId) }
                            item.copy(isSubscribed = isSubscribed)
                        }
                        is SearchItem.Playlist -> item
                    }
                }

            // Step 2: Apply Local Sorting Fallback with Partitioning
            // We separate videos from "static" items (Channels, Playlists) to ensure
            // unsortable items stay pinned to the top rather than being buried.
            val sortedItems = if (sort == SearchSort.RELEVANCE) {
                updatedItems // Use original mixed extractor order
            } else {
                val (videos, staticItems) = updatedItems.partition { it is SearchItem.Video }

                val sortedVideos = when (sort) {
                    SearchSort.UPLOAD_DATE -> {
                        // Strict chronological sort matching subscription feed.
                        // We use withIndex to preserve original server order for items with identical timestamps
                        // (common when parsing textual dates like "2 hours ago").
                        videos.withIndex().sortedWith(
                            compareByDescending<IndexedValue<SearchItem>> {
                                (it.value as SearchItem.Video).video.rawUploadDate ?: 0L
                            }.thenBy { it.index }
                        ).map { it.value }
                    }
                    SearchSort.VIEW_COUNT -> {
                        videos.sortedByDescending { (it as SearchItem.Video).video.viewCount }
                    }
                    SearchSort.RATING -> {
                        // Fallback to view count for popularity if rating is unavailable
                        videos.sortedByDescending { (it as SearchItem.Video).video.viewCount }
                    }
                    else -> videos
                }

                // For newest sort, we skip static items to avoid pinning channels/playlists to the top
                if (sort == SearchSort.UPLOAD_DATE) sortedVideos else staticItems + sortedVideos
            }

            SearchUiState.Success(sortedItems, isLoadingMore)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Initial)

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null
    private var nextPage: Page? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<String>> = _searchQuery
        .debounce(300.milliseconds)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                flow {
                    emit(searchRepository.getSearchSuggestions(query))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = libraryRepository.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused
    private val isIncognitoMode = preferencesManager.isIncognitoMode

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            searchJob?.cancel()
            _internalUiState.value = SearchUiState.Initial
            nextPage = null
        }
    }

    fun onSortChange(sort: SearchSort) {
        if (_searchSort.value == sort) return
        _searchSort.value = sort
        triggerSearch()
    }

    private fun triggerSearch() {
        if (_searchQuery.value.isNotBlank()) {
            _internalUiState.value = SearchUiState.Loading
            _isSortingNewest.value = (_searchSort.value == SearchSort.UPLOAD_DATE)
            search(_searchQuery.value)
        }
    }

    fun toggleGridView() {
        viewModelScope.launch {
            preferencesManager.setSearchGridView(!isGridView.value)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        nextPage = null

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // If it's not already in Loading state (from onSortChange), set it now.
            if (_internalUiState.value !is SearchUiState.Loading && !_isSortingNewest.value) {
                _internalUiState.value = SearchUiState.Loading
            }

            // Save to history if not paused and not incognito
            if (!isSearchHistoryPaused.first() && !isIncognitoMode.first()) {
                libraryRepository.addSearchQuery(query)
            }

            if (_isSortingNewest.value) {
                performDeepSearch(query)
            } else {
                searchVideosUseCase(query, _searchSort.value)
                    .onSuccess { result ->
                        nextPage = result.nextPage
                        _internalUiState.value = SearchUiState.Success(result.items)
                        _isSortingNewest.value = false
                    }
                    .onFailure { exception ->
                        _internalUiState.value = SearchUiState.Error(PlayTubeError.fromThrowable(exception))
                        _isSortingNewest.value = false
                    }
            }
        }
    }

    private suspend fun performDeepSearch(query: String) {
        val allItems = mutableListOf<SearchItem>()
        val currentKeys = mutableSetOf<String>()
        var currentPage: Page? = null
        val maxPages = 5 // Deep fetch 5 pages immediately for "Newest" sort

        try {
            // Initial Page
            val initialResult = searchVideosUseCase(query, SearchSort.UPLOAD_DATE).getOrThrow()
            allItems.addAll(initialResult.items)
            currentKeys.addAll(initialResult.items.map { it.uniqueKey })
            currentPage = initialResult.nextPage

            // Progressively fetch and update UI (similar to Subscriptions)
            _internalUiState.value = SearchUiState.Success(allItems.toList())

            for (i in 1 until maxPages) {
                if (currentPage == null) break
                
                delay(100) // Small delay to prevent rate limiting and allow UI to breathe
                val nextResult = searchVideosUseCase.fetchNextPage(query, SearchSort.UPLOAD_DATE, page = currentPage).getOrThrow()
                
                val newItems = nextResult.items.filter { it.uniqueKey !in currentKeys }
                allItems.addAll(newItems)
                currentKeys.addAll(newItems.map { it.uniqueKey })
                currentPage = nextResult.nextPage
                
                // Update UI incrementally so user sees results coming in
                _internalUiState.value = SearchUiState.Success(
                    items = allItems.toList(),
                    isLoadingMore = i < maxPages - 1 && currentPage != null
                )
            }

            nextPage = currentPage
            _isSortingNewest.value = false
        } catch (e: Exception) {
            if (allItems.isNotEmpty()) {
                _internalUiState.value = SearchUiState.Success(allItems.toList())
            } else {
                _internalUiState.value = SearchUiState.Error(PlayTubeError.fromThrowable(e))
            }
            _isSortingNewest.value = false
        }
    }

    fun loadNextPage() {
        val currentQuery = _searchQuery.value
        val currentPage = nextPage
        // Re-enabled pagination for UPLOAD_DATE (Newest) to fetch more recent content
        if (isFetchingNextPage.value || currentPage == null || currentQuery.isBlank()) return

        isFetchingNextPage.value = true
        viewModelScope.launch {
            searchVideosUseCase.fetchNextPage(currentQuery, _searchSort.value, page = currentPage)
                .onSuccess { result ->
                    _internalUiState.update { currentState ->
                        if (currentState is SearchUiState.Success) {
                            nextPage = result.nextPage
                            
                            // Strict de-duplication to prevent "accumulation" bugs
                            val currentKeys = currentState.items.map { it.uniqueKey }.toSet()
                            val filteredNewItems = result.items.filter { it.uniqueKey !in currentKeys }

                            currentState.copy(
                                items = currentState.items + filteredNewItems
                            )
                        } else {
                            currentState
                        }
                    }
                }
            isFetchingNextPage.value = false
        }
    }

    fun deleteSearchQuery(query: String) {
        viewModelScope.launch {
            libraryRepository.deleteSearchQuery(query)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            libraryRepository.clearSearchHistory()
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

    fun toggleSubscription(channel: SearchItem.Channel) {
        viewModelScope.launch {
            val channelId = VideoUtils.extractChannelId(channel.id) ?: channel.id
            toggleSubscriptionUseCase(
                SubscriptionEntity(
                    channelId = channelId,
                    name = channel.name,
                    thumbnailUrl = channel.thumbnailUrl,
                    subscriberCount = channel.subscriberCount
                )
            )
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

            // Fallback to standalone progressive stream if adaptive audio pairing fails
            val finalUrl = if (isAdaptive && audioUrl == null) {
                bundle.videoStreams.find { !it.isAdaptive }?.url ?: url
            } else url

            val finalIsAdaptive = isAdaptive && audioUrl != null

            downloadVideoUseCase(
                videoId = video.id,
                url = finalUrl,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                uploaderName = video.uploaderName,
                quality = quality,
                format = format,
                audioUrl = if (finalIsAdaptive) audioUrl else null
            )
            _snackbarMessage.emit("Downloading started")
            _downloadState.value = DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadDialogState.Idle
    }
}

@Immutable
sealed interface SearchUiState {
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val items: List<SearchItem>, val isLoadingMore: Boolean = false) : SearchUiState
    data class Error(val error: PlayTubeError) : SearchUiState
}
