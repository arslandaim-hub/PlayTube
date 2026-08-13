/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val connectivityObserver: ConnectivityObserver,
    private val libraryRepository: com.arslandaim.playtube.domain.repository.LibraryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val localPlaylists: StateFlow<List<com.arslandaim.playtube.data.local.LocalPlaylistEntity>> = libraryRepository.getLocalPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlistSelectionState = MutableStateFlow(PlaylistSelectionState())
    val playlistSelectionState = _playlistSelectionState.asStateFlow()

    val connectivityStatus: StateFlow<ConnectivityObserver.Status> = connectivityObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectivityObserver.Status.Available)

    val isOffline: StateFlow<Boolean> = connectivityStatus
        .map { it == ConnectivityObserver.Status.Lost || it == ConnectivityObserver.Status.Unavailable }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPipEnabled: StateFlow<Boolean> = preferencesManager.isPipEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isBackgroundPlayEnabled: StateFlow<Boolean> = preferencesManager.isBackgroundPlayEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isIncognitoMode: StateFlow<Boolean> = preferencesManager.isIncognitoMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isDynamicColorEnabled: StateFlow<Boolean> = preferencesManager.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isOnboardingCompleted: StateFlow<Boolean?> = preferencesManager.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleIncognitoMode() {
        viewModelScope.launch {
            val current = preferencesManager.isIncognitoMode.first()
            preferencesManager.setIncognitoMode(!current)
        }
    }

    private val _showOfflineDialog = MutableStateFlow(false)
    val showOfflineDialog: StateFlow<Boolean> = _showOfflineDialog.asStateFlow()

    init {
        viewModelScope.launch {
            // Check offline status on startup to trigger dialog
            val initialStatus = connectivityStatus.first()
            if (initialStatus == ConnectivityObserver.Status.Lost || initialStatus == ConnectivityObserver.Status.Unavailable) {
                _showOfflineDialog.value = true
            }
        }
    }

    fun dismissOfflineDialog() {
        _showOfflineDialog.value = false
    }

    fun setBarsVisibility(visible: Boolean) {
        _uiState.update { it.copy(isBarsVisible = visible) }
    }

    fun setPipMode(enabled: Boolean) {
        _uiState.update { it.copy(isInPipMode = enabled) }
    }

    fun setPlayerScreen(isPlayer: Boolean) {
        _uiState.update { it.copy(isPlayerScreen = isPlayer) }
    }

    private var playlistSelectionJob: Job? = null

    fun showPlaylistSelection(video: com.arslandaim.playtube.domain.model.VideoItem) {
        playlistSelectionJob?.cancel()
        playlistSelectionJob = viewModelScope.launch {
            libraryRepository.getPlaylistsContainingVideo(video.id).collectLatest { playlists ->
                _playlistSelectionState.value = PlaylistSelectionState(
                    isVisible = true, 
                    video = video,
                    playlistsWithVideo = playlists.toSet()
                )
            }
        }
    }

    fun hidePlaylistSelection() {
        playlistSelectionJob?.cancel()
        playlistSelectionJob = null
        _playlistSelectionState.value = PlaylistSelectionState(isVisible = false, video = null)
    }

    fun addVideoToPlaylist(playlistId: Int, video: com.arslandaim.playtube.domain.model.VideoItem) {
        viewModelScope.launch {
            libraryRepository.addVideoToLocalPlaylist(playlistId, video)
            hidePlaylistSelection()
        }
    }

    fun createLocalPlaylist(name: String) {
        viewModelScope.launch {
            val id = libraryRepository.createLocalPlaylist(name)
            _playlistSelectionState.value.video?.let { video ->
                addVideoToPlaylist(id.toInt(), video)
            }
        }
    }
}

data class MainUiState(
    val isBarsVisible: Boolean = true,
    val isInPipMode: Boolean = false,
    val isPlayerScreen: Boolean = false
)

data class PlaylistSelectionState(
    val isVisible: Boolean = false,
    val video: com.arslandaim.playtube.domain.model.VideoItem? = null,
    val playlistsWithVideo: Set<Int> = emptySet()
)
