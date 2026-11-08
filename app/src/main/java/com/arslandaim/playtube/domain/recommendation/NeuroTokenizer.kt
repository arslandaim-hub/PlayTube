/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

import java.util.Locale

/**
 * Tokenizes video metadata into semantic weight vectors and structured topics.
 */
object NeuroTokenizer {
    private val ALLOWED_2CHAR_TOKENS = setOf("ai", "vr", "ar", "ui", "ux", "pc", "os", "3d", "4k", "8k", "f1", "go", "js", "ts", "ml", "dl", "ev", "tv")

    private val STOP_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "with", "is", "are", "was", "were", "of",
        "how", "what", "why", "when", "where", "who", "which", "this", "that", "these", "those", "from", "into", "onto",
        "their", "they", "them", "then", "there", "than", "will", "would", "shall", "should", "could", "must", "might",
        "video", "youtube", "play", "tube", "official", "today", "yesterday", "tomorrow", "very", "really", "just", "only",
        "about", "above", "after", "again", "against", "full", "hd", "episode", "part", "season", "new", "latest",
        "best", "top", "viral", "mv", "music", "lyrics", "audio", "1080p", "720p", "high", "quality", "standard",
        "watch", "first", "make", "get", "check", "take", "look", "know", "vlog", "live", "clip", "stream", "show",
        "channel", "trailer", "guy", "guys", "view", "views", "like", "subscribe", "comment", "share", "ep", "vs"
    )

    private fun isValidToken(token: String): Boolean {
        return (token.length > 2 || token in ALLOWED_2CHAR_TOKENS) && token !in STOP_WORDS
    }

    /**
     * Converts raw text into a frequency-weighted unigram vector for vector scoring.
     */
    fun tokenize(text: String): Map<String, Float> {
        val tokens = text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { isValidToken(it) }

        if (tokens.isEmpty()) return emptyMap()

        val counts = mutableMapOf<String, Int>()
        tokens.forEach { token ->
            counts[token] = (counts[token] ?: 0) + 1
        }

        val maxCount = counts.values.maxOrNull()?.toFloat() ?: 1f
        return counts.mapValues { it.value / maxCount }
    }

    /**
     * Extracts multi-word topic phrases, category domains, and meaningful keywords.
     */
    fun tokenizeTopics(text: String): Map<String, Float> {
        val rawTokens = text.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() }

        val validTokens = rawTokens.map { it.lowercase() }
            .filter { isValidToken(it) }

        if (validTokens.isEmpty()) return emptyMap()

        val topicMap = mutableMapOf<String, Float>()

        // 1. Catalog Category Domain Mapping
        val primaryCategory = NeuroTopicCatalog.findPrimaryTopic(validTokens.toSet())
        if (primaryCategory != null) {
            topicMap[primaryCategory] = 1.5f
        }

        // 2. Multi-word Phrases (Bigrams)
        for (i in 0 until rawTokens.size - 1) {
            val t1 = rawTokens[i].lowercase()
            val t2 = rawTokens[i + 1].lowercase()

            if (isValidToken(t1) && isValidToken(t2)) {
                val phrase = "${rawTokens[i].replaceFirstChar { it.uppercase() }} ${rawTokens[i + 1].replaceFirstChar { it.uppercase() }}"
                topicMap[phrase] = 1.2f
            }
        }

        // 3. Fallback to clean single keywords if no phrase/category was extracted
        if (topicMap.size <= 1) {
            validTokens.forEach { token ->
                val capitalized = token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                if (!topicMap.containsKey(capitalized)) {
                    topicMap[capitalized] = 1.0f
                }
            }
        }

        return topicMap
    }

    /**
     * Extracts unigrams and bigrams for feature set comparisons.
     */
    fun extractFeatureSet(text: String): Set<String> {
        val tokens = text.lowercase()
            .split(Regex("[^a-zA-Z0-9]+"))
            .filter { isValidToken(it) }

        val features = mutableSetOf<String>()
        features.addAll(tokens)

        // Add bigrams
        for (i in 0 until tokens.size - 1) {
            features.add("${tokens[i]} ${tokens[i+1]}")
        }

        return features
    }
}
