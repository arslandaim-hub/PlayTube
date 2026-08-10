/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
    private val videoRepository: VideoRepository,
    private val libraryRepository: LibraryRepository
) {
    private var cachedRecommendations: List<VideoItem>? = null
    private var lastFetchTime = 0L
    private val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes

    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<VideoItem>> = coroutineScope {
        if (!forceRefresh && cachedRecommendations != null && (System.currentTimeMillis() - lastFetchTime < CACHE_DURATION)) {
            return@coroutineScope Result.success(cachedRecommendations!!)
        }

        try {
            // 2. Multi-Seed Strategy
            val topInterests = libraryRepository.getTopInterests(20)
            val subscriptions = libraryRepository.getSubscriptions().first()
            val watchHistory = libraryRepository.getRecentHistory(100)
            
            val seedKeywords = mutableListOf<String>()
            val relatedVideoSeeds = mutableListOf<String>() // Video IDs to fetch related from
            
            // Interest-based seeds (Keywords)
            if (topInterests.isNotEmpty()) {
                seedKeywords.addAll(topInterests.take(5).map { it.keyword }) // Reduced from 10 to 5 for speed
            }
            
            // Recent-based seeds (Related Videos)
            if (watchHistory.isNotEmpty()) {
                relatedVideoSeeds.addAll(watchHistory.take(3).map { it.videoId })
            }

            // Subscription-based seeds (Sample channels)
            val subTopicSeeds = if (subscriptions.isNotEmpty()) {
                subscriptions.shuffled().take(2).map { it.name } // Reduced from 3 to 2
            } else emptyList()

            // 3. Fetch Candidates from 3 sources
            val candidates = mutableListOf<VideoItem>()

            // Source A: Keyword Search (Broad)
            val searchTopics = (seedKeywords + subTopicSeeds).distinct().take(6) // Reduced from 8 to 6
            searchTopics.chunked(3).forEach { chunk -> // Increased chunk size for parallelization
                val deferred = chunk.map { topic ->
                    async {
                        try {
                            searchRepository.search(topic).items
                                .filterIsInstance<SearchItem.Video>()
                                .map { it.video }
                        } catch (e: Exception) { emptyList() }
                    }
                }
                candidates.addAll(deferred.awaitAll().flatten())
            }

            // Source B: Related Videos (Deep/Specific)
            val highPrioritySeeds = watchHistory.take(3).map { it.videoId } // Reduced from 5 to 3
            highPrioritySeeds.forEach { videoId ->
                val deferred = async {
                    try {
                        videoRepository.getStreamBundle(videoId).relatedVideos
                    } catch (e: Exception) { emptyList() }
                }
                candidates.addAll(deferred.await())
            }

            // 4. Filtering
            val watchedIds = watchHistory.map { it.videoId }.toSet()
            val blacklist = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
            
            val filteredCandidates = candidates
                .distinctBy { it.id }
                .filter { (it.id !in watchedIds) && (it.id !in blacklist) }

            // 5. Scoring & Ranking
            val scoredVideos = filteredCandidates.map { video ->
                var score = 0f
                
                topInterests.forEach { interest ->
                    if (video.title.contains(interest.keyword, ignoreCase = true)) {
                        score += interest.weight * 0.5f
                    }
                }
                
                if (subscriptions.any { it.channelId == video.uploaderUrl || it.name == video.uploaderName }) {
                    score += 5f
                }

                if (video.uploadDate?.contains("day", ignoreCase = true) == true || 
                    video.uploadDate?.contains("hour", ignoreCase = true) == true) {
                    score += 1f
                }

                video to score
            }.sortedByDescending { it.second }

            // 6. Diversification & Smart Shuffle
            val topPool = scoredVideos.take(120).map { it.first }
            
            val finalRecommendations = if (topPool.size >= 40) {
                val highPriority = topPool.take(30).shuffled()
                val mediumPriority = topPool.drop(30).shuffled()
                (highPriority + mediumPriority).take(60)
            } else {
                val fallbacks = mutableListOf<VideoItem>()
                val fallbackTopics = listOf("Trending", "New Music", "Popular News").shuffled()
                
                fallbackTopics.take(2).forEach { topic ->
                    try {
                        fallbacks.addAll(
                            searchRepository.search(topic).items
                                .filterIsInstance<SearchItem.Video>()
                                .map { it.video }
                        )
                    } catch (e: Exception) { /* ignore */ }
                }

                (topPool + fallbacks)
                    .distinctBy { it.id }
                    .filter { (it.id !in watchedIds) && (it.id !in blacklist) }
                    .shuffled()
                    .take(60)
            }

            cachedRecommendations = finalRecommendations
            lastFetchTime = System.currentTimeMillis()
            Result.success(finalRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
