/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.data.network

import android.util.Xml
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.utils.VideoUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRssParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun fetchChannelFeed(channelId: String): List<VideoItem> {
        val cid = if (channelId.startsWith("UC") && !channelId.contains("/")) channelId else VideoUtils.extractChannelId(channelId) ?: channelId
        val url = "https://www.youtube.com/feeds/videos.xml?channel_id=$cid"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return emptyList()
            val xmlString = response.body?.string() ?: return emptyList()
            parseFeedXml(xmlString, cid)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseFeedXml(xml: String, channelId: String): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inEntry = false

        var videoId = ""
        var title = ""
        var authorName = ""
        var publishedIso = ""
        var thumbnailUrl = ""
        var viewCount = 0L

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName == "entry") {
                        inEntry = true
                        videoId = ""
                        title = ""
                        authorName = ""
                        publishedIso = ""
                        thumbnailUrl = ""
                        viewCount = 0L
                    } else if (inEntry) {
                        when (tagName) {
                            "yt:videoId" -> videoId = parser.nextText().trim()
                            "title" -> title = parser.nextText().trim()
                            "name" -> if (authorName.isEmpty()) authorName = parser.nextText().trim()
                            "published" -> publishedIso = parser.nextText().trim()
                            "media:thumbnail" -> {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank() && thumbnailUrl.isEmpty()) {
                                    thumbnailUrl = url
                                }
                            }
                            "media:statistics" -> {
                                val views = parser.getAttributeValue(null, "views")
                                viewCount = views?.toLongOrNull() ?: 0L
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "entry") {
                        if (videoId.isNotBlank() && title.isNotBlank()) {
                            val rawDate = try {
                                OffsetDateTime.parse(publishedIso).toInstant().toEpochMilli()
                            } catch (e: Exception) {
                                System.currentTimeMillis()
                            }
                            val formattedDate = VideoUtils.formatRelativeTime(publishedIso)
                            val finalThumb = if (thumbnailUrl.isNotBlank()) thumbnailUrl else VideoUtils.getThumbnailForList(videoId)

                            videos.add(
                                VideoItem(
                                    id = videoId,
                                    title = title,
                                    thumbnailUrl = finalThumb,
                                    uploaderName = authorName.ifBlank { "Channel" },
                                    uploaderUrl = Constants.YouTube.CHANNEL_URL_PREFIX + channelId,
                                    viewCount = viewCount,
                                    uploadDate = formattedDate,
                                    rawUploadDate = rawDate,
                                    duration = 0L,
                                    isShort = false // Hard to reliably detect from RSS, usually mixed. Will skip filtering here or rely on backend.
                                )
                            )
                        }
                        inEntry = false
                    }
                }
            }
            eventType = parser.next()
        }
        return videos
    }
}
