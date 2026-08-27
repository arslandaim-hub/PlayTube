/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.recommendation.NeuroScoring
import com.arslandaim.playtube.domain.recommendation.NeuroTokenizer
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

        val alpha = NeuroScoring.calculateLearningRate(watchRatio ?: 0.5f)
        val learningRate = 1f - alpha
        val finalWeight = baseWeight * learningRate
        
        val keywordVectors = NeuroTokenizer.tokenize(text)
        keywordVectors.forEach { (kw, frequency) ->
            libraryRepository.updateInterest(kw, finalWeight * frequency)
        }
        
        // Optimized: Predictable interest decay based on timestamp and a slight probability
        // to avoid database contention on every single update
        if (System.currentTimeMillis() % 50 == 0L) {
            libraryRepository.applyInterestDecay(0.95f) 
        }
    }
}
