package com.arslandaim.playtube.domain.model

data class VideoItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val viewCount: Long,
    val uploadDate: String?,
    val duration: Long // in seconds
)
