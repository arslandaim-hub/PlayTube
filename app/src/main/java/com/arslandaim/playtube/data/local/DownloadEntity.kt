/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.arslandaim.playtube.domain.model.VideoItem

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val filePath: String,
    val totalSize: Long,
    val downloadedSize: Long,
    val status: DownloadStatus,
    val quality: String?,
    val format: String?,
    val videoUrl: String?,
    val audioUrl: String? = null,
    val playlistId: String? = null,
    val playlistTitle: String? = null
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
        duration = 0
    )
}

enum class DownloadStatus {
    WAITING, DOWNLOADING, COMPLETED, FAILED, PAUSED, PENDING
}
