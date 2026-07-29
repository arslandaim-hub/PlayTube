/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.domain.repository.DataManagerRepository
import com.arslandaim.playtube.domain.repository.ImportProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val dataManagerRepository: DataManagerRepository
) : ViewModel() {

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress = _importProgress.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    fun importHistory(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            dataManagerRepository.importTakeoutHistory(uri).collectLatest { progress ->
                _importProgress.value = progress
                if (progress is ImportProgress.Success) {
                    _snackbarMessage.emit("Successfully imported ${progress.importedCount} videos")
                    _isProcessing.value = false
                } else if (progress is ImportProgress.Error) {
                    _snackbarMessage.emit("Error: ${progress.message}")
                    _isProcessing.value = false
                }
            }
        }
    }

    fun importSubscriptions(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            dataManagerRepository.importTakeoutSubscriptions(uri).collectLatest { progress ->
                _importProgress.value = progress
                if (progress is ImportProgress.Success) {
                    _snackbarMessage.emit("Successfully imported ${progress.importedCount} channels")
                    _isProcessing.value = false
                } else if (progress is ImportProgress.Error) {
                    _snackbarMessage.emit("Error: ${progress.message}")
                    _isProcessing.value = false
                }
            }
        }
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = dataManagerRepository.createBackup(uri)
            if (result.isSuccess) {
                _snackbarMessage.emit("Backup created successfully")
            } else {
                _snackbarMessage.emit("Failed to create backup")
            }
            _isProcessing.value = false
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = dataManagerRepository.restoreBackup(uri)
            if (result.isSuccess) {
                _snackbarMessage.emit("Backup restored successfully. App will now refresh.")
            } else {
                _snackbarMessage.emit("Failed to restore backup")
            }
            _isProcessing.value = false
        }
    }

    fun clearProgress() {
        _importProgress.value = null
    }
}
