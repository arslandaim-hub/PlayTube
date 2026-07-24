/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(query: String, sort: SearchSort = SearchSort.RELEVANCE): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.search(query, sort))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchNextPage(query: String, sort: SearchSort, page: Page): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.fetchNextPage(query, sort, page))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
