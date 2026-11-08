/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import org.junit.Assert.*
import org.junit.Test

class VideoUtilsTest {

    @Test
    fun `extractVideoId correctly parses various formats`() {
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://www.youtube.com/v/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("https://youtu.be/dQw4w9WgXcQ?t=10"))
        assertEquals("dQw4w9WgXcQ", VideoUtils.extractVideoId("dQw4w9WgXcQ")) // Raw ID
    }

    @Test
    fun `extractPlaylistId correctly parses various formats`() {
        assertEquals("PL12345", VideoUtils.extractPlaylistId("https://www.youtube.com/playlist?list=PL12345"))
        assertEquals("PL12345", VideoUtils.extractPlaylistId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL12345"))
        assertEquals("PL12345", VideoUtils.extractPlaylistId("PL12345")) // Raw ID
    }

    @Test
    fun `formatNumber formats correctly`() {
        assertEquals("1K", VideoUtils.formatNumber(1000))
        assertEquals("1.5K", VideoUtils.formatNumber(1500))
        assertEquals("1M", VideoUtils.formatNumber(1_000_000))
        assertEquals("2.5B", VideoUtils.formatNumber(2_500_000_000L))
        assertEquals("500", VideoUtils.formatNumber(500))
    }

    @Test
    fun `formatDuration formats correctly`() {
        assertEquals("0:05", VideoUtils.formatDuration(5))
        assertEquals("1:00", VideoUtils.formatDuration(60))
        assertEquals("1:01:01", VideoUtils.formatDuration(3661))
    }

    @Test
    fun `extractChannelId identifies UC IDs`() {
        assertEquals("UC1234567890123456789012", VideoUtils.extractChannelId("UC1234567890123456789012"))
        assertEquals("UC1234567890123456789012", VideoUtils.extractChannelId("https://www.youtube.com/channel/UC1234567890123456789012"))
        assertNull(VideoUtils.extractChannelId("https://www.youtube.com/@handle"))
    }

    @Test
    fun `isShort correctly identifies shorts from VideoItem`() {
        val normalVideo = com.arslandaim.playtube.domain.model.VideoItem(
            id = "abc12345678",
            title = "Full Length Documentary",
            thumbnailUrl = "https://i.ytimg.com/vi/abc12345678/hqdefault.jpg",
            uploaderName = "Test Channel",
            uploaderUrl = "https://youtube.com/channel/test",
            viewCount = 1000,
            uploadDate = "1 day ago",
            duration = 600,
            isShort = false
        )
        assertFalse(VideoUtils.isShort(normalVideo))

        val explicitShort = normalVideo.copy(isShort = true)
        assertTrue(VideoUtils.isShort(explicitShort))

        val titleHashtagShort = normalVideo.copy(title = "Funny Dog Video #shorts", duration = 30)
        assertTrue(VideoUtils.isShort(titleHashtagShort))

        val shortTitleAndDuration = normalVideo.copy(title = "Cute Cat Short", duration = 45)
        assertTrue(VideoUtils.isShort(shortTitleAndDuration))
    }
}
