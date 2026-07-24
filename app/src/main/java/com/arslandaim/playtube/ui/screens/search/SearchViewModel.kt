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
import com.arslandaim.playtube.utils.VideoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
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
) : ViewModel() {

    private val _internalUiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    
    private var isFetchingNextPage by mutableStateOf(false)

    val uiState: StateFlow<SearchUiState> = combine(
        _internalUiState,
        libraryRepository.getHistory(),
        libraryRepository.getSubscriptions(),
        snapshotFlow { isFetchingNextPage }
    ) { state, history, subs, isLoadingMore ->
        if (state is SearchUiState.Success) {
            val historyMap = history.associateBy({ it.videoId }, { if (it.durationMs > 0) it.progressMs.toFloat() / it.durationMs else null })
            
            val updatedItems = state.items.map { item ->
                when (item) {
                    is SearchItem.Video -> {
                        SearchItem.Video(item.video.copy(watchProgress = historyMap[item.video.id]))
                    }
                    is SearchItem.Channel -> {
                        val channelId = VideoUtils.extractChannelId(item.id) ?: item.id
                        val isSubscribed = subs.any { it.channelId.contains(channelId) || channelId.contains(it.channelId) }
                        item.copy(isSubscribed = isSubscribed)
                    }
                    is SearchItem.Playlist -> item
                }
            }
            SearchUiState.Success(updatedItems, isLoadingMore)
        } else {
            state
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Initial)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSort = MutableStateFlow(SearchSort.RELEVANCE)
    val searchSort: StateFlow<SearchSort> = _searchSort.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null
    private var nextPage: Page? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val suggestions: StateFlow<List<String>> = _searchQuery
        .debounce(300.milliseconds)
        .filter { it.isNotBlank() }
        .combine(preferencesManager.isSearchHistoryPaused) { query, paused ->
            if (paused) emptyList() else searchRepository.getSearchSuggestions(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistory: StateFlow<List<SearchHistoryEntity>> = libraryRepository.getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused

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
        if (_searchQuery.value.isNotBlank()) {
            search(_searchQuery.value)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) return
        _searchQuery.value = query
        nextPage = null

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _internalUiState.value = SearchUiState.Loading

            // Save to history if not paused
            if (!isSearchHistoryPaused.first()) {
                libraryRepository.addSearchQuery(query)
            }

            searchVideosUseCase(query, _searchSort.value)
                .onSuccess { result ->
                    nextPage = result.nextPage
                    _internalUiState.value = SearchUiState.Success(result.items)
                }
                .onFailure { exception ->
                    val errorMessage = if (exception is java.net.UnknownHostException || exception is java.io.IOException) {
                        "No internet connection"
                    } else {
                        exception.message ?: "Unknown error"
                    }
                    _internalUiState.value = SearchUiState.Error(errorMessage)
                }
        }
    }

    fun loadNextPage() {
        val currentQuery = _searchQuery.value
        val currentPage = nextPage
        if (isFetchingNextPage || currentPage == null || currentQuery.isBlank()) return

        isFetchingNextPage = true
        viewModelScope.launch {
            searchVideosUseCase.fetchNextPage(currentQuery, _searchSort.value, currentPage)
                .onSuccess { result ->
                    val currentState = _internalUiState.value
                    if (currentState is SearchUiState.Success) {
                        nextPage = result.nextPage
                        _internalUiState.value = SearchUiState.Success(currentState.items + result.items)
                    }
                }
            isFetchingNextPage = false
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
            _snackbarMessage.emit(if (isFavorite) "Removed from Favorites" else "Added to Favorites")
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

@Immutable
sealed interface SearchUiState {
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val items: List<SearchItem>, val isLoadingMore: Boolean = false) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
