/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import com.arslandaim.playtube.domain.model.ChannelDetails
import com.arslandaim.playtube.domain.model.PlaylistDetails
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.utils.VideoUtils
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor() : VideoRepository {
    private val streamCache = LruCache<String, StreamBundle>(50)

    override suspend fun getStreamBundle(videoId: String): StreamBundle {
        if (videoId.isBlank()) throw IllegalArgumentException("Video ID cannot be blank")
        streamCache.get(videoId)?.let { return it }
        
        return withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val videoStreams = mutableListOf<StreamItem>()
            streamInfo.videoStreams?.forEach {
                videoStreams.add(
                    StreamItem(
                        url = it.url ?: "",
                        quality = it.getResolution() ?: "Unknown",
                        format = it.format?.suffix ?: "mp4",
                        isAdaptive = false
                    )
                )
            }
            streamInfo.videoOnlyStreams?.forEach {
                videoStreams.add(
                    StreamItem(
                        url = it.url ?: "",
                        quality = it.getResolution() ?: "Unknown",
                        format = it.format?.suffix ?: "webm",
                        isAdaptive = true
                    )
                )
            }

            val audioStreams = streamInfo.audioStreams?.map {
                StreamItem(
                    url = it.url ?: "",
                    quality = "${it.averageBitrate}kbps",
                    format = it.format?.suffix ?: "m4a",
                    languageTag = it.audioLocale?.language,
                    trackType = it.audioTrackType?.name
                )
            } ?: emptyList()

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

            val bundle = StreamBundle(
                videoStreams = videoStreams.sortedByDescending {
                    it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 
                },
                audioStreams = audioStreams,
                title = streamInfo.name ?: "Unknown",
                uploaderName = streamInfo.uploaderName ?: "Unknown",
                uploaderUrl = streamInfo.uploaderUrl,
                uploaderThumbnailUrl = streamInfo.uploaderAvatars.find { it.width in 80..180 }?.url ?: streamInfo.uploaderAvatars.firstOrNull()?.url,
                description = streamInfo.description?.content,
                viewCount = streamInfo.viewCount,
                uploadDate = streamInfo.uploadDate?.toString(),
                thumbnailUrl = streamInfo.thumbnails?.maxByOrNull { it.width }?.url ?: streamInfo.thumbnails?.firstOrNull()?.url,
                relatedVideos = streamInfo.relatedItems
                    ?.filterIsInstance<StreamInfoItem>()
                    ?.map { item ->
                        val videoId = VideoUtils.extractVideoId(item.url)
                        VideoItem(
                            id = videoId,
                            title = item.name ?: "Unknown Title",
                            thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                            uploaderName = item.uploaderName ?: "Unknown Channel",
                            uploaderUrl = item.uploaderUrl ?: "",
                            viewCount = item.viewCount,
                            duration = item.duration,
                            uploadDate = item.uploadDate?.toString() ?: ""
                        )
                    } ?: emptyList(),
                bestAudioStreamUrl = bestAudioStream?.url
            )
            streamCache.put(videoId, bundle)
            bundle
        }
    }

    override suspend fun getChannelDetails(channelUrl: String): ChannelDetails {
        return withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube
            val channelInfo = ChannelInfo.getInfo(service, channelUrl)

            // Explicitly fetch the "Videos" tab
            val videos = try {
                val videosTabLinkHandler = channelInfo.tabs.find {
                    it.url.endsWith("/videos") || it.url.contains("flow=grid")
                } ?: service.channelTabLHFactory.fromUrl(channelInfo.url + "/videos")

                val extractor = service.getChannelTabExtractor(videosTabLinkHandler)
                extractor.fetchPage()
                extractor.initialPage.items
                    .filterIsInstance<StreamInfoItem>()
                    .map { item ->
                        val videoId = VideoUtils.extractVideoId(item.url)
                        VideoItem(
                            id = videoId,
                            title = item.name ?: "Unknown Title",
                            thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                            uploaderName = item.uploaderName ?: "Unknown Channel",
                            uploaderUrl = item.uploaderUrl ?: "",
                            viewCount = item.viewCount,
                            duration = item.duration,
                            uploadDate = item.uploadDate?.toString() ?: ""
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            // Explicitly fetch the "Playlists" tab
            val playlists = try {
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

            ChannelDetails(
                id = channelInfo.id ?: "",
                name = channelInfo.name ?: "Unknown",
                description = channelInfo.description,
                bannerUrl = channelInfo.banners?.find { it.width in 800..1500 }?.url ?: channelInfo.banners?.firstOrNull()?.url,
                avatarUrl = channelInfo.avatars?.find { it.width in 150..300 }?.url ?: channelInfo.avatars?.firstOrNull()?.url,
                subscriberCount = channelInfo.subscriberCount,
                videos = videos,
                playlists = playlists
            )
        }
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
                        val videoId = VideoUtils.extractVideoId(item.url)
                        VideoItem(
                            id = videoId,
                            title = item.name ?: "Unknown Title",
                            thumbnailUrl = VideoUtils.getBestThumbnailUrl(videoId),
                            uploaderName = item.uploaderName ?: "Unknown Channel",
                            uploaderUrl = item.uploaderUrl ?: "",
                            viewCount = item.viewCount,
                            duration = item.duration,
                            uploadDate = item.uploadDate?.toString() ?: ""
                        )
                    }
            )
        }
    }
}
