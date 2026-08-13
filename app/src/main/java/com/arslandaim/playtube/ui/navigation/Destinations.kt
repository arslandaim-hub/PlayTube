/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    val isTopLevel: Boolean get() = false

    @Serializable data object Home : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Subscriptions : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Library : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data class Search(val query: String? = null) : Destination {
        override val isTopLevel: Boolean get() = true
    }
    @Serializable data object Settings : Destination
    @Serializable data object History : Destination
    @Serializable data object SubscriptionsList : Destination
    @Serializable data object Downloads : Destination
    @Serializable data class Channel(val channelUrl: String) : Destination
    @Serializable data class Player(
        val videoId: String,
        val title: String? = null,
        val thumbnailUrl: String? = null
    ) : Destination
    @Serializable data class Playlist(val playlistId: String) : Destination
    @Serializable data object Onboarding : Destination
    @Serializable data object DataManagement : Destination
}

fun String?.toDestination(): Destination? {
    val route = this ?: return null
    return when {
        route.contains("Home") -> Destination.Home
        route.contains("Subscriptions") -> Destination.Subscriptions
        route.contains("Library") -> Destination.Library
        route.contains("Search") -> Destination.Search()
        route.contains("Settings") -> Destination.Settings
        route.contains("History") -> Destination.History
        route.contains("SubscriptionsList") -> Destination.SubscriptionsList
        route.contains("Downloads") -> Destination.Downloads
        route.contains("Onboarding") -> Destination.Onboarding
        route.contains("DataManagement") -> Destination.DataManagement
        else -> null
    }
}
