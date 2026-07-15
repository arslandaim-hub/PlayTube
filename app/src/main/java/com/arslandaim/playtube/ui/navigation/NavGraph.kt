/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.arslandaim.playtube.ui.screens.home.HomeScreen
import com.arslandaim.playtube.ui.screens.home.HomeViewModel
import com.arslandaim.playtube.ui.screens.library.LibraryScreen
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.ui.screens.channel.ChannelScreen
import com.arslandaim.playtube.ui.screens.channel.ChannelViewModel
import com.arslandaim.playtube.ui.screens.history.HistoryScreen
import com.arslandaim.playtube.ui.screens.player.PlayerScreen
import com.arslandaim.playtube.ui.screens.player.PlayerViewModel
import com.arslandaim.playtube.ui.screens.playlist.PlaylistScreen
import com.arslandaim.playtube.ui.screens.playlist.PlaylistViewModel
import com.arslandaim.playtube.ui.screens.search.SearchScreen
import com.arslandaim.playtube.ui.screens.search.SearchViewModel
import com.arslandaim.playtube.ui.screens.settings.SettingsScreen
import com.arslandaim.playtube.ui.screens.settings.SettingsViewModel
import com.arslandaim.playtube.ui.screens.subscriptions.SubscriptionsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    onBarsVisibilityChange: (Boolean) -> Unit
) {
    val activity = LocalActivity.current as ComponentActivity
    val playerViewModel: PlayerViewModel = hiltViewModel(activity)
    val libraryViewModel: LibraryViewModel = hiltViewModel(activity)

    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    
    val slideSpring = spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            onBarsVisibilityChange(true)
            val isBottomTab = initialState.destination.route in listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Library.route) &&
                             targetState.destination.route in listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Library.route)
            
            if (isBottomTab) {
                fadeIn(animationSpec = tween(300))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeIn(animationSpec = tween(300))
            }
        },
        exitTransition = {
            val isBottomTab = initialState.destination.route in listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Library.route) &&
                             targetState.destination.route in listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Library.route)
            
            if (isBottomTab) {
                fadeOut(animationSpec = tween(300))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeOut(animationSpec = tween(300))
            }
        },
        popEnterTransition = {
            onBarsVisibilityChange(true)
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = slideSpring
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = slideSpring
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Home.route) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Screen.Channel.createRoute(channelUrl))
                }
            )
        }
        composable(Screen.Subscriptions.route) {
            SubscriptionsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channelUrl ->
                    navController.navigate(Screen.Channel.createRoute(channelUrl))
                },
                showTopAppBar = false
            )
        }
        composable(Screen.SubscriptionsList.route) {
            SubscriptionsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channelUrl ->
                    navController.navigate(Screen.Channel.createRoute(channelUrl))
                },
                showTopAppBar = true
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onSeeAllHistory = { navController.navigate(Screen.History.route) },
                onSeeAllSubscriptions = { navController.navigate(Screen.SubscriptionsList.route) }
            )
        }
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onViewHistory = { navController.navigate(Screen.History.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            HistoryScreen(
                settingsViewModel = settingsViewModel,
                historyViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                }
            )
        }
        composable(Screen.Channel.route) { backStackEntry ->
            val channelUrl = backStackEntry.arguments?.getString("channelUrl") ?: ""
            val viewModel: ChannelViewModel = hiltViewModel()
            ChannelScreen(
                channelUrl = java.net.URLDecoder.decode(channelUrl, "UTF-8"),
                viewModel = viewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.Playlist.createRoute(playlistId))
                }
            )
        }
        composable(Screen.Playlist.route) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            val viewModel: PlaylistViewModel = hiltViewModel()
            PlaylistScreen(
                playlistId = playlistId,
                viewModel = viewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                }
            )
        }
        composable(Screen.Search.route) {
            val viewModel: SearchViewModel = hiltViewModel()
            SearchScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Screen.Channel.createRoute(channelUrl))
                },
                onBack = { navController.popBackStack() }
            )
        }
        // Removed separate Player composable as it's now a global overlay
    }
}
