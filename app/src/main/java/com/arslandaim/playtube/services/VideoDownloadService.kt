/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arslandaim.playtube.data.local.MissionDao
import com.arslandaim.playtube.data.local.MissionStatus
import com.arslandaim.playtube.data.local.ChunkType
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.data.network.ParallelDownloader
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.NativeMediaMuxer
import com.arslandaim.playtube.utils.PTLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class VideoDownloadService : Service() {

    @Inject lateinit var missionDao: MissionDao
    @Inject lateinit var downloadDao: DownloadDao
    @Inject lateinit var okHttpClient: OkHttpClient
    @Inject lateinit var videoRepository: VideoRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var downloader: ParallelDownloader
    private val muxer = NativeMediaMuxer()
    
    private val activeMissions = mutableMapOf<Long, Job>()
    private var foregroundMissionId: Long = -1L

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        downloader = ParallelDownloader(okHttpClient, missionDao)
        createNotificationChannel()
        
        // Recover from stale states (app crash/kill)
        serviceScope.launch {
            try {
                missionDao.getAllMissions().first().forEach { mission ->
                    if (mission.status == MissionStatus.DOWNLOADING || mission.status == MissionStatus.MUXING) {
                        PTLog.d("VideoDownloadService", "Recovering stale mission ${mission.videoId}")
                        missionDao.updateStatus(mission.id, MissionStatus.PAUSED)
                        downloadDao.setDownloadStatus(mission.videoId, DownloadStatus.PAUSED)
                    }
                }
            } catch (e: Exception) {
                PTLog.e("VideoDownloadService", "Stale recovery failed", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val missionId = intent?.getLongExtra("missionId", -1L) ?: -1L
        
        when (action) {
            ACTION_START -> if (missionId != -1L) startMission(missionId)
            ACTION_STOP -> if (missionId != -1L) stopMission(missionId)
        }
        
        return START_REDELIVER_INTENT
    }

    private fun startMission(missionId: Long) {
        if (activeMissions.containsKey(missionId)) return

        val job = serviceScope.launch {
            try {
                var mission = missionDao.getMissionById(missionId) ?: return@launch
                
                updateNotification(missionId, "Preparing...", 0)
                
                // Resolve Metadata if URLs are missing (common for playlist downloads)
                if (mission.videoUrl.isNullOrBlank()) {
                    PTLog.d("VideoDownloadService", "Mission ${mission.videoId} missing URLs, resolving...")
                    val metadata = fetchStreamMetadata(mission.videoId, mission.quality)
                    if (metadata != null) {
                        val resolvedExtension = if (metadata.format.contains("webm", ignoreCase = true)) "webm" else "mp4"
                        val newFilePath = File(getExternalFilesDir(null), "${mission.videoId}.$resolvedExtension").absolutePath

                        mission = mission.copy(
                            videoUrl = metadata.videoUrl,
                            audioUrl = metadata.audioUrl,
                            format = metadata.format,
                            quality = metadata.quality,
                            outputFilePath = newFilePath
                        )
                        missionDao.updateMission(mission)
                        
                        // Sync with main download table for UI consistency and offline playback
                        downloadDao.getDownloadById(mission.videoId)?.let { download ->
                            downloadDao.updateDownload(download.copy(
                                videoUrl = metadata.videoUrl,
                                audioUrl = metadata.audioUrl,
                                format = metadata.format,
                                quality = metadata.quality,
                                filePath = newFilePath
                            ))
                        }
                    } else {
                        throw Exception("Failed to resolve stream metadata")
                    }
                }

                executeDownload(missionId, mission)
            } catch (e: Exception) {
                if (e is CancellationException) {
                    PTLog.d("VideoDownloadService", "Mission $missionId cancelled/paused")
                    missionDao.updateStatus(missionId, MissionStatus.PAUSED)
                    missionDao.getMissionById(missionId)?.let {
                        downloadDao.setDownloadStatus(it.videoId, DownloadStatus.PAUSED)
                    }
                    return@launch
                }

                PTLog.e("VideoDownloadService", "Mission $missionId failed", e)
                
                if (e is ExpiredUrlException) {
                    // Try to re-resolve and retry once
                    try {
                        PTLog.w("VideoDownloadService", "URL expired for mission $missionId, retrying...")
                        val mission = missionDao.getMissionById(missionId) ?: throw e
                        val metadata = fetchStreamMetadata(mission.videoId, mission.quality) ?: throw e
                        
                        val resolvedExtension = if (metadata.format.contains("webm", ignoreCase = true)) "webm" else "mp4"
                        val newFilePath = File(getExternalFilesDir(null), "${mission.videoId}.$resolvedExtension").absolutePath

                        val updatedMission = mission.copy(
                            videoUrl = metadata.videoUrl,
                            audioUrl = metadata.audioUrl,
                            format = metadata.format,
                            quality = metadata.quality,
                            outputFilePath = newFilePath
                        )
                        missionDao.updateMission(updatedMission)

                        downloadDao.getDownloadById(mission.videoId)?.let { download ->
                            downloadDao.updateDownload(download.copy(
                                videoUrl = metadata.videoUrl,
                                audioUrl = metadata.audioUrl,
                                format = metadata.format,
                                quality = metadata.quality,
                                filePath = newFilePath
                            ))
                        }

                        executeDownload(missionId, updatedMission)
                        return@launch
                    } catch (retryEx: Exception) {
                        PTLog.e("VideoDownloadService", "Retry failed for $missionId", retryEx)
                    }
                }

                missionDao.updateStatus(missionId, MissionStatus.FAILED)
                missionDao.getMissionById(missionId)?.let {
                    downloadDao.setDownloadStatus(it.videoId, DownloadStatus.FAILED)
                }
                updateNotification(missionId, "Download failed", 0, true)
            } finally {
                activeMissions.remove(missionId)
                if (foregroundMissionId == missionId) {
                    foregroundMissionId = -1L
                    val nextMissionId = activeMissions.keys.firstOrNull()
                    if (nextMissionId != null) {
                        promoteToForeground(nextMissionId)
                    } else {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                } else {
                    notificationManager.cancel(NOTIFICATION_ID_BASE + missionId.toInt())
                }
            }
        }
        
        activeMissions[missionId] = job
        if (foregroundMissionId == -1L) {
            foregroundMissionId = missionId
            startForeground(NOTIFICATION_ID_BASE + missionId.toInt(), createInitialNotification())
        } else {
            notificationManager.notify(NOTIFICATION_ID_BASE + missionId.toInt(), createInitialNotification())
        }
    }

    private val lastUpdateMap = mutableMapOf<Long, Long>()

    private suspend fun executeDownload(missionId: Long, mission: com.arslandaim.playtube.data.local.DownloadMissionEntity) {
        updateNotification(missionId, "Fetching size...", 0)
        
        val videoUrl = mission.videoUrl ?: throw Exception("Video URL is null")
        
        // Fetch sizes upfront
        val totalVideoSizeRemote = downloader.getFileSize(videoUrl)
        if (totalVideoSizeRemote == -403L) throw ExpiredUrlException()
        if (totalVideoSizeRemote <= 0) throw Exception("Failed to probe video size")
        
        val totalAudioSizeRemote = mission.audioUrl?.let { 
            val size = downloader.getFileSize(it)
            if (size == -403L) throw ExpiredUrlException()
            if (size <= 0) throw Exception("Failed to probe audio size")
            size
        } ?: 0L
        
        val combinedTotalSize = totalVideoSizeRemote + totalAudioSizeRemote
        
        missionDao.updateMission(mission.copy(totalBytes = combinedTotalSize, status = MissionStatus.DOWNLOADING))
        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, mission.downloadedBytes, combinedTotalSize)
        
        val videoFile = File(cacheDir, "${mission.videoId}_video.tmp")
        val audioFile = File(cacheDir, "${mission.videoId}_audio.tmp")
        
        // 1. Download Video
        val videoSize = try {
            downloader.download(videoUrl, videoFile, missionId, ChunkType.VIDEO) { progress ->
                val currentTime = System.currentTimeMillis()
                val lastUpdate = lastUpdateMap[missionId] ?: 0L
                
                if (currentTime - lastUpdate >= 1000L || progress == combinedTotalSize) {
                    lastUpdateMap[missionId] = currentTime
                    val percent = if (combinedTotalSize > 0) (progress * 100 / combinedTotalSize).toInt() else 0
                    updateNotification(missionId, "Downloading video...", percent / 2)
                    serviceScope.launch(Dispatchers.IO) {
                        missionDao.updateProgress(missionId, progress)
                        downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, progress, combinedTotalSize)
                    }
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (e.message?.contains("403") == true) throw ExpiredUrlException()
            throw e
        }

        // 2. Download Audio if available
        var audioSize = 0L
        if (mission.audioUrl != null) {
            audioSize = try {
                downloader.download(mission.audioUrl, audioFile, missionId, ChunkType.AUDIO) { progress ->
                    val currentTotal = videoSize + progress
                    val currentTime = System.currentTimeMillis()
                    val lastUpdate = lastUpdateMap[missionId] ?: 0L

                    if (currentTime - lastUpdate >= 1000L || currentTotal == combinedTotalSize) {
                        lastUpdateMap[missionId] = currentTime
                        val percent = if (combinedTotalSize > 0) (currentTotal * 100 / combinedTotalSize).toInt() else 50
                        updateNotification(missionId, "Downloading audio...", percent)
                        serviceScope.launch(Dispatchers.IO) {
                            missionDao.updateProgress(missionId, currentTotal)
                            downloadDao.updateProgress(mission.videoId, DownloadStatus.DOWNLOADING, currentTotal, combinedTotalSize)
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true) throw ExpiredUrlException()
                throw e
            }
        }
        
        val finalTotal = videoSize + audioSize
        missionDao.updateMission(mission.copy(totalBytes = finalTotal))

        // 3. Muxing
        missionDao.updateStatus(missionId, MissionStatus.MUXING)
        updateNotification(missionId, "Muxing tracks...", 95)
        
        val finalFile = File(mission.outputFilePath ?: File(getExternalFilesDir(null), "${mission.videoId}.${if (mission.format?.contains("webm", true) == true) "webm" else "mp4"}").absolutePath)
        
        if (mission.audioUrl != null) {
            muxer.mux(videoFile, audioFile, finalFile)
        } else {
            if (finalFile.exists()) finalFile.delete()
            if (!videoFile.renameTo(finalFile)) {
                videoFile.copyTo(finalFile, overwrite = true)
                videoFile.delete()
            }
        }
        
        videoFile.delete()
        audioFile.delete()

        missionDao.updateMission(mission.copy(totalBytes = finalTotal, status = MissionStatus.COMPLETED))
        downloadDao.updateProgress(mission.videoId, DownloadStatus.COMPLETED, finalTotal, finalTotal)
        updateNotification(missionId, "Download complete", 100, true)
    }

    private data class StreamMetadata(val videoUrl: String, val audioUrl: String?, val format: String, val quality: String)

    private suspend fun fetchStreamMetadata(videoId: String, preferredQuality: String?): StreamMetadata? {
        return try {
            val bundle = videoRepository.getStreamBundle(videoId)
            
            val videoStream = if (!preferredQuality.isNullOrBlank()) {
                bundle.videoStreams.find { it.quality.contains(preferredQuality, ignoreCase = true) }
                    ?: bundle.videoStreams.find { 
                        val res = it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        val prefRes = preferredQuality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0
                        res <= prefRes 
                    } ?: bundle.videoStreams.firstOrNull()
            } else {
                bundle.videoStreams.find { it.quality.contains("360") }
                    ?: bundle.videoStreams.find { it.quality.contains("480") }
                    ?: bundle.videoStreams.firstOrNull()
            }

            if (videoStream == null) return null

            val videoUrl = videoStream.url
            val format = videoStream.format
            val isWebm = format.contains("webm", ignoreCase = true)
            val audioUrl = if (videoStream.isAdaptive) {
                val compatibleStreams = bundle.audioStreams.filter { audio ->
                    if (isWebm) {
                        audio.format.contains("webm", ignoreCase = true) || 
                        audio.format.contains("opus", ignoreCase = true)
                    } else {
                        audio.format.contains("m4a", ignoreCase = true) || 
                        audio.format.contains("aac", ignoreCase = true)
                    }
                }

                val bestAudio = compatibleStreams.filter { it.trackType == "ORIGINAL" }
                    .maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?: compatibleStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                
                bestAudio?.url
            } else null

            if (videoStream.isAdaptive && audioUrl == null) {
                val progressiveStream = bundle.videoStreams.find { !it.isAdaptive }
                if (progressiveStream != null) {
                    return StreamMetadata(progressiveStream.url, null, progressiveStream.format, progressiveStream.quality)
                }
                return null
            }

            StreamMetadata(videoUrl, audioUrl, format, videoStream.quality)
        } catch (e: Exception) {
            PTLog.e("VideoDownloadService", "Failed to fetch metadata for $videoId", e)
            null
        }
    }

    private class ExpiredUrlException : Exception("URL expired")

    private fun promoteToForeground(missionId: Long) {
        foregroundMissionId = missionId
        // Update notification to be ongoing and promoted
        val notification = createInitialNotification() // Or get current state if we cached it
        startForeground(NOTIFICATION_ID_BASE + missionId.toInt(), notification)
    }

    private fun stopMission(missionId: Long) {
        activeMissions[missionId]?.cancel()
        activeMissions.remove(missionId)
        notificationManager.cancel(NOTIFICATION_ID_BASE + missionId.toInt())
        
        serviceScope.launch {
            missionDao.updateStatus(missionId, MissionStatus.PAUSED)
        }
        
        if (foregroundMissionId == missionId) {
            foregroundMissionId = -1L
            val nextMissionId = activeMissions.keys.firstOrNull()
            if (nextMissionId != null) {
                promoteToForeground(nextMissionId)
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun updateNotification(missionId: Long, content: String, progress: Int, finished: Boolean = false) {
        val builder = NotificationCompat.Builder(this, Constants.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("PlayTube Downloader")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(!finished)
            
        notificationManager.notify(NOTIFICATION_ID_BASE + missionId.toInt(), builder.build())
    }

    private fun createInitialNotification(): Notification {
        return NotificationCompat.Builder(this, Constants.DOWNLOAD_CHANNEL_ID)
            .setContentTitle("PlayTube Downloader")
            .setContentText("Initializing mission...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.DOWNLOAD_CHANNEL_ID,
                "Video Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "com.arslandaim.playtube.action.START_DOWNLOAD"
        const val ACTION_STOP = "com.arslandaim.playtube.action.STOP_DOWNLOAD"
        const val NOTIFICATION_ID_BASE = 1000
    }
}
