/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.UploadDateFilter
import com.arslandaim.playtube.domain.model.DurationFilter
import org.schabi.newpipe.extractor.Page

interface SearchRepository {
    suspend fun search(
        query: String,
        sort: SearchSort = SearchSort.RELEVANCE,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL
    ): PaginatedList<SearchItem>

    suspend fun fetchNextPage(
        query: String,
        sort: SearchSort,
        uploadDate: UploadDateFilter = UploadDateFilter.ALL,
        duration: DurationFilter = DurationFilter.ALL,
        page: Page
    ): PaginatedList<SearchItem>

    suspend fun getSearchSuggestions(query: String): List<String>
}
