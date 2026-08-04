/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.repository.ImportManager
import com.arslandaim.playtube.domain.repository.DataManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataManagementViewModel @Inject constructor(
    private val dataManagerRepository: DataManagerRepository,
    private val importManager: ImportManager
) : ViewModel() {

    private val _localSnackbarMessage = MutableSharedFlow<String>()
    val localSnackbarMessage = _localSnackbarMessage.asSharedFlow()

    val importProgress = importManager.importProgress
    val importSnackbarMessage = importManager.snackbarMessage
    val isProcessing = importManager.isProcessing

    fun importHistory(uri: Uri) {
        importManager.importHistory(uri)
    }

    fun importSubscriptions(uri: Uri) {
        importManager.importSubscriptions(uri)
    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            val result = dataManagerRepository.createBackup(uri)
            if (result.isSuccess) {
                _localSnackbarMessage.emit("Backup created successfully")
            } else {
                _localSnackbarMessage.emit("Failed to create backup")
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val result = dataManagerRepository.restoreBackup(uri)
            if (result.isSuccess) {
                _localSnackbarMessage.emit("Backup restored successfully. App will now refresh.")
            } else {
                _localSnackbarMessage.emit("Failed to restore backup")
            }
        }
    }

    fun clearProgress() {
        importManager.clearProgress()
    }
}
