/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.SearchRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
    private val libraryRepository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(): Result<List<VideoItem>> = coroutineScope {
        try {
            // 1. Get Top Interests from Local Intelligence Engine
            val topInterests = libraryRepository.getTopInterests(20)
            
            val isHistoryEnabled = preferencesManager.isHistoryEnabled.first()
            // Optimized: Fetch a fixed recent set directly from DB instead of full Flow collection
            val watchHistory = if (isHistoryEnabled) libraryRepository.getRecentHistory(100) else emptyList()
            
            val seedKeywords = mutableListOf<String>()
            
            // Use top interests as primary seeds
            if (topInterests.isNotEmpty()) {
                seedKeywords.addAll(topInterests.take(8).map { it.keyword })
            }
            
            // Supplemental seeds from recent history if profile is sparse
            if (seedKeywords.size < 4 && watchHistory.isNotEmpty()) {
                watchHistory.take(5).forEach { video ->
                    seedKeywords.addAll(extractKeywords(video.title).take(2))
                }
            }
            
            // Fallback to defaults if profile is empty
            if (seedKeywords.isEmpty()) {
                seedKeywords.addAll(listOf("Technology", "Science", "Nature", "News", "Music", "Education"))
            }

            // 2. Fetch Candidate Videos with Throttling (3 at a time)
            val topics = seedKeywords.distinct().take(12) // Get more candidates
            val candidates = mutableListOf<VideoItem>()
            
            topics.chunked(3).forEach { chunk ->
                val deferred = chunk.map { topic ->
                    async {
                        try {
                            searchRepository.search(topic).items
                                .filterIsInstance<SearchItem.Video>()
                                .map { it.video }
                        } catch (e: Exception) {
                            emptyList<VideoItem>()
                        }
                    }
                }
                candidates.addAll(deferred.awaitAll().flatten())
            }

            // 3. Filtering & Diversification
            // Filter out ANY video the user has already watched (global filtering)
            val watchedIds = watchHistory.map { it.videoId }.toSet()
            val filteredCandidates = candidates
                .distinctBy { it.id }
                .filter { it.id !in watchedIds }
            
            // Interleave results from different topics to ensure diversity
            val shuffledRecommendations = filteredCandidates.shuffled().take(60)

            Result.success(shuffledRecommendations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of")
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in stopWords }
    }
}
