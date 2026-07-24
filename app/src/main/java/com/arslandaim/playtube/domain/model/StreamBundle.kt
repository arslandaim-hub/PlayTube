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
    val relatedVideos: List<VideoItem> = emptyList(),
    val nextRelatedVideosPage: Page? = null,
    val bestAudioStreamUrl: String? = null,
    val subtitles: List<SubtitleItem> = emptyList()
)
