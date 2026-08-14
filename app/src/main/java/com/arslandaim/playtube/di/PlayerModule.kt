/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.arslandaim.playtube.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideBandwidthMeter(@ApplicationContext context: Context): BandwidthMeter {
        return DefaultBandwidthMeter.getSingletonInstance(context)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideVideoCache(@ApplicationContext context: Context): SimpleCache {
        val cacheDirectory = File(context.cacheDir, "video_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(Constants.VIDEO_CACHE_SIZE)
        val databaseProvider = StandaloneDatabaseProvider(context)
        
        // SimpleCache constructor performs disk I/O to initialize the index.
        // We use a lock-free check or rely on Hilt's Singleton thread-safety,
        // but ensuring it doesn't block the main thread is key.
        return SimpleCache(cacheDirectory, evictor, databaseProvider)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    @Named("HttpDataSourceFactory")
    fun provideHttpDataSourceFactory(
        okHttpClient: OkHttpClient
    ): DataSource.Factory {
        val userAgent = Constants.DEFAULT_USER_AGENT
        return OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(mapOf(
                "Accept-Language" to "en-US,en;q=0.9",
                "Referer" to "${Constants.YouTube.BASE_URL}/",
                "Cookie" to Constants.YouTube.CONSENT_COOKIE
            ))
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideDataSourceFactory(
        @ApplicationContext context: Context,
        @Named("HttpDataSourceFactory") httpDataSourceFactory: DataSource.Factory,
        bandwidthMeter: BandwidthMeter,
        cacheProvider: javax.inject.Provider<SimpleCache>
    ): DataSource.Factory {
        return DataSource.Factory {
            val cache = cacheProvider.get()
            val defaultDataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
            
            // Phase 2: Wrap with SABRDataSource for chunked loading
            val sabrFactory = com.arslandaim.playtube.ui.screens.player.SABRDataSourceFactory(
                defaultDataSourceFactory,
                bandwidthMeter
            )
            
            CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(sabrFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource()
        }
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideExoPlayer(
        @ApplicationContext context: Context,
        dataSourceFactory: DataSource.Factory
    ): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                45000,  // Min buffer: 45s (Up from 30s)
                180000, // Max buffer: 180s (Up from 120s)
                3500,   // Buffer for playback: 3.5s (Up from 2.5s)
                7000    // Buffer for playback after rebuffer: 7s (Up from 5s)
            )
            .setBackBuffer(20000, true) // 20s back buffer (Up from 15s)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val renderersFactory = androidx.media3.exoplayer.DefaultRenderersFactory(context)
            .setExtensionRendererMode(androidx.media3.exoplayer.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .forceEnableMediaCodecAsynchronousQueueing()

        return ExoPlayer.Builder(context, renderersFactory)
            .setLooper(android.os.Looper.getMainLooper())
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setDeviceVolumeControlEnabled(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
    }
}
