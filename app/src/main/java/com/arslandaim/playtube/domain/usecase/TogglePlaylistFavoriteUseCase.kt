/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.PlaylistFavoriteEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TogglePlaylistFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    suspend operator fun invoke(favorite: PlaylistFavoriteEntity) {
        val isFavorite = repository.isPlaylistFavorite(favorite.playlistId).first()
        if (isFavorite) {
            repository.removeFromPlaylistFavorites(favorite)
        } else {
            repository.addToPlaylistFavorites(favorite)
        }
    }
}
