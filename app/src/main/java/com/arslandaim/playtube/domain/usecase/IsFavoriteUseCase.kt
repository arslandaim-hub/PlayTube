package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class IsFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(videoId: String): Flow<Boolean> = repository.isFavorite(videoId)
}
