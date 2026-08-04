/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist ORDER BY timestamp DESC")
    fun getAllBlacklisted(): Flow<List<BlacklistEntity>>

    @Query("SELECT * FROM blacklist ORDER BY timestamp DESC")
    suspend fun getAllBlacklistedStatic(): List<BlacklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlacklistEntity)

    @Delete
    suspend fun delete(entity: BlacklistEntity)

    @Query("DELETE FROM blacklist WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSync(entity: BlacklistEntity)

    @Delete
    fun deleteSync(entity: BlacklistEntity)

    @Query("SELECT * FROM blacklist ORDER BY timestamp DESC")
    fun getAllBlacklistedStaticSync(): List<BlacklistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE id = :id)")
    suspend fun isBlacklisted(id: String): Boolean
}
