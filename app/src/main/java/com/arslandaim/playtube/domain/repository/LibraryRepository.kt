/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.PlaylistFavoriteEntity
import com.arslandaim.playtube.data.local.SearchHistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    // History
    fun getHistory(): Flow<List<HistoryEntity>>
    suspend fun addToHistory(history: HistoryEntity)
    suspend fun updateWatchProgress(videoId: String, progressMs: Long, durationMs: Long)
    suspend fun removeFromHistory(videoId: String)
    suspend fun clearHistory()

    // Favorites
    fun getFavorites(): Flow<List<FavoriteEntity>>
    fun isFavorite(videoId: String): Flow<Boolean>
    suspend fun addToFavorites(favorite: FavoriteEntity)
    suspend fun removeFromFavorites(favorite: FavoriteEntity)

    // Playlist Favorites
    fun getPlaylistFavorites(): Flow<List<PlaylistFavoriteEntity>>
    fun isPlaylistFavorite(playlistId: String): Flow<Boolean>
    suspend fun addToPlaylistFavorites(favorite: PlaylistFavoriteEntity)
    suspend fun removeFromPlaylistFavorites(favorite: PlaylistFavoriteEntity)

    // Subscriptions
    fun getSubscriptions(): Flow<List<SubscriptionEntity>>
    fun isSubscribed(channelId: String): Flow<Boolean>
    suspend fun subscribe(subscription: SubscriptionEntity)
    suspend fun unsubscribe(subscription: SubscriptionEntity)
    suspend fun unsubscribeByIdFuzzy(channelId: String)

    // Search History
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>
    suspend fun addSearchQuery(query: String)
    suspend fun deleteSearchQuery(query: String)
    suspend fun clearSearchHistory()
}
