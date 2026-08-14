/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arslandaim.playtube.domain.model.VideoItem

@Entity(tableName = "feed_cache")
data class FeedCacheEntity(
    @PrimaryKey val feedKey: String, // e.g., "home_trending", "subs_all", "subs_channel_<id>"
    val videos: List<VideoItem>,
    val timestamp: Long = System.currentTimeMillis()
)
