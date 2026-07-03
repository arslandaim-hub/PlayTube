package com.arslandaim.playtube.di

import com.arslandaim.playtube.ui.screens.player.MiniPlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ManagerModule {

    @Provides
    @Singleton
    fun provideMiniPlayerManager(): MiniPlayerManager {
        return MiniPlayerManager()
    }
}
