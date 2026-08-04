/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.net.Uri
import com.arslandaim.playtube.domain.repository.DataManagerRepository
import com.arslandaim.playtube.domain.repository.ImportProgress
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportManager @Inject constructor(
    private val dataManagerRepository: DataManagerRepository
) {
    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress = _importProgress.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var activeJob: Job? = null

    fun importHistory(uri: Uri) {
        if (_isProcessing.value) return
        
        activeJob = scope.launch {
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
        if (_isProcessing.value) return
        
        activeJob = scope.launch {
            _isProcessing.value = true
            dataManagerRepository.importTakeoutSubscriptions(uri).collectLatest { progress ->
                _importProgress.value = progress
                if (progress is ImportProgress.Success) {
                    _snackbarMessage.emit("Successfully imported ${progress.importedCount} channels with metadata")
                    _isProcessing.value = false
                } else if (progress is ImportProgress.Error) {
                    _snackbarMessage.emit("Error: ${progress.message}")
                    _isProcessing.value = false
                }
            }
        }
    }

    fun clearProgress() {
        if (!_isProcessing.value) {
            _importProgress.value = null
        }
    }
}
