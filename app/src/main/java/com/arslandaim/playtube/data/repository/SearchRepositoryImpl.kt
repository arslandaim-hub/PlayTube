/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor() : SearchRepository {
    override suspend fun search(query: String, sort: SearchSort): PaginatedList<SearchItem> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube

                // NewPipe YouTube extractor expects specific string labels for sort filters.
                // These are case-sensitive and must match the service's available sort filters.
                val sortFilter = when (sort) {
                    SearchSort.RELEVANCE -> "relevance"
                    SearchSort.UPLOAD_DATE -> "upload_date"
                    SearchSort.VIEW_COUNT -> "view_count"
                    SearchSort.RATING -> "rating"
                }

                val contentFilter = if (sort == SearchSort.UPLOAD_DATE) listOf("videos") else listOf("all")
                
                android.util.Log.d("SearchRepository", "Searching for: $query with sort filter: $sortFilter and content filter: $contentFilter")
                
                val extractor = youtubeService.getSearchExtractor(
                    query,
                    contentFilter,
                    sortFilter
                )
                extractor.fetchPage()

                val page = extractor.initialPage
                val items = page.items.mapNotNull { item ->
                    when (item) {
                        is StreamInfoItem -> SearchItem.Video(mapToVideoItem(item))
                        is ChannelInfoItem -> SearchItem.Channel(
                            id = item.url, // Usually full URL in NewPipe
                            name = item.name ?: "Unknown Channel",
                            thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
                            subscriberCount = item.subscriberCount,
                            description = item.description
                        )
                        is PlaylistInfoItem -> SearchItem.Playlist(
                            PlaylistItem(
                                id = VideoUtils.extractPlaylistId(item.url),
                                title = item.name ?: "Unknown Playlist",
                                thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "",
                                uploaderName = item.uploaderName ?: "Unknown Channel",
                                uploaderUrl = item.uploaderUrl,
                                streamCount = item.streamCount
                            )
                        )
                        else -> null
                    }
                }

                PaginatedList(items, if (page.hasNextPage()) page.nextPage else null)
            } catch (e: Exception) {
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    override suspend fun fetchNextPage(query: String, sort: SearchSort, page: Page): PaginatedList<SearchItem> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube

                val sortFilter = when (sort) {
                    SearchSort.RELEVANCE -> "relevance"
                    SearchSort.UPLOAD_DATE -> "upload_date"
                    SearchSort.VIEW_COUNT -> "view_count"
                    SearchSort.RATING -> "rating"
                }

                val contentFilter = if (sort == SearchSort.UPLOAD_DATE) listOf("videos") else listOf("all")
                
                android.util.Log.d("SearchRepository", "Fetching next page for: $query with sort filter: $sortFilter and content filter: $contentFilter")
                
                val extractor = youtubeService.getSearchExtractor(
                    query,
                    contentFilter,
                    sortFilter
                )
                val nextPage = extractor.getPage(page)
                
                val items = nextPage.items.mapNotNull { item ->
                    when (item) {
                        is StreamInfoItem -> SearchItem.Video(mapToVideoItem(item))
                        is ChannelInfoItem -> SearchItem.Channel(
                            id = item.url,
                            name = item.name ?: "Unknown Channel",
                            thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
                            subscriberCount = item.subscriberCount,
                            description = item.description
                        )
                        is PlaylistInfoItem -> SearchItem.Playlist(
                            PlaylistItem(
                                id = VideoUtils.extractPlaylistId(item.url),
                                title = item.name ?: "Unknown Playlist",
                                thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "",
                                uploaderName = item.uploaderName ?: "Unknown Channel",
                                uploaderUrl = item.uploaderUrl,
                                streamCount = item.streamCount
                            )
                        )
                        else -> null
                    }
                }

                PaginatedList(items, if (nextPage.hasNextPage()) nextPage.nextPage else null)
            } catch (e: Exception) {
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    private fun mapToVideoItem(item: StreamInfoItem): VideoItem {
        val videoId = VideoUtils.extractVideoId(item.url)
        val uploadDate = item.textualUploadDate ?: item.uploadDate?.offsetDateTime()?.toLocalDate()?.toString() ?: ""
        val rawUploadDate = item.uploadDate?.instant?.toEpochMilli() ?: VideoUtils.parseTextualUploadDate(item.textualUploadDate)
        
        return VideoItem(
            id = videoId,
            title = item.name ?: "Unknown Title",
            thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
            uploaderName = item.uploaderName ?: "Unknown Channel",
            uploaderUrl = item.uploaderUrl ?: "",
            uploaderThumbnailUrl = item.uploaderAvatars?.firstOrNull()?.url,
            viewCount = item.viewCount,
            subscriberCount = null,
            duration = item.duration,
            uploadDate = uploadDate,
            rawUploadDate = rawUploadDate
        )
    }

    override suspend fun getSearchSuggestions(query: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube
                youtubeService.suggestionExtractor.suggestionList(query)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}
