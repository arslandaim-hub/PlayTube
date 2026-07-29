/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.content.Context
import android.net.Uri
import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.domain.repository.DataManagerRepository
import com.arslandaim.playtube.domain.repository.ImportProgress
import com.arslandaim.playtube.domain.usecase.UpdateUserInterestsUseCase
import com.arslandaim.playtube.utils.VideoUtils
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManagerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: PlayTubeDatabase,
    private val preferencesManager: PreferencesManager,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase,
    private val gson: Gson
) : DataManagerRepository {

    override fun importTakeoutHistory(uri: Uri): Flow<ImportProgress> = flow {
        emit(ImportProgress.Loading(0f, "Analyzing file..."))
        
        var importedCount = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))
                reader.beginArray()
                
                val batchSize = 100
                val currentBatch = mutableListOf<HistoryEntity>()
                
                while (reader.hasNext()) {
                    val item = gson.fromJson<TakeoutHistoryItem>(reader, TakeoutHistoryItem::class.java)
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
                        
                        // Intelligent Weighting
                        updateUserInterestsUseCase(entity.title, 0.3f, 1.0f)
                    }

                    if (currentBatch.size >= batchSize) {
                        database.historyDao().insertAllIgnore(currentBatch)
                        importedCount += currentBatch.size
                        currentBatch.clear()
                        emit(ImportProgress.Loading(0.5f, "Imported $importedCount items..."))
                    }
                }
                
                if (currentBatch.isNotEmpty()) {
                    database.historyDao().insertAllIgnore(currentBatch)
                    importedCount += currentBatch.size
                }
                
                reader.endArray()
            }
            emit(ImportProgress.Success(importedCount))
        } catch (e: Exception) {
            emit(ImportProgress.Error(e.message ?: "Unknown error during history import"))
        }
    }.flowOn(Dispatchers.IO)

    override fun importTakeoutSubscriptions(uri: Uri): Flow<ImportProgress> = flow {
        emit(ImportProgress.Loading(0f, "Parsing subscriptions..."))
        
        var importedCount = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = inputStream.bufferedReader()
                reader.readLine() // Skip CSV header
                
                val batchSize = 50
                val currentBatch = mutableListOf<SubscriptionEntity>()
                
                reader.forEachLine { line ->
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val channelId = parts[0].trim().removePrefix("\"").removeSuffix("\"")
                        val channelTitle = parts[1].trim().removePrefix("\"").removeSuffix("\"")
                        
                        if (channelId.startsWith("UC")) {
                            currentBatch.add(
                                SubscriptionEntity(
                                    channelId = channelId,
                                    name = channelTitle
                                )
                            )
                        }
                    }
                    
                    if (currentBatch.size >= batchSize) {
                        runBlocking {
                            database.subscriptionDao().insertAllIgnore(currentBatch)
                        }
                        importedCount += currentBatch.size
                        currentBatch.clear()
                    }
                }
                
                database.subscriptionDao().insertAllIgnore(currentBatch)
                importedCount += currentBatch.size
            }
            emit(ImportProgress.Success(importedCount))
        } catch (e: Exception) {
            emit(ImportProgress.Error(e.message ?: "Unknown error during subscription import"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val history = database.historyDao().getAllHistoryStatic()
            val favorites = database.favoriteDao().getAllFavoritesStatic()
            val playlistFavorites = database.playlistFavoriteDao().getAllPlaylistFavoritesStatic()
            val subscriptions = database.subscriptionDao().getAllSubscriptionsStatic()
            val searchHistory = database.searchHistoryDao().getAllSearchHistoryStatic()
            val userInterests = database.userInterestDao().getAllInterestsStatic()
            
            val prefs = PlayTubePreferences(
                isHistoryEnabled = preferencesManager.isHistoryEnabled.first(),
                isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused.first(),
                isPipEnabled = preferencesManager.isPipEnabled.first(),
                isBackgroundPlayEnabled = preferencesManager.isBackgroundPlayEnabled.first(),
                isSubtitlesEnabled = preferencesManager.isSubtitlesEnabled.first(),
                isOnboardingCompleted = preferencesManager.isOnboardingCompleted.first(),
                isSearchGridView = preferencesManager.isSearchGridView.first()
            )

            val backup = PlayTubeBackup(
                version = 1,
                timestamp = System.currentTimeMillis(),
                history = history,
                favorites = favorites,
                playlistFavorites = playlistFavorites,
                subscriptions = subscriptions,
                searchHistory = searchHistory,
                userInterests = userInterests,
                preferences = prefs
            )

            val json = gson.toJson(backup)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zos ->
                    val entry = ZipEntry("backup.json")
                    zos.putNextEntry(entry)
                    val writer = OutputStreamWriter(zos)
                    writer.write(json)
                    writer.flush()
                    zos.closeEntry()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun restoreBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    val entry = zis.nextEntry
                    if (entry?.name == "backup.json") {
                        val reader = InputStreamReader(zis)
                        val backup = gson.fromJson(reader, PlayTubeBackup::class.java)
                        
                        database.runInTransaction {
                            database.historyDao().clearHistorySync()
                            database.favoriteDao().clearFavorites()
                            database.playlistFavoriteDao().clearPlaylistFavorites()
                            database.subscriptionDao().clearSubscriptions()
                            database.searchHistoryDao().clearAllSearchHistorySync()
                            database.userInterestDao().clearInterests()

                            database.historyDao().insertAllIgnoreSync(backup.history)
                            database.favoriteDao().insertAllIgnoreSync(backup.favorites)
                            database.playlistFavoriteDao().insertAllIgnoreSync(backup.playlistFavorites)
                            database.subscriptionDao().insertAllIgnoreSync(backup.subscriptions)
                            database.searchHistoryDao().insertAllIgnoreSync(backup.searchHistory)
                            database.userInterestDao().insertAllIgnoreSync(backup.userInterests)
                        }

                        // Restore preferences
                        preferencesManager.setHistoryEnabled(backup.preferences.isHistoryEnabled)
                        preferencesManager.setSearchHistoryPaused(backup.preferences.isSearchHistoryPaused)
                        preferencesManager.setPipEnabled(backup.preferences.isPipEnabled)
                        preferencesManager.setBackgroundPlayEnabled(backup.preferences.isBackgroundPlayEnabled)
                        preferencesManager.setSubtitlesEnabled(backup.preferences.isSubtitlesEnabled)
                        preferencesManager.setOnboardingCompleted(backup.preferences.isOnboardingCompleted)
                        preferencesManager.setSearchGridView(backup.preferences.isSearchGridView)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTakeoutTime(time: String?): Long {
        if (time == null) return System.currentTimeMillis()
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(time).toEpochMilli()
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

data class TakeoutHistoryItem(
    val title: String?,
    val titleUrl: String?,
    val subtitles: List<TakeoutSubtitle>?,
    val time: String?
)
data class TakeoutSubtitle(val name: String?)

data class PlayTubeBackup(
    val version: Int,
    val timestamp: Long,
    val history: List<HistoryEntity>,
    val favorites: List<FavoriteEntity>,
    val playlistFavorites: List<PlaylistFavoriteEntity>,
    val subscriptions: List<SubscriptionEntity>,
    val searchHistory: List<SearchHistoryEntity>,
    val userInterests: List<UserInterestEntity>,
    val preferences: PlayTubePreferences
)

data class PlayTubePreferences(
    val isHistoryEnabled: Boolean,
    val isSearchHistoryPaused: Boolean,
    val isPipEnabled: Boolean,
    val isBackgroundPlayEnabled: Boolean,
    val isSubtitlesEnabled: Boolean,
    val isOnboardingCompleted: Boolean,
    val isSearchGridView: Boolean
)
