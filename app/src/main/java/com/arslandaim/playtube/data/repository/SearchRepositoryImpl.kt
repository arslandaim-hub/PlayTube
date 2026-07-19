/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.SearchRepository
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Locale

class SearchRepositoryImpl @Inject constructor() : SearchRepository {
    override suspend fun search(query: String, sort: SearchSort): List<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeService = ServiceList.YouTube
                // Correct YouTube Sort Filters for NewPipe: "date", "viewCount", "rating"
                val queryHandler = youtubeService.getSearchQHFactory().fromQuery(
                    query, 
                    listOf("videos"), 
                    sort.value
                )
                val extractor = youtubeService.getSearchExtractor(queryHandler)
                extractor.fetchPage()

                val allItems = mutableListOf<StreamInfoItem>()
                
                // Fetch first page
                val page = extractor.initialPage
                allItems.addAll(page.items.filterIsInstance<StreamInfoItem>())

                // Fetch second page if available to make the list bigger
                if (page.hasNextPage()) {
                    try {
                        val nextPage = extractor.getPage(page.nextPage)
                        allItems.addAll(nextPage.items.filterIsInstance<StreamInfoItem>())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val videos = allItems.distinctBy { it.url }.map { item ->
                    val videoId = VideoUtils.extractVideoId(item.url)
                    VideoItem(
                        id = videoId,
                        title = item.name ?: "Unknown Title",
                        thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                        uploaderName = item.uploaderName ?: "Unknown Channel",
                        uploaderUrl = item.uploaderUrl ?: "",
                        uploaderThumbnailUrl = item.uploaderAvatars?.firstOrNull()?.url,
                        viewCount = item.viewCount,
                        subscriberCount = null,
                        uploadDate = item.textualUploadDate ?: item.uploadDate?.toString() ?: "",
                        duration = item.duration
                    )
                }

                // SECONDARY LOCAL SORT: Guarantee correct order even if API results are slightly mixed
                when (sort) {
                    SearchSort.UPLOAD_DATE -> videos.sortedByDescending { it.uploadDate?.parseDate() ?: 0L }
                    SearchSort.VIEW_COUNT -> videos.sortedByDescending { it.viewCount }
                    else -> videos
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    // Helper to parse varying date strings from YouTube for accurate local sorting
    private fun String.parseDate(): Long {
        if (isEmpty()) return 0
        val lowerText = this.lowercase(Locale.US)
        
        // 1. Try ISO date first (e.g., "2024-05-10")
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = format.parse(this)
            if (date != null) return date.time
        } catch (e: Exception) { /* Continue to relative parsing */ }

        // 2. Smart Relative Parsing (e.g., "2 hours ago", "5 minutes ago", "Streamed 2 days ago")
        try {
            val now = System.currentTimeMillis()
            var parts = lowerText.split(" ")
            
            // Skip "Streamed" or "Premiered" prefixes
            if (parts.isNotEmpty() && (parts[0] == "streamed" || parts[0] == "premiered")) {
                parts = parts.drop(1)
            }

            if (parts.size < 2) {
                if (lowerText.contains("just now")) return now
                return 0
            }
            
            val amount = parts[0].toLongOrNull() ?: when(parts[0]) {
                "a", "an" -> 1L
                else -> return 0
            }
            val unit = parts[1]

            return when {
                unit.contains("minute") -> now - (amount * 60 * 1000)
                unit.contains("hour") -> now - (amount * 60 * 60 * 1000)
                unit.contains("day") -> now - (amount * 24 * 60 * 60 * 1000)
                unit.contains("week") -> now - (amount * 7 * 24 * 60 * 60 * 1000)
                unit.contains("month") -> now - (amount * 30 * 24 * 60 * 60 * 1000)
                unit.contains("year") -> now - (amount * 365 * 24 * 60 * 60 * 1000)
                else -> 0
            }
        } catch (e: Exception) {
            return 0
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
