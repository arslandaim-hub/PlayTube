/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

data class VideoItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val uploaderThumbnailUrl: String? = null,
    val viewCount: Long,
    val subscriberCount: Long? = null,
    val uploadDate: String?,
    val duration: Long // in seconds
)
