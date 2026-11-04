/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import android.util.LruCache
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.PTLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResolvedStreamCache @Inject constructor() {

    private class CacheEntry(
        val bundle: StreamBundle,
        val timestampMs: Long
    )

    private val cache = LruCache<String, CacheEntry>(Constants.STREAM_CACHE_SIZE)
    private val DEFAULT_TTL_MS = 15 * 60 * 1000L // 15 minutes

    @Synchronized
    fun get(videoId: String): StreamBundle? {
        if (videoId.isBlank()) return null
        val entry = cache.get(videoId) ?: return null
        val now = System.currentTimeMillis()

        // Check 1: Expiry based on TTL
        if (now - entry.timestampMs > DEFAULT_TTL_MS) {
            cache.remove(videoId)
            PTLog.d("ResolvedStreamCache", "Expired stream cache hit for $videoId (TTL exceeded)")
            return null
        }

        // Check 2: Expiry based on stream URL parameters (e.g., expire=1700000000)
        val sampleUrl = entry.bundle.videoStreams.firstOrNull()?.url
            ?: entry.bundle.audioStreams.firstOrNull()?.url
        if (sampleUrl != null && isUrlExpired(sampleUrl)) {
            cache.remove(videoId)
            PTLog.d("ResolvedStreamCache", "Expired stream URL cache hit for $videoId")
            return null
        }

        PTLog.d("ResolvedStreamCache", "Cache HIT for video $videoId (0ms network wait)")
        return entry.bundle
    }

    @Synchronized
    fun put(videoId: String, bundle: StreamBundle) {
        if (videoId.isBlank()) return
        cache.put(videoId, CacheEntry(bundle, System.currentTimeMillis()))
        PTLog.d("ResolvedStreamCache", "Cached resolved streams for video $videoId")
    }

    @Synchronized
    fun evict(videoId: String) {
        if (videoId.isBlank()) return
        cache.remove(videoId)
        PTLog.d("ResolvedStreamCache", "Evicted cached stream bundle for video $videoId")
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    private fun isUrlExpired(url: String): Boolean {
        try {
            val uri = android.net.Uri.parse(url)
            val expireStr = uri.getQueryParameter("expire")
            if (expireStr != null) {
                val expireTimeSec = expireStr.toLongOrNull() ?: return false
                val currentTimeSec = System.currentTimeMillis() / 1000
                // Expired if current time is within 60 seconds of expiration
                return currentTimeSec >= (expireTimeSec - 60)
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return false
    }
}
