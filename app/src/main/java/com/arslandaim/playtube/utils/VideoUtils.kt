/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.utils

object VideoUtils {
    fun extractVideoId(url: String?): String {
        if (url == null) return ""
        val trimmed = url.trim()
        
        val id = when {
            trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&")
            trimmed.contains("/shorts/") -> trimmed.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
            trimmed.contains("/embed/") -> trimmed.substringAfter("/embed/").substringBefore("?").substringBefore("/")
            trimmed.contains("/v/") -> trimmed.substringAfter("/v/").substringBefore("?").substringBefore("/")
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            trimmed.contains("/vi/") -> trimmed.substringAfter("/vi/").substringBefore("/")
            !trimmed.contains("/") && !trimmed.contains("=") -> trimmed // Already an ID
            else -> trimmed.substringAfterLast("/").substringBefore("?")
        }.trim()

        // YouTube video IDs are 11 characters.
        return if (id.length == 11) id else ""
    }

    fun extractPlaylistId(url: String?): String {
        if (url == null) return ""
        val trimmed = url.trim()
        if (!trimmed.contains("/") && !trimmed.contains("=")) return trimmed

        return when {
            trimmed.contains("list=") -> trimmed.substringAfter("list=").substringBefore("&")
            else -> trimmed.substringAfterLast("/")
        }.trim()
    }

    private const val THUMB_BASE_URL = "https://i.ytimg.com/vi"

    fun getHq720ThumbnailUrl(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/hq720.jpg"
    }

    fun getMaxResThumbnail(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/maxresdefault.jpg"
    }

    fun getSdResThumbnailUrl(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/sddefault.jpg"
    }

    fun getHighResThumbnail(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/hqdefault.jpg"
    }

    fun getMediumResThumbnail(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/mqdefault.jpg"
    }

    fun getLowResThumbnail(videoId: String): String {
        return "$THUMB_BASE_URL/$videoId/default.jpg"
    }

    fun getBestThumbnailUrl(videoId: String): String {
        return getMaxResThumbnail(videoId)
    }

    fun getThumbnailForList(videoId: String): String {
        return getHighResThumbnail(videoId)
    }

    fun getFallbackThumbnailUrl(videoId: String): String {
        return getHighResThumbnail(videoId)
    }

    fun formatNumber(number: Long): String {
        return when {
            number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> "$number"
        }.replace(".0", "")
    }

    fun formatViewCount(views: Long): String {
        if (views == -1L) return "Upcoming"
        return formatNumber(views)
    }

    fun formatUploadDate(date: String?): String {
        if (date == null) return ""
        return when {
            // Handle absolute ISO timestamps for upcoming streams
            date.contains("T") && date.contains("-") -> {
                try {
                    val odt = java.time.OffsetDateTime.parse(date)
                    val now = java.time.OffsetDateTime.now()
                    if (odt.isAfter(now)) {
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, HH:mm")
                        "Starts ${odt.format(formatter)}"
                    } else {
                        formatRelativeTime(date)
                    }
                } catch (e: Exception) {
                    formatRelativeTime(date)
                }
            }
            else -> date
        }
    }

    /**
     * Formats an ISO 8601 date string into a relative time string (e.g. "2 hours ago").
     */
    fun formatRelativeTime(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        return try {
            val odt = java.time.OffsetDateTime.parse(isoDate)
            val timeMillis = odt.toInstant().toEpochMilli()
            android.text.format.DateUtils.getRelativeTimeSpanString(
                timeMillis,
                System.currentTimeMillis(),
                android.text.format.DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s)
        } else {
            String.format(java.util.Locale.getDefault(), "%d:%02d", m, s)
        }
    }

    fun extractChannelId(url: String?): String? {
        if (url == null) return null
        val trimmed = url.trim()
        
        // If it's already just a UC... ID
        if (trimmed.startsWith("UC") && trimmed.length == 24) {
            return trimmed
        }
        
        // Match standard UC... IDs in the URL
        if (trimmed.contains("/channel/UC")) {
            val id = "UC" + trimmed.substringAfter("/channel/UC").substringBefore("/")
            if (id.length == 24) return id
        }
        
        // Handles and custom URLs don't contain the channel ID directly
        return null
    }

    fun sanitizeDescription(html: String?): String {
        if (html == null) return ""
        return try {
            androidx.core.text.HtmlCompat.fromHtml(
                html,
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
            ).toString().trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
    }

    /**
     * Parses textual upload dates like "5 minutes ago", "2 hours ago", "1 day ago", "yesterday", "streamed 2 hours ago"
     * into an approximate timestamp (Long) for sorting purposes.
     */
    fun parseTextualUploadDate(text: String?): Long {
        if (text == null || text.isBlank()) return 0L
        
        val cleanText = text.lowercase()
            .replace(" ago", "")
            .replace(" streaming", "")
            .replace("streamed ", "")
            .trim()
            
        // Direct matches for common single-unit strings
        if (cleanText.contains("yesterday")) return System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        
        val parts = cleanText.split(" ")
        
        var amount: Long? = null
        var unit: String? = null
        
        // Find the numeric amount and the unit following it
        for (i in parts.indices) {
            val num = parts[i].toLongOrNull()
            if (num != null) {
                amount = num
                if (i + 1 < parts.size) {
                    unit = parts[i + 1]
                }
                break
            }
        }
        
        // Handle "a year ago", "an hour ago"
        if (amount == null) {
            if (cleanText.contains("a ") || cleanText.contains("an ") || cleanText.contains("one ")) {
                amount = 1L
                unit = parts.last() // Usually the unit
            }
        }
        
        if (amount == null || unit == null) return 0L
        
        val now = System.currentTimeMillis()
        val multiplier = when {
            unit.contains("second") -> 1000L
            unit.contains("minute") -> 60 * 1000L
            unit.contains("hour") -> 60 * 60 * 1000L
            unit.contains("day") -> 24 * 60 * 60 * 1000L
            unit.contains("week") -> 7 * 24 * 60 * 60 * 1000L
            unit.contains("month") -> 30 * 24 * 60 * 60 * 1000L
            unit.contains("year") -> 365 * 24 * 60 * 60 * 1000L
            else -> 0L
        }
        
        if (multiplier == 0L) return 0L
        return now - (amount * multiplier)
    }
}
