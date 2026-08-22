/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.recommendation

/**
 * Defines a hierarchical taxonomy of content domains for vector-graph hybrid recommendations.
 */
object NeuroTopicCatalog {
    val TOPICS = listOf(
        TopicNode("Technology", listOf("tech", "gadgets", "review", "programming", "coding", "software", "hardware", "ai", "robotics")),
        TopicNode("Gaming", listOf("gameplay", "walkthrough", "nintendo", "playstation", "xbox", "pc gaming", "esports", "streamer")),
        TopicNode("Science", listOf("space", "physics", "biology", "chemistry", "nature", "documentary", "astronomy", "evolution")),
        TopicNode("Music", listOf("song", "mv", "concert", "lyrics", "cover", "instrumental", "pop", "rock", "jazz", "hip hop")),
        TopicNode("Education", listOf("tutorial", "lecture", "course", "learn", "explanation", "history", "math", "language")),
        TopicNode("News", listOf("politics", "world", "economy", "current events", "journalism", "report", "breaking")),
        TopicNode("Entertainment", listOf("movie", "trailer", "vlog", "comedy", "funny", "celebrity", "show", "animation"))
    )

    data class TopicNode(
        val name: String,
        val keywords: List<String>,
        val children: List<TopicNode> = emptyList(),
        val relatedTopics: List<String> = emptyList()
    )

    /**
     * Finds the most relevant topic for a set of keywords.
     */
    fun findPrimaryTopic(tokens: Set<String>): String? {
        return TOPICS.maxByOrNull { topic ->
            topic.keywords.count { it in tokens }
        }?.name
    }

    /**
     * Returns adjacent topics in the graph to enable serendipitous discovery.
     */
    fun getAdjacentTopics(topicName: String): List<String> {
        return when (topicName) {
            "Technology" -> listOf("Science", "Gaming", "Education")
            "Gaming" -> listOf("Technology", "Entertainment")
            "Science" -> listOf("Technology", "Education")
            "Music" -> listOf("Entertainment", "Education")
            "Education" -> listOf("Science", "Technology", "News")
            "News" -> listOf("Politics", "Economy", "Technology")
            "Entertainment" -> listOf("Music", "Gaming", "Vlog")
            else -> TOPICS.shuffled().take(2).map { it.name }
        }
    }
}
