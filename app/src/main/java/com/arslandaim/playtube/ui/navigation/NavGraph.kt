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
import com.arslandaim.playtube.MainViewModel
import com.arslandaim.playtube.ui.screens.home.HomeScreen
import com.arslandaim.playtube.ui.screens.home.HomeViewModel
import com.arslandaim.playtube.ui.screens.onboarding.InterestsSelectionScreen
import com.arslandaim.playtube.ui.screens.onboarding.OnboardingViewModel
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
import com.arslandaim.playtube.ui.screens.settings.UpdateViewModel
import com.arslandaim.playtube.ui.screens.settings.DataManagementScreen
import com.arslandaim.playtube.ui.screens.settings.DataManagementViewModel
import com.arslandaim.playtube.ui.screens.subscriptions.SubscriptionsScreen
import com.arslandaim.playtube.ui.screens.subscriptions.SubscriptionFeedScreen
import com.arslandaim.playtube.ui.screens.subscriptions.SubscriptionsFeedViewModel

import androidx.navigation.toRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: Destination = Destination.Home,
    onBarsVisibilityChange: (Boolean) -> Unit
) {
    val activity = LocalActivity.current as ComponentActivity
    val playerViewModel: PlayerViewModel = hiltViewModel(activity)
    val libraryViewModel: LibraryViewModel = hiltViewModel(activity)
    val mainViewModel: MainViewModel = hiltViewModel(activity)

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
        startDestination = startDestination,
        enterTransition = {
            onBarsVisibilityChange(true)
            val initial = initialState.destination.route?.toDestination()
            val target = targetState.destination.route?.toDestination()
            val isBottomTab = initial?.isTopLevel == true && target?.isTopLevel == true
            
            if (isBottomTab) {
                fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeIn(animationSpec = tween(200))
            }
        },
        exitTransition = {
            val initial = initialState.destination.route?.toDestination()
            val target = targetState.destination.route?.toDestination()
            val isBottomTab = initial?.isTopLevel == true && target?.isTopLevel == true
            
            if (isBottomTab) {
                fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing))
            } else {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = slideSpring
                ) + fadeOut(animationSpec = tween(200))
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
        composable<Destination.Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onChannelClick = { channelUrl: String ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Destination.Subscriptions> {
            val viewModel: SubscriptionsFeedViewModel = hiltViewModel()
            SubscriptionFeedScreen(
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onChannelClick = { channelUrl: String ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSubscriptionsList = {
                    navController.navigate(Destination.SubscriptionsList) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable<Destination.SubscriptionsList> {
            SubscriptionsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBackClick = { navController.popBackStack() },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                showTopAppBar = true
            )
        }
        composable<Destination.Library> {
            LibraryScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                },
                onSeeAllHistory = { navController.navigate(Destination.History) { launchSingleTop = true } },
                onSeeAllSubscriptions = { navController.navigate(Destination.SubscriptionsList) { launchSingleTop = true } },
                onSeeAllDownloads = { navController.navigate(Destination.Downloads) { launchSingleTop = true } }
            )
        }
        composable<Destination.Downloads> {
            com.arslandaim.playtube.ui.screens.library.DownloadsScreen(
                viewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                }
            )
        }
        composable<Destination.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val updateViewModel: UpdateViewModel = hiltViewModel(activity)
            SettingsScreen(
                viewModel = viewModel,
                updateViewModel = updateViewModel,
                onViewHistory = { navController.navigate(Destination.History) { launchSingleTop = true } },
                onDataManagement = { navController.navigate(Destination.DataManagement) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.DataManagement> {
            val viewModel: DataManagementViewModel = hiltViewModel()
            DataManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.History> {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            HistoryScreen(
                settingsViewModel = settingsViewModel,
                historyViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onDiscoverVideos = {
                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Home) { inclusive = true }
                    }
                }
            )
        }
        composable<Destination.Channel> { backStackEntry ->
            val channel: Destination.Channel = backStackEntry.toRoute()
            val viewModel: ChannelViewModel = hiltViewModel()
            ChannelScreen(
                channelUrl = channel.channelUrl,
                viewModel = viewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                }
            )
        }
        composable<Destination.Playlist> { backStackEntry ->
            val playlist: Destination.Playlist = backStackEntry.toRoute()
            val viewModel: PlaylistViewModel = hiltViewModel()
            PlaylistScreen(
                playlistId = playlist.playlistId,
                viewModel = viewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                }
            )
        }
        composable<Destination.Search> { backStackEntry ->
            val search: Destination.Search = backStackEntry.toRoute()
            val viewModel: SearchViewModel = hiltViewModel()
            val mainViewModel: MainViewModel = hiltViewModel(activity)
            val updateViewModel: UpdateViewModel = hiltViewModel(activity)

            androidx.compose.runtime.LaunchedEffect(search.query) {
                if (!search.query.isNullOrBlank()) {
                    viewModel.onQueryChange(search.query)
                    viewModel.search(search.query)
                }
            }

            SearchScreen(
                viewModel = viewModel,
                mainViewModel = mainViewModel,
                updateViewModel = updateViewModel,
                libraryViewModel = libraryViewModel,
                onBarsVisibilityChange = onBarsVisibilityChange,
                onVideoClick = { video ->
                    playerViewModel.loadVideo(video)
                },
                onAddToPlaylistClick = { video ->
                    mainViewModel.showPlaylistSelection(video)
                },
                onChannelClick = { channelUrl ->
                    navController.navigate(Destination.Channel(channelUrl))
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Destination.Playlist(playlistId))
                },
                onNavigateToDownloads = {
                    navController.navigate(Destination.Downloads) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Destination.Settings) {
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<Destination.Onboarding> {
            val viewModel: OnboardingViewModel = hiltViewModel()
            InterestsSelectionScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate(Destination.Home) {
                        popUpTo(Destination.Onboarding) { inclusive = true }
                    }
                }
            )
        }
    }
}
