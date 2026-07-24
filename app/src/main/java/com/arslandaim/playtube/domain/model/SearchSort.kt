/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

enum class SearchSort(val label: String, val value: String) {
    RELEVANCE("Relevance", ""),
    UPLOAD_DATE("Newest", "upload_date"),
    VIEW_COUNT("Most Viewed", "view_count"),
    RATING("Top Rated", "rating")
}
