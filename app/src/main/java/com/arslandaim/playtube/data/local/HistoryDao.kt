/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(history: List<HistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllIgnoreSync(history: List<HistoryEntity>)

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAllHistoryStatic(): List<HistoryEntity>

    @Query("UPDATE history SET progressMs = :progress, durationMs = :duration, timestamp = :timestamp WHERE videoId = :videoId")
    suspend fun updateProgress(videoId: String, progress: Long, duration: Long, timestamp: Long)

    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int): List<HistoryEntity>

    @Query("DELETE FROM history WHERE videoId = :videoId")
    suspend fun deleteHistory(videoId: String)

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history")
    fun clearHistorySync()
}
