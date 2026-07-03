package com.arslandaim.playtube.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheSize = 50L * 1024L * 1024L // 50MB (Increased from 10MB)
        val cacheDirectory = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDirectory, cacheSize)

        val dispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 20
        }

        return OkHttpClient.Builder()
            .cache(cache)
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS) // Increased for slow networks
            .readTimeout(30, TimeUnit.SECONDS)    // Increased for slow networks
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
