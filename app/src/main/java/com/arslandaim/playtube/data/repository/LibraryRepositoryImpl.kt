/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LibraryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao,
    private val favoriteDao: FavoriteDao,
    private val playlistFavoriteDao: PlaylistFavoriteDao,
    private val subscriptionDao: SubscriptionDao,
    private val searchHistoryDao: SearchHistoryDao
) : LibraryRepository {

    override fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    override suspend fun addToHistory(history: HistoryEntity) {
        historyDao.insertHistory(history)
    }

    override suspend fun removeFromHistory(videoId: String) {
        historyDao.deleteHistory(videoId)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    override fun getFavorites(): Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()

    override fun isFavorite(videoId: String): Flow<Boolean> = favoriteDao.isFavorite(videoId)

    override suspend fun addToFavorites(favorite: FavoriteEntity) {
        favoriteDao.insertFavorite(favorite)
    }

    override suspend fun removeFromFavorites(favorite: FavoriteEntity) {
        favoriteDao.deleteFavorite(favorite)
    }

    override fun getPlaylistFavorites(): Flow<List<PlaylistFavoriteEntity>> = 
        playlistFavoriteDao.getAllPlaylistFavorites()

    override fun isPlaylistFavorite(playlistId: String): Flow<Boolean> = 
        playlistFavoriteDao.isPlaylistFavorite(playlistId)

    override suspend fun addToPlaylistFavorites(favorite: PlaylistFavoriteEntity) {
        playlistFavoriteDao.insertPlaylistFavorite(favorite)
    }

    override suspend fun removeFromPlaylistFavorites(favorite: PlaylistFavoriteEntity) {
        playlistFavoriteDao.deletePlaylistFavorite(favorite)
    }

    override fun getSubscriptions(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    override fun isSubscribed(channelId: String): Flow<Boolean> = subscriptionDao.isSubscribed(channelId)

    override suspend fun subscribe(subscription: SubscriptionEntity) {
        subscriptionDao.insertSubscription(subscription)
    }

    override suspend fun unsubscribe(subscription: SubscriptionEntity) {
        subscriptionDao.deleteSubscription(subscription)
    }

    override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = searchHistoryDao.getAllSearchHistory()

    override suspend fun addSearchQuery(query: String) {
        searchHistoryDao.insertSearchQuery(SearchHistoryEntity(query))
    }

    override suspend fun deleteSearchQuery(query: String) {
        searchHistoryDao.deleteSearchQuery(query)
    }

    override suspend fun clearSearchHistory() {
        searchHistoryDao.clearAllSearchHistory()
    }
}
