/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.utils.PTLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InFlightRequestCoalescer @Inject constructor() {

    private val inFlightRequests = ConcurrentHashMap<String, Deferred<StreamBundle>>()
    private val mutex = Mutex()

    suspend fun coalesce(
        videoId: String,
        fetcher: suspend () -> StreamBundle
    ): StreamBundle {
        val deferred = inFlightRequests[videoId]
        if (deferred != null) {
            PTLog.d("InFlightRequestCoalescer", "Coalescing in-flight stream request for video $videoId (Awaiting existing Deferred)")
            return deferred.await()
        }

        val newDeferred = CompletableDeferred<StreamBundle>()
        val existing = mutex.withLock {
            val current = inFlightRequests[videoId]
            if (current != null) {
                current
            } else {
                inFlightRequests[videoId] = newDeferred
                null
            }
        }

        if (existing != null) {
            PTLog.d("InFlightRequestCoalescer", "Coalescing in-flight stream request for video $videoId (Race winner awaited)")
            return existing.await()
        }

        return try {
            val result = fetcher()
            newDeferred.complete(result)
            result
        } catch (e: Throwable) {
            newDeferred.completeExceptionally(e)
            throw e
        } finally {
            inFlightRequests.remove(videoId)
        }
    }
}
