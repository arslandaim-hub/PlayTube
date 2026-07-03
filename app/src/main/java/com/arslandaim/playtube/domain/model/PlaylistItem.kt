package com.arslandaim.playtube.domain.model

data class PlaylistItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val streamCount: Long
)
