/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedCacheDao {
    @Query("SELECT * FROM feed_cache WHERE feedKey = :key")
    fun getFeed(key: String): Flow<FeedCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: FeedCacheEntity)

    @Query("DELETE FROM feed_cache WHERE feedKey = :key")
    suspend fun deleteFeed(key: String)

    @Query("DELETE FROM feed_cache")
    suspend fun clearAll()
}
