/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.model.PaginatedList
import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.SubtitleItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.utils.VideoUtils
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val preferencesManager: com.arslandaim.playtube.data.local.PreferencesManager
) : VideoRepository {
    private val streamCache = LruCache<String, StreamBundle>(50)

    override suspend fun getStreamBundle(videoId: String, forceRefresh: Boolean): StreamBundle {
        if (videoId.isBlank()) throw IllegalArgumentException("Video ID cannot be blank")
        
        val isIncognito = preferencesManager.isIncognitoMode.first()
        
        if (!forceRefresh && !isIncognito) {
            streamCache.get(videoId)?.let { 
                if (!it.isExpired()) return it 
            }
        }
        
        return withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val isLive = streamInfo.streamType == StreamType.LIVE_STREAM || 
                         streamInfo.streamType == StreamType.AUDIO_LIVE_STREAM ||
                         streamInfo.streamType.name == "LIVE"

            val videoStreamsDeferred = async {
                val streamsMap = mutableMapOf<String, StreamItem>()
                
                if (isLive) {
                    streamInfo.hlsUrl?.let { url ->
                        val item = StreamItem(
                            url = url,
                            quality = "Auto (Live)",
                            format = "m3u8",
                            isAdaptive = false
                        )
                        streamsMap[item.quality] = item
                    }
                } else {
                    // 1. Add legacy muxed streams first
                    streamInfo.videoStreams?.forEach {
                        val resolution = it.getResolution() ?: "Unknown"
                        streamsMap[resolution] = StreamItem(
                            url = it.url ?: "",
                            quality = resolution,
                            format = it.format?.suffix ?: "mp4",
                            isAdaptive = false
                        )
                    }
                    
                    // 2. Overwrite with adaptive (video-only) streams for better performance
                    // YouTube throttles legacy muxed streams much more heavily than adaptive ones.
                    streamInfo.videoOnlyStreams?.forEach {
                        val resolution = it.getResolution() ?: "Unknown"
                        streamsMap[resolution] = StreamItem(
                            url = it.url ?: "",
                            quality = resolution,
                            format = it.format?.suffix ?: "webm",
                            isAdaptive = true
                        )
                    }
                }

                val streams = streamsMap.values.toMutableList()
                if (!isLive) {
                    streams.sortByDescending {
                        it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 
                    }
                }
                streams
            }

            val audioStreamsDeferred = async {
                streamInfo.audioStreams?.map {
                    StreamItem(
                        url = it.url ?: "",
                        quality = "${it.averageBitrate}kbps",
                        format = it.format?.suffix ?: "m4a",
                        languageTag = it.audioLocale?.language,
                        trackType = it.audioTrackType?.name
                    )
                } ?: emptyList()
            }

            val subtitlesDeferred = async {
                streamInfo.subtitles?.map {
                    SubtitleItem(
                        url = it.url ?: "",
                        languageTag = it.languageTag ?: "und",
                        format = it.format?.suffix ?: "vtt",
                        isAutoGenerated = it.isAutoGenerated
                    )
                }?.sortedWith(compareBy({ it.isAutoGenerated }, { it.languageTag })) ?: emptyList()
            }

            val bestAudioStream = streamInfo.audioStreams?.let { streams ->
                val originals = streams.filter { it.audioTrackType == AudioTrackType.ORIGINAL }
                if (originals.isNotEmpty()) {
                    originals.maxByOrNull { it.averageBitrate }
                } else {
                    val english = streams.filter { it.audioLocale?.language == "en" }
                    if (english.isNotEmpty()) {
                        english.maxByOrNull { it.averageBitrate }
                    } else {
                        streams.maxByOrNull { it.averageBitrate }
                    }
                }
            }

            val videoStreams = videoStreamsDeferred.await()
            val isUpcoming = (isLive && videoStreams.isEmpty()) || streamInfo.viewCount == -1L
            val scheduledStartTime = if (isUpcoming) {
                streamInfo.uploadDate?.offsetDateTime()?.toString() ?: streamInfo.textualUploadDate
            } else null

            val bundle = StreamBundle(
                videoStreams = videoStreams,
                audioStreams = audioStreamsDeferred.await(),
                title = streamInfo.name ?: "Unknown",
                uploaderName = streamInfo.uploaderName ?: "Unknown",
                uploaderUrl = streamInfo.uploaderUrl,
                uploaderThumbnailUrl = streamInfo.uploaderAvatars.maxByOrNull { it.width }?.url ?: streamInfo.uploaderAvatars.firstOrNull()?.url,
                uploaderSubscriberCount = streamInfo.uploaderSubscriberCount,
                description = VideoUtils.sanitizeDescription(streamInfo.description?.content),
                viewCount = streamInfo.viewCount,
                uploadDate = streamInfo.textualUploadDate ?: streamInfo.uploadDate?.offsetDateTime()?.toLocalDate()?.toString(),
                thumbnailUrl = streamInfo.thumbnails.maxByOrNull { it.width }?.url ?: streamInfo.thumbnails.firstOrNull()?.url,
                isLive = isLive,
                isUpcoming = isUpcoming,
                scheduledStartTime = scheduledStartTime,
                relatedVideos = streamInfo.relatedItems
                    ?.filterIsInstance<StreamInfoItem>()
                    ?.map { item ->
                        mapToVideoItem(item)
                    } ?: emptyList(),
                nextRelatedVideosPage = null,
                bestAudioStreamUrl = bestAudioStream?.url,
                subtitles = subtitlesDeferred.await()
            )
            
            if (!isIncognito) {
                streamCache.put(videoId, bundle)
            }
            bundle
        }
    }

    override suspend fun getCachedStreamBundle(videoId: String): StreamBundle? {
        val cached = streamCache.get(videoId)
        return if (cached != null && !cached.isExpired()) cached else null
    }

    override suspend fun preloadStreamBundle(videoId: String) {
        if (videoId.isBlank()) return
        val cached = streamCache.get(videoId)
        if (cached != null && !cached.isExpired()) {
            return
        }
        try {
            getStreamBundle(videoId, forceRefresh = true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun fetchNextRelatedPage(videoId: String, page: Page): PaginatedList<VideoItem> {
        // Not implemented as NewPipe doesn't easily expose this for YouTube related videos in current extractor version
        return PaginatedList(emptyList(), null)
    }

    override suspend fun getChannelDetails(channelUrl: String): ChannelDetails {
        return withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube
            
            // Fix: If it's a raw Channel ID (UC...), wrap it in a proper URL for the extractor
            val finalUrl = if (channelUrl.startsWith("UC") && !channelUrl.contains("/")) {
                "https://www.youtube.com/channel/$channelUrl"
            } else {
                channelUrl
            }
            
            val channelInfo = ChannelInfo.getInfo(service, finalUrl)
            val channelAvatarUrl = channelInfo.avatars?.find { it.width in 150..300 }?.url ?: channelInfo.avatars?.firstOrNull()?.url
            
            val videosDeferred = async {
                try {
                    val videosTabLinkHandler = channelInfo.tabs.find {
                        it.url.endsWith("/videos") || it.url.contains("flow=grid")
                    } ?: service.channelTabLHFactory.fromUrl(channelInfo.url + "/videos")

                    val extractor = service.getChannelTabExtractor(videosTabLinkHandler)
                    extractor.fetchPage()
                    val page = extractor.initialPage
                    val videos = page.items
                        .filterIsInstance<StreamInfoItem>()
                        .map { item ->
                            mapToVideoItem(item, channelAvatarUrl)
                        }
                    
                    Pair(videos, if (page.hasNextPage()) page.nextPage else null)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Pair(emptyList<VideoItem>(), null)
                }
            }

            val playlistsDeferred = async {
                try {
                    val playlistsTabLinkHandler = channelInfo.tabs.find {
                        it.url.endsWith("/playlists")
                    } ?: service.channelTabLHFactory.fromUrl(channelInfo.url + "/playlists")

                    val extractor = service.getChannelTabExtractor(playlistsTabLinkHandler)
                    extractor.fetchPage()
                    extractor.initialPage.items
                        .filterIsInstance<PlaylistInfoItem>()
                        .map { item ->
                            PlaylistItem(
                                id = VideoUtils.extractPlaylistId(item.url),
                                title = item.name ?: "Unknown Playlist",
                                thumbnailUrl = item.thumbnails?.find { it.width in 400..800 }?.url ?: item.thumbnails?.firstOrNull()?.url ?: "",
                                uploaderName = item.uploaderName ?: "Unknown Channel",
                                uploaderUrl = item.uploaderUrl ?: "",
                                streamCount = item.streamCount
                            )
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }

            val (videos, nextPage) = videosDeferred.await()

            ChannelDetails(
                id = channelInfo.id ?: "",
                name = channelInfo.name ?: "Unknown",
                description = channelInfo.description,
                bannerUrl = channelInfo.banners?.find { it.width in 800..1500 }?.url ?: channelInfo.banners?.firstOrNull()?.url,
                avatarUrl = channelAvatarUrl,
                subscriberCount = channelInfo.subscriberCount,
                videos = videos,
                nextVideosPage = nextPage,
                playlists = playlistsDeferred.await()
            )
        }
    }

    override suspend fun fetchNextChannelVideosPage(channelUrl: String, page: Page): PaginatedList<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val videosTabLinkHandler = service.channelTabLHFactory.fromUrl(channelUrl + "/videos")
                val extractor = service.getChannelTabExtractor(videosTabLinkHandler)
                val nextPage = extractor.getPage(page)
                
                val videos = nextPage.items.filterIsInstance<StreamInfoItem>().map { item: StreamInfoItem ->
                    mapToVideoItem(item)
                }

                PaginatedList(videos, if (nextPage.hasNextPage()) nextPage.nextPage else null)
            } catch (e: Exception) {
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    override suspend fun getTrendingVideos(): PaginatedList<VideoItem> {
        return withContext(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val kioskInfo = org.schabi.newpipe.extractor.kiosk.KioskInfo.getInfo(service, "https://www.youtube.com/feed/trending")
                PaginatedList(
                    items = kioskInfo.relatedItems.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) },
                    nextPage = kioskInfo.nextPage
                )
            } catch (e: Exception) {
                e.printStackTrace()
                PaginatedList(emptyList(), null)
            }
        }
    }

    override suspend fun fetchNextTrendingPage(page: Page): PaginatedList<VideoItem> {
        // Fallback as generic Page-based fetching is not directly exposed for kiosks in this version
        return PaginatedList(emptyList(), null)
    }

    private fun mapToVideoItem(item: StreamInfoItem, uploaderThumbnailUrl: String? = null): VideoItem {
        val vId = VideoUtils.extractVideoId(item.url)
        return VideoItem(
            id = vId,
            title = item.name ?: "Unknown Title",
            thumbnailUrl = VideoUtils.getBestThumbnailUrl(vId),
            uploaderName = item.uploaderName ?: "Unknown Channel",
            uploaderUrl = item.uploaderUrl ?: "",
            uploaderThumbnailUrl = uploaderThumbnailUrl ?: item.uploaderAvatars?.firstOrNull()?.url,
            viewCount = item.viewCount,
            subscriberCount = null,
            duration = item.duration,
            uploadDate = item.textualUploadDate ?: item.uploadDate?.offsetDateTime()?.toLocalDate()?.toString() ?: "",
            rawUploadDate = item.uploadDate?.instant?.toEpochMilli(),
            watchProgress = null
        )
    }

    override suspend fun getPlaylistDetails(playlistUrl: String): PlaylistDetails {
        return withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube
            val playlistInfo = PlaylistInfo.getInfo(service, playlistUrl)

            PlaylistDetails(
                id = VideoUtils.extractPlaylistId(playlistInfo.url),
                title = playlistInfo.name ?: "Unknown Playlist",
                uploaderName = playlistInfo.uploaderName ?: "Unknown Channel",
                uploaderUrl = playlistInfo.uploaderUrl,
                thumbnailUrl = playlistInfo.thumbnails?.find { it.width in 400..800 }?.url ?: playlistInfo.thumbnails?.firstOrNull()?.url ?: "",
                videos = playlistInfo.relatedItems
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        mapToVideoItem(item)
                    }
            )
        }
    }
}
