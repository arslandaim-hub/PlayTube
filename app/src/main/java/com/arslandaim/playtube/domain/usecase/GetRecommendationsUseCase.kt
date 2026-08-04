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
    suspend operator fun invoke(): Result<List<VideoItem>> = coroutineScope {
        try {
            // 1. Check if Recommendations are Paused or Learned Data should be ignored
            // For now, if paused, we still show generic/trending but stop updating interests.
            // If the profile is empty, we use a larger default set.

            // 2. Multi-Seed Strategy
            val topInterests = libraryRepository.getTopInterests(20)
            val subscriptions = libraryRepository.getSubscriptions().first()
            val watchHistory = libraryRepository.getRecentHistory(100)
            
            val seedKeywords = mutableListOf<String>()
            val relatedVideoSeeds = mutableListOf<String>() // Video IDs to fetch related from
            
            // Interest-based seeds (Keywords)
            if (topInterests.isNotEmpty()) {
                seedKeywords.addAll(topInterests.take(10).map { it.keyword })
            }
            
            // Recent-based seeds (Related Videos)
            if (watchHistory.isNotEmpty()) {
                relatedVideoSeeds.addAll(watchHistory.take(3).map { it.videoId })
            }

            // Subscription-based seeds (Sample channels)
            val subTopicSeeds = if (subscriptions.isNotEmpty()) {
                subscriptions.shuffled().take(3).map { it.name }
            } else emptyList()

            // 3. Fetch Candidates from 3 sources
            val candidates = mutableListOf<VideoItem>()

            // Source A: Keyword Search (Broad)
            val searchTopics = (seedKeywords + subTopicSeeds).distinct().take(8)
            searchTopics.chunked(2).forEach { chunk ->
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
            // Enhanced: Take related videos from top 5 most recent history items
            val highPrioritySeeds = watchHistory.take(5).map { it.videoId }
            highPrioritySeeds.chunked(2).forEach { chunk ->
                val deferred = chunk.map { videoId ->
                    async {
                        try {
                            videoRepository.getStreamBundle(videoId).relatedVideos
                        } catch (e: Exception) { emptyList() }
                    }
                }
                candidates.addAll(deferred.awaitAll().flatten())
            }

            // 4. Filtering (Critical for Accuracy and User Experience)
            val watchedIds = watchHistory.map { it.videoId }.toSet()
            val blacklist = libraryRepository.getBlacklistStatic().map { it.id }.toSet()
            
            val filteredCandidates = candidates
                .distinctBy { it.id }
                .filter { (it.id !in watchedIds) && (it.id !in blacklist) }

            // 5. Scoring & Ranking
            // Simple scoring: Matches interest keywords +1, From subscribed channel +2
            val scoredVideos = filteredCandidates.map { video ->
                var score = 0f
                
                // Keyword match bonus
                topInterests.forEach { interest ->
                    if (video.title.contains(interest.keyword, ignoreCase = true)) {
                        score += interest.weight * 0.5f
                    }
                }
                
                // Subscription bonus
                if (subscriptions.any { it.channelId == video.uploaderUrl || it.name == video.uploaderName }) {
                    score += 5f
                }

                // Recency/View bonus (Heuristic)
                if (video.uploadDate?.contains("day", ignoreCase = true) == true || 
                    video.uploadDate?.contains("hour", ignoreCase = true) == true) {
                    score += 1f
                }

                video to score
            }.sortedByDescending { it.second }

            // 6. Diversification & Smart Shuffle
            // We use a pool of the top 150 scored videos.
            // Shuffling the top results ensures the feed feels fresh on every open.
            val topPool = scoredVideos.take(150).map { it.first }
            
            // Smart Shuffle logic: prioritize the top 40% of the pool but shuffle them
            val finalRecommendations = if (topPool.size >= 40) {
                val highPriority = topPool.take(30).shuffled()
                val mediumPriority = topPool.drop(30).shuffled()
                (highPriority + mediumPriority).take(60)
            } else {
                // Refined Fallback: Pull from a wider variety of "Trending" topics if candidates are low
                val fallbacks = mutableListOf<VideoItem>()
                val fallbackTopics = listOf("Trending", "New Music", "Popular News", "Tech Reviews").shuffled()
                
                fallbackTopics.take(3).forEach { topic ->
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

            Result.success(finalRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
