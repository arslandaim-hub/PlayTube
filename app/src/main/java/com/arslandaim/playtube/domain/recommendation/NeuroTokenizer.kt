/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

/**
 * Tokenizes video metadata into semantic weight vectors.
 */
object NeuroTokenizer {
    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of",
        "how", "what", "why", "when", "where", "who", "which", "this", "that", "these", "those", "from", "into", "onto",
        "with", "from", "their", "they", "them", "then", "there", "than", "that", "this", "these", "those",
        "will", "would", "shall", "should", "could", "must", "might", "video", "youtube", "play", "tube", "official",
        "today", "yesterday", "tomorrow", "very", "really", "just", "only", "about", "above", "after", "again", "against",
        "full", "hd", "4k", "episode", "part", "season", "new", "latest", "best", "top", "viral",
        "mv", "music", "lyrics", "audio", "video", "1080p", "720p", "high", "quality", "standard", "definition"
    )

    /**
     * Converts raw text into a frequency-weighted vector.
     */
    fun tokenize(text: String): Map<String, Float> {
        val tokens = text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && it !in STOP_WORDS }
        
        if (tokens.isEmpty()) return emptyMap()
        
        val counts = mutableMapOf<String, Int>()
        tokens.forEach { token ->
            counts[token] = (counts[token] ?: 0) + 1
        }
        
        val maxCount = counts.values.maxOrNull()?.toFloat() ?: 1f
        return counts.mapValues { it.value / maxCount }
    }

    /**
     * Extracts unigrams and bigrams for better semantic matching.
     */
    fun extractFeatureSet(text: String): Set<String> {
        val tokens = text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() && it !in STOP_WORDS }
        
        val features = mutableSetOf<String>()
        features.addAll(tokens.filter { it.length > 2 })
        
        // Add bigrams
        for (i in 0 until tokens.size - 1) {
            features.add("${tokens[i]} ${tokens[i+1]}")
        }
        
        return features
    }
}
