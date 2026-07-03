package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String): Result<List<VideoItem>> {
        return try {
            Result.success(repository.search(query))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
