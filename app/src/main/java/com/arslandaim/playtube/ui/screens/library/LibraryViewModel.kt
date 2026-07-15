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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val syncSubscriptionMetadataUseCase: SyncSubscriptionMetadataUseCase
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

    private val _subscriptionSearchQuery = MutableStateFlow("")
    val subscriptionSearchQuery: StateFlow<String> = _subscriptionSearchQuery.asStateFlow()

    val filteredSubscriptions: StateFlow<List<SubscriptionEntity>> = combine(
        subscriptions,
        _subscriptionSearchQuery
    ) { subs, query ->
        if (query.isBlank()) subs
        else subs.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
