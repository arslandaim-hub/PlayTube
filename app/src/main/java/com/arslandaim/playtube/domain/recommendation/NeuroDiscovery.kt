/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

import kotlin.random.Random

/**
 * Implements the epsilon-greedy serendipity engine for content discovery.
 */
object NeuroDiscovery {
    private const val EPSILON = 0.20f // 20% exploration threshold

    /**
     * Determines if the current recommendation slot should explore new topics.
     */
    fun shouldExplore(): Boolean {
        return Random.nextFloat() < EPSILON
    }

    /**
     * Selects adjacent topics to explore based on current high-affinity nodes.
     */
    fun getDiscoverySeeds(topTopics: List<String>): List<String> {
        if (topTopics.isEmpty()) {
            return NeuroTopicCatalog.TOPICS.shuffled().take(3).map { it.name }
        }
        val explorationSeeds = mutableListOf<String>()
        topTopics.forEach { topic ->
            explorationSeeds.addAll(NeuroTopicCatalog.getAdjacentTopics(topic))
        }
        return explorationSeeds.distinct().shuffled().take(3)
    }

    /**
     * Applies a serendipity boost to candidates that belong to discovery clusters.
     */
    fun applySerendipityBoost(score: Float, isDiscoveryCandidate: Boolean): Float {
        return if (isDiscoveryCandidate) score * 1.15f else score
    }
}
