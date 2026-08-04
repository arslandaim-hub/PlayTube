/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateWatchProgressUseCase @Inject constructor(
    private val repository: LibraryRepository,
    private val preferencesManager: PreferencesManager
) {
    suspend operator fun invoke(videoId: String, progressMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        if (preferencesManager.isIncognitoMode.first()) return

        val ratio = progressMs.toFloat() / durationMs
        
        // Threshold Logic:
        // 1. If watched less than 5%, don't save progress (don't clutter history with misclicks)
        // 2. If watched more than 95%, mark as fully completed
        val finalProgress = when {
            ratio < 0.05f -> return 
            ratio > 0.95f -> durationMs
            else -> progressMs
        }

        repository.updateWatchProgress(videoId, finalProgress, durationMs)
    }
}
