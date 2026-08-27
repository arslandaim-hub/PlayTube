/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_missions")
data class DownloadMissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val title: String,
    val quality: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: MissionStatus = MissionStatus.QUEUED,
    val outputFilePath: String? = null,
    val creationTime: Long = System.currentTimeMillis(),
    val videoUrl: String? = null,
    val audioUrl: String? = null,
    val format: String? = null
)

enum class MissionStatus {
    QUEUED, DOWNLOADING, MUXING, COMPLETED, FAILED, PAUSED
}
