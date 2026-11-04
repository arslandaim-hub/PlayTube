/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.di.ApplicationScope
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.utils.InstantPlaybackTracker
import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPrefetcher @Inject constructor(
    private val videoRepository: VideoRepository,
    @ApplicationScope private val externalScope: CoroutineScope
) {
    private val prefetchSemaphore = Semaphore(2)

    /**
     * Immediate Tap Intercept: Triggered the exact millisecond user taps a video card
     * BEFORE navigation or player sheet animation completes.
     */
    fun onVideoTapped(videoId: String) {
        if (videoId.isBlank()) return
        InstantPlaybackTracker.onVideoTapped(videoId)
        externalScope.launch(Dispatchers.IO) {
            try {
                PTLog.d("PlaybackPrefetcher", "Immediate tap intercept prefetch for video $videoId")
                videoRepository.getStreamBundle(videoId, forceRefresh = false)
            } catch (e: Exception) {
                PTLog.w("PlaybackPrefetcher", "Tap intercept prefetch failed for $videoId: ${e.message}")
            }
        }
    }

    /**
     * Viewport / Top-Items Prefetch: Automatically pre-fetches stream URLs for top visible items in feeds.
     */
    fun prefetchViewport(videoIds: List<String>) {
        if (videoIds.isEmpty()) return
        val topIds = videoIds.take(3)
        externalScope.launch(Dispatchers.IO) {
            for (id in topIds) {
                if (id.isBlank()) continue
                launch {
                    prefetchSemaphore.withPermit {
                        try {
                            PTLog.d("PlaybackPrefetcher", "Viewport pre-fetching top feed video $id")
                            videoRepository.getStreamBundle(id, forceRefresh = false)
                        } catch (e: Exception) {
                            PTLog.w("PlaybackPrefetcher", "Viewport prefetch failed for $id: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
