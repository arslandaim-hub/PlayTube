/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.data.local.DownloadStatus
import com.arslandaim.playtube.data.local.PlaylistFavoriteEntity
import com.arslandaim.playtube.data.local.PlaylistFavoriteDao
import com.arslandaim.playtube.domain.usecase.CancelDownloadUseCase
import com.arslandaim.playtube.domain.usecase.DeleteDownloadUseCase
import com.arslandaim.playtube.domain.usecase.GetDownloadsUseCase
import com.arslandaim.playtube.domain.usecase.GetFavoritesUseCase
import com.arslandaim.playtube.domain.usecase.GetHistoryUseCase
import com.arslandaim.playtube.domain.usecase.GetSubscriptionsUseCase
import com.arslandaim.playtube.domain.usecase.ResumeDownloadUseCase
import com.arslandaim.playtube.domain.usecase.SyncSubscriptionMetadataUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.domain.usecase.ToggleSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getDownloadsUseCase: GetDownloadsUseCase,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val getFavoritesUseCase: GetFavoritesUseCase,
    private val getSubscriptionsUseCase: GetSubscriptionsUseCase,
    private val deleteDownloadUseCase: DeleteDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val resumeDownloadUseCase: ResumeDownloadUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val toggleSubscriptionUseCase: ToggleSubscriptionUseCase,
    private val syncSubscriptionMetadataUseCase: SyncSubscriptionMetadataUseCase,
    private val playlistFavoriteDao: PlaylistFavoriteDao
) : ViewModel() {

    val downloads: StateFlow<List<DownloadEntity>> = getDownloadsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadedVideoIds: StateFlow<Set<String>> = downloads
        .map { list -> 
            list.filter { it.status == DownloadStatus.COMPLETED }
                .map { it.videoId }
                .toSet() 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val history: StateFlow<List<HistoryEntity>> = getHistoryUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteEntity>> = getFavoritesUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionEntity>> = getSubscriptionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playlists: StateFlow<List<PlaylistFavoriteEntity>> = playlistFavoriteDao.getAllPlaylistFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _subscriptionSearchQuery = MutableStateFlow("")
    val subscriptionSearchQuery: StateFlow<String> = _subscriptionSearchQuery.asStateFlow()

    private val _offlineSearchQuery = MutableStateFlow("")
    val offlineSearchQuery: StateFlow<String> = _offlineSearchQuery.asStateFlow()

    val filteredDownloads: StateFlow<List<DownloadEntity>> = combine(
        downloads,
        _offlineSearchQuery
    ) { list, query ->
        if (query.isBlank()) list
        else list.filter { it.title.contains(query, ignoreCase = true) || it.uploaderName.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storageUsage: StateFlow<StorageInfo> = downloads
        .map { list ->
            val totalBytes = list.sumOf { it.downloadedSize }
            StorageInfo(
                usedBytes = totalBytes,
                usedText = formatSize(totalBytes)
            )
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StorageInfo())

    val filteredSubscriptions: StateFlow<List<SubscriptionEntity>> = combine(
        subscriptions,
        _subscriptionSearchQuery
    ) { subs, query ->
        if (query.isBlank()) subs
        else subs.filter { it.name.contains(query, ignoreCase = true) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        syncSubscriptions()
    }

    private fun syncSubscriptions() {
        viewModelScope.launch {
            syncSubscriptionMetadataUseCase()
        }
    }

    fun onSubscriptionSearchQueryChange(query: String) {
        _subscriptionSearchQuery.value = query
    }

    fun onOfflineSearchQueryChange(query: String) {
        _offlineSearchQuery.value = query
    }

    fun clearWatchedDownloads() {
        viewModelScope.launch {
            val watchedIds = history.value
                .filter { it.durationMs > 0 && it.progressMs.toFloat() / it.durationMs > 0.9f }
                .map { it.videoId }
                .toSet()
            
            downloads.value.forEach { download ->
                if (watchedIds.contains(download.videoId) && download.status == DownloadStatus.COMPLETED) {
                    deleteDownload(download.videoId)
                }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun deleteDownload(videoId: String) {
        viewModelScope.launch {
            deleteDownloadUseCase(videoId)
        }
    }

    fun cancelDownload(videoId: String) {
        viewModelScope.launch {
            cancelDownloadUseCase(videoId)
        }
    }

    fun resumeDownload(videoId: String) {
        viewModelScope.launch {
            resumeDownloadUseCase(videoId)
        }
    }

    fun removeFavorite(favorite: FavoriteEntity) {
        viewModelScope.launch {
            toggleFavoriteUseCase(favorite)
        }
    }

    fun toggleSubscription(subscription: SubscriptionEntity) {
        viewModelScope.launch {
            toggleSubscriptionUseCase(subscription)
        }
    }
}

data class StorageInfo(
    val usedBytes: Long = 0,
    val usedText: String = "0 B"
)
