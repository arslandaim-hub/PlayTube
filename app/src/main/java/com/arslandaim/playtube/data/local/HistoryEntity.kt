/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.arslandaim.playtube.domain.model.VideoItem

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val progressMs: Long = 0,
    val durationMs: Long = 0
) {
    fun toVideoItem() = VideoItem(
        id = videoId,
        title = title,
        thumbnailUrl = thumbnailUrl,
        uploaderName = uploaderName,
        uploaderUrl = null,
        viewCount = 0,
        uploadDate = null,
        rawUploadDate = null,
        duration = durationMs / 1000,
        watchProgress = if (durationMs > 0) progressMs.toFloat() / durationMs else null
    )
}
