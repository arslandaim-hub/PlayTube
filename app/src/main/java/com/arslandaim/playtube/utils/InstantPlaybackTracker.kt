/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Singleton

@Singleton
object InstantPlaybackTracker {

    private class TrackSession(
        val videoId: String,
        val tapTimeMs: Long
    ) {
        var streamResolvedTimeMs: Long = 0L
        var firstFrameTimeMs: Long = 0L
    }

    private val sessions = ConcurrentHashMap<String, TrackSession>()

    fun onVideoTapped(videoId: String) {
        if (videoId.isBlank()) return
        val now = SystemClock.elapsedRealtime()
        sessions[videoId] = TrackSession(videoId, now)
        PTLog.d("InstantPlaybackTracker", "[$videoId] Tap recorded at $now ms")
    }

    fun onStreamResolved(videoId: String) {
        val session = sessions[videoId] ?: return
        if (session.streamResolvedTimeMs == 0L) {
            session.streamResolvedTimeMs = SystemClock.elapsedRealtime()
            val tapToStreamResolvedMs = session.streamResolvedTimeMs - session.tapTimeMs
            PTLog.i(
                "InstantPlaybackTracker",
                "[$videoId] tapToStreamResolvedMs: $tapToStreamResolvedMs ms"
            )
        }
    }

    fun onFirstFrameRendered(videoId: String) {
        val session = sessions.remove(videoId) ?: return
        session.firstFrameTimeMs = SystemClock.elapsedRealtime()
        val tapToStreamResolvedMs = if (session.streamResolvedTimeMs > 0) session.streamResolvedTimeMs - session.tapTimeMs else -1L
        val streamResolvedToFirstFrameMs = if (session.streamResolvedTimeMs > 0) session.firstFrameTimeMs - session.streamResolvedTimeMs else -1L
        val totalTapToFirstFrameMs = session.firstFrameTimeMs - session.tapTimeMs

        PTLog.i(
            "InstantPlaybackTracker",
            """
            ===================================================================
            INSTANT PLAYBACK METRICS [$videoId]:
            - tapToStreamResolvedMs        : $tapToStreamResolvedMs ms
            - streamResolvedToFirstFrameMs : $streamResolvedToFirstFrameMs ms
            - totalTapToFirstFrameMs       : $totalTapToFirstFrameMs ms
            ===================================================================
            """.trimIndent()
        )
    }
}
