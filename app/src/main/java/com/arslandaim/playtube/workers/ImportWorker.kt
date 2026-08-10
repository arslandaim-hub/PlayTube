/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.Data
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.PlayTubeDatabase
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.UpdateUserInterestsUseCase
import com.arslandaim.playtube.utils.PTLog
import com.arslandaim.playtube.utils.VideoUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@HiltWorker
class ImportWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val database: PlayTubeDatabase,
    private val videoRepository: VideoRepository,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase,
    private val gson: Gson
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URI = "uri"
        const val KEY_TYPE = "type"
        const val TYPE_HISTORY = "history"
        const val TYPE_SUBSCRIPTIONS = "subscriptions"
        
        const val PROGRESS_KEY = "progress"
        const val STATUS_KEY = "status"
        const val COUNT_KEY = "count"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriString = inputData.getString(KEY_URI) ?: return@withContext Result.failure()
        val type = inputData.getString(KEY_TYPE) ?: return@withContext Result.failure()
        val uri = Uri.parse(uriString)

        try {
            when (type) {
                TYPE_HISTORY -> importHistory(uri)
                TYPE_SUBSCRIPTIONS -> importSubscriptions(uri)
                else -> Result.failure()
            }
        } catch (e: Exception) {
            PTLog.e("ImportWorker", "Import failed", e)
            Result.failure(Data.Builder().putString("error", e.message).build())
        }
    }

    private suspend fun importHistory(uri: Uri): Result {
        updateProgress(0f, "Analyzing history...")
        
        var importedCount = 0
        val inputStream = getStreamForFile(uri, "watch-history.json") ?: return Result.failure()

        inputStream.use { stream ->
            val reader = JsonReader(InputStreamReader(stream, "UTF-8"))
            reader.beginArray()
            
            val batchSize = 100
            val currentBatch = mutableListOf<HistoryEntity>()
            
            while (reader.hasNext()) {
                if (isStopped) return Result.retry()
                
                val item = gson.fromJson<com.arslandaim.playtube.data.repository.TakeoutHistoryItem>(reader, com.arslandaim.playtube.data.repository.TakeoutHistoryItem::class.java)
                val videoId = VideoUtils.extractVideoId(item.titleUrl)
                
                if (videoId.isNotBlank() && item.title != null) {
                    val entity = HistoryEntity(
                        videoId = videoId,
                        title = item.title.removePrefix("Watched "),
                        thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                        uploaderName = item.subtitles?.firstOrNull()?.name ?: "Unknown",
                        timestamp = parseTakeoutTime(item.time)
                    )
                    currentBatch.add(entity)
                    updateUserInterestsUseCase(entity.title, 0.3f, 1.0f)
                }

                if (currentBatch.size >= batchSize) {
                    database.historyDao().insertAllIgnoreSync(currentBatch)
                    importedCount += currentBatch.size
                    currentBatch.clear()
                    updateProgress(0.5f, "Imported $importedCount history items...")
                }
            }
            
            if (currentBatch.isNotEmpty()) {
                database.historyDao().insertAllIgnoreSync(currentBatch)
                importedCount += currentBatch.size
            }
            
            reader.endArray()
        }
        
        return Result.success(Data.Builder().putInt(COUNT_KEY, importedCount).build())
    }

    private suspend fun importSubscriptions(uri: Uri): Result {
        updateProgress(0f, "Analyzing subscriptions...")
        
        val channelIds = mutableListOf<String>()
        val inputStream = getStreamForFile(uri, "subscriptions.csv") ?: return Result.failure()

        inputStream.use { stream ->
            val reader = stream.bufferedReader()
            reader.readLine() // Skip header
            
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 2) {
                    val channelId = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                    if (channelId.startsWith("UC")) {
                        channelIds.add(channelId)
                    }
                }
            }
        }

        if (channelIds.isEmpty()) return Result.success(Data.Builder().putInt(COUNT_KEY, 0).build())

        var importedCount = 0
        val totalCount = channelIds.size

        channelIds.chunked(5).forEachIndexed { chunkIndex, chunk ->
            if (isStopped) return@forEachIndexed
            
            val startIndex = chunkIndex * 5
            updateProgress(
                startIndex.toFloat() / totalCount,
                "Importing channels ${startIndex + 1} to ${minOf(startIndex + 5, totalCount)} of $totalCount..."
            )

            val results = chunk.map { channelId ->
                try {
                    val details = videoRepository.getChannelInfo(channelId)
                    SubscriptionEntity(
                        channelId = channelId,
                        name = details.name,
                        thumbnailUrl = details.avatarUrl,
                        subscriberCount = details.subscriberCount
                    )
                } catch (e: Exception) {
                    SubscriptionEntity(channelId = channelId, name = "Unknown Channel")
                }
            }
            
            results.forEach { database.subscriptionDao().insertSubscription(it) }
            importedCount += results.size
            delay(300L) // Prevent throttling
        }

        return Result.success(Data.Builder().putInt(COUNT_KEY, importedCount).build())
    }

    private fun getStreamForFile(uri: Uri, targetFileName: String): InputStream? {
        val cr = context.contentResolver
        val zipInputStream = ZipInputStream(cr.openInputStream(uri))
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (entry.name.endsWith(targetFileName, ignoreCase = true)) {
                return zipInputStream
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        return null
    }

    private fun parseTakeoutTime(time: String?): Long {
        if (time == null) return System.currentTimeMillis()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                java.time.Instant.parse(time).toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun updateProgress(progress: Float, status: String) {
        val data = Data.Builder()
            .putFloat(PROGRESS_KEY, progress)
            .putString(STATUS_KEY, status)
            .build()
        setProgress(data)
        setForeground(createForegroundInfo(status, (progress * 100).toInt()))
    }

    private fun createForegroundInfo(status: String, progress: Int): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, "download_channel")
            .setContentTitle("Importing Data")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(2, notification)
        }
    }
}
