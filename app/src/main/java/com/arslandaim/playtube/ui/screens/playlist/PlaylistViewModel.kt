/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.data.local.PlaylistFavoriteEntity
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetPlaylistDetailsUseCase
import com.arslandaim.playtube.domain.usecase.IsPlaylistFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.TogglePlaylistFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistDetailsUseCase: GetPlaylistDetailsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase,
    private val togglePlaylistFavoriteUseCase: TogglePlaylistFavoriteUseCase,
    private val isPlaylistFavoriteUseCase: IsPlaylistFavoriteUseCase,
    private val videoRepository: VideoRepository,
    val downloadRepository: DownloadRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlaylistUiState>(PlaylistUiState.Loading)
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val downloadedVideoIds: StateFlow<Set<String>> = downloadRepository.getAllDownloads()
        .map { list -> 
            list.filter { it.status == com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun loadPlaylist(playlistId: String) {
        val playlistUrl = if (playlistId.startsWith("http")) {
            playlistId
        } else {
            "https://www.youtube.com/playlist?list=$playlistId"
        }
        viewModelScope.launch {
            _uiState.value = PlaylistUiState.Loading
            
            // Watch favorite status
            launch {
                isPlaylistFavoriteUseCase(playlistId).collectLatest {
                    _isFavorite.value = it
                }
            }

            getPlaylistDetailsUseCase(playlistUrl)
                .onSuccess { details ->
                    _uiState.value = PlaylistUiState.Success(details)
                }
                .onFailure { exception ->
                    _uiState.value = PlaylistUiState.Error(exception.message ?: "Unknown error")
                }
        }
    }

    fun downloadPlaylist() {
        val state = _uiState.value as? PlaylistUiState.Success ?: return
        val details = state.details
        
        viewModelScope.launch {
            _snackbarMessage.emit("Playlist download started")
            details.videos.forEach { video ->
                downloadVideoUseCase(
                    videoId = video.id,
                    url = null, // Will be fetched by the worker
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName,
                    quality = null, // Will be fetched by the worker
                    format = null, // Will be fetched by the worker
                    audioUrl = null,
                    playlistId = details.id,
                    playlistTitle = details.title
                )
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value as? PlaylistUiState.Success ?: return
        val details = state.details
        viewModelScope.launch {
            togglePlaylistFavoriteUseCase(
                PlaylistFavoriteEntity(
                    playlistId = details.id,
                    title = details.title,
                    thumbnailUrl = details.thumbnailUrl,
                    uploaderName = details.uploaderName
                )
            )
        }
    }
}

sealed interface PlaylistUiState {
    object Loading : PlaylistUiState
    data class Success(val details: PlaylistDetails) : PlaylistUiState
    data class Error(val message: String) : PlaylistUiState
}
