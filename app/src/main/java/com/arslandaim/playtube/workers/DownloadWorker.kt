/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.domain.repository.VideoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val okHttpClient: OkHttpClient,
    private val videoRepository: VideoRepository
) : CoroutineWorker(context, params) {

    companion object {
        private val downloadMutex = Mutex()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val videoId = inputData.getString("videoId") ?: return@withContext Result.failure()
        var videoUrl = inputData.getString("url")
        var audioUrl = inputData.getString("audioUrl")
        val title = inputData.getString("title") ?: "video"
        var format = inputData.getString("format") ?: "mp4"

        // Wait for turn in queue
        downloadMutex.withLock {
            // Re-check status inside lock. If it's no longer WAITING/PENDING (e.g. cancelled/paused), skip.
            val currentDownload = downloadDao.getDownloadById(videoId)
            val currentStatus = currentDownload?.status
            if (currentStatus != DownloadStatus.WAITING && currentStatus != DownloadStatus.PENDING) {
                android.util.Log.d("DownloadWorker", "Skipping $videoId as status is $currentStatus")
                return@withContext Result.success()
            }

            // Fetch metadata if missing
            if (videoUrl == null) {
                try {
                    android.util.Log.d("DownloadWorker", "Fetching metadata for $videoId")
                    val bundle = videoRepository.getStreamBundle(videoId)
                    val videoStream = bundle.videoStreams.find { it.quality.contains("360") }
                        ?: bundle.videoStreams.find { it.quality.contains("480") }
                        ?: bundle.videoStreams.firstOrNull()

                    if (videoStream == null) {
                        downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
                        return@withContext Result.failure()
                    }

                    videoUrl = videoStream.url
                    format = videoStream.format
                    audioUrl = if (videoStream.isAdaptive) {
                        bundle.audioStreams.find {
                            it.format.contains("m4a", ignoreCase = true) ||
                            it.format.contains("aac", ignoreCase = true)
                        }?.url ?: bundle.audioStreams.firstOrNull()?.url
                    } else null

                    // Update DB with fetched metadata
                    currentDownload?.let {
                        downloadDao.updateDownload(it.copy(
                            videoUrl = videoUrl,
                            audioUrl = audioUrl,
                            quality = videoStream.quality,
                            format = format
                        ))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DownloadWorker", "Failed to fetch metadata for $videoId", e)
                    downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
                    return@withContext Result.failure()
                }
            }
            
            // Once we have the lock and metadata, start downloading
            doDownload(videoId, videoUrl!!, audioUrl, title, format)
        }
    }

    private suspend fun doDownload(
        videoId: String,
        videoUrl: String,
        audioUrl: String?,
        title: String,
        format: String
    ): Result {
        setForeground(createForegroundInfo(title, 0))

        val extension = if (format.contains("webm", ignoreCase = true)) "webm" else "mp4"
        val finalFile = File(applicationContext.getExternalFilesDir(null), "$videoId.$extension")
        val videoFile = if (audioUrl != null) File(applicationContext.cacheDir, "${videoId}_video.tmp") else finalFile
        val audioFile = if (audioUrl != null) File(applicationContext.cacheDir, "${videoId}_audio.tmp") else null

        return try {
            android.util.Log.d("DownloadWorker", "Starting work for $videoId: $title")
            // Pre-calculate total size to avoid jumps in UI
            var totalVideoSize = getRemoteFileSize(videoUrl)
            var totalAudioSize = audioUrl?.let { getRemoteFileSize(it) } ?: 0L
            var combinedTotalSize = totalVideoSize + totalAudioSize
            
            android.util.Log.d("DownloadWorker", "Sizes: video=$totalVideoSize, audio=$totalAudioSize, combined=$combinedTotalSize")

            downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, 0, combinedTotalSize)

            // Download Video
            android.util.Log.d("DownloadWorker", "Downloading video to ${videoFile.absolutePath}")
            val videoSize = downloadFile(videoUrl, videoFile, videoId, title, 0, audioUrl != null, combinedTotalSize)
            if (videoSize <= 0) {
                android.util.Log.e("DownloadWorker", "Video download failed: size=$videoSize")
                return Result.failure()
            }
            android.util.Log.d("DownloadWorker", "Video downloaded successfully: $videoSize bytes")
            
            // If initial HEAD request failed, update total size now that we have it from GET
            if (totalVideoSize == 0L) {
                totalVideoSize = videoSize
                combinedTotalSize = totalVideoSize + totalAudioSize
                android.util.Log.d("DownloadWorker", "Updated combinedTotalSize after video GET: $combinedTotalSize")
            }

            // Download Audio if needed
            val audioSize = if (audioUrl != null && audioFile != null) {
                android.util.Log.d("DownloadWorker", "Downloading audio to ${audioFile.absolutePath}")
                val size = downloadFile(audioUrl, audioFile, videoId, title, videoSize, true, combinedTotalSize)
                
                if (size <= 0) {
                    android.util.Log.e("DownloadWorker", "Audio download failed: size=$size")
                    throw Exception("Audio download failed")
                }
                android.util.Log.d("DownloadWorker", "Audio downloaded successfully: $size bytes")

                // Update total size if HEAD failed for audio
                val finalTotal = videoSize + size
                downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, finalTotal, finalTotal)
                android.util.Log.d("DownloadWorker", "Updated total size after audio GET: $finalTotal")
                size
            } else 0L

            if (audioUrl != null && audioFile != null) {
                // Mux Video and Audio
                android.util.Log.d("DownloadWorker", "Muxing video and audio for $videoId")
                setForeground(createForegroundInfo("Muxing $title", 99))
                muxVideoAudio(videoFile, audioFile, finalFile, format)
                android.util.Log.d("DownloadWorker", "Muxing complete, deleting temporary files")
                videoFile.delete()
                audioFile.delete()
            }

            android.util.Log.d("DownloadWorker", "Download task completed successfully for $videoId")
            val totalFinalSize = videoSize + audioSize
            downloadDao.updateProgress(videoId, DownloadStatus.COMPLETED, totalFinalSize, totalFinalSize)
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Work failed for $videoId: ${e.message}", e)
            if (videoFile.exists() && audioUrl != null) {
                android.util.Log.d("DownloadWorker", "Deleting failed video temp file: ${videoFile.name}")
                videoFile.delete()
            }
            audioFile?.let { if (it.exists()) {
                android.util.Log.d("DownloadWorker", "Deleting failed audio temp file: ${it.name}")
                it.delete()
            } }
            if (finalFile.exists()) {
                android.util.Log.d("DownloadWorker", "Deleting potentially corrupted final file: ${finalFile.name}")
                finalFile.delete()
            }
            
            // Only update to FAILED if it wasn't explicitly PAUSED by the user/system
            val currentDownload = downloadDao.getDownloadById(videoId)
            if (currentDownload?.status != DownloadStatus.PAUSED) {
                downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
            }

            Result.failure()
        }
    }

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    private suspend fun downloadFile(
        url: String,
        file: File,
        videoId: String,
        title: String,
        previousDownloaded: Long,
        isPart: Boolean,
        combinedTotalSize: Long
    ): Long {
        android.util.Log.d("DownloadWorker", "Starting download: $url")
        
        val totalSize = getRemoteFileSize(url)
        
        // If range download is possible (size > 0), use parallel chunks
        if (totalSize > 1024 * 1024) { // > 1MB
            val result = downloadParallel(url, file, totalSize, videoId, title, previousDownloaded, isPart, combinedTotalSize)
            if (result > 0) return result
            android.util.Log.w("DownloadWorker", "Parallel download failed or not supported, falling back to single stream")
        }

        // Fallback to single stream download
        return downloadSingleStream(url, file, videoId, title, previousDownloaded, isPart, combinedTotalSize)
    }

    private suspend fun downloadParallel(
        url: String,
        file: File,
        totalSize: Long,
        videoId: String,
        title: String,
        previousDownloaded: Long,
        isPart: Boolean,
        combinedTotalSize: Long
    ): Long = withContext(Dispatchers.IO) {
        val numChunks = 4
        val chunkSize = totalSize / numChunks
        val downloadedBytes = AtomicLong(0L)
        var lastUpdateTime = 0L

        try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(totalSize)
            }

            val jobs = (0 until numChunks).map { i ->
                val start = i * chunkSize
                val end = if (i == numChunks - 1) totalSize - 1 else (i + 1) * chunkSize - 1
                
                async {
                    downloadChunk(url, file, start, end, videoId, title, isPart, previousDownloaded, combinedTotalSize, downloadedBytes) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastUpdateTime > 500) {
                            lastUpdateTime = currentTime
                            val currentTotalDownloaded = previousDownloaded + downloadedBytes.get()
                            val effectiveTotalSize = if (combinedTotalSize > 0) combinedTotalSize else (previousDownloaded + totalSize)
                            val displayProgress = if (effectiveTotalSize > 0) ((currentTotalDownloaded * 100) / effectiveTotalSize).toInt() else 0
                            
                            setForeground(createForegroundInfo(
                                if (isPart) "Downloading $title (part)" else "Downloading $title",
                                displayProgress
                            ))
                            downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, currentTotalDownloaded, effectiveTotalSize)
                        }
                    }
                }
            }

            jobs.awaitAll()
            downloadedBytes.get()
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Parallel download failed: ${e.message}")
            -1
        }
    }

    private suspend fun downloadSingleStream(
        url: String,
        file: File,
        videoId: String,
        title: String,
        previousDownloaded: Long,
        isPart: Boolean,
        combinedTotalSize: Long
    ): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Download failed: ${response.code}")
                val body = response.body ?: throw Exception("Empty body")
                val totalSize = body.contentLength()
                var downloaded = 0L
                var lastUpdateTime = 0L

                file.outputStream().use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (isStopped) throw CancellationException("Worker stopped")
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime > 500) {
                                lastUpdateTime = currentTime
                                val currentTotalDownloaded = previousDownloaded + downloaded
                                val effectiveTotalSize = if (combinedTotalSize > 0) combinedTotalSize else (previousDownloaded + totalSize)
                                val displayProgress = if (effectiveTotalSize > 0) ((currentTotalDownloaded * 100) / effectiveTotalSize).toInt() else 0
                                
                                setForeground(createForegroundInfo(
                                    if (isPart) "Downloading $title" else "Downloading $title",
                                    displayProgress
                                ))
                                downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, currentTotalDownloaded, effectiveTotalSize.coerceAtLeast(currentTotalDownloaded))
                            }
                        }
                    }
                }
                downloaded
            }
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Single stream download failed", e)
            -1
        }
    }

    private suspend fun downloadChunk(
        url: String,
        file: File,
        start: Long,
        end: Long,
        videoId: String,
        title: String,
        isPart: Boolean,
        previousDownloaded: Long,
        combinedTotalSize: Long,
        downloadedBytes: AtomicLong,
        onProgress: suspend () -> Unit
    ) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .addHeader("Range", "bytes=$start-$end")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Chunk download failed: ${response.code}")

            val body = response.body ?: throw Exception("Empty response body for chunk")

            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(start)
                body.byteStream().use { input ->
                    val buffer = ByteArray(65536)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isStopped) throw CancellationException("Worker stopped")
                        raf.write(buffer, 0, bytesRead)
                        downloadedBytes.addAndGet(bytesRead.toLong())
                        onProgress()
                    }
                }
            }
        }
    }

    private fun getRemoteFileSize(url: String): Long {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .head()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                val length = response.header("Content-Length")?.toLong() ?: response.body.contentLength()
                if (response.isSuccessful && length > 0) length else 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun muxVideoAudio(videoFile: File, audioFile: File, outputFile: File, videoFormat: String) {
        android.util.Log.d("DownloadWorker", "Starting robust muxing for ${outputFile.name}")
        
        val tempOutputFile = File(applicationContext.cacheDir, "${outputFile.name}.mux.tmp")
        if (tempOutputFile.exists()) tempOutputFile.delete()
        
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            val outputFormat = if (videoFormat.contains("webm", ignoreCase = true)) {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            } else {
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            }

            muxer = MediaMuxer(tempOutputFile.absolutePath, outputFormat)

            // Video Track Setup
            var videoTrackIndex = -1
            var videoFormatSelected: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true) {
                    videoExtractor.selectTrack(i)
                    videoTrackIndex = muxer.addTrack(format)
                    videoFormatSelected = format
                    break
                }
            }

            // Audio Track Setup
            var audioTrackIndex = -1
            var audioFormatSelected: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                if (format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                    audioExtractor.selectTrack(i)
                    audioTrackIndex = muxer.addTrack(format)
                    audioFormatSelected = format
                    break
                }
            }

            if (videoTrackIndex == -1 || audioTrackIndex == -1) {
                throw Exception("Required tracks missing: video=$videoTrackIndex, audio=$audioTrackIndex")
            }

            muxer.start()

            val buffer = ByteBuffer.allocate(2 * 1024 * 1024) // 2MB buffer
            val bufferInfo = MediaCodec.BufferInfo()

            // Process Video
            videoExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            var videoStartTime: Long = -1
            while (true) {
                bufferInfo.size = videoExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                
                if (videoStartTime == -1L) videoStartTime = videoExtractor.sampleTime
                bufferInfo.presentationTimeUs = videoExtractor.sampleTime - videoStartTime
                bufferInfo.offset = 0
                bufferInfo.flags = videoExtractor.sampleFlags
                
                muxer.writeSampleData(videoTrackIndex, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Process Audio
            audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            var audioStartTime: Long = -1
            while (true) {
                bufferInfo.size = audioExtractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break
                
                if (audioStartTime == -1L) audioStartTime = audioExtractor.sampleTime
                bufferInfo.presentationTimeUs = audioExtractor.sampleTime - audioStartTime
                bufferInfo.offset = 0
                bufferInfo.flags = audioExtractor.sampleFlags
                
                muxer.writeSampleData(audioTrackIndex, buffer, bufferInfo)
                audioExtractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            // Finalize
            if (outputFile.exists()) outputFile.delete()
            if (!tempOutputFile.renameTo(outputFile)) {
                // Fallback: Copy instead of rename
                tempOutputFile.inputStream().use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempOutputFile.delete()
            }
            android.util.Log.d("DownloadWorker", "Muxing successful: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            android.util.Log.e("DownloadWorker", "Muxing critical error: ${e.message}", e)
            if (tempOutputFile.exists()) tempOutputFile.delete()
            throw e
        } finally {
            try { muxer?.release() } catch (ex: Exception) {}
            try { videoExtractor.release() } catch (ex: Exception) {}
            try { audioExtractor.release() } catch (ex: Exception) {}
        }
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val id = "download_channel"
        val notification = NotificationCompat.Builder(context, id)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1, notification)
        }
    }
}
