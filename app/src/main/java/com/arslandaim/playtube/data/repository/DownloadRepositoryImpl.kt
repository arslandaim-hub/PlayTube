/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.*
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.workers.DownloadWorker
import com.arslandaim.playtube.utils.PTLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
        enqueueDownloadWork(videoId, url, audioUrl, title, format, quality)
    }

    private fun enqueueDownloadWork(
        videoId: String,
        url: String?,
        audioUrl: String?,
        title: String,
        format: String?,
        quality: String?
    ) {
        val data = workDataOf(
            "videoId" to videoId,
            "url" to url,
            "title" to title,
            "audioUrl" to audioUrl,
            "format" to format,
            "quality" to quality
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
        
        // Clean up partial files
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(videoId)) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            PTLog.e("DownloadRepository", "Failed to clean up partial files for $videoId", e)
        }
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
                enqueueDownloadWork(it.videoId, it.videoUrl, it.audioUrl, it.title, it.format, it.quality)
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
                PTLog.e("DownloadRepository", "Failed to delete file: ${entity.filePath}", e)
            }
        }
        downloadDao.clearAll()
    }

    override suspend fun saveToPublicStorage(videoId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = downloadDao.getDownloadById(videoId) ?: return@withContext Result.failure(Exception("Download not found"))
            if (entity.status != DownloadStatus.COMPLETED) return@withContext Result.failure(Exception("Download not completed"))

            val file = File(entity.filePath)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            val extension = if (entity.format?.contains("webm", ignoreCase = true) == true) "webm" else "mp4"
            val mimeType = if (extension == "webm") "video/webm" else "video/mp4"
            val displayName = "${entity.title}_${entity.videoId}.$extension"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/PlayTube")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return@withContext Result.failure(Exception("Failed to insert into MediaStore"))

            resolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            PTLog.e("DownloadRepository", "Failed to save to public storage", e)
            Result.failure(e)
        }
    }
}
