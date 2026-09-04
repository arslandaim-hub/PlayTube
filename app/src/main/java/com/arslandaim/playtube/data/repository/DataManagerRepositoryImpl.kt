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
import com.arslandaim.playtube.utils.PTLog
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
            PTLog.e("DataManager", "Error opening ZIP stream", e)
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
            val localPlaylists = database.localPlaylistDao().getAllLocalPlaylistsStatic()
            val localPlaylistVideos = database.localPlaylistDao().getAllLocalPlaylistVideosStatic()
            
            val prefs = PlayTubePreferences(
                isHistoryEnabled = preferencesManager.isHistoryEnabled.first(),
                isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused.first(),
                isPipEnabled = preferencesManager.isPipEnabled.first(),
                isBackgroundPlayEnabled = preferencesManager.isBackgroundPlayEnabled.first(),
                isSubtitlesEnabled = preferencesManager.isSubtitlesEnabled.first(),
                isOnboardingCompleted = preferencesManager.isOnboardingCompleted.first(),
                isSearchGridView = preferencesManager.isSearchGridView.first(),
                isAutoUpdateEnabled = preferencesManager.isAutoUpdateEnabled.first(),
                isRecommendationsPaused = preferencesManager.isRecommendationsPaused.first(),
                isPlayerGesturesEnabled = preferencesManager.isPlayerGesturesEnabled.first()
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
                localPlaylists = localPlaylists,
                localPlaylistVideos = localPlaylistVideos,
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
            PTLog.e("DataManager", "Failed to create backup", e)
            Result.failure(e)
        }
    }

    override suspend fun restoreBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var backup: PlayTubeBackup? = null

            // 1. Try reading as ZIP file first
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (!entry.isDirectory && (name.endsWith("backup.json", ignoreCase = true) || name.endsWith(".json", ignoreCase = true)) && !name.contains("__MACOSX")) {
                                val reader = InputStreamReader(zis)
                                backup = gson.fromJson(reader, PlayTubeBackup::class.java)
                                if (backup != null) break
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                PTLog.w("DataManager", "Failed to open backup as ZIP, falling back to plain JSON: ${e.message}")
            }

            // 2. Fallback: Try reading as plain JSON file directly if ZIP didn't produce backup
            if (backup == null) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = InputStreamReader(inputStream)
                    backup = gson.fromJson(reader, PlayTubeBackup::class.java)
                }
            }

            val parsedBackup = backup ?: return@withContext Result.failure(Exception("Could not parse backup file or missing backup data"))

            // Perform Room database restore inside a transaction
            database.runInTransaction {
                database.historyDao().clearHistorySync()
                database.favoriteDao().clearFavorites()
                database.playlistFavoriteDao().clearPlaylistFavorites()
                database.subscriptionDao().clearSubscriptions()
                database.searchHistoryDao().clearAllSearchHistorySync()
                database.userInterestDao().clearInterestsSync()
                database.localPlaylistDao().clearLocalPlaylistVideosSync()
                database.localPlaylistDao().clearLocalPlaylistsSync()
                database.blacklistDao().getAllBlacklistedStaticSync().forEach { 
                    database.blacklistDao().deleteSync(it) 
                }

                database.historyDao().insertAllIgnoreSync(parsedBackup.history ?: emptyList())
                database.favoriteDao().insertAllIgnoreSync(parsedBackup.favorites ?: emptyList())
                database.playlistFavoriteDao().insertAllIgnoreSync(parsedBackup.playlistFavorites ?: emptyList())
                database.subscriptionDao().insertAllIgnoreSync(parsedBackup.subscriptions ?: emptyList())
                database.searchHistoryDao().insertAllIgnoreSync(parsedBackup.searchHistory ?: emptyList())
                database.userInterestDao().insertAllIgnoreSync(parsedBackup.userInterests ?: emptyList())
                
                parsedBackup.blacklist?.let { list ->
                    list.forEach { database.blacklistDao().insertSync(it) }
                }

                parsedBackup.localPlaylists?.let { playlists ->
                    if (playlists.isNotEmpty()) {
                        database.localPlaylistDao().insertLocalPlaylistsSync(playlists)
                    }
                }

                parsedBackup.localPlaylistVideos?.let { videos ->
                    if (videos.isNotEmpty()) {
                        database.localPlaylistDao().insertLocalPlaylistVideosSync(videos)
                    }
                }
            }

            // Restore preferences if present in backup
            parsedBackup.preferences?.let { prefs ->
                coroutineScope {
                    launch { preferencesManager.setHistoryEnabled(prefs.isHistoryEnabled) }
                    launch { preferencesManager.setSearchHistoryPaused(prefs.isSearchHistoryPaused) }
                    launch { preferencesManager.setPipEnabled(prefs.isPipEnabled) }
                    launch { preferencesManager.setBackgroundPlayEnabled(prefs.isBackgroundPlayEnabled) }
                    launch { preferencesManager.setSubtitlesEnabled(prefs.isSubtitlesEnabled) }
                    launch { preferencesManager.setOnboardingCompleted(prefs.isOnboardingCompleted) }
                    launch { preferencesManager.setSearchGridView(prefs.isSearchGridView) }
                    launch { preferencesManager.setAutoUpdateEnabled(prefs.isAutoUpdateEnabled) }
                    launch { preferencesManager.setRecommendationsPaused(prefs.isRecommendationsPaused) }
                    prefs.isPlayerGesturesEnabled?.let { gestures ->
                        launch { preferencesManager.setPlayerGesturesEnabled(gestures) }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            PTLog.e("DataManager", "Failed to restore backup", e)
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
    @SerializedName("title") val title: String?,
    @SerializedName("titleUrl") val titleUrl: String?,
    @SerializedName("subtitles") val subtitles: List<TakeoutSubtitle>?,
    @SerializedName("time") val time: String?
)
data class TakeoutSubtitle(
    @SerializedName("name") val name: String?
)

data class PlayTubeBackup(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    @SerializedName("history") val history: List<HistoryEntity>? = emptyList(),
    @SerializedName("favorites") val favorites: List<FavoriteEntity>? = emptyList(),
    @SerializedName("playlistFavorites") val playlistFavorites: List<PlaylistFavoriteEntity>? = emptyList(),
    @SerializedName("subscriptions") val subscriptions: List<SubscriptionEntity>? = emptyList(),
    @SerializedName("searchHistory") val searchHistory: List<SearchHistoryEntity>? = emptyList(),
    @SerializedName("userInterests") val userInterests: List<UserInterestEntity>? = emptyList(),
    @SerializedName("blacklist") val blacklist: List<BlacklistEntity>? = emptyList(),
    @SerializedName("localPlaylists") val localPlaylists: List<LocalPlaylistEntity>? = emptyList(),
    @SerializedName("localPlaylistVideos") val localPlaylistVideos: List<LocalPlaylistVideoEntity>? = emptyList(),
    @SerializedName("preferences") val preferences: PlayTubePreferences? = null
)

data class PlayTubePreferences(
    @SerializedName("isHistoryEnabled") val isHistoryEnabled: Boolean = true,
    @SerializedName("isSearchHistoryPaused") val isSearchHistoryPaused: Boolean = false,
    @SerializedName("isPipEnabled") val isPipEnabled: Boolean = false,
    @SerializedName("isBackgroundPlayEnabled") val isBackgroundPlayEnabled: Boolean = false,
    @SerializedName("isSubtitlesEnabled") val isSubtitlesEnabled: Boolean = false,
    @SerializedName("isOnboardingCompleted") val isOnboardingCompleted: Boolean = false,
    @SerializedName("isSearchGridView") val isSearchGridView: Boolean = false,
    @SerializedName("isAutoUpdateEnabled") val isAutoUpdateEnabled: Boolean = false,
    @SerializedName("isRecommendationsPaused") val isRecommendationsPaused: Boolean = false,
    @SerializedName("isPlayerGesturesEnabled") val isPlayerGesturesEnabled: Boolean? = true
)
