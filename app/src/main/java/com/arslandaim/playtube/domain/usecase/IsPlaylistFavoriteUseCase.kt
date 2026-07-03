/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsPlaylistFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(playlistId: String): Flow<Boolean> = repository.isPlaylistFavorite(playlistId)
}
