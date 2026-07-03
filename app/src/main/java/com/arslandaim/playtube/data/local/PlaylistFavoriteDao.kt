package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistFavoriteDao {
    @Query("SELECT * FROM playlist_favorites ORDER BY timestamp DESC")
    fun getAllPlaylistFavorites(): Flow<List<PlaylistFavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_favorites WHERE playlistId = :playlistId)")
    fun isPlaylistFavorite(playlistId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistFavorite(favorite: PlaylistFavoriteEntity)

    @Delete
    suspend fun deletePlaylistFavorite(favorite: PlaylistFavoriteEntity)
}
