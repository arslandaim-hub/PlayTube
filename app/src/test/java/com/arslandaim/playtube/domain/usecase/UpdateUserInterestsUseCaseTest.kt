package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.*
import com.arslandaim.playtube.domain.repository.LibraryRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateUserInterestsUseCaseTest {

    private lateinit var fakeRepository: FakeLibraryRepository
    private lateinit var fakePreferencesManager: PreferencesManager
    private lateinit var updateUserInterestsUseCase: UpdateUserInterestsUseCase

    @Before
    fun setup() {
        fakeRepository = FakeLibraryRepository()
        fakePreferencesManager = mockk(relaxed = true) {
            io.mockk.every { isRecommendationsPaused } returns flowOf(false)
            io.mockk.every { isIncognitoMode } returns flowOf(false)
        }
        updateUserInterestsUseCase = UpdateUserInterestsUseCase(fakeRepository, fakePreferencesManager)
    }

    @Test
    fun testWeightLogicForLowWatchRatio() = runBlocking {
        val title = "Cool Tech Video"
        val baseWeight = 1.0f
        val watchRatio = 0.05f // 5% watch ratio

        updateUserInterestsUseCase(title, baseWeight, watchRatio)

        // Expected finalWeight = 1.0 * 0.1 = 0.1
        val weight = fakeRepository.interests["cool"] ?: 0f
        assertEquals(0.1f, weight, 0.001f)
    }

    @Test
    fun testWeightLogicForHighWatchRatio() = runBlocking {
        val title = "Awesome Science Documentary"
        val baseWeight = 1.0f
        val watchRatio = 0.95f // 95% watch ratio

        updateUserInterestsUseCase(title, baseWeight, watchRatio)

        // Expected finalWeight = 1.0 * 1.5 = 1.5
        val weight = fakeRepository.interests["awesome"] ?: 0f
        assertEquals(1.5f, weight, 0.001f)
    }

    @Test
    fun testStopWordFiltering() = runBlocking {
        val title = "The Best Official Video of Today"
        
        updateUserInterestsUseCase(title, 1.0f, 1.0f)

        assertTrue(!fakeRepository.interests.containsKey("the"))
        assertTrue(!fakeRepository.interests.containsKey("official"))
        assertTrue(!fakeRepository.interests.containsKey("video"))
    }

    class FakeLibraryRepository : LibraryRepository {
        val interests = mutableMapOf<String, Float>()

        override fun getHistory(): Flow<List<HistoryEntity>> = flowOf(emptyList())
        override suspend fun getRecentHistory(limit: Int): List<HistoryEntity> = emptyList()
        override suspend fun addToHistory(history: HistoryEntity) {}
        override suspend fun updateWatchProgress(videoId: String, progressMs: Long, durationMs: Long) {}
        override suspend fun removeFromHistory(videoId: String) {}
        override suspend fun clearHistory() {}
        override suspend fun getWatchProgressForVideos(videoIds: List<String>): Map<String, Float?> = emptyMap()
        override fun getFavorites(): Flow<List<FavoriteEntity>> = flowOf(emptyList())
        override fun isFavorite(videoId: String): Flow<Boolean> = flowOf(false)
        override suspend fun addToFavorites(favorite: FavoriteEntity) {}
        override suspend fun removeFromFavorites(favorite: FavoriteEntity) {}
        override fun getPlaylistFavorites(): Flow<List<PlaylistFavoriteEntity>> = flowOf(emptyList())
        override fun isPlaylistFavorite(playlistId: String): Flow<Boolean> = flowOf(false)
        override suspend fun addToPlaylistFavorites(favorite: PlaylistFavoriteEntity) {}
        override suspend fun removeFromPlaylistFavorites(favorite: PlaylistFavoriteEntity) {}
        override fun getSubscriptions(): Flow<List<SubscriptionEntity>> = flowOf(emptyList())
        override fun isSubscribed(channelId: String): Flow<Boolean> = flowOf(false)
        override suspend fun subscribe(subscription: SubscriptionEntity) {}
        override suspend fun unsubscribe(subscription: SubscriptionEntity) {}
        override suspend fun unsubscribeByIdFuzzy(channelId: String) {}
        override fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = flowOf(emptyList())
        override suspend fun addSearchQuery(query: String) {}
        override suspend fun deleteSearchQuery(query: String) {}
        override suspend fun clearSearchHistory() {}
        override suspend fun getTopInterests(limit: Int): List<UserInterestEntity> = emptyList()
        override suspend fun updateInterest(keyword: String, weightDelta: Float) {
            interests[keyword] = (interests[keyword] ?: 0f) + weightDelta
        }
        override suspend fun applyInterestDecay(decayFactor: Float) {}
        override suspend fun clearAllInterests() {}
        override suspend fun hasInterests(): Boolean = false
        override fun getBlacklist(): Flow<List<BlacklistEntity>> = flowOf(emptyList())
        override suspend fun getBlacklistStatic(): List<BlacklistEntity> = emptyList()
        override suspend fun addToBlacklist(id: String, type: BlacklistType) {}
        override suspend fun removeFromBlacklist(id: String) {}
        override suspend fun isBlacklisted(id: String): Boolean = false
    }
}
