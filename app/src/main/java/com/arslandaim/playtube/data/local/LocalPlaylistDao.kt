/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPlaylistDao {
    @Query("SELECT * FROM local_playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<LocalPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createPlaylist(playlist: LocalPlaylistEntity): Long

    @Delete
    suspend fun deletePlaylist(playlist: LocalPlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: LocalPlaylistEntity)

    // Videos
    @Query("SELECT * FROM local_playlist_videos WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getVideosForPlaylist(playlistId: Int): Flow<List<LocalPlaylistVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addVideoToPlaylist(video: LocalPlaylistVideoEntity)

    @Query("DELETE FROM local_playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeVideoFromPlaylist(playlistId: Int, videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM local_playlist_videos WHERE videoId = :videoId)")
    fun isVideoInAnyPlaylist(videoId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM local_playlist_videos WHERE playlistId = :playlistId AND videoId = :videoId)")
    suspend fun isVideoInPlaylist(playlistId: Int, videoId: String): Boolean

    @Query("SELECT DISTINCT videoId FROM local_playlist_videos")
    fun getAllSavedVideoIds(): Flow<List<String>>

    @Query("SELECT playlistId FROM local_playlist_videos WHERE videoId = :videoId")
    fun getPlaylistsContainingVideo(videoId: String): Flow<List<Int>>

    @Query("UPDATE local_playlists SET thumbnailUrl = :thumbnailUrl WHERE id = :id")
    suspend fun updatePlaylistThumbnail(id: Int, thumbnailUrl: String?)

    @Query("SELECT thumbnailUrl FROM local_playlist_videos WHERE playlistId = :playlistId ORDER BY addedAt DESC LIMIT 1")
    suspend fun getLastVideoThumbnail(playlistId: Int): String?

    // Static queries for backup and restore
    @Query("SELECT * FROM local_playlists")
    suspend fun getAllLocalPlaylistsStatic(): List<LocalPlaylistEntity>

    @Query("SELECT * FROM local_playlist_videos")
    suspend fun getAllLocalPlaylistVideosStatic(): List<LocalPlaylistVideoEntity>

    @Query("DELETE FROM local_playlists")
    fun clearLocalPlaylistsSync()

    @Query("DELETE FROM local_playlist_videos")
    fun clearLocalPlaylistVideosSync()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLocalPlaylistsSync(playlists: List<LocalPlaylistEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertLocalPlaylistVideosSync(videos: List<LocalPlaylistVideoEntity>)
}
