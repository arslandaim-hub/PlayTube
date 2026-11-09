/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.work.*
import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.domain.repository.DownloadRepository
import com.arslandaim.playtube.services.VideoDownloadService
import com.arslandaim.playtube.utils.PTLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val missionDao: MissionDao
) : DownloadRepository {

    private val workManager = WorkManager.getInstance(context)

    override fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    override suspend fun getDownloadByVideoId(videoId: String): DownloadEntity? = 
        downloadDao.getDownloadById(videoId)

    override suspend fun getDownloadByVideoIdResilient(videoId: String): DownloadEntity? = withContext(Dispatchers.IO) {
        val entity = downloadDao.getDownloadById(videoId) ?: return@withContext null
        if (entity.status != DownloadStatus.COMPLETED) return@withContext entity

        val file = File(entity.filePath)
        if (file.exists()) return@withContext entity

        // Resilience: Try fallback extensions for existing broken playlist downloads
        val baseDir = context.getExternalFilesDir(null)
        val webmFile = File(baseDir, "$videoId.webm")
        val mp4File = File(baseDir, "$videoId.mp4")

        val fixedFile = when {
            webmFile.exists() -> webmFile
            mp4File.exists() -> mp4File
            else -> null
        }

        if (fixedFile != null) {
            val updated = entity.copy(filePath = fixedFile.absolutePath)
            downloadDao.updateDownload(updated)
            return@withContext updated
        }

        entity
    }

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
        playlistTitle: String?,
        subtitleUrl: String?,
        subtitleLanguage: String?
    ) {
        val extension = if (format?.contains("webm", ignoreCase = true) == true) "webm" else "mp4"
        val filePath = File(context.getExternalFilesDir(null), "$videoId.$extension").absolutePath
        val subtitlePath = if (!subtitleUrl.isNullOrBlank()) {
            val lang = subtitleLanguage ?: "en"
            val (_, mimeType) = com.arslandaim.playtube.utils.CaptionTrackResolver.normalizeSubtitleUrl(subtitleUrl)
            val subExt = if (mimeType == androidx.media3.common.MimeTypes.APPLICATION_TTML) "ttml" else "vtt"
            File(context.getExternalFilesDir(null), "${videoId}_$lang.$subExt").absolutePath
        } else null

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
            playlistTitle = playlistTitle,
            subtitlePath = subtitlePath,
            subtitleLanguage = subtitleLanguage
        )
        downloadDao.insertDownload(entity)
        
        // New resilient logic
        val mission = DownloadMissionEntity(
            videoId = videoId,
            title = title,
            quality = quality ?: "Unknown",
            videoUrl = url,
            audioUrl = audioUrl,
            format = format,
            outputFilePath = filePath,
            subtitleUrl = subtitleUrl,
            subtitlePath = subtitlePath,
            subtitleLanguage = subtitleLanguage
        )
        val missionId = missionDao.insertMission(mission)
        
        startDownloadService(missionId)
    }

    private fun startDownloadService(missionId: Long) {
        try {
            val intent = Intent(context, VideoDownloadService::class.java).apply {
                action = VideoDownloadService.ACTION_START
                putExtra("missionId", missionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            PTLog.e("DownloadRepository", "Failed to start foreground download service for mission $missionId", e)
            throw e
        }
    }

    override suspend fun cancelDownload(videoId: String) {
        val mission = missionDao.getMissionByVideoId(videoId)
        mission?.let {
            runCatching {
                val intent = Intent(context, VideoDownloadService::class.java).apply {
                    action = VideoDownloadService.ACTION_STOP
                    putExtra("missionId", it.id)
                }
                context.startService(intent)
            }
            missionDao.deleteMission(it)
        }
        
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
        val mission = missionDao.getMissionByVideoId(videoId)
        mission?.let {
            runCatching {
                val intent = Intent(context, VideoDownloadService::class.java).apply {
                    action = VideoDownloadService.ACTION_STOP
                    putExtra("missionId", it.id)
                }
                context.startService(intent)
            }
            missionDao.updateStatus(it.id, MissionStatus.PAUSED)
        }
        
        workManager.cancelUniqueWork(videoId)
        val entity = downloadDao.getDownloadById(videoId)
        entity?.let {
            downloadDao.updateDownload(it.copy(status = DownloadStatus.PAUSED))
        }
    }

    override suspend fun resumeDownload(videoId: String) {
        val entity = downloadDao.getDownloadById(videoId)
        val mission = missionDao.getMissionByVideoId(videoId)

        if (mission != null) {
            val wasFailed = mission.status == MissionStatus.FAILED || entity?.status == DownloadStatus.FAILED
            val updatedMission = if (wasFailed) {
                missionDao.deleteChunksForMission(mission.id)
                mission.copy(
                    status = MissionStatus.QUEUED,
                    videoUrl = null,
                    audioUrl = null,
                    subtitleUrl = null,
                    downloadedBytes = 0L
                )
            } else {
                mission.copy(status = MissionStatus.QUEUED)
            }
            missionDao.updateMission(updatedMission)

            entity?.let {
                downloadDao.updateDownload(
                    it.copy(
                        status = DownloadStatus.WAITING,
                        videoUrl = if (wasFailed) null else it.videoUrl,
                        audioUrl = if (wasFailed) null else it.audioUrl,
                        downloadedSize = if (wasFailed) 0L else it.downloadedSize
                    )
                )
            }

            startDownloadService(mission.id)
            return
        }
        
        entity?.let {
            if (it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED || it.status == DownloadStatus.WAITING) {
                val wasFailed = it.status == DownloadStatus.FAILED
                val freshVideoUrl = if (wasFailed) null else it.videoUrl
                val freshAudioUrl = if (wasFailed) null else it.audioUrl

                downloadDao.updateDownload(
                    it.copy(
                        status = DownloadStatus.WAITING,
                        videoUrl = freshVideoUrl,
                        audioUrl = freshAudioUrl,
                        downloadedSize = if (wasFailed) 0L else it.downloadedSize
                    )
                )

                val newMission = DownloadMissionEntity(
                    videoId = it.videoId,
                    title = it.title,
                    quality = it.quality ?: "Unknown",
                    videoUrl = freshVideoUrl,
                    audioUrl = freshAudioUrl,
                    format = it.format,
                    outputFilePath = it.filePath,
                    subtitlePath = it.subtitlePath,
                    subtitleLanguage = it.subtitleLanguage
                )
                val missionId = missionDao.insertMission(newMission)
                startDownloadService(missionId)
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

            var file = File(entity.filePath)
            if (!file.exists()) {
                // Task: Resilient fallback check for alternate extensions
                val baseDir = context.getExternalFilesDir(null)
                val webmFile = File(baseDir, "$videoId.webm")
                val mp4File = File(baseDir, "$videoId.mp4")
                
                file = when {
                    webmFile.exists() -> webmFile
                    mp4File.exists() -> mp4File
                    else -> return@withContext Result.failure(Exception("File not found at ${entity.filePath}"))
                }
                
                // Sync the DB path if we found it elsewhere
                downloadDao.updateDownload(entity.copy(filePath = file.absolutePath))
            }

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
