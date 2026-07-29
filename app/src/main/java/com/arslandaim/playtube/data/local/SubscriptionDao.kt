/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE channelId = :channelId OR channelId LIKE '%' || :channelId || '%')")
    fun isSubscribed(channelId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(subscriptions: List<SubscriptionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAllIgnoreSync(subscriptions: List<SubscriptionEntity>)

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllSubscriptionsStatic(): List<SubscriptionEntity>

    @Query("DELETE FROM subscriptions")
    fun clearSubscriptions()

    @Query("DELETE FROM subscriptions WHERE channelId = :channelId OR channelId LIKE '%' || :channelId || '%'")
    suspend fun deleteSubscriptionByIdFuzzy(channelId: String)

    @Delete
    suspend fun deleteSubscription(subscription: SubscriptionEntity)
}
