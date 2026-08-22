/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

import kotlin.math.ln
import kotlin.math.pow

/**
 * Implements candidate ranking using Flow's composite scoring formula.
 */
object NeuroScoring {

    /**
     * Score = (CosineSimilarity * AffinityWeight) + (FreshnessScore * TimeWeight) - FatiguePenalty
     */
    fun calculateScore(
        candidateVector: Map<String, Float>,
        userProfile: Map<String, Float>,
        uploadTimestamp: Long,
        channelId: String,
        recentChannelCounts: Map<String, Int>,
        isSubscription: Boolean,
        hourOfDay: Int = -1,
        isWeekend: Boolean = false
    ): Float {
        // 1. Vector Affinity
        val affinity = NeuroVectorMath.cosineSimilarity(candidateVector, userProfile)
        var affinityWeight = if (isSubscription) 1.2f else 1.0f

        // Phase 2: Time-of-Day Contextual weighting
        if (hourOfDay != -1) {
            val isMorning = hourOfDay in 6..11
            val isNight = hourOfDay in 22..23 || hourOfDay in 0..4
            
            // Adjust weights based on temporal affinity (Music/Relaxing more at night, etc)
            if (isNight && candidateVector.containsKey("relax")) affinityWeight += 0.2f
            if (isMorning && candidateVector.containsKey("music")) affinityWeight += 0.15f
            if (isWeekend) affinityWeight += 0.1f
        }

        // 2. Freshness Score (Exponential Decay)
        // Freshness drops by half every 7 days
        val ageDays = (System.currentTimeMillis() - uploadTimestamp).toFloat() / (1000 * 60 * 60 * 24)
        val freshness = 0.5f.pow(ageDays / 7f).coerceIn(0.1f, 1.0f)
        val timeWeight = 0.3f

        // 3. Fatigue Penalty (Logarithmic decay for channel saturation)
        val occurrences = recentChannelCounts[channelId] ?: 0
        val fatiguePenalty = if (occurrences > 0) {
            0.1f * ln(occurrences.toFloat() + 1f)
        } else 0f

        return (affinity * affinityWeight) + (freshness * timeWeight) - fatiguePenalty
    }

    /**
     * Calculates learning rate based on watch duration.
     */
    fun calculateLearningRate(watchRatio: Float): Float {
        return when {
            watchRatio < 0.1f -> 0.98f // Very slow learning for skips
            watchRatio > 0.8f -> 0.80f // Faster learning for complete watches
            else -> 0.85f // Default base alpha
        }
    }
}
