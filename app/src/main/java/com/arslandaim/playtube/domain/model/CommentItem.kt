/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

data class CommentItem(
    val authorName: String,
    val authorThumbnailUrl: String?,
    val authorUrl: String?,
    val commentText: String,
    val publishedTime: String?,
    val likeCount: Int = 0,
    val isHeartedByUploader: Boolean = false,
    val replyCount: Int = 0,
    val commentId: String
)
