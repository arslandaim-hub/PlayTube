/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

enum class SearchSort(val label: String, val value: String) {
    RELEVANCE("Relevance", "relevance"),
    UPLOAD_DATE("Newest", "date"),
    VIEW_COUNT("Most Viewed", "viewCount"),
    RATING("Top Rated", "rating")
}
