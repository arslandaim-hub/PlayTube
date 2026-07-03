/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import javax.inject.Inject

class AddToHistoryUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(history: HistoryEntity) {
        repository.addToHistory(history)
    }
}
