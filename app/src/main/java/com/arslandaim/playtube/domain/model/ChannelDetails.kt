package com.arslandaim.playtube.domain.model

data class ChannelDetails(
    val id: String,
    val name: String,
    val description: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val subscriberCount: Long?,
    val videos: List<VideoItem>,
    val playlists: List<PlaylistItem> = emptyList()
)
