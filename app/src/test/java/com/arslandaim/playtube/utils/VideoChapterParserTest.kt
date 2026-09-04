/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import org.junit.Assert.*
import org.junit.Test

class VideoChapterParserTest {

    @Test
    fun testParseDescriptionWithStandardChapters() {
        val description = """
            Welcome to the video!
            
            Timestamps:
            00:00 Introduction
            02:15 Setup & Installation
            05:40 Writing First Feature
            12:30 Conclusion
        """.trimIndent()

        val chapters = VideoChapterParser.parseChapters(description)

        assertEquals(4, chapters.size)
        assertEquals("Introduction", chapters[0].title)
        assertEquals(0L, chapters[0].startMs)

        assertEquals("Setup & Installation", chapters[1].title)
        assertEquals((2 * 60 + 15) * 1000L, chapters[1].startMs)

        assertEquals("Writing First Feature", chapters[2].title)
        assertEquals((5 * 60 + 40) * 1000L, chapters[2].startMs)

        assertEquals("Conclusion", chapters[3].title)
        assertEquals((12 * 60 + 30) * 1000L, chapters[3].startMs)
    }

    @Test
    fun testParseDescriptionWithHourTimestamps() {
        val description = """
            0:00 Intro
            1:15:30 Long Deep Dive
        """.trimIndent()

        val chapters = VideoChapterParser.parseChapters(description)

        assertEquals(2, chapters.size)
        assertEquals("Intro", chapters[0].title)
        assertEquals(0L, chapters[0].startMs)

        assertEquals("Long Deep Dive", chapters[1].title)
        assertEquals((1 * 3600 + 15 * 60 + 30) * 1000L, chapters[1].startMs)
    }

    @Test
    fun testParseDescriptionWithNoChaptersReturnsEmpty() {
        val description = "Just a regular description without any timestamps."
        val chapters = VideoChapterParser.parseChapters(description)
        assertTrue(chapters.isEmpty())
    }
}
