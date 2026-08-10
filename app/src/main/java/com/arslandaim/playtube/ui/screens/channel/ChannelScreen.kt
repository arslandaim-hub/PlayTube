/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.InfiniteScrollEffect
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import com.arslandaim.playtube.ui.theme.GlassAlpha

@Composable
fun ChannelScreen(
    channelUrl: String,
    viewModel: ChannelViewModel,
    libraryViewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsState()
    val favorites by libraryViewModel.favorites.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    ChannelContent(
        channelUrl = channelUrl,
        uiState = uiState,
        isSubscribed = isSubscribed,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        downloadState = downloadState,
        snackbarMessage = viewModel.snackbarMessage,
        onLoadChannel = viewModel::loadChannel,
        onLoadMore = viewModel::loadNextPage,
        onToggleSubscription = viewModel::toggleSubscription,
        onFavoriteClick = viewModel::toggleFavorite,
        onDownloadClick = viewModel::prepareDownload,
        onDownloadConfirm = viewModel::download,
        onDismissDownload = viewModel::dismissDownloadDialog,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onNavigateToDownloads = onNavigateToDownloads,
        onBack = onBack,
        onVideoClick = onVideoClick,
        onPlaylistClick = onPlaylistClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelContent(
    channelUrl: String,
    uiState: ChannelUiState,
    isSubscribed: Boolean?,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    downloadState: DownloadDialogState,
    snackbarMessage: SharedFlow<String>,
    onLoadChannel: (String) -> Unit,
    onLoadMore: () -> Unit,
    onToggleSubscription: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.videos), stringResource(R.string.playlists))
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(channelUrl) {
        onLoadChannel(channelUrl)
    }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val listState = rememberLazyListState()
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    // Parallax Calculation
    val bannerHeight = 200.dp
    val bannerHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { bannerHeight.toPx() }
    
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else {
                bannerHeightPx
            }
        }
    }

    val bannerProgress = (1f - (scrollOffset / bannerHeightPx)).coerceIn(0f, 1f)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is ChannelUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChannelUiState.Success -> {
                    val details = uiState.details
                    
                    InfiniteScrollEffect(
                        listState = listState,
                        enabled = details.nextVideosPage != null && !uiState.isFetchingNextPage,
                        onLoadMore = onLoadMore
                    )
                    
                    // Immersive Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .graphicsLayer {
                                translationY = -scrollOffset * 0.5f
                                alpha = bannerProgress
                                scaleX = 1f + (1f - bannerProgress) * 0.2f
                                scaleY = 1f + (1f - bannerProgress) * 0.2f
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(details.bannerUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient Overlay for better contrast
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent, Color.Black.copy(alpha = 0.4f))
                                    )
                                )
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(bannerHeight - 60.dp))
                            
                            // Modern Channel Header with Overlapping Avatar
                            Box(contentAlignment = Alignment.TopCenter) {
                                // Content Surface
                                Surface(
                                    modifier = Modifier
                                        .padding(top = 50.dp) // Room for top half of avatar
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(modifier = Modifier.height(40.dp)) // Offset for bottom half of avatar

                                        Text(
                                            text = details.name,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Stats Row
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            details.subscriberCount?.let { count ->
                                                Text(
                                                    text = if (count < 0) "Subscribers hidden" else "${VideoUtils.formatNumber(count)} Subscribers",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = " • ",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "${details.videos.size}+ Videos",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(20.dp))

                                        // Subscribe Button
                                        if (isSubscribed != null) {
                                            Button(
                                                onClick = onToggleSubscription,
                                                colors = if (isSubscribed == true) {
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                } else {
                                                    ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                            ) {
                                                Text(
                                                    text = if (isSubscribed == true) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }

                                        details.description?.let { desc ->
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(top = 16.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Floating Avatar
                                Surface(
                                    modifier = Modifier.size(100.dp),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                                    tonalElevation = 8.dp
                                ) {
                                    AsyncImage(
                                        model = details.avatarUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        
                        // Sticky Tabs with Glass Effect
                        stickyHeader {
                            GlassSurface(tonalElevation = 0.dp) {
                                PrimaryTabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    containerColor = Color.Transparent,
                                    divider = {}
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { 
                                                Text(
                                                    text = title,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedTabIndex == 0) {
                            items(details.videos, key = { it.id }) { video ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    VideoItemRow(
                                        video = video,
                                        isDownloaded = downloadedIds.contains(video.id),
                                        isFavorite = favoriteIds.contains(video.id),
                                        onFavoriteClick = { onFavoriteClick(video) },
                                        onDownloadClick = { onDownloadClick(video) },
                                        onClick = { onVideoClick(video) }
                                    )
                                }
                            }
                            
                            // Load More Indicator
                            if (details.nextVideosPage != null) {
                                item {
                                    InfiniteScrollEffect(
                                        listState = listState,
                                        enabled = details.nextVideosPage != null && !uiState.isFetchingNextPage,
                                        onLoadMore = onLoadMore
                                    )

                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        } else {
                            items(details.playlists, key = { it.id }) { playlist ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                    ModernPlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
                                }
                            }
                        }
                    }
                }
                is ChannelUiState.Error -> {
                    val isNetworkError = uiState.error is PlayTubeError.Network
                    EmptyState(
                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                        description = if (isNetworkError) "Your downloads are still available offline." else uiState.error.getMessage(),
                        actionText = if (isNetworkError) "Go to Offline Hub" else stringResource(R.string.retry),
                        onActionClick = { 
                            if (isNetworkError) onNavigateToDownloads() else onLoadChannel(channelUrl)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Floating Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Download Dialogs
            when (val currentDownloadState = downloadState) {
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
                    videoStreams = currentDownloadState.bundle.videoStreams,
                    onDismiss = { onDismissDownload() },
                    onDownload = { stream ->
                        onDownloadConfirm(
                            currentDownloadState.video,
                            currentDownloadState.bundle,
                            stream.url,
                            stream.quality,
                            stream.format,
                            stream.isAdaptive
                        )
                    }
                )
            }
            }
        }
    }
}

@Composable
fun ModernPlaylistItem(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                ThumbnailImage(
                    videoId = "",
                    thumbnailUrl = playlist.thumbnailUrl,
                    quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
                // Playlist Overlay
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.35f)
                        .align(Alignment.CenterEnd),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${playlist.streamCount} videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
