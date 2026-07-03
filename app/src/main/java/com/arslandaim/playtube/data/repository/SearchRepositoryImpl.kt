package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor() : SearchRepository {
    override suspend fun search(query: String): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube
                val queryHandler = youtubeService.getSearchQHFactory().fromQuery(query)
                val extractor = youtubeService.getSearchExtractor(queryHandler)
                extractor.fetchPage()

                val allItems = mutableListOf<StreamInfoItem>()
                
                // Fetch first page
                val page = extractor.initialPage
                allItems.addAll(page.items.filterIsInstance<StreamInfoItem>())

                // Fetch second page if available to make the list bigger
                // In NewPipeExtractor, we check hasNextPage on the Page object
                if (page.hasNextPage()) {
                    try {
                        val nextPage = extractor.getPage(page.nextPage)
                        allItems.addAll(nextPage.items.filterIsInstance<StreamInfoItem>())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                allItems.distinctBy { it.url }.map { item ->
                        val videoId = VideoUtils.extractVideoId(item.url)
                        VideoItem(
                            id = videoId,
                            title = item.name ?: "Unknown Title",
                            thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                            uploaderName = item.uploaderName ?: "Unknown Channel",
                            uploaderUrl = item.uploaderUrl ?: "",
                            viewCount = item.viewCount,
                            uploadDate = item.uploadDate?.toString() ?: "",
                            duration = item.duration
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube
                val suggestionExtractor = youtubeService.suggestionExtractor
                suggestionExtractor.suggestionList(query)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
