package com.arslandaim.playtube.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_favorites")
data class PlaylistFavoriteEntity(
    @PrimaryKey val playlistId: String,
    val title: String,
    val thumbnailUrl: String,
    val uploaderName: String,
    val timestamp: Long = System.currentTimeMillis()
)
