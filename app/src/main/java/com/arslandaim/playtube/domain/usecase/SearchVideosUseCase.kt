/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.usecase

import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.UploadDateFilter
import com.arslandaim.playtube.domain.model.DurationFilter
import com.arslandaim.playtube.domain.repository.SearchRepository
import kotlinx.coroutines.CancellationException
import org.schabi.newpipe.extractor.Page
import javax.inject.Inject

class SearchVideosUseCase @Inject constructor(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL
    ): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.search(query, sort, uploadDate, duration))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }

    suspend fun fetchNextPage(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL,
        page: Page
    ): Result<PaginatedList<SearchItem>> {
        return try {
            Result.success(repository.fetchNextPage(query, sort, uploadDate, duration, page))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
