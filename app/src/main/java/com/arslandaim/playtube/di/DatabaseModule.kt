package com.arslandaim.playtube.di

import android.content.Context
import androidx.room.Room
import com.arslandaim.playtube.data.local.DownloadDao
import com.arslandaim.playtube.data.local.FavoriteDao
import com.arslandaim.playtube.data.local.HistoryDao
import com.arslandaim.playtube.data.local.PlayTubeDatabase
import com.arslandaim.playtube.data.local.PlaylistFavoriteDao
import com.arslandaim.playtube.data.local.SearchHistoryDao
import com.arslandaim.playtube.data.local.SubscriptionDao
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
        return Room.databaseBuilder(
            context,
            PlayTubeDatabase::class.java,
            "playtube_db"
        )
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
}
