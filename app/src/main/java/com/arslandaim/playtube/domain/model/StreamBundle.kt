/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import org.schabi.newpipe.extractor.Page

data class StreamBundle(
    val videoStreams: List<StreamItem>,
    val audioStreams: List<StreamItem>,
    val title: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val uploaderThumbnailUrl: String?,
    val uploaderSubscriberCount: Long? = null,
    val description: String?,
    val viewCount: Long,
    val uploadDate: String?,
    val thumbnailUrl: String?,
    val isLive: Boolean = false,
    val isUpcoming: Boolean = false,
    val scheduledStartTime: String? = null,
    val relatedVideos: List<VideoItem> = emptyList(),
    val nextRelatedVideosPage: Page? = null,
    val bestAudioStreamUrl: String? = null,
    val subtitles: List<SubtitleItem> = emptyList(),
    val extractedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean {
        // YouTube stream URLs typically expire in 6 hours. We use 5.5 hours to be safe.
        val expiryTimeMs = 5.5 * 60 * 60 * 1000
        return System.currentTimeMillis() - extractedAt > expiryTimeMs
    }
}
