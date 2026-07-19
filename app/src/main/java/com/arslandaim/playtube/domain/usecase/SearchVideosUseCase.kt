/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String, sort: SearchSort = SearchSort.RELEVANCE): Result<List<VideoItem>> {
        return try {
            Result.success(repository.search(query, sort))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
