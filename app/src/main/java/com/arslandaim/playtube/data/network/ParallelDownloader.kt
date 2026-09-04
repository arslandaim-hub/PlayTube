/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.data.local.DownloadChunkEntity
import com.arslandaim.playtube.data.local.MissionDao
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicLong

class ParallelDownloader(
    private val client: OkHttpClient,
    private val missionDao: MissionDao
) {
    private val semaphore = Semaphore(4) // Max 4 parallel chunks
    private val CHUNK_SIZE = 4L * 1024 * 1024 // 4MB

    suspend fun download(
        url: String,
        outputFile: File,
        missionId: Long,
        type: com.arslandaim.playtube.data.local.ChunkType,
        onProgress: (Long) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val totalSize = getFileSize(url)
        if (totalSize == -403L) throw Exception("403")
        if (totalSize <= 0) throw Exception("Failed to get file size")
        
        // Pre-allocate file
        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(totalSize)
        }

        val existingChunks = missionDao.getChunksForMission(missionId).filter { it.type == type }
        val chunks = if (existingChunks.isEmpty()) {
            createChunks(missionId, totalSize, type)
        } else {
            existingChunks
        }

        val downloadedBytes = AtomicLong(chunks.sumOf { it.bytesDownloaded })
        
        val deferreds = chunks.filter { !it.isCompleted }.map { chunk ->
            async {
                downloadChunkWithRetry(url, outputFile, chunk, downloadedBytes, onProgress)
            }
        }

        deferreds.awaitAll()
        totalSize
    }

    private suspend fun createChunks(
        missionId: Long,
        totalSize: Long,
        type: com.arslandaim.playtube.data.local.ChunkType
    ): List<DownloadChunkEntity> {
        val chunks = mutableListOf<DownloadChunkEntity>()
        var start = 0L
        var index = 0
        while (start < totalSize) {
            val end = (start + CHUNK_SIZE - 1).coerceAtMost(totalSize - 1)
            val chunk = DownloadChunkEntity(
                missionId = missionId,
                chunkIndex = index++,
                startByte = start,
                endByte = end,
                type = type
            )
            val id = missionDao.insertChunk(chunk)
            chunks.add(chunk.copy(id = id))
            start = end + 1
        }
        return chunks
    }

    private suspend fun downloadChunkWithRetry(
        url: String,
        outputFile: File,
        chunk: DownloadChunkEntity,
        downloadedBytes: AtomicLong,
        onProgress: (Long) -> Unit
    ) {
        var attempt = 0
        val maxRetries = 5
        while (attempt < maxRetries) {
            try {
                downloadChunk(url, outputFile, chunk, downloadedBytes, onProgress)
                return
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e.message?.contains("403") == true || e.message?.contains("Forbidden") == true) throw e
                
                attempt++
                if (attempt >= maxRetries) throw e
                val delayTime = (1000L * attempt * attempt).coerceAtMost(15000L)
                PTLog.w("ParallelDownloader", "Retrying chunk ${chunk.chunkIndex} (attempt $attempt/$maxRetries): ${e.message}")
                delay(delayTime)
            }
        }
    }

    private suspend fun downloadChunk(
        url: String,
        outputFile: File,
        chunk: DownloadChunkEntity,
        downloadedBytes: AtomicLong,
        onProgress: (Long) -> Unit
    ) {
        semaphore.acquire()
        try {
            val start = chunk.startByte + chunk.bytesDownloaded
            if (start > chunk.endByte) {
                missionDao.updateChunkProgress(chunk.id, chunk.bytesDownloaded, true)
                return
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                .header("Accept-Encoding", "identity")
                .header("Range", "bytes=$start-${chunk.endByte}")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Chunk download failed: ${response.code}")
                val body = response.body ?: throw Exception("Empty body")
                
                RandomAccessFile(outputFile, "rw").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var currentChunkProgress = chunk.bytesDownloaded
                    
                    body.byteStream().use { input ->
                        while (input.read(buffer).also { read = it } != -1) {
                            currentCoroutineContext().ensureActive()
                            raf.write(buffer, 0, read)
                            currentChunkProgress += read
                            downloadedBytes.addAndGet(read.toLong())
                            onProgress(downloadedBytes.get())
                            
                            // Periodically update DB to avoid excessive writes
                            if (currentChunkProgress % (512 * 1024) == 0L) {
                                missionDao.updateChunkProgress(chunk.id, currentChunkProgress, false)
                            }
                        }
                    }
                    missionDao.updateChunkProgress(chunk.id, currentChunkProgress, true)
                }
            }
        } finally {
            semaphore.release()
        }
    }

    suspend fun getFileSize(url: String): Long = withContext(Dispatchers.IO) {
        var attempt = 0
        val maxRetries = 5
        while (attempt < maxRetries) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                    .header("Accept-Encoding", "identity")
                    .head()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 403) return@withContext -403L
                    if (response.isSuccessful) {
                        val len = response.header("Content-Length")?.toLongOrNull() ?: response.body?.contentLength() ?: 0L
                        if (len > 0L) return@withContext len
                    }
                    
                    // Fallback: Try GET with Range 0-0 if HEAD fails or returned 0
                    val getRequest = Request.Builder()
                        .url(url)
                        .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                        .header("Accept-Encoding", "identity")
                        .header("Range", "bytes=0-0")
                        .build()

                    client.newCall(getRequest).execute().use { getResponse ->
                        if (getResponse.code == 403) return@withContext -403L
                        val contentRange = getResponse.header("Content-Range")
                        val total = contentRange?.substringAfterLast("/")?.toLongOrNull() ?: 0L
                        if (total > 0L) return@withContext total
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                PTLog.w("ParallelDownloader", "Failed to probe file size (attempt ${attempt + 1}/$maxRetries): ${e.message}")
            }
            attempt++
            if (attempt < maxRetries) {
                delay(1000L * attempt)
            }
        }
        0L
    }
}
