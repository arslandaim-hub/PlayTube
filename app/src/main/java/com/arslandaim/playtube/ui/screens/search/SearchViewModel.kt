/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.data.local.SearchHistoryEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.usecase.SearchVideosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

import androidx.compose.runtime.Immutable

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchVideosUseCase: SearchVideosUseCase,
    private val searchRepository: SearchRepository,
    private val libraryRepository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Initial)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSort = MutableStateFlow(SearchSort.RELEVANCE)
    val searchSort: StateFlow<SearchSort> = _searchSort.asStateFlow()

    private var searchJob: kotlinx.coroutines.Job? = null

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
            _uiState.value = SearchUiState.Initial
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

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading

            // Save to history if not paused
            if (!isSearchHistoryPaused.first()) {
                libraryRepository.addSearchQuery(query)
            }

            searchVideosUseCase(query, _searchSort.value)
                .onSuccess { videos ->
                    _uiState.value = SearchUiState.Success(videos)
                }
                .onFailure { exception ->
                    val errorMessage = if (exception is java.net.UnknownHostException || exception is java.io.IOException) {
                        "No internet connection"
                    } else {
                        exception.message ?: "Unknown error"
                    }
                    _uiState.value = SearchUiState.Error(errorMessage)
                }
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
}

@Immutable
sealed interface SearchUiState {
    object Initial : SearchUiState
    object Loading : SearchUiState
    data class Success(val videos: List<VideoItem>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}
