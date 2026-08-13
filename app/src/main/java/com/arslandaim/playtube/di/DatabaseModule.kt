/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.FavoriteDao
import com.arslandaim.playtube.data.local.HistoryDao
import com.arslandaim.playtube.data.local.PlayTubeDatabase
import com.arslandaim.playtube.data.local.PlaylistFavoriteDao
import com.arslandaim.playtube.data.local.SearchHistoryDao
import com.arslandaim.playtube.data.local.SubscriptionDao
import com.arslandaim.playtube.data.local.UserInterestDao
import com.arslandaim.playtube.data.local.BlacklistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlayTubeDatabase {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `blacklist` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        return Room.databaseBuilder(
            context,
            PlayTubeDatabase::class.java,
            "playtube_db"
        )
        .addMigrations(MIGRATION_9_10)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideDownloadDao(database: PlayTubeDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun provideHistoryDao(database: PlayTubeDatabase): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideFavoriteDao(database: PlayTubeDatabase): FavoriteDao {
        return database.favoriteDao()
    }

    @Provides
    fun providePlaylistFavoriteDao(database: PlayTubeDatabase): PlaylistFavoriteDao {
        return database.playlistFavoriteDao()
    }

    @Provides
    fun provideSubscriptionDao(database: PlayTubeDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }

    @Provides
    fun provideSearchHistoryDao(database: PlayTubeDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun provideUserInterestDao(database: PlayTubeDatabase): UserInterestDao {
        return database.userInterestDao()
    }

    @Provides
    fun provideBlacklistDao(database: PlayTubeDatabase): BlacklistDao {
        return database.blacklistDao()
    }

    @Provides
    fun provideLocalPlaylistDao(database: PlayTubeDatabase): com.arslandaim.playtube.data.local.LocalPlaylistDao {
        return database.localPlaylistDao()
    }
}
