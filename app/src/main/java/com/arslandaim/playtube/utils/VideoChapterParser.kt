/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

import java.util.Locale

data class VideoChapter(
    val title: String,
    val startMs: Long
) {
    fun formattedTimestamp(): String {
        val totalSeconds = startMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}

object VideoChapterParser {
    // Matches timestamps like 0:00, 00:00, 1:23:45, 01:23:45
    private val TIMESTAMP_REGEX = Regex("""(?:\b(?:\d{1,2}:)?\d{1,2}:\d{2}\b)""")

    fun parseChapters(description: String?): List<VideoChapter> {
        if (description.isNullOrBlank()) return emptyList()

        val chapters = mutableListOf<VideoChapter>()
        val lines = description.lines()

        for (line in lines) {
            val match = TIMESTAMP_REGEX.find(line) ?: continue
            val timeStr = match.value
            val startMs = parseTimestampToMs(timeStr) ?: continue

            // Extract the title text around or after the timestamp
            var title = line.replace(timeStr, "").trim()
            // Clean up leading/trailing punctuation like "-", ":", "•", "–"
            title = title.replace(Regex("""^[—–\-:•\s]+|[—–\-:•\s]+$"""), "").trim()

            if (title.isBlank()) {
                title = "Chapter at $timeStr"
            }

            chapters.add(VideoChapter(title = title, startMs = startMs))
        }

        // Sort chapters chronologically and filter out invalid timestamp ordering
        val sorted = chapters.distinctBy { it.startMs }.sortedBy { it.startMs }
        return if (sorted.size >= 2) sorted else emptyList()
    }

    private fun parseTimestampToMs(timeStr: String): Long? {
        val parts = timeStr.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> {
                val minutes = parts[0]
                val seconds = parts[1]
                (minutes * 60 + seconds) * 1000L
            }
            3 -> {
                val hours = parts[0]
                val minutes = parts[1]
                val seconds = parts[2]
                (hours * 3600 + minutes * 60 + seconds) * 1000L
            }
            else -> null
        }
    }
}
