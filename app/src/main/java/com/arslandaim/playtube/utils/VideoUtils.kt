package com.arslandaim.playtube.utils

object VideoUtils {
    fun extractVideoId(url: String?): String {
        if (url == null) return ""
        val trimmed = url.trim()
        if (!trimmed.contains("/") && !trimmed.contains("=")) return trimmed // Already an ID
        
        return when {
            trimmed.contains("v=") -> trimmed.substringAfter("v=").substringBefore("&")
            trimmed.contains("/shorts/") -> trimmed.substringAfter("/shorts/").substringBefore("?")
            trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?")
            else -> trimmed.substringAfterLast("/")
        }.trim()
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

    fun getHighResThumbnail(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }

    fun getMaxResThumbnail(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    }

    fun getBestThumbnailUrl(videoId: String): String {
        // hqdefault (480x360) is the sweet spot for mobile lists.
        // It's much smaller than maxresdefault (1920x1080) but still high quality.
        return getHighResThumbnail(videoId)
    }

    fun getMediumResThumbnail(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
    }

    fun getLowResThumbnail(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/default.jpg"
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
        return "${formatNumber(views)} views"
    }

    fun formatUploadDate(date: String?): String {
        if (date == null) return ""
        return date
    }
}
