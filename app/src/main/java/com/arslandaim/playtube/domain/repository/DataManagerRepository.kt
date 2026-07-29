/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface DataManagerRepository {
    // YouTube Takeout
    fun importTakeoutHistory(uri: Uri): Flow<ImportProgress>
    fun importTakeoutSubscriptions(uri: Uri): Flow<ImportProgress>

    // Native Backup
    suspend fun createBackup(uri: Uri): Result<Unit>
    suspend fun restoreBackup(uri: Uri): Result<Unit>
}

sealed class ImportProgress {
    data class Loading(val progress: Float, val status: String) : ImportProgress()
    data class Success(val importedCount: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}
