/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserInterestsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) {
    /**
     * Updates user interests based on video metadata.
     * @param text The title or uploader name to extract keywords from.
     * @param baseWeight The initial weight (e.g. 1.0 for title, 2.0 for uploader).
     * @param watchRatio The fraction of the video watched (0.0 to 1.0). 
     *                   If null, it's treated as a neutral interaction (1.0).
     */
    suspend operator fun invoke(text: String, baseWeight: Float = 1.0f, watchRatio: Float? = null) {
        if (preferencesManager.isRecommendationsPaused.first() || 
            preferencesManager.isIncognitoMode.first()) return

        val watchWeightFactor = when {
            watchRatio == null -> 1.0f
            watchRatio < 0.1f -> 0.1f // Very low weight for misclicks
            watchRatio > 0.9f -> 1.5f // Bonus weight for completing a video
            else -> watchRatio
        }

        val finalWeight = baseWeight * watchWeightFactor
        
        val keywords = extractKeywords(text)
        keywords.forEach { kw ->
            libraryRepository.updateInterest(kw, finalWeight)
        }
        
        // Optimized: Predictable interest decay based on timestamp and a slight probability
        // to avoid database contention on every single update
        if (System.currentTimeMillis() % 50 == 0L) {
            libraryRepository.applyInterestDecay(0.95f) 
        }
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of",
            "how", "what", "why", "when", "where", "who", "which", "this", "that", "these", "those", "from", "into", "onto",
            "with", "from", "their", "they", "them", "then", "there", "than", "that", "this", "these", "those",
            "will", "would", "shall", "should", "could", "must", "might", "video", "youtube", "play", "tube", "official",
            "today", "yesterday", "tomorrow", "very", "really", "just", "only", "about", "above", "after", "again", "against",
            "full", "hd", "4k", "2024", "2025", "2026", "episode", "part", "season", "new", "latest", "best", "top", "viral",
            "mv", "music", "lyrics", "audio", "video", "1080p", "720p", "high", "quality", "standard", "definition"
        )
        return text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 3 && it !in stopWords }
            .distinct()
    }
}
