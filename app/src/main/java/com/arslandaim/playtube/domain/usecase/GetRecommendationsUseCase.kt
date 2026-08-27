/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.recommendation.NeuroDiscovery
import com.arslandaim.playtube.domain.recommendation.NeuroScoring
import com.arslandaim.playtube.domain.recommendation.NeuroTokenizer
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.Calendar
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
                seedKeywords.addAll(topInterests.take(5).map { it.keyword }) 
            }
            
            // Recent-based seeds (Related Videos)
            if (watchHistory.isNotEmpty()) {
                relatedVideoSeeds.addAll(watchHistory.take(3).map { it.videoId })
            }

            // Subscription-based seeds (Sample channels)
            val subTopicSeeds = if (subscriptions.isNotEmpty()) {
                subscriptions.shuffled().take(2).map { it.name } 
            } else emptyList()

            // 3. Fetch Candidates from sources
            val candidates = mutableListOf<VideoItem>()

            // Source A: Keyword Search (Broad)
            val discoveryMode = NeuroDiscovery.shouldExplore()
            val baseTopics = (seedKeywords + subTopicSeeds).distinct().take(6)
            
            // Fix Cold Start: Ensure searchTopics is never empty
            val searchTopics = when {
                discoveryMode -> (baseTopics + NeuroDiscovery.getDiscoverySeeds(seedKeywords)).distinct().take(8)
                baseTopics.isEmpty() -> {
                    // Default to major categories if user has no interests yet
                    listOf("Gaming", "Music", "Science", "Technology", "News").shuffled().take(3)
                }
                else -> baseTopics
            }

            searchTopics.chunked(3).forEach { chunk -> 
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
            val highPrioritySeeds = watchHistory.take(3).map { it.videoId } 
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
            
            var filteredCandidates = candidates
                .distinctBy { it.id }
                .filter { (it.id !in watchedIds) && (it.id !in blacklist) }

            // Final Fallback: If still empty or very small, pull directly from the curated Trending kiosk
            if (filteredCandidates.size < 20) {
                try {
                    val trending = videoRepository.getTrendingVideos().items
                        .filter { (it.id !in watchedIds) && (it.id !in blacklist) }
                    filteredCandidates = (filteredCandidates + trending).distinctBy { it.id }
                } catch (e: Exception) { /* ignore fallback errors */ }
            }

            // 5. Scoring & Ranking using NeuroEngine
            val userProfile = topInterests.associate { it.keyword to it.weight }
            val recentChannelCounts = watchHistory.groupingBy { it.uploaderName }.eachCount()
            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            val isWeekend = calendar.get(Calendar.DAY_OF_WEEK).let { it == Calendar.SATURDAY || it == Calendar.SUNDAY }

            val scoredVideos = filteredCandidates.map { video ->
                val candidateVector = NeuroTokenizer.tokenize("${video.title} ${video.uploaderName}")
                val uploadTimestamp = parseUploadDate(video.uploadDate)
                val isSubscription = subscriptions.any { it.channelId == video.uploaderUrl || it.name == video.uploaderName }
                
                var score = NeuroScoring.calculateScore(
                    candidateVector = candidateVector,
                    userProfile = userProfile,
                    uploadTimestamp = uploadTimestamp,
                    channelId = video.uploaderUrl ?: video.uploaderName,
                    recentChannelCounts = recentChannelCounts,
                    isSubscription = isSubscription,
                    hourOfDay = hourOfDay,
                    isWeekend = isWeekend
                )

                if (discoveryMode) {
                    val discoverySeeds = NeuroDiscovery.getDiscoverySeeds(seedKeywords)
                    val isDiscovery = discoverySeeds.any { video.title.contains(it, ignoreCase = true) }
                    score = NeuroDiscovery.applySerendipityBoost(score, isDiscovery)
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
                topPool.shuffled().take(60)
            }

            cachedRecommendations = finalRecommendations
            lastFetchTime = System.currentTimeMillis()
            Result.success(finalRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUploadDate(dateStr: String?): Long {
        if (dateStr == null) return System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        return try {
            when {
                dateStr.contains("hour", ignoreCase = true) -> now - (dayMs / 24)
                dateStr.contains("day", ignoreCase = true) -> {
                    val days = dateStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                    now - (days * dayMs)
                }
                dateStr.contains("week", ignoreCase = true) -> {
                    val weeks = dateStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                    now - (weeks * 7 * dayMs)
                }
                dateStr.contains("month", ignoreCase = true) -> {
                    val months = dateStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                    now - (months * 30 * dayMs)
                }
                dateStr.contains("year", ignoreCase = true) -> {
                    val years = dateStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                    now - (years * 365 * dayMs)
                }
                else -> now
            }
        } catch (e: Exception) {
            now
        }
    }
}
