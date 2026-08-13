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
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.PTLog
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
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
        val preferredQuality = inputData.getString("quality")

        // Wait for turn in queue
        downloadMutex.withLock {
            // Re-check status inside lock. If it's no longer WAITING/PENDING (e.g. cancelled/paused), skip.
            val currentDownload = downloadDao.getDownloadById(videoId)
            val currentStatus = currentDownload?.status
            if (currentStatus != DownloadStatus.WAITING && currentStatus != DownloadStatus.PENDING) {
                PTLog.d("DownloadWorker", "Skipping $videoId as status is $currentStatus")
                return@withContext Result.success()
            }

            // Fetch metadata if missing (typically for playlists)
            if (videoUrl == null) {
                val metadata = fetchStreamMetadata(videoId, preferredQuality)
                if (metadata == null) {
                    downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
                    return@withContext Result.failure()
                }

                videoUrl = metadata.videoUrl
                format = metadata.format
                audioUrl = metadata.audioUrl

                // Update DB with fetched metadata
                currentDownload?.let {
                    downloadDao.updateDownload(it.copy(
                        videoUrl = videoUrl,
                        audioUrl = audioUrl,
                        quality = metadata.quality,
                        format = format
                    ))
                }
            }
            
            // Once we have the lock and metadata, start downloading
            try {
                doDownload(videoId, videoUrl!!, audioUrl, title, format)
            } catch (e: ExpiredUrlException) {
                PTLog.w("DownloadWorker", "URL expired for $videoId, re-fetching metadata...")
                try {
                    val metadata = fetchStreamMetadata(videoId, preferredQuality)
                    if (metadata == null) return@withContext Result.failure()
                    
                    videoUrl = metadata.videoUrl
                    audioUrl = metadata.audioUrl
                    
                    // Update DB with fresh URLs
                    currentDownload?.let {
                        downloadDao.updateDownload(it.copy(videoUrl = videoUrl, audioUrl = audioUrl))
                    }

                    // Retry download once
                    doDownload(videoId, videoUrl!!, audioUrl, title, format)
                } catch (ex: Exception) {
                    PTLog.e("DownloadWorker", "Failed to refresh metadata for $videoId", ex)
                    downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
                    Result.failure()
                }
            }
        }
    }

    private data class StreamMetadata(val videoUrl: String, val audioUrl: String?, val format: String, val quality: String)

    private suspend fun fetchStreamMetadata(videoId: String, preferredQuality: String?): StreamMetadata? {
        return try {
            PTLog.d("DownloadWorker", "Fetching metadata for $videoId (Preferred: $preferredQuality)")
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
            val audioUrl = if (videoStream.isAdaptive) {
                bundle.audioStreams.find {
                    it.format.contains("m4a", ignoreCase = true) ||
                    it.format.contains("aac", ignoreCase = true)
                }?.url ?: bundle.audioStreams.firstOrNull()?.url
            } else null

            if (videoStream.isAdaptive && audioUrl == null) {
                PTLog.e("DownloadWorker", "Adaptive stream selected but no audio found for $videoId")
                return null
            }

            StreamMetadata(videoUrl, audioUrl, format, videoStream.quality)
        } catch (e: Exception) {
            PTLog.e("DownloadWorker", "Failed to fetch metadata for $videoId", e)
            null
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
            PTLog.d("DownloadWorker", "Starting work for $videoId: $title")
            // Pre-calculate total size to avoid jumps in UI
            var totalVideoSize = getRemoteFileSize(videoUrl)
            var totalAudioSize = audioUrl?.let { getRemoteFileSize(it) } ?: 0L
            var combinedTotalSize = totalVideoSize + totalAudioSize
            
            PTLog.d("DownloadWorker", "Sizes: video=$totalVideoSize, audio=$totalAudioSize, combined=$combinedTotalSize")

            val currentDownload = downloadDao.getDownloadById(videoId)
            val initialDownloadedSize = currentDownload?.downloadedSize ?: 0L
            
            downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, initialDownloadedSize, combinedTotalSize)

            // Download Video
            PTLog.d("DownloadWorker", "Downloading video to ${videoFile.absolutePath}")
            val videoSize = downloadFile(videoUrl, videoFile, videoId, title, 0, audioUrl != null, combinedTotalSize)
            if (videoSize <= 0) {
                PTLog.e("DownloadWorker", "Video download failed: size=$videoSize")
                return Result.failure()
            }
            PTLog.d("DownloadWorker", "Video downloaded successfully: $videoSize bytes")
            
            // If initial HEAD request failed, update total size now that we have it from GET
            if (totalVideoSize == 0L) {
                totalVideoSize = videoSize
                combinedTotalSize = totalVideoSize + totalAudioSize
                PTLog.d("DownloadWorker", "Updated combinedTotalSize after video GET: $combinedTotalSize")
            }

            // Download Audio if needed
            val audioSize = if (audioUrl != null && audioFile != null) {
                PTLog.d("DownloadWorker", "Downloading audio to ${audioFile.absolutePath}")
                val size = downloadFile(audioUrl, audioFile, videoId, title, videoSize, true, combinedTotalSize)
                
                if (size <= 0) {
                    PTLog.e("DownloadWorker", "Audio download failed: size=$size")
                    throw Exception("Audio download failed")
                }
                PTLog.d("DownloadWorker", "Audio downloaded successfully: $size bytes")

                // Update total size if HEAD failed for audio
                val finalTotal = videoSize + size
                downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, finalTotal, finalTotal)
                PTLog.d("DownloadWorker", "Updated total size after audio GET: $finalTotal")
                size
            } else 0L

            if (audioUrl != null && audioFile != null) {
                // Mux Video and Audio
                PTLog.d("DownloadWorker", "Muxing video and audio for $videoId")
                setForeground(createForegroundInfo("Muxing $title", 99))
                muxVideoAudio(videoFile, audioFile, finalFile, format)
                PTLog.d("DownloadWorker", "Muxing complete, deleting temporary files")
                videoFile.delete()
                audioFile.delete()
            }

            PTLog.d("DownloadWorker", "Download task completed successfully for $videoId")
            val totalFinalSize = videoSize + audioSize
            downloadDao.updateProgress(videoId, DownloadStatus.COMPLETED, totalFinalSize, totalFinalSize)
            Result.success()
        } catch (e: Exception) {
            PTLog.e("DownloadWorker", "Work failed for $videoId: ${e.message}", e)

            if (e is ExpiredUrlException) throw e

            // Only update to FAILED if it wasn't explicitly PAUSED by the user/system
            val currentDownload = downloadDao.getDownloadById(videoId)
            if (currentDownload?.status != DownloadStatus.PAUSED) {
                downloadDao.updateProgress(videoId, DownloadStatus.FAILED, 0, 0)
            }

            Result.failure()
        } finally {
            // Only clean up temp files IF successful. 
            // On failure or pause, we keep them for resuming.
            // Exception: If we finished muxing, we already deleted them.
        }
    }

    private val userAgent = Constants.DEFAULT_USER_AGENT

    private suspend fun downloadFile(
        url: String,
        file: File,
        videoId: String,
        title: String,
        previousDownloaded: Long,
        isPart: Boolean,
        combinedTotalSize: Long
    ): Long {
        PTLog.d("DownloadWorker", "Starting download: $url")
        
        val totalSize = getRemoteFileSize(url)
        val existingSize = if (file.exists()) file.length() else 0L
        
        if (totalSize > 0 && existingSize >= totalSize) {
            PTLog.d("DownloadWorker", "File already fully downloaded: ${file.name}")
            // Update progress for the skipped file
            val currentDownloaded = previousDownloaded + existingSize
            val effectiveTotalSize = if (combinedTotalSize > 0) combinedTotalSize else currentDownloaded
            downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, currentDownloaded, effectiveTotalSize)
            return existingSize
        }
        
        // Use parallel chunks for ANY file larger than 1MB to avoid single-connection throttling
        if (totalSize > 1024 * 1024) { 
            val result = downloadParallel(url, file, totalSize, videoId, title, previousDownloaded, isPart, combinedTotalSize)
            if (result > 0) return result
            PTLog.w("DownloadWorker", "Parallel download failed, falling back to single stream")
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
        // Aggressive chunk scaling to bypass YouTube's per-connection speed limits
        val numChunks = when {
            totalSize < 512 * 1024 -> 1 
            totalSize < 5 * 1024 * 1024 -> 4 // 4 chunks for audio or small videos
            totalSize < 20 * 1024 * 1024 -> 8 
            totalSize < 100 * 1024 * 1024 -> 12
            else -> 16 // Max 16 chunks for high-res videos to maximize throughput
        }
        
        if (numChunks == 1) return@withContext -1 
        
        val chunkSize = totalSize / numChunks
        val downloadedBytes = AtomicLong(0L)
        var lastUpdateTime = 0L
        var lastProgressUpdate = 0
        val progressMutex = Mutex()
        val partFiles = mutableListOf<File>()

        try {
            val jobs = (0 until numChunks).map { i ->
                val start = i * chunkSize
                val end = if (i == numChunks - 1) totalSize - 1 else (i + 1) * chunkSize - 1
                val partFile = File(applicationContext.cacheDir, "${file.name}.part$i")
                partFiles.add(partFile)
                
                async {
                    downloadChunk(url, partFile, start, end, videoId, title, isPart, previousDownloaded, combinedTotalSize, downloadedBytes) {
                        progressMutex.withLock {
                            val currentTime = System.currentTimeMillis()
                            val currentTotalDownloaded = previousDownloaded + downloadedBytes.get()
                            val effectiveTotalSize = if (combinedTotalSize > 0) combinedTotalSize else (previousDownloaded + totalSize)
                            val progress = if (effectiveTotalSize > 0) ((currentTotalDownloaded * 100) / effectiveTotalSize).toInt() else 0

                            if (isStopped) throw CancellationException("Worker stopped during parallel download")

                            if (progress > lastProgressUpdate || currentTime - lastUpdateTime > 1000) {
                                lastUpdateTime = currentTime
                                lastProgressUpdate = progress
                                
                                setForeground(createForegroundInfo(
                                    if (isPart) "Downloading $title" else "Downloading $title",
                                    progress
                                ))
                                downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, currentTotalDownloaded, effectiveTotalSize)
                            }
                        }
                    }
                }
            }

            jobs.awaitAll()
            
            // CLEAN TRUNCATE: Ensure the file is fresh before merging to avoid corruption
            if (file.exists()) file.delete()

            // Sequentially merge all part files into the final destination
            file.outputStream().use { output ->
                partFiles.forEach { part ->
                    part.inputStream().use { input ->
                        input.copyTo(output)
                    }
                    part.delete()
                }
            }

            downloadedBytes.get()
        } catch (e: Exception) {
            PTLog.e("DownloadWorker", "Parallel download failed: ${e.message}")
            // Do NOT delete part files here to allow resuming later
            if (e is ExpiredUrlException) throw e
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
        val existingSize = if (file.exists()) file.length() else 0L
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .apply {
                if (existingSize > 0) {
                    addHeader("Range", "bytes=$existingSize-")
                }
            }
            .build()

        var response: okhttp3.Response? = null
        try {
            val result = withTimeoutOrNull(300_000L) {
                response = okHttpClient.newCall(request).execute()
                
                // Handle 416 (Range Not Satisfiable) - usually means file changed or offset is wrong
                if (response!!.code == 416) {
                    PTLog.w("DownloadWorker", "Range not satisfiable for $videoId, restarting full download")
                    file.delete()
                    return@withTimeoutOrNull downloadSingleStream(url, file, videoId, title, previousDownloaded, isPart, combinedTotalSize)
                }

                if (!response!!.isSuccessful) throw Exception("Download failed: ${response!!.code}")
                
                val body = response!!.body ?: throw Exception("Empty body")
                val totalSize = if (existingSize > 0) {
                    // Content-Length in a 206 response is the size of the range, not the whole file
                    existingSize + body.contentLength()
                } else {
                    body.contentLength()
                }

                var downloaded = existingSize
                var lastUpdateTime = 0L

                java.io.FileOutputStream(file, existingSize > 0).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(128 * 1024)
                        var bytesRead: Int
                        var lastProgressUpdate = 0
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            
                            val effectiveTotalSize = if (combinedTotalSize > 0) combinedTotalSize else (previousDownloaded + totalSize)
                            val progress = if (effectiveTotalSize > 0) (((previousDownloaded + downloaded) * 100) / effectiveTotalSize).toInt() else 0
                            
                            if (isStopped) throw CancellationException("Worker stopped during single stream download")

                            val currentTime = System.currentTimeMillis()
                            if (progress > lastProgressUpdate || currentTime - lastUpdateTime > 1000) {
                                lastUpdateTime = currentTime
                                lastProgressUpdate = progress
                                
                                setForeground(createForegroundInfo("Downloading $title", progress))
                                downloadDao.updateProgress(videoId, DownloadStatus.DOWNLOADING, previousDownloaded + downloaded, effectiveTotalSize.coerceAtLeast(previousDownloaded + downloaded))
                            }
                        }
                        output.flush()
                    }
                }
                downloaded
            } ?: throw IOException("Network read timeout for $videoId")
            result
        } catch (e: Exception) {
            PTLog.e("DownloadWorker", "Single stream download failed", e)
            -1
        } finally {
            response?.close()
        }
    }

    private suspend fun downloadChunk(
        url: String,
        partFile: File,
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
        val existingSize = if (partFile.exists()) partFile.length() else 0L
        if (existingSize >= (end - start + 1)) {
            // Already fully downloaded this chunk
            downloadedBytes.addAndGet(existingSize)
            onProgress()
            return
        }

        val actualStart = start + existingSize
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .addHeader("Range", "bytes=$actualStart-$end")
            .build()

        var response: okhttp3.Response? = null
        try {
            withTimeoutOrNull(300_000L) {
                response = okHttpClient.newCall(request).execute()
                
                if (response!!.code == 403) throw ExpiredUrlException()
                
                if (response!!.code == 416) {
                    PTLog.w("DownloadWorker", "Range not satisfiable for chunk of $videoId, restarting chunk")
                    partFile.delete()
                    return@withTimeoutOrNull downloadChunk(url, partFile, start, end, videoId, title, isPart, previousDownloaded, combinedTotalSize, downloadedBytes, onProgress)
                }

                if (!response!!.isSuccessful) throw Exception("Chunk download failed: ${response!!.code}")

                val body = response!!.body ?: throw Exception("Empty response body for chunk")

                downloadedBytes.addAndGet(existingSize)

                java.io.FileOutputStream(partFile, existingSize > 0).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(65536)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive()
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes.addAndGet(bytesRead.toLong())
                            onProgress()
                        }
                    }
                }
            } ?: throw IOException("Network read timeout for chunk of $videoId")
        } finally {
            response?.close()
        }
    }

    private class ExpiredUrlException : Exception("URL expired")


    private fun getRemoteFileSize(url: String): Long {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .head()
                .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (response.code == 403) throw ExpiredUrlException()
                val length = response.header("Content-Length")?.toLong() ?: response.body.contentLength()
                if (response.isSuccessful && length > 0) length else 0L
            }
        } catch (e: Exception) {
            if (e is ExpiredUrlException) throw e
            0L
        }
    }

    private fun muxVideoAudio(videoFile: File, audioFile: File, outputFile: File, videoFormat: String) {
        PTLog.d("DownloadWorker", "Starting robust muxing for ${outputFile.name}")
        
        val tempOutputFile = File(applicationContext.cacheDir, "${outputFile.name}.mux.tmp")
        if (tempOutputFile.exists()) tempOutputFile.delete()
        
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            try {
                videoExtractor.setDataSource(videoFile.absolutePath)
                audioExtractor.setDataSource(audioFile.absolutePath)
            } catch (e: Exception) {
                PTLog.e("DownloadWorker", "Failed to set data source for muxer. Files might be corrupted.")
                videoFile.delete()
                audioFile.delete()
                throw IllegalStateException("Media stream corrupted or unreadable")
            }

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
                if (isStopped) throw CancellationException("Worker stopped during video muxing")
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
                if (isStopped) throw CancellationException("Worker stopped during audio muxing")
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
            PTLog.d("DownloadWorker", "Muxing successful: ${outputFile.length()} bytes")
        } catch (e: Exception) {
            PTLog.e("DownloadWorker", "Muxing critical error: ${e.message}", e)
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
