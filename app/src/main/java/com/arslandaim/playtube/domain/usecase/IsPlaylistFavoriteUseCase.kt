package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsPlaylistFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(playlistId: String): Flow<Boolean> = repository.isPlaylistFavorite(playlistId)
}
