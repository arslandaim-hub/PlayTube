package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.domain.repository.LibraryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFavoritesUseCase @Inject constructor(
    private val repository: LibraryRepository
) {
    operator fun invoke(): Flow<List<FavoriteEntity>> = repository.getFavorites()
}
