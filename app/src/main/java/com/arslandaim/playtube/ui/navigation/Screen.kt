/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Subscriptions : Screen("channels")
    object Library : Screen("library")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object History : Screen("history")
    object SubscriptionsList : Screen("subscriptions_list")
    object Channel : Screen("channel/{channelUrl}") {
        fun createRoute(channelUrl: String) = "channel/${URLEncoder.encode(channelUrl, StandardCharsets.UTF_8.toString())}"
    }
    object Player : Screen("player/{videoId}?title={title}&thumbnail={thumbnail}") {
        fun createRoute(videoId: String, title: String? = null, thumbnailUrl: String? = null): String {
            val encodedTitle = title?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
            val encodedThumbnail = thumbnailUrl?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.toString()) } ?: ""
            return "player/$videoId?title=$encodedTitle&thumbnail=$encodedThumbnail"
        }
    }
    object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
}
