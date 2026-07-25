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
            val isHistoryEnabled = preferencesManager.isHistoryEnabled.first()
            val isSearchHistoryPaused = preferencesManager.isSearchHistoryPaused.first()

            val watchHistory = if (isHistoryEnabled) libraryRepository.getHistory().first().take(10) else emptyList()
            val searchHistory = if (!isSearchHistoryPaused) libraryRepository.getSearchHistory().first().take(10) else emptyList()
            val subscriptions = libraryRepository.getSubscriptions().first().take(10)

            // 1. Extract Keywords & Channel Affinity
            val keywords = mutableMapOf<String, Float>()
            val channelAffinity = mutableMapOf<String, Float>()

            // Process Watch History
            watchHistory.forEachIndexed { index, item ->
                val weight = 1.0f / (index + 1)
                extractKeywords(item.title).forEach { kw ->
                    keywords[kw] = (keywords[kw] ?: 0f) + (weight * 2.0f) // Higher weight for watched
                }
                channelAffinity[item.uploaderName] = (channelAffinity[item.uploaderName] ?: 0f) + (weight * 3.0f)
            }

            // Process Search History
            searchHistory.forEachIndexed { index, item ->
                val weight = 1.0f / (index + 1)
                extractKeywords(item.query).forEach { kw ->
                    keywords[kw] = (keywords[kw] ?: 0f) + (weight * 1.5f)
                }
            }

            // 2. Fetch Candidate Videos
            val topics = (searchHistory.map { it.query } + keywords.entries.sortedByDescending { it.value }.take(3).map { it.key } + listOf("trending"))
                .distinct()
                .take(5)

            val candidates = topics.map { topic ->
                async {
                    try {
                        searchRepository.search(topic).items
                            .filterIsInstance<SearchItem.Video>()
                            .map { it.video }
                    } catch (e: Exception) {
                        emptyList<VideoItem>()
                    }
                }
            }.awaitAll().flatten().distinctBy { it.id }

            // 3. Scoring Logic
            val scoredVideos = candidates.map { video ->
                var score = 0f
                
                // Keyword Matching
                val videoKeywords = extractKeywords(video.title)
                videoKeywords.forEach { kw ->
                    score += keywords[kw] ?: 0f
                }

                // Channel Affinity
                score += (channelAffinity[video.uploaderName] ?: 0f) * 5.0f

                // Subscribed Channel Boost
                if (subscriptions.any { it.name == video.uploaderName }) {
                    score += 10.0f
                }

                Pair(video, score)
            }

            val finalRecommendations = scoredVideos
                .sortedByDescending { it.second }
                .map { it.first }
                .take(60)

            Result.success(finalRecommendations)
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
