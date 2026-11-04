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
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.BandwidthMeter
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import com.arslandaim.playtube.data.network.ClientGateTracker
import com.arslandaim.playtube.data.network.PoTokenProvider
import com.arslandaim.playtube.data.network.YouTubeHttpDataSource
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
        return SimpleCache(cacheDirectory, evictor, databaseProvider)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    @Named("HttpDataSourceFactory")
    fun provideHttpDataSourceFactory(
        okHttpClient: OkHttpClient,
        poTokenProvider: PoTokenProvider,
        clientGateTracker: ClientGateTracker
    ): DataSource.Factory {
        val userAgent = Constants.DEFAULT_USER_AGENT
        return DataSource.Factory {
            YouTubeHttpDataSource(
                client = okHttpClient,
                userAgent = userAgent,
                poTokenProvider = poTokenProvider,
                clientGateTracker = clientGateTracker
            )
        }
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
        // Zero-Latency Media3 LoadControl Tuning
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15000,  // Min buffer: 15s
                50000,  // Max buffer: 50s
                600,    // Buffer for playback: 600ms (Instant <300-600ms playback start!)
                1500    // Buffer for playback after rebuffer: 1.5s
            )
            .setBackBuffer(120000, true) // 120s back buffer for instant backward seek
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
