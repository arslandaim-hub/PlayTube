/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.content.Context
import android.net.Uri
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.domain.repository.DataManagerRepository
import com.arslandaim.playtube.domain.repository.ImportProgress
import com.arslandaim.playtube.domain.usecase.UpdateUserInterestsUseCase
import com.arslandaim.playtube.workers.ImportWorker
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*
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
    private val gson: Gson
) : DataManagerRepository {

    override fun importTakeoutHistory(uri: Uri): Flow<ImportProgress> = flow {
        emit(ImportProgress.Loading(0f, "Queuing background import..."))
        
        val workRequest = OneTimeWorkRequestBuilder<ImportWorker>()
            .setInputData(workDataOf(
                ImportWorker.KEY_URI to uri.toString(),
                ImportWorker.KEY_TYPE to ImportWorker.TYPE_HISTORY
            ))
            .build()
        
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)

        // Observe the work status and emit progress
        val workInfoFlow = workManager.getWorkInfoByIdFlow(workRequest.id)
        
        workInfoFlow.collect { workInfo ->
            if (workInfo == null) return@collect
            
            val progress = workInfo.progress.getFloat(ImportWorker.PROGRESS_KEY, 0f)
            val status = workInfo.progress.getString(ImportWorker.STATUS_KEY) ?: "Importing..."
            
            when (workInfo.state) {
                androidx.work.WorkInfo.State.RUNNING -> {
                    emit(ImportProgress.Loading(progress, status))
                }
                androidx.work.WorkInfo.State.SUCCEEDED -> {
                    val count = workInfo.outputData.getInt(ImportWorker.COUNT_KEY, 0)
                    emit(ImportProgress.Success(count))
                    currentCoroutineContext().cancel()
                }
                androidx.work.WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString("error") ?: "Background import failed"
                    emit(ImportProgress.Error(error))
                    currentCoroutineContext().cancel()
                }
                else -> {}
            }
        }
    }.catch { e -> 
        if (e !is CancellationException) {
            emit(ImportProgress.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    override fun importTakeoutSubscriptions(uri: Uri): Flow<ImportProgress> = flow {
        emit(ImportProgress.Loading(0f, "Queuing background import..."))
        
        val workRequest = OneTimeWorkRequestBuilder<ImportWorker>()
            .setInputData(workDataOf(
                ImportWorker.KEY_URI to uri.toString(),
                ImportWorker.KEY_TYPE to ImportWorker.TYPE_SUBSCRIPTIONS
            ))
            .build()
        
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)

        val workInfoFlow = workManager.getWorkInfoByIdFlow(workRequest.id)
        
        workInfoFlow.collect { workInfo ->
            if (workInfo == null) return@collect
            
            val progress = workInfo.progress.getFloat(ImportWorker.PROGRESS_KEY, 0f)
            val status = workInfo.progress.getString(ImportWorker.STATUS_KEY) ?: "Importing..."
            
            when (workInfo.state) {
                androidx.work.WorkInfo.State.RUNNING -> {
                    emit(ImportProgress.Loading(progress, status))
                }
                androidx.work.WorkInfo.State.SUCCEEDED -> {
                    val count = workInfo.outputData.getInt(ImportWorker.COUNT_KEY, 0)
                    emit(ImportProgress.Success(count))
                    currentCoroutineContext().cancel()
                }
                androidx.work.WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString("error") ?: "Background import failed"
                    emit(ImportProgress.Error(error))
                    currentCoroutineContext().cancel()
                }
                else -> {}
            }
        }
    }.catch { e ->
        if (e !is CancellationException) {
            emit(ImportProgress.Error(e.message ?: "Unknown error"))
        }
    }.flowOn(Dispatchers.IO)

    private fun getStreamForFile(uri: Uri, targetFileName: String): java.io.InputStream? {
        val cr = context.contentResolver
        val mimeType = cr.getType(uri)
        val isZip = mimeType == "application/zip" || 
                    uri.path?.endsWith(".zip", ignoreCase = true) == true ||
                    mimeType == "application/x-zip-compressed"
        
        if (!isZip) return try { cr.openInputStream(uri) } catch (e: Exception) { null }
        
        // Handle ZIP
        return try {
            val zipInputStream = ZipInputStream(cr.openInputStream(uri))
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                // Google Takeout often nests files like "Takeout/YouTube and YouTube Music/history/watch-history.json"
                val name = entry.name
                if (name.endsWith(targetFileName, ignoreCase = true)) {
                    return zipInputStream // Caller must close it
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            null
        } catch (e: Exception) {
            android.util.Log.e("DataManager", "Error opening ZIP stream", e)
            null
        }
    }

    override suspend fun createBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val history = database.historyDao().getAllHistoryStatic()
            val favorites = database.favoriteDao().getAllFavoritesStatic()
            val playlistFavorites = database.playlistFavoriteDao().getAllPlaylistFavoritesStatic()
            val subscriptions = database.subscriptionDao().getAllSubscriptionsStatic()
            val searchHistory = database.searchHistoryDao().getAllSearchHistoryStatic()
            val userInterests = database.userInterestDao().getAllInterestsStatic()
            val blacklist = database.blacklistDao().getAllBlacklistedStatic()
            
            val prefs = PlayTubePreferences(
                isHistoryEnabled = preferencesManager.isHistoryEnabled.first(),
                isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused.first(),
                isPipEnabled = preferencesManager.isPipEnabled.first(),
                isBackgroundPlayEnabled = preferencesManager.isBackgroundPlayEnabled.first(),
                isSubtitlesEnabled = preferencesManager.isSubtitlesEnabled.first(),
                isOnboardingCompleted = preferencesManager.isOnboardingCompleted.first(),
                isSearchGridView = preferencesManager.isSearchGridView.first(),
                isAutoUpdateEnabled = preferencesManager.isAutoUpdateEnabled.first(),
                isRecommendationsPaused = preferencesManager.isRecommendationsPaused.first()
            )

            val backup = PlayTubeBackup(
                version = 2,
                timestamp = System.currentTimeMillis(),
                history = history,
                favorites = favorites,
                playlistFavorites = playlistFavorites,
                subscriptions = subscriptions,
                searchHistory = searchHistory,
                userInterests = userInterests,
                blacklist = blacklist,
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
                            ?: return@withContext Result.failure(Exception("Failed to parse backup file"))
                        
                        database.runInTransaction {
                            database.historyDao().clearHistorySync()
                            database.favoriteDao().clearFavorites()
                            database.playlistFavoriteDao().clearPlaylistFavorites()
                            database.subscriptionDao().clearSubscriptions()
                            database.searchHistoryDao().clearAllSearchHistorySync()
                            database.userInterestDao().clearInterestsSync()
                            database.blacklistDao().getAllBlacklistedStaticSync().forEach { 
                                database.blacklistDao().deleteSync(it) 
                            }

                            database.historyDao().insertAllIgnoreSync(backup.history)
                            database.favoriteDao().insertAllIgnoreSync(backup.favorites)
                            database.playlistFavoriteDao().insertAllIgnoreSync(backup.playlistFavorites)
                            database.subscriptionDao().insertAllIgnoreSync(backup.subscriptions)
                            database.searchHistoryDao().insertAllIgnoreSync(backup.searchHistory)
                            database.userInterestDao().insertAllIgnoreSync(backup.userInterests)
                            backup.blacklist?.let { list ->
                                list.forEach { database.blacklistDao().insertSync(it) }
                            }
                        }

                        // Restore preferences outside transaction as DataStore is not part of Room transaction
                        coroutineScope {
                            launch { preferencesManager.setHistoryEnabled(backup.preferences.isHistoryEnabled) }
                            launch { preferencesManager.setSearchHistoryPaused(backup.preferences.isSearchHistoryPaused) }
                            launch { preferencesManager.setPipEnabled(backup.preferences.isPipEnabled) }
                            launch { preferencesManager.setBackgroundPlayEnabled(backup.preferences.isBackgroundPlayEnabled) }
                            launch { preferencesManager.setSubtitlesEnabled(backup.preferences.isSubtitlesEnabled) }
                            launch { preferencesManager.setOnboardingCompleted(backup.preferences.isOnboardingCompleted) }
                            launch { preferencesManager.setSearchGridView(backup.preferences.isSearchGridView) }
                            launch { preferencesManager.setAutoUpdateEnabled(backup.preferences.isAutoUpdateEnabled) }
                            launch { preferencesManager.setRecommendationsPaused(backup.preferences.isRecommendationsPaused) }
                        }
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
    val blacklist: List<BlacklistEntity>? = emptyList(),
    val preferences: PlayTubePreferences
)

data class PlayTubePreferences(
    val isHistoryEnabled: Boolean,
    val isSearchHistoryPaused: Boolean,
    val isPipEnabled: Boolean,
    val isBackgroundPlayEnabled: Boolean,
    val isSubtitlesEnabled: Boolean,
    val isOnboardingCompleted: Boolean,
    val isSearchGridView: Boolean,
    val isAutoUpdateEnabled: Boolean = false,
    val isRecommendationsPaused: Boolean = false
)
