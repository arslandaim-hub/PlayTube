/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.repository

import android.util.LruCache
import com.arslandaim.playtube.data.local.PreferencesManager
import com.arslandaim.playtube.data.network.NewPipeInitializer
import com.arslandaim.playtube.domain.model.*
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.utils.CaptionTrackResolver
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.PTLog
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.comments.CommentsInfoItem
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.*
import com.arslandaim.playtube.data.network.InFlightRequestCoalescer
import com.arslandaim.playtube.data.network.ResolvedStreamCache
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val initializer: NewPipeInitializer,
    private val resolvedStreamCache: ResolvedStreamCache,
    private val inFlightRequestCoalescer: InFlightRequestCoalescer
) : VideoRepository {
    private val commentsCache = LruCache<String, CommentsInfoItem>(100)

    private suspend fun ensureInit() {
        initializer.ensureInitialized()
    }

    override suspend fun getStreamBundle(videoId: String, forceRefresh: Boolean): StreamBundle {
        if (videoId.isBlank()) throw IllegalArgumentException("Video ID cannot be blank")
        
        ensureInit()
        val isIncognito = preferencesManager.isIncognitoMode.first()
        
        if (forceRefresh) {
            resolvedStreamCache.evict(videoId)
        } else if (!isIncognito) {
            resolvedStreamCache.get(videoId)?.let { 
                return it 
            }
        }
        
        return inFlightRequestCoalescer.coalesce(videoId) {
            withContext(Dispatchers.IO) {
                try {
                    val service = ServiceList.YouTube
                    val videoUrl = Constants.YouTube.VIDEO_URL_PREFIX + videoId
                    val streamInfo = StreamInfo.getInfo(service, videoUrl)

                    val isLive = streamInfo.streamType == StreamType.LIVE_STREAM || 
                                 streamInfo.streamType == StreamType.AUDIO_LIVE_STREAM ||
                                 streamInfo.streamType.name == "LIVE"

                    val videoStreamsDeferred = async { extractVideoStreams(streamInfo, isLive) }
                    val audioStreamsDeferred = async { extractAudioStreams(streamInfo) }
                    val subtitlesDeferred = async { extractSubtitles(streamInfo) }

                    val bestAudioStream = selectBestAudioStream(streamInfo)

                    val videoStreams = videoStreamsDeferred.await()
                    val isUpcoming = (isLive && videoStreams.isEmpty()) || streamInfo.viewCount == -1L
                    val scheduledStartTime = if (isUpcoming) {
                        streamInfo.uploadDate?.offsetDateTime()?.toString() ?: streamInfo.textualUploadDate
                    } else null

                    val relatedVideos = streamInfo.relatedItems
                        ?.filterIsInstance<StreamInfoItem>()
                        ?.map { mapToVideoItem(it) } ?: emptyList()
                    val firstRelatedId = relatedVideos.firstOrNull { it.id.isNotBlank() && it.id != videoId }?.id
                    val nextRelatedPage = firstRelatedId?.let {
                        Page(Constants.YouTube.VIDEO_URL_PREFIX + it, it)
                    }

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
                        likeCount = streamInfo.likeCount,
                        uploadDate = streamInfo.textualUploadDate ?: streamInfo.uploadDate?.offsetDateTime()?.toLocalDate()?.toString(),
                        thumbnailUrl = streamInfo.thumbnails.maxByOrNull { it.width }?.url ?: VideoUtils.getBestThumbnailUrl(videoId),
                        isLive = isLive,
                        isUpcoming = isUpcoming,
                        scheduledStartTime = scheduledStartTime,
                        relatedVideos = relatedVideos,
                        nextRelatedVideosPage = nextRelatedPage,
                        bestAudioStreamUrl = bestAudioStream?.url,
                        subtitles = subtitlesDeferred.await()
                    )
                    
                    if (!isIncognito) resolvedStreamCache.put(videoId, bundle)
                    bundle
                } catch (e: Exception) {
                    PTLog.e("VideoRepository", "Error fetching stream bundle for $videoId", e)
                    throw e
                }
            }
        }
    }

    private fun extractVideoStreams(streamInfo: StreamInfo, isLive: Boolean): List<StreamItem> {
        val streamsMap = mutableMapOf<String, StreamItem>()
        if (isLive) {
            val liveUrl = streamInfo.hlsUrl?.takeIf { it.isNotBlank() }
                ?: streamInfo.dashMpdUrl?.takeIf { it.isNotBlank() }
            liveUrl?.let { url ->
                val format = if (url == streamInfo.dashMpdUrl) "mpd" else "m3u8"
                val item = StreamItem(url = url, quality = "Auto (Live)", format = format, isAdaptive = false)
                streamsMap[item.quality] = item
            }
        } else {
            // 1. Progressive (muxed) streams as initial fallback
            streamInfo.videoStreams
                ?.filter { !it.url.isNullOrBlank() }
                ?.forEach {
                    val res = it.getResolution() ?: "Unknown"
                    streamsMap[res] = StreamItem(
                        url = it.url ?: "",
                        quality = res,
                        format = it.format?.suffix ?: "mp4",
                        isAdaptive = false
                    )
                }

            // 2. Adaptive video-only streams override progressive streams across all resolutions.
            // Prefer WebM (VP9) first because MatroskaExtractor starts immediately on chunked SABR byte-range streams
            // with C.LENGTH_UNSET without requiring end-of-file moov atom seeks, then MP4 (AVC1), and avoid yt_otf streams.
            streamInfo.videoOnlyStreams
                ?.filter { !it.url.isNullOrBlank() }
                ?.groupBy { it.getResolution() ?: "Unknown" }
                ?.forEach { (res, group) ->
                    val nonOtfGroup = group.filter { it.url?.contains("source=yt_otf") != true }
                    val candidates = nonOtfGroup.ifEmpty { group }
                    val bestAdaptive = candidates.maxWithOrNull(
                        compareBy<VideoStream> { stream ->
                            val codec = stream.codec?.lowercase() ?: ""
                            val suffix = stream.format?.suffix?.lowercase() ?: ""
                            when {
                                codec.startsWith("vp9") || codec.startsWith("vp09") || suffix == "webm" -> 3
                                codec.startsWith("avc1") || (suffix == "mp4" && !codec.startsWith("av01")) -> 2
                                else -> 1
                            }
                        }.thenBy { it.bitrate }
                    )
                    if (bestAdaptive != null) {
                        streamsMap[res] = StreamItem(
                            url = bestAdaptive.url ?: "",
                            quality = res,
                            format = bestAdaptive.format?.suffix ?: "webm",
                            isAdaptive = true
                        )
                    }
                }

            // 3. Fallback to HLS/DASH manifest if no direct streams were extracted
            if (streamsMap.isEmpty()) {
                val fallbackUrl = streamInfo.hlsUrl?.takeIf { it.isNotBlank() }
                    ?: streamInfo.dashMpdUrl?.takeIf { it.isNotBlank() }
                fallbackUrl?.let { url ->
                    val format = if (url == streamInfo.dashMpdUrl) "mpd" else "m3u8"
                    streamsMap["Auto"] = StreamItem(url = url, quality = "Auto", format = format, isAdaptive = false)
                }
            }
        }
        val streams = streamsMap.values.toMutableList()
        if (!isLive) streams.sortByDescending { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        return streams
    }

    private fun extractAudioStreams(streamInfo: StreamInfo): List<StreamItem> {
        val streams = streamInfo.audioStreams?.filter { !it.url.isNullOrBlank() } ?: return emptyList()

        val distinctLocales = streams.mapNotNull { it.audioLocale?.language }.filter { it.isNotBlank() }.distinct()
        val hasDubbedOrDescriptive = streams.any {
            it.audioTrackType == AudioTrackType.DUBBED ||
            it.audioTrackType == AudioTrackType.DESCRIPTIVE ||
            (it.audioTrackName?.contains("dubbed", ignoreCase = true) == true || it.audioTrackName?.contains("descriptive", ignoreCase = true) == true)
        }
        val hasMultipleTracks = distinctLocales.size > 1 || hasDubbedOrDescriptive

        // Group streams by (locale, trackType, format) and pick the highest bitrate stream for each format
        // Keeping both m4a and webm per track ensures adaptive MP4 and WebM downloads always have a compatible audio stream to mux
        val distinctTracks = streams.groupBy {
            val lang = it.audioLocale?.toLanguageTag() ?: it.audioLocale?.language ?: "default"
            val type = it.audioTrackType?.name ?: it.audioTrackName ?: "default"
            val format = it.format?.suffix?.lowercase() ?: "m4a"
            "$lang:$type:$format"
        }.values.mapNotNull { trackGroup ->
            trackGroup.maxByOrNull { it.averageBitrate }
        }

        // Sort distinct tracks: ORIGINAL first, non-dubbed second, English/system locale third, highest bitrate fourth
        val sortedTracks = distinctTracks.sortedWith(
            compareByDescending<AudioStream> { it.audioTrackType == AudioTrackType.ORIGINAL }
                .thenByDescending { it.audioTrackType != AudioTrackType.DUBBED }
                .thenByDescending { it.audioLocale?.language == "en" }
                .thenByDescending { it.averageBitrate }
                .thenBy { it.audioLocale?.displayName ?: it.audioTrackName ?: "" }
        )

        return sortedTracks.map {
            val bitrateKbps = it.averageBitrate.takeIf { b -> b > 0 } ?: 128
            val langName = it.audioLocale?.displayName?.replaceFirstChar { c -> c.uppercase() }
                ?: it.audioLocale?.displayLanguage?.replaceFirstChar { c -> c.uppercase() }
            val trackType = when (it.audioTrackType?.name) {
                "ORIGINAL" -> "Original"
                "DUBBED" -> "Dubbed"
                "DESCRIPTIVE" -> "Descriptive"
                else -> it.audioTrackName
            }
            val labelParts = mutableListOf<String>()
            if (hasMultipleTracks) {
                if (!langName.isNullOrBlank()) labelParts.add(langName)
                if (!trackType.isNullOrBlank() && !labelParts.contains(trackType)) labelParts.add("($trackType)")
            }
            labelParts.add("${bitrateKbps}kbps")

            val displayQuality = labelParts.joinToString(" ")

            StreamItem(
                url = it.url ?: "",
                quality = displayQuality,
                format = it.format?.suffix ?: "m4a",
                languageTag = it.audioLocale?.toLanguageTag() ?: it.audioLocale?.language,
                trackType = it.audioTrackType?.name ?: it.audioTrackName ?: "ORIGINAL"
            )
        }
    }

    private fun extractSubtitles(streamInfo: StreamInfo): List<SubtitleItem> {
        return streamInfo.subtitles?.map { sub ->
            val rawLang = (sub.languageTag ?: "und").lowercase(java.util.Locale.ROOT)
            val trackId = "$rawLang:${if (sub.isAutoGenerated) "asr" else "manual"}"
            val (normalizedUrl, mimeType) = CaptionTrackResolver.normalizeSubtitleUrl(
                rawUrl = sub.url ?: "",
                format = sub.format?.suffix
            )
            val formatSuffix = if (mimeType == androidx.media3.common.MimeTypes.APPLICATION_TTML) "ttml" else (sub.format?.suffix ?: "vtt")
            val label = CaptionTrackResolver.buildDisplayLabel(rawLang, sub.isAutoGenerated)

            SubtitleItem(
                id = trackId,
                url = normalizedUrl,
                languageTag = rawLang,
                format = formatSuffix,
                isAutoGenerated = sub.isAutoGenerated,
                name = label
            )
        }?.sortedWith(compareBy({ it.isAutoGenerated }, { it.languageTag })) ?: emptyList()
    }

    private fun selectBestAudioStream(streamInfo: StreamInfo): AudioStream? {
        val streams = streamInfo.audioStreams ?: return null
        val originals = streams.filter { it.audioTrackType == AudioTrackType.ORIGINAL }
        if (originals.isNotEmpty()) {
            return originals.maxByOrNull { it.averageBitrate }
        }
        val nonDubbed = streams.filter {
            it.audioTrackType != AudioTrackType.DUBBED &&
            it.audioTrackType != AudioTrackType.DESCRIPTIVE &&
            (it.audioTrackName == null || it.audioTrackName?.contains("dubbed", ignoreCase = true) == false)
        }
        val candidatePool = if (nonDubbed.isNotEmpty()) nonDubbed else streams

        return candidatePool.filter { it.audioLocale?.language == "en" }.maxByOrNull { it.averageBitrate }
            ?: candidatePool.maxByOrNull { it.averageBitrate }
    }

    override suspend fun getCachedStreamBundle(videoId: String): StreamBundle? {
        return resolvedStreamCache.get(videoId)
    }

    override suspend fun preloadStreamBundle(videoId: String) {
        if (videoId.isBlank()) return
        if (resolvedStreamCache.get(videoId) != null) return
        try { getStreamBundle(videoId, forceRefresh = false) } catch (e: Exception) { PTLog.w("VideoRepository", "Preload failed for $videoId: ${e.message}") }
    }

    override suspend fun fetchNextRelatedPage(videoId: String, page: Page): PaginatedList<VideoItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val seedVideoId = page.id?.takeIf { it.isNotBlank() }
                    ?: VideoUtils.extractVideoId(page.url)
                if (seedVideoId.isBlank()) return@withContext PaginatedList(emptyList(), null)

                val bundle = getStreamBundle(seedVideoId, forceRefresh = false)
                val items = bundle.relatedVideos.filter { it.id.isNotBlank() && it.id != videoId && it.id != seedVideoId }
                val nextSeedId = items.firstOrNull()?.id
                val nextPage = nextSeedId?.let { Page(Constants.YouTube.VIDEO_URL_PREFIX + it, it) }
                PaginatedList(items, nextPage)
            } catch (e: Exception) {
                PTLog.w("VideoRepository", "Failed to fetch next related videos page for $videoId: ${e.message}")
                PaginatedList(emptyList(), null)
            }
        }
    }

    override suspend fun getChannelInfo(channelUrl: String): ChannelInfoBasic {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val finalUrl = if (channelUrl.startsWith("UC") && !channelUrl.contains("/")) Constants.YouTube.CHANNEL_URL_PREFIX + channelUrl else channelUrl
                val info = ChannelInfo.getInfo(ServiceList.YouTube, finalUrl)
                ChannelInfoBasic(id = info.id ?: "", name = info.name ?: "Unknown", avatarUrl = info.avatars?.find { it.width in 150..300 }?.url ?: info.avatars?.firstOrNull()?.url, subscriberCount = info.subscriberCount)
            } catch (e: Exception) {
                PTLog.e("VideoRepository", "Error fetching channel info for $channelUrl", e)
                throw e
            }
        }
    }

    override suspend fun getChannelDetails(channelUrl: String): ChannelDetails {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val finalUrl = if (channelUrl.startsWith("UC") && !channelUrl.contains("/")) Constants.YouTube.CHANNEL_URL_PREFIX + channelUrl else channelUrl
                val info = ChannelInfo.getInfo(ServiceList.YouTube, finalUrl)
                val avatar = info.avatars?.find { it.width in 150..300 }?.url ?: info.avatars?.firstOrNull()?.url
                
                val videosTask = async {
                    try {
                        val handler = info.tabs.find { it.url.endsWith("/videos") || it.url.contains("flow=grid") } ?: ServiceList.YouTube.channelTabLHFactory.fromUrl(info.url + "/videos")
                        val extractor = ServiceList.YouTube.getChannelTabExtractor(handler)
                        extractor.fetchPage()
                        val page = extractor.initialPage
                        Pair(page.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it, avatar) }, if (page.hasNextPage()) page.nextPage else null)
                    } catch (e: Exception) { PTLog.w("VideoRepository", "Failed to fetch channel videos", e); Pair(emptyList<VideoItem>(), null) }
                }

                val playlistsTask = async {
                    try {
                        val handler = info.tabs.find { it.url.endsWith("/playlists") } ?: ServiceList.YouTube.channelTabLHFactory.fromUrl(info.url + "/playlists")
                        val extractor = ServiceList.YouTube.getChannelTabExtractor(handler)
                        extractor.fetchPage()
                        extractor.initialPage.items.filterIsInstance<PlaylistInfoItem>().map { 
                            PlaylistItem(id = VideoUtils.extractPlaylistId(it.url), title = it.name ?: "Unknown", thumbnailUrl = it.thumbnails?.find { t -> t.width in 400..800 }?.url ?: it.thumbnails?.firstOrNull()?.url ?: "", uploaderName = it.uploaderName ?: "Unknown", uploaderUrl = it.uploaderUrl ?: "", streamCount = it.streamCount)
                        }
                    } catch (e: Exception) { PTLog.w("VideoRepository", "Failed to fetch channel playlists", e); emptyList() }
                }

                val (videos, nextPage) = videosTask.await()
                ChannelDetails(id = info.id ?: "", name = info.name ?: "Unknown", description = info.description, bannerUrl = info.banners?.find { it.width in 800..1500 }?.url ?: info.banners?.firstOrNull()?.url, avatarUrl = avatar, subscriberCount = info.subscriberCount, videos = videos, nextVideosPage = nextPage, playlists = playlistsTask.await())
            } catch (e: Exception) {
                PTLog.e("VideoRepository", "Error fetching channel details for $channelUrl", e)
                throw e
            }
        }
    }

    override suspend fun fetchNextChannelVideosPage(channelUrl: String, page: Page): PaginatedList<VideoItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val handler = ServiceList.YouTube.channelTabLHFactory.fromUrl(channelUrl + "/videos")
                val nextPage = ServiceList.YouTube.getChannelTabExtractor(handler).getPage(page)
                PaginatedList(nextPage.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) }, if (nextPage.hasNextPage()) nextPage.nextPage else null)
            } catch (e: Exception) { PTLog.e("VideoRepository", "Error fetching next channel page", e); PaginatedList(emptyList(), null) }
        }
    }

    override suspend fun getTrendingVideos(): PaginatedList<VideoItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                // 1. Try primary YouTube Trending kiosk
                val primaryItems: List<VideoItem> = try {
                    val kiosk = KioskInfo.getInfo(ServiceList.YouTube, Constants.YouTube.TRENDING_URL)
                    val mapped = kiosk.relatedItems.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) }
                    if (mapped.isNotEmpty()) {
                        return@withContext PaginatedList(items = mapped, nextPage = kiosk.nextPage)
                    }
                    emptyList()
                } catch (e: Exception) {
                    PTLog.w("VideoRepository", "Primary trending kiosk unavailable (${e.message}), falling back to active YouTube kiosks")
                    emptyList()
                }

                // 2. Fallback: aggregate active YouTube Trending kiosks (music, gaming, podcasts) + trending search
                val kioskIds = listOf("trending_music", "trending_gaming", "trending_podcasts_episodes")
                val kioskDeferreds = kioskIds.map { kioskId ->
                    async {
                        try {
                            val extractor = ServiceList.YouTube.kioskList.getExtractorById(kioskId, null)
                            extractor.fetchPage()
                            extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) }
                        } catch (e: Exception) {
                            emptyList()
                        }
                    }
                }

                val searchDeferred = async {
                    try {
                        val searchExtractor = ServiceList.YouTube.getSearchExtractor("Trending videos", listOf("videos"), null)
                        searchExtractor.fetchPage()
                        val page = searchExtractor.initialPage
                        Pair(
                            page.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) },
                            if (page.hasNextPage()) page.nextPage else null
                        )
                    } catch (e: Exception) {
                        Pair(emptyList(), null)
                    }
                }

                val kioskLists = kioskDeferreds.map { it.await() }
                val (searchItems, searchNextPage) = searchDeferred.await()

                // Interleave results across kiosks and search so the Trending feed is diverse
                val interleaved = mutableListOf<VideoItem>()
                val allSources = kioskLists.filter { it.isNotEmpty() } + listOfNotNull(searchItems.takeIf { it.isNotEmpty() })
                val maxLen = allSources.maxOfOrNull { it.size } ?: 0
                for (i in 0 until maxLen) {
                    for (source in allSources) {
                        if (i < source.size) {
                            interleaved.add(source[i])
                        }
                    }
                }

                val combined = (primaryItems + interleaved).distinctBy { it.id }
                PaginatedList(items = combined, nextPage = searchNextPage)
            } catch (e: Exception) { 
                PTLog.e("VideoRepository", "Error fetching trending videos", e)
                PaginatedList(emptyList(), null) 
            }
        }
    }

    override suspend fun fetchNextTrendingPage(page: Page): PaginatedList<VideoItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val nextKiosk = KioskInfo.getMoreItems(ServiceList.YouTube, Constants.YouTube.TRENDING_URL, page)
                val items = nextKiosk.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) }
                if (items.isNotEmpty()) {
                    return@withContext PaginatedList(
                        items = items,
                        nextPage = if (nextKiosk.hasNextPage()) nextKiosk.nextPage else null
                    )
                }
            } catch (e: Exception) {
                PTLog.w("VideoRepository", "Primary next trending page failed, falling back to search pagination: ${e.message}")
            }
            try {
                val searchExtractor = ServiceList.YouTube.getSearchExtractor("Trending videos", listOf("videos"), null)
                val nextPage = searchExtractor.getPage(page)
                PaginatedList(
                    items = nextPage.items.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) },
                    nextPage = if (nextPage.hasNextPage()) nextPage.nextPage else null
                )
            } catch (e: Exception) {
                PTLog.e("VideoRepository", "Error fetching next trending page", e)
                PaginatedList(emptyList(), null)
            }
        }
    }

    private fun mapToVideoItem(item: StreamInfoItem, uploaderThumbnailUrl: String? = null): VideoItem {
        val vId = VideoUtils.extractVideoId(item.url)
        val isShort = VideoUtils.isShort(item)
        return VideoItem(id = vId, title = item.name ?: "Unknown", thumbnailUrl = VideoUtils.getThumbnailForList(vId), uploaderName = item.uploaderName ?: "Unknown", uploaderUrl = item.uploaderUrl ?: "", uploaderThumbnailUrl = uploaderThumbnailUrl ?: item.uploaderAvatars?.firstOrNull()?.url, viewCount = item.viewCount, subscriberCount = null, duration = item.duration, uploadDate = item.textualUploadDate ?: item.uploadDate?.offsetDateTime()?.toLocalDate()?.toString() ?: "", rawUploadDate = item.uploadDate?.instant?.toEpochMilli(), watchProgress = null, isShort = isShort)
    }

    override suspend fun getPlaylistDetails(playlistUrl: String): PlaylistDetails {
        ensureInit()
        return withContext(Dispatchers.IO) {
            try {
                val info = PlaylistInfo.getInfo(ServiceList.YouTube, playlistUrl)
                PlaylistDetails(id = VideoUtils.extractPlaylistId(info.url), title = info.name ?: "Unknown", uploaderName = info.uploaderName ?: "Unknown", uploaderUrl = info.uploaderUrl, thumbnailUrl = info.thumbnails?.find { it.width in 400..800 }?.url ?: info.thumbnails?.firstOrNull()?.url ?: "", videos = info.relatedItems.filterIsInstance<StreamInfoItem>().map { mapToVideoItem(it) })
            } catch (e: Exception) {
                PTLog.e("VideoRepository", "Error fetching playlist details for $playlistUrl", e)
                throw e
            }
        }
    }

    override suspend fun getComments(videoId: String): PaginatedList<CommentItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            val url = Constants.YouTube.VIDEO_URL_PREFIX + videoId
            val commentsInfo = CommentsInfo.getInfo(ServiceList.YouTube, url)
            val items = commentsInfo.relatedItems.filterIsInstance<CommentsInfoItem>().map { mapToCommentItem(it) }
            PaginatedList(items, if (commentsInfo.hasNextPage()) commentsInfo.nextPage else null)
        }
    }

    override suspend fun fetchNextCommentsPage(videoId: String, page: Page): PaginatedList<CommentItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            val url = Constants.YouTube.VIDEO_URL_PREFIX + videoId
            val nextPage = CommentsInfo.getMoreItems(ServiceList.YouTube, url, page)
            val items = nextPage.items.filterIsInstance<CommentsInfoItem>().map { mapToCommentItem(it) }
            PaginatedList(items, if (nextPage.hasNextPage()) nextPage.nextPage else null)
        }
    }

    override suspend fun getCommentReplies(videoId: String, comment: CommentItem): PaginatedList<CommentItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            val infoItem = commentsCache.get(comment.commentId) 
                ?: throw IllegalStateException("Comment info not found in cache")
            
            val repliesPage = infoItem.getReplies()
            if (repliesPage == null) {
                return@withContext PaginatedList(emptyList(), null)
            }
            
            val videoUrl = Constants.YouTube.VIDEO_URL_PREFIX + videoId
            val result = CommentsInfo.getMoreItems(ServiceList.YouTube, videoUrl, repliesPage)
            val items = result.items.filterIsInstance<CommentsInfoItem>().map { mapToCommentItem(it) }
            PaginatedList(items, if (result.hasNextPage()) result.nextPage else null)
        }
    }

    override suspend fun fetchNextCommentRepliesPage(videoId: String, commentId: String, page: Page): PaginatedList<CommentItem> {
        ensureInit()
        return withContext(Dispatchers.IO) {
            val videoUrl = Constants.YouTube.VIDEO_URL_PREFIX + videoId
            val result = CommentsInfo.getMoreItems(ServiceList.YouTube, videoUrl, page)
            val items = result.items.filterIsInstance<CommentsInfoItem>().map { mapToCommentItem(it) }
            PaginatedList(items, if (result.hasNextPage()) result.nextPage else null)
        }
    }

    private fun mapToCommentItem(item: CommentsInfoItem): CommentItem {
        item.commentId?.let { commentsCache.put(it, item) }
        return CommentItem(
            authorName = item.uploaderName ?: "Unknown",
            authorThumbnailUrl = item.uploaderAvatars?.firstOrNull()?.url,
            authorUrl = item.uploaderUrl,
            commentText = VideoUtils.sanitizeDescription(item.commentText.content),
            publishedTime = item.textualUploadDate,
            likeCount = item.likeCount,
            isHeartedByUploader = item.isHeartedByUploader,
            replyCount = item.replyCount,
            commentId = item.commentId ?: ""
        )
    }
}
