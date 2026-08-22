/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.domain.model.VideoItem

object HistoryUtils {
    /**
     * Standardizes how watch progress is mapped from History entities to Video items.
     * Use this across all ViewModels to ensure consistent UI feedback.
     */
    fun List<VideoItem>.applyHistory(history: List<HistoryEntity>): List<VideoItem> {
        if (history.isEmpty()) return this
        
        val historyMap = history.associateBy(
            { it.videoId },
            { if (it.durationMs > 0) it.progressMs.toFloat() / it.durationMs else null }
        )
        
        return this.map { video ->
            video.copy(watchProgress = historyMap[video.id])
        }
    }

    /**
     * Filters history items that are partially watched (between 5% and 95%).
     * Returns them as VideoItem objects for UI display.
     */
    fun getContinuePlaying(history: List<HistoryEntity>): List<VideoItem> {
        return history.filter { 
            it.durationMs > 0 && 
            (it.progressMs.toFloat() / it.durationMs) in 0.05f..0.95f 
        }.sortedByDescending { it.timestamp }
        .map { it.toVideoItem() }
    }
}
