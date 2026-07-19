/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.content.Context
import androidx.work.*
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.workers.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    override suspend fun getDownloadByVideoId(videoId: String): DownloadEntity? = 
        downloadDao.getDownloadById(videoId)

    override suspend fun startDownload(
        videoId: String,
        url: String?,
        title: String,
        thumbnailUrl: String,
        uploaderName: String,
        quality: String?,
        format: String?,
        audioUrl: String?,
        playlistId: String?,
        playlistTitle: String?
    ) {
        val extension = if (format?.contains("webm", ignoreCase = true) == true) "webm" else "mp4"
        val filePath = File(context.getExternalFilesDir(null), "$videoId.$extension").absolutePath
        val entity = DownloadEntity(
            videoId = videoId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            uploaderName = uploaderName,
            filePath = filePath,
            totalSize = 0,
            downloadedSize = 0,
            status = DownloadStatus.WAITING,
            quality = quality,
            format = format,
            videoUrl = url,
            audioUrl = audioUrl,
            playlistId = playlistId,
            playlistTitle = playlistTitle
        )
        downloadDao.insertDownload(entity)
        enqueueDownloadWork(videoId, url, audioUrl, title, format)
    }

    private fun enqueueDownloadWork(
        videoId: String,
        url: String?,
        audioUrl: String?,
        title: String,
        format: String?
    ) {
        val data = workDataOf(
            "videoId" to videoId,
            "url" to url,
            "title" to title,
            "audioUrl" to audioUrl,
            "format" to format
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .addTag(videoId)
            .build()

        workManager.enqueueUniqueWork(
            videoId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override suspend fun cancelDownload(videoId: String) {
        workManager.cancelUniqueWork(videoId)
        downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
    }

    override suspend fun pauseDownload(videoId: String) {
        workManager.cancelUniqueWork(videoId)
        val entity = downloadDao.getDownloadById(videoId)
        entity?.let {
            downloadDao.updateDownload(it.copy(status = DownloadStatus.PAUSED))
        }
    }

    override suspend fun resumeDownload(videoId: String) {
        val entity = downloadDao.getDownloadById(videoId)
        entity?.let {
            if (it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED) {
                downloadDao.updateDownload(it.copy(status = DownloadStatus.WAITING))
                enqueueDownloadWork(it.videoId, it.videoUrl, it.audioUrl, it.title, it.format)
            }
        }
    }

    override suspend fun pauseAllActiveDownloads() {
        val active = downloadDao.getActiveDownloads()
        active.forEach { 
            pauseDownload(it.videoId)
        }
    }

    override suspend fun resumeAllPausedDownloads() {
        val paused = downloadDao.getPausedDownloads()
        paused.forEach {
            resumeDownload(it.videoId)
        }
    }

    override suspend fun deleteDownload(videoId: String) {
        cancelDownload(videoId)
        val entity = downloadDao.getDownloadById(videoId)
        entity?.let {
            File(it.filePath).delete()
            downloadDao.deleteDownload(it)
        }
    }

    override suspend fun clearAllDownloads() {
        workManager.cancelAllWork()
        val allDownloads = downloadDao.getAllDownloadsList()
        allDownloads.forEach { entity ->
            try {
                val file = File(entity.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("DownloadRepository", "Failed to delete file: ${entity.filePath}", e)
            }
        }
        downloadDao.clearAll()
    }
}
