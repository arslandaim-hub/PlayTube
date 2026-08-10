/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.repository

import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.model.ChannelInfoBasic
import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import org.schabi.newpipe.extractor.Page

interface VideoRepository {
    suspend fun getStreamBundle(videoId: String, forceRefresh: Boolean = false): StreamBundle
    suspend fun getCachedStreamBundle(videoId: String): StreamBundle?
    suspend fun preloadStreamBundle(videoId: String)
    suspend fun fetchNextRelatedPage(videoId: String, page: Page): PaginatedList<VideoItem>
    suspend fun getChannelDetails(channelUrl: String): ChannelDetails
    suspend fun getChannelInfo(channelUrl: String): ChannelInfoBasic
    suspend fun fetchNextChannelVideosPage(channelUrl: String, page: Page): PaginatedList<VideoItem>
    suspend fun getTrendingVideos(): PaginatedList<VideoItem>
    suspend fun fetchNextTrendingPage(page: Page): PaginatedList<VideoItem>
    suspend fun getPlaylistDetails(playlistUrl: String): PlaylistDetails
}
