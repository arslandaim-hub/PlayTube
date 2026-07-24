/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
sealed interface SearchItem {
    @Immutable
    data class Video(val video: VideoItem) : SearchItem
    @Immutable
    data class Channel(
        val id: String,
        val name: String,
        val thumbnailUrl: String?,
        val subscriberCount: Long?,
        val description: String?,
        val isSubscribed: Boolean = false
    ) : SearchItem
    @Immutable
    data class Playlist(val playlist: PlaylistItem) : SearchItem
}
