package com.arslandaim.playtube.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.BuildConfig
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.model.UpdateInfo
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.UpdateRepository
import com.arslandaim.playtube.utils.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val downloadRepository: DownloadRepository,
    private val libraryRepository: LibraryRepository,
    private val updateRepository: UpdateRepository,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        // Automatically check for updates when internet becomes available
        viewModelScope.launch {
            connectivityObserver.observe().collectLatest { status ->
                if (status == ConnectivityObserver.Status.Available) {
                    checkForUpdates()
                }
            }
        }
    }

    fun checkForUpdates() {
        if (_updateState.value is UpdateState.Checking) return

        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            updateRepository.getLatestUpdate()
                .onSuccess { info ->
                    if (info == null) {
                        _updateState.value = UpdateState.UpToDate
                        return@onSuccess
                    }

                    if (isNewerVersion(info.versionName)) {
                        _updateState.value = UpdateState.UpdateAvailable(info)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                }
                .onFailure { error ->
                    _updateState.value = UpdateState.Error(error.message ?: "Failed to check for updates")
                }
        }
    }

    private fun isNewerVersion(latestVersion: String): Boolean {
        val current = BuildConfig.VERSION_NAME.split(".").mapNotNull { it.toIntOrNull() }
        val latest = latestVersion.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(current.size, latest.size)) {
            if (latest[i] > current[i]) return true
            if (latest[i] < current[i]) return false
        }
        return latest.size > current.size
    }

    val isHistoryEnabled: StateFlow<Boolean> = preferencesManager.isHistoryEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSearchHistoryPaused: StateFlow<Boolean> = preferencesManager.isSearchHistoryPaused
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPipEnabled: StateFlow<Boolean> = preferencesManager.isPipEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setHistoryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setHistoryEnabled(enabled)
        }
    }

    fun setSearchHistoryPaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesManager.setSearchHistoryPaused(paused)
        }
    }

    fun setPipEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPipEnabled(enabled)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            downloadRepository.clearAllDownloads()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            libraryRepository.clearHistory()
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            libraryRepository.clearSearchHistory()
        }
    }
}

sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(val info: UpdateInfo) : UpdateState
    object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}
