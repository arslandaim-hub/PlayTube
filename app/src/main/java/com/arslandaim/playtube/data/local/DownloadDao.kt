/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads")
    suspend fun getAllDownloadsList(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownloadById(videoId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, downloadedSize = :downloadedSize, totalSize = :totalSize WHERE videoId = :videoId")
    suspend fun updateProgress(videoId: String, status: DownloadStatus, downloadedSize: Long, totalSize: Long)

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' OR status = 'PENDING'")
    suspend fun getActiveDownloads(): List<DownloadEntity>

    @Query("SELECT * FROM downloads WHERE status = 'PAUSED'")
    suspend fun getPausedDownloads(): List<DownloadEntity>

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
