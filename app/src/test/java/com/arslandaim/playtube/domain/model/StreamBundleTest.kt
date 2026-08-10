/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.domain.model

import org.junit.Assert.*
import org.junit.Test

class StreamBundleTest {

    @Test
    fun `isExpired returns false for newly created bundle`() {
        val bundle = createBundle(System.currentTimeMillis())
        assertFalse(bundle.isExpired())
    }

    @Test
    fun `isExpired returns true for old bundle`() {
        // 6 hours ago
        val sixHoursAgo = System.currentTimeMillis() - (6 * 60 * 60 * 1000)
        val bundle = createBundle(sixHoursAgo)
        assertTrue(bundle.isExpired())
    }

    @Test
    fun `isExpired returns false for bundle near expiry threshold`() {
        // 5 hours ago (threshold is 5.5)
        val fiveHoursAgo = System.currentTimeMillis() - (5 * 60 * 60 * 1000)
        val bundle = createBundle(fiveHoursAgo)
        assertFalse(bundle.isExpired())
    }

    private fun createBundle(extractedAt: Long): StreamBundle {
        return StreamBundle(
            videoStreams = emptyList(),
            audioStreams = emptyList(),
            title = "Test",
            uploaderName = "Uploader",
            uploaderUrl = null,
            uploaderThumbnailUrl = null,
            description = null,
            viewCount = 0,
            uploadDate = null,
            thumbnailUrl = null,
            extractedAt = extractedAt
        )
    }
}
