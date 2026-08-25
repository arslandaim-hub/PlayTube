/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.SubscriptionFeedSkeleton
import com.arslandaim.playtube.ui.components.VideoList
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SubscriptionFeedScreen(
    viewModel: SubscriptionsFeedViewModel,
    libraryViewModel: com.arslandaim.playtube.ui.screens.library.LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSubscriptionsList: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    
    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }
    
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val onRefresh = remember(viewModel) { { viewModel.refresh() } }
    val onFavoriteClick = remember(viewModel) { { video: VideoItem -> viewModel.toggleFavorite(video) } }
    val onDownloadClick = remember(viewModel) { { video: VideoItem -> viewModel.prepareDownload(video) } }
    val onDownloadConfirm = remember(viewModel) {
        { video: VideoItem, bundle: com.arslandaim.playtube.domain.model.StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean ->
            viewModel.download(video, bundle, url, quality, format, isAdaptive)
        }
    }
    val onDismissDownload = remember(viewModel) { { viewModel.dismissDownloadDialog() } }
    val onChannelSelected = remember(viewModel) { { channelId: String? -> viewModel.onChannelSelected(channelId) } }
    val onLoadMore = remember(viewModel) { { viewModel.loadMore() } }

    var isReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300) // Wait for transition
        viewModel.loadSubscriptionsFeed()
        isReady = true
    }

    SubscriptionFeedContent(
        state = state,
        isRefreshing = isRefreshing,
        downloadState = downloadState,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        savedVideoIds = savedVideoIds,
        snackbarMessage = viewModel.snackbarMessage,
        onRefresh = onRefresh,
        onFavoriteClick = onFavoriteClick,
        onDownloadClick = onDownloadClick,
        onDownloadConfirm = onDownloadConfirm,
        onDismissDownload = onDismissDownload,
        onChannelSelected = onChannelSelected,
        onLoadMore = onLoadMore,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onNavigateToDownloads = onNavigateToDownloads,
        onNavigateToSubscriptionsList = onNavigateToSubscriptionsList,
        isReady = isReady
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionFeedContent(
    state: SubscriptionsFeedUIState,
    isRefreshing: Boolean,
    downloadState: DownloadDialogState,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    savedVideoIds: Set<String>,
    snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String>,
    onRefresh: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, com.arslandaim.playtube.domain.model.StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onChannelSelected: (String?) -> Unit,
    onLoadMore: () -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSubscriptionsList: () -> Unit,
    isReady: Boolean
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snackbarMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(Unit) {
        onBarsVisibilityChange(true)
    }

    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)
    val bubbleListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Immersive Ambient Header Glow
        val activeFeed = state.activeFeed
        val firstVideoThumbnail = activeFeed.videos.firstOrNull()?.thumbnailUrl
        val selectedChannelThumbnail = state.subscriptions.find { it.channelId == state.selectedChannelId }?.thumbnailUrl
        val ambientColorSource = selectedChannelThumbnail ?: firstVideoThumbnail

        if (!ambientColorSource.isNullOrBlank()) {
            val blurEffect = remember(ambientColorSource) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    android.graphics.RenderEffect.createBlurEffect(
                        120f, 120f, android.graphics.Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                } else null
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .graphicsLayer {
                        alpha = 0.35f
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            renderEffect = blurEffect
                        }
                    }
                    .then(
                        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                            Modifier.blur(100.dp)
                        } else Modifier
                    )
            ) {
                com.arslandaim.playtube.ui.components.ThumbnailImage(
                    videoId = "",
                    thumbnailUrl = ambientColorSource,
                    quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.Low,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Vertical gradient to blend into background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
            }
        }

        val pullToRefreshState = rememberPullToRefreshState()
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize().nestedScroll(scrollVisibilityConnection),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                val isLoadingInitial = activeFeed.isLoading && activeFeed.videos.isEmpty() && !state.isThoroughSearching
                
                if (isLoadingInitial) {
                    if (state.subscriptions.isEmpty()) {
                        SubscriptionFeedSkeleton()
                    } else {
                        Column {
                            SubscriptionFilterBar(
                                subscriptions = state.subscriptions,
                                selectedChannelId = state.selectedChannelId,
                                onChannelSelected = onChannelSelected,
                                onViewAllClick = onNavigateToSubscriptionsList,
                                listState = bubbleListState
                            )
                            com.arslandaim.playtube.ui.components.VideoListSkeleton()
                        }
                    }
                } else if (activeFeed.error != null && activeFeed.videos.isEmpty()) {
                    val isNetworkError = activeFeed.error is PlayTubeError.Network
                    
                    EmptyState(
                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                        description = if (isNetworkError) "Your downloads are still available offline." else activeFeed.error.getMessage(),
                        actionText = if (isNetworkError) "Checkout Downloads" else stringResource(R.string.retry),
                        onActionClick = { 
                            if (isNetworkError) onNavigateToDownloads() else onRefresh()
                        }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (activeFeed.videos.isEmpty() && !activeFeed.isLoading && !state.isThoroughSearching && activeFeed.error == null) {
                            EmptyState(
                                icon = Icons.Default.ErrorOutline,
                                title = stringResource(R.string.no_subscriptions_videos),
                                description = if (state.subscriptions.isEmpty()) "Subscribe to channels to see their latest videos here." else "No new videos from your subscriptions yet.",
                                actionText = if (state.subscriptions.isEmpty()) "Explore Content" else stringResource(R.string.retry),
                                onActionClick = onRefresh
                            )
                        } else if (isReady || state.isThoroughSearching || activeFeed.videos.isNotEmpty()) {
                            VideoList(
                                videos = activeFeed.videos,
                                downloadedIds = downloadedIds,
                                favoriteIds = favoriteIds,
                                savedVideoIds = savedVideoIds,
                                onVideoClick = onVideoClick,
                                onChannelClick = onChannelClick,
                                onFavoriteClick = onFavoriteClick,
                                onNotInterestedClick = null,
                                onDownloadClick = onDownloadClick,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onLoadMore = onLoadMore,
                                isLoadingMore = activeFeed.isLoadingMore,
                                header = {
                                    if (state.subscriptions.isNotEmpty()) {
                                        Column {
                                            SubscriptionFilterBar(
                                                subscriptions = state.subscriptions,
                                                selectedChannelId = state.selectedChannelId,
                                                onChannelSelected = onChannelSelected,
                                                onViewAllClick = onNavigateToSubscriptionsList,
                                                listState = bubbleListState
                                            )
                                            
                                            if (state.isThoroughSearching) {
                                                LinearProgressIndicator(
                                                    progress = { state.thoroughSearchProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(2.dp),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = Color.Transparent
                                                )
                                                Text(
                                                    text = "Updating feed... ${(state.thoroughSearchProgress * 100).toInt()}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(
                                    top = 0.dp,
                                    bottom = 100.dp
                                )
                            )
                        }
                    }
                }
            }
        }

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

        state.activeFeed.error?.let { error ->
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(error.getMessage())
            }
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@Composable
private fun SubscriptionFilterBar(
    subscriptions: List<com.arslandaim.playtube.data.local.SubscriptionEntity>,
    selectedChannelId: String?,
    onChannelSelected: (String?) -> Unit,
    onViewAllClick: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "all") {
                val currentOnSelected = remember(onChannelSelected) { { onChannelSelected(null) } }
                ChannelBubble(
                    name = "All",
                    thumbnailUrl = null,
                    isSelected = selectedChannelId == null,
                    onClick = currentOnSelected
                )
            }

            items(
                items = subscriptions,
                key = { it.channelId }
            ) { sub ->
                val currentOnSelected = remember(sub.channelId, onChannelSelected) {
                    { onChannelSelected(sub.channelId) }
                }
                ChannelBubble(
                    name = sub.name,
                    thumbnailUrl = sub.thumbnailUrl,
                    isSelected = selectedChannelId == sub.channelId,
                    onClick = currentOnSelected
                )
            }

            item(key = "view_all") {
                // Sleek circular View All Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(56.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = onViewAllClick
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), CircleShape)
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View All",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelBubble(
    name: String,
    thumbnailUrl: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.1f else 1f, label = "BubbleScale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.7f, label = "BubbleAlpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .then(
                    if (isSelected) Modifier.border(
                        width = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ) else Modifier.border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailUrl != null) {
                com.arslandaim.playtube.ui.components.ThumbnailImage(
                    videoId = "",
                    thumbnailUrl = thumbnailUrl,
                    quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.Medium,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Subscriptions,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
