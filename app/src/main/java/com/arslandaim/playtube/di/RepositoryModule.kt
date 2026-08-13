/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.di

import com.arslandaim.playtube.data.repository.*
import com.arslandaim.playtube.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(
        videoRepositoryImpl: VideoRepositoryImpl
    ): VideoRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        libraryRepositoryImpl: LibraryRepositoryImpl
    ): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindDataManagerRepository(
        dataManagerRepositoryImpl: DataManagerRepositoryImpl
    ): DataManagerRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        updateRepositoryImpl: UpdateRepositoryImpl
    ): UpdateRepository

    @Binds
    @Singleton
    abstract fun bindSponsorBlockRepository(
        sponsorBlockRepositoryImpl: SponsorBlockRepositoryImpl
    ): SponsorBlockRepository
}
