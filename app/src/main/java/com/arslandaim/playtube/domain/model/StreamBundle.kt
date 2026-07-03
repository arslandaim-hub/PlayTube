package com.arslandaim.playtube.domain.model

data class StreamBundle(
    val videoStreams: List<StreamItem>,
    val audioStreams: List<StreamItem>,
    val title: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val uploaderThumbnailUrl: String?,
    val description: String?,
    val viewCount: Long,
    val uploadDate: String?,
    val thumbnailUrl: String?,
    val relatedVideos: List<VideoItem> = emptyList(),
    val bestAudioStreamUrl: String? = null
)
