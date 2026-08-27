/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DownloadEntity::class,
        HistoryEntity::class,
        FavoriteEntity::class,
        SubscriptionEntity::class,
        SearchHistoryEntity::class,
        PlaylistFavoriteEntity::class,
        UserInterestEntity::class,
        BlacklistEntity::class,
        LocalPlaylistEntity::class,
        LocalPlaylistVideoEntity::class,
        FeedCacheEntity::class,
        DownloadMissionEntity::class,
        DownloadChunkEntity::class
    ],
    version = 15
)
@TypeConverters(Converters::class)
abstract class PlayTubeDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun missionDao(): MissionDao
    abstract fun historyDao(): HistoryDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun playlistFavoriteDao(): PlaylistFavoriteDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun userInterestDao(): UserInterestDao
    abstract fun blacklistDao(): BlacklistDao
    abstract fun localPlaylistDao(): LocalPlaylistDao
    abstract fun feedCacheDao(): FeedCacheDao
}
