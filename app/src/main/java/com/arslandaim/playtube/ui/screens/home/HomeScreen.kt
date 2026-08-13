/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.VideoListSkeleton
import com.arslandaim.playtube.ui.components.VideoList
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.EmptyState

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import com.arslandaim.playtube.ui.theme.GlassAlpha
import com.arslandaim.playtube.R
import com.arslandaim.playtube.utils.PlayTubeError
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    libraryViewModel: com.arslandaim.playtube.ui.screens.library.LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onNavigateToDownloads: () -> Unit // New parameter
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    
    // Optimized: Using remember(favorites) for ID mapping to avoid O(N) mapping on every recomposition
    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        isRefreshing = isRefreshing,
        downloadState = downloadState,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        savedVideoIds = savedVideoIds,
        snackbarMessage = viewModel.snackbarMessage,
        onRefresh = viewModel::refresh,
        onLoadMore = viewModel::loadNextTrendingPage,
        onFavoriteClick = viewModel::toggleFavorite,
        onNotInterestedClick = viewModel::markNotInterested,
        onDownloadClick = viewModel::prepareDownload,
        onDownloadConfirm = viewModel::download,
        onDismissDownload = viewModel::dismissDownloadDialog,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onPersonalizedNotifyShown = viewModel::onPersonalizedNotifyShown,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onNavigateToDownloads = onNavigateToDownloads // Pass it down
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeState,
    isRefreshing: Boolean,
    downloadState: DownloadDialogState,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    savedVideoIds: Set<String>,
    snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String>,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onNotInterestedClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, com.arslandaim.playtube.domain.model.StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onPersonalizedNotifyShown: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onNavigateToDownloads: () -> Unit // New parameter
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Header Scroll State
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val connection = remember(headerHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // Bottom Bar Hiding Logic (Binary Toggle for global Bars)
                if (delta < -15f) onBarsVisibilityChange(false)
                if (delta > 15f) onBarsVisibilityChange(true)

                // Scrolling Down: Collapse Header
                if (delta < 0 && headerOffsetPx > -headerHeightPx) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                
                // Scrolling Up: Expand Header
                // Check if we are at the top of the list or scrolling up significantly
                if (delta > 0 && headerOffsetPx < 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    val consumed = newOffset - headerOffsetPx
                    headerOffsetPx = newOffset
                    return Offset(0f, consumed)
                }

                return Offset.Zero
            }
        }
    }

    // Ensure Bars are initially visible
    LaunchedEffect(Unit) {
        onBarsVisibilityChange(true)
    }

    LaunchedEffect(Unit) {
        onBarsVisibilityChange(true)
    }

    val pullToRefreshState = rememberPullToRefreshState()

    // Floating Notification State
    var showPersonalizedNotify by remember { mutableStateOf(false) }
    
    LaunchedEffect(state.isPersonalized) {
        if (state.isPersonalized) {
            showPersonalizedNotify = true
            kotlinx.coroutines.delay(4000)
            showPersonalizedNotify = false
            onPersonalizedNotifyShown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onRefresh() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection)
        ) {
            val videos = state.trendingVideos
            val isLoading = state.isTrendingLoading

            AnimatedContent(
                targetState = isLoading && videos.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "HomeContentTransition"
            ) { loading ->
                if (loading) {
                    VideoListSkeleton()
                } else if (state.error != null && videos.isEmpty()) {
                    val isNetworkError = state.error is PlayTubeError.Network
                    
                    EmptyState(
                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                        description = if (isNetworkError) "Your downloads are still available offline." else state.error.getMessage(),
                        actionText = if (isNetworkError) "Checkout Downloads" else stringResource(R.string.retry),
                        onActionClick = { 
                            if (isNetworkError) onNavigateToDownloads() else onRefresh()
                        }
                    )
                } else if (!isLoading && videos.isEmpty() && state.error == null) {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.no_videos_found),
                        description = "Couldn't find any videos right now. Try refreshing later.",
                        actionText = stringResource(R.string.retry),
                        onActionClick = { onRefresh() }
                    )
                } else {
                    VideoList(
                        videos = videos,
                        downloadedIds = downloadedIds,
                        favoriteIds = favoriteIds,
                        savedVideoIds = savedVideoIds,
                        onVideoClick = onVideoClick,
                        onChannelClick = onChannelClick,
                        onFavoriteClick = onFavoriteClick,
                        onNotInterestedClick = onNotInterestedClick,
                        onDownloadClick = onDownloadClick,
                        onAddToPlaylistClick = onAddToPlaylistClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        contentPadding = PaddingValues(
                            top = with(density) { headerHeightPx.toDp() },
                            bottom = 100.dp
                        )
                    )
                }
            }
        }

        // Header placeholder (could be used for categories if added later)
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { layoutCoordinates ->
                    val newHeight = layoutCoordinates.size.height.toFloat()
                    if (headerHeightPx != newHeight) {
                        headerHeightPx = newHeight
                    }
                }
        )

        // Quick Action Dialogs
        when (val downloadDialogState = downloadState) {
            DownloadDialogState.Idle -> {}
            is DownloadDialogState.Loading -> {
                AlertDialog(
                    onDismissRequest = { onDismissDownload() },
                    confirmButton = {},
                    title = { Text(stringResource(R.string.loading)) },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                )
            }
            is DownloadDialogState.ShowDialog -> {
                DownloadSelectionSheet(
                    videoStreams = downloadDialogState.bundle.videoStreams,
                    onDismiss = { onDismissDownload() },
                    onDownload = { stream ->
                        onDownloadConfirm(
                            downloadDialogState.video,
                            downloadDialogState.bundle,
                            stream.url,
                            stream.quality,
                            stream.format,
                            stream.isAdaptive
                        )
                    }
                )
            }
        }

        state.error?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(error.getMessage())
            }
        }

        // Floating Personalized Notification
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp), // Float above bottom bar
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = showPersonalizedNotify,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = GlassAlpha),
                    shape = CircleShape,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.personalized_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.personalized_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}
