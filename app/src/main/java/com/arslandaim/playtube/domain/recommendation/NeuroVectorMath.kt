/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

import kotlin.math.sqrt

/**
 * High-performance dense vector math for recommendation scoring.
 */
object NeuroVectorMath {

    /**
     * Calculates the cosine similarity between two vectors.
     * Formula: (A . B) / (||A|| * ||B||)
     */
    fun cosineSimilarity(vectorA: Map<String, Float>, vectorB: Map<String, Float>): Float {
        if (vectorA.isEmpty() || vectorB.isEmpty()) return 0f

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        val allKeys = vectorA.keys + vectorB.keys
        for (key in allKeys) {
            val valA = vectorA[key] ?: 0f
            val valB = vectorB[key] ?: 0f
            dotProduct += valA * valB
            normA += valA * valA
            normB += valB * valB
        }

        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0f) 0f else dotProduct / denominator
    }

    /**
     * Updates the user taste centroid using a dynamic learning rate.
     * Formula: U_new = alpha * U_old + (1 - alpha) * V_new
     */
    fun updateCentroid(
        currentProfile: Map<String, Float>,
        newInteraction: Map<String, Float>,
        alpha: Float = 0.85f
    ): Map<String, Float> {
        val updatedProfile = currentProfile.toMutableMap()
        val complement = 1f - alpha

        // Apply decay to current profile weights
        for (key in updatedProfile.keys) {
            updatedProfile[key] = updatedProfile[key]!! * alpha
        }

        // Add new interaction components
        for ((key, weight) in newInteraction) {
            val currentVal = updatedProfile[key] ?: 0f
            updatedProfile[key] = currentVal + (weight * complement)
        }

        // Trim low-weight dimensions to keep vector dense but manageable
        return updatedProfile.filterValues { it > 0.01f }
    }
}
