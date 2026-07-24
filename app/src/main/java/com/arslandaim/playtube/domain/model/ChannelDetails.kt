/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import org.schabi.newpipe.extractor.Page

data class ChannelDetails(
    val id: String,
    val name: String,
    val description: String?,
    val bannerUrl: String?,
    val avatarUrl: String?,
    val subscriberCount: Long?,
    val videos: List<VideoItem>,
    val nextVideosPage: Page? = null,
    val playlists: List<PlaylistItem> = emptyList()
)
