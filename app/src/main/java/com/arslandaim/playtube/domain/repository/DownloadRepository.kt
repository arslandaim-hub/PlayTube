package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.data.local.DownloadEntity
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    suspend fun getDownloadByVideoId(videoId: String): DownloadEntity?
    suspend fun startDownload(
        videoId: String,
        url: String?,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        quality: String?,
        format: String?,
        audioUrl: String? = null,
        playlistId: String? = null,
        playlistTitle: String? = null
    )
    suspend fun cancelDownload(videoId: String)
    suspend fun pauseDownload(videoId: String)
    suspend fun resumeDownload(videoId: String)
    suspend fun pauseAllActiveDownloads()
    suspend fun resumeAllPausedDownloads()
    suspend fun deleteDownload(videoId: String)
    suspend fun clearAllDownloads()
}
