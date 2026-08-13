/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

object Constants {
    const val DOWNLOAD_CHANNEL_ID = "download_channel"
    const val PLAYBACK_CHANNEL_ID = "playback_channel"
    
    const val VIDEO_CACHE_SIZE = 200L * 1024L * 1024L // 200MB
    const val STREAM_CACHE_SIZE = 50
    
    fun calculateGridColumns(screenWidthDp: Int): Int {
        return when {
            screenWidthDp >= 1200 -> 4
            screenWidthDp >= 900 -> 3
            screenWidthDp >= 600 -> 2
            else -> 1
        }
    }

    const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    
    object QualityThresholds {
        const val P144 = 0L
        const val P360 = 500_000L
        const val P480 = 1_000_000L
        const val P720 = 2_500_000L
        const val P1080 = 5_000_000L
        const val P1440 = 10_000_000L
        const val P2160 = 20_000_000L
    }

    object YouTube {
        const val BASE_URL = "https://www.youtube.com"
        const val VIDEO_URL_PREFIX = "https://www.youtube.com/watch?v="
        const val CHANNEL_URL_PREFIX = "https://www.youtube.com/channel/"
        const val TRENDING_URL = "https://www.youtube.com/feed/trending"
        
        val SEARCH_FILTERS = listOf("all")
        val SEARCH_FILTERS_UPLOAD_DATE = listOf("videos")
        
        const val CONSENT_COOKIE = "CONSENT=YES+cb.20210328-17-p0.en+FX+456; SOCS=CAESEwgDEgk0ODE3Nzk3MjQaAmVuIAEaBgiA_LyaBg"
    }
}
