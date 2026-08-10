/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    open val navigationRoute: String get() = route
    open val isTopLevel: Boolean get() = false

    object Home : Screen("home") {
        override val isTopLevel: Boolean get() = true
    }
    object Subscriptions : Screen("subscriptions") {
        override val isTopLevel: Boolean get() = true
    }
    object Library : Screen("library") {
        override val isTopLevel: Boolean get() = true
    }
    object Search : Screen("search?query={query}") {
        override val navigationRoute: String get() = "search"
        override val isTopLevel: Boolean get() = true
        fun createRoute(query: String? = null) = if (query != null) "search?query=${URLEncoder.encode(query, StandardCharsets.UTF_8.toString())}" else navigationRoute
    }
    object Settings : Screen("settings")
    object History : Screen("history")
    object SubscriptionsList : Screen("subscriptions_list")
    object Downloads : Screen("downloads")
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
    object Onboarding : Screen("onboarding")
    object DataManagement : Screen("data_management")

    companion object {
        private val screens by lazy {
            listOf(
                Home, Subscriptions, Library, Search, Settings, History,
                SubscriptionsList, Downloads, Channel, Player, Playlist,
                Onboarding, DataManagement
            )
        }

        fun fromRoute(route: String?): Screen? {
            val baseRoute = route?.split("?")?.firstOrNull()?.split("/")?.firstOrNull() ?: return null
            return screens.find { 
                it.route.split("?").firstOrNull()?.split("/")?.firstOrNull() == baseRoute 
            }
        }
    }
}
