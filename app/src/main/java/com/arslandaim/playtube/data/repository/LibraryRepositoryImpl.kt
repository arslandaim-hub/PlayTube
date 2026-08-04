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
    private val searchHistoryDao: SearchHistoryDao,
    private val userInterestDao: UserInterestDao,
    private val blacklistDao: BlacklistDao
) : LibraryRepository {

    override fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    override suspend fun getRecentHistory(limit: Int): List<HistoryEntity> = 
        historyDao.getRecentHistory(limit)

    override suspend fun addToHistory(history: HistoryEntity) {
        historyDao.insertHistory(history)
    }

    override suspend fun updateWatchProgress(videoId: String, progressMs: Long, durationMs: Long) {
        historyDao.updateProgress(videoId, progressMs, durationMs, System.currentTimeMillis())
    }

    override suspend fun removeFromHistory(videoId: String) {
        historyDao.deleteHistory(videoId)
    }

    override suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    override suspend fun getWatchProgressForVideos(videoIds: List<String>): Map<String, Float?> {
        if (videoIds.isEmpty()) return emptyMap()
        return historyDao.getWatchProgressForVideos(videoIds).associate { 
            it.videoId to if (it.durationMs > 0) it.progressMs.toFloat() / it.durationMs else null 
        }
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

    override suspend fun unsubscribeByIdFuzzy(channelId: String) {
        subscriptionDao.deleteSubscriptionByIdFuzzy(channelId)
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

    override suspend fun getTopInterests(limit: Int): List<UserInterestEntity> = 
        userInterestDao.getTopInterests(limit)

    override suspend fun updateInterest(keyword: String, weightDelta: Float) {
        val existing = userInterestDao.getInterest(keyword)
        val newWeight = (existing?.weight ?: 0f) + weightDelta
        userInterestDao.insertOrUpdate(UserInterestEntity(keyword, newWeight))
    }

    override suspend fun applyInterestDecay(decayFactor: Float) {
        userInterestDao.applyDecay(decayFactor)
        userInterestDao.purgeLowInterests()
    }

    override suspend fun clearAllInterests() {
        userInterestDao.clearInterests()
    }

    override suspend fun hasInterests(): Boolean = userInterestDao.getInterestsCount() > 0

    override fun getBlacklist(): Flow<List<BlacklistEntity>> = blacklistDao.getAllBlacklisted()

    override suspend fun getBlacklistStatic(): List<BlacklistEntity> = blacklistDao.getAllBlacklistedStatic()

    override suspend fun addToBlacklist(id: String, type: BlacklistType) {
        blacklistDao.insert(BlacklistEntity(id, type))
    }

    override suspend fun removeFromBlacklist(id: String) {
        blacklistDao.deleteById(id)
    }

    override suspend fun isBlacklisted(id: String): Boolean = blacklistDao.isBlacklisted(id)
}
