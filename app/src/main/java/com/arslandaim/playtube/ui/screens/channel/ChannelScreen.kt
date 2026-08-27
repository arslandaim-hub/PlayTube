/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.arslandaim.playtube.R
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.InfiniteScrollEffect
import com.arslandaim.playtube.ui.components.InfiniteScrollGridEffect
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.VideoUtils
import com.arslandaim.playtube.utils.Constants
import kotlinx.coroutines.flow.SharedFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.lazy.grid.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSubscribed by viewModel.isSubscribed.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    ChannelContent(
        channelUrl = channelUrl,
        uiState = uiState,
        isSubscribed = isSubscribed,
        downloadedIds = downloadedIds,
        savedVideoIds = savedVideoIds,
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
        onAddToPlaylistClick = onAddToPlaylistClick,
        onPlaylistClick = onPlaylistClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelTopBar(
    channelName: String,
    scrollProgress: Float,
    onBack: () -> Unit
) {
    GlassSurface(
        containerColor = MaterialTheme.colorScheme.surface.copy(
            alpha = (scrollProgress * 0.9f).coerceIn(0f, 0.9f)
        ),
        border = if (scrollProgress > 0.8f) null else androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer { alpha = (scrollProgress - 0.5f).coerceIn(0f, 1f) * 2f }
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(
                        Color.Black.copy(alpha = (0.4f * (1f - scrollProgress)).coerceIn(0f, 0.4f)),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (scrollProgress > 0.5f) MaterialTheme.colorScheme.onSurface else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ChannelHeader(
    details: com.arslandaim.playtube.domain.model.ChannelDetails,
    isSubscribed: Boolean?,
    onToggleSubscription: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background
                    ),
                    startY = -100f
                )
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar and Name Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(90.dp),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    3.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = details.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Stats Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    details.subscriberCount?.let { count ->
                        Text(
                            text = if (count < 0) stringResource(R.string.subscribers_hidden) 
                                   else stringResource(R.string.subscribers_count, VideoUtils.formatNumber(count)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isSubscribed == true) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        details.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 18.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                lineHeight = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelContent(
    channelUrl: String,
    uiState: ChannelUiState,
    isSubscribed: Boolean?,
    downloadedIds: Set<String>,
    savedVideoIds: Set<String>,
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
    onAddToPlaylistClick: (VideoItem) -> Unit,
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

    val gridState = rememberLazyGridState()
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    // Parallax & Fade Calculation
    val bannerHeight = 200.dp
    val bannerHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { bannerHeight.toPx() }
    
    val scrollOffset by remember {
        derivedStateOf {
            if (gridState.firstVisibleItemIndex == 0) {
                gridState.firstVisibleItemScrollOffset.toFloat()
            } else {
                bannerHeightPx
            }
        }
    }

    val bannerProgress = (1f - (scrollOffset / bannerHeightPx)).coerceIn(0f, 1f)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = Constants.calculateGridColumns(screenWidth)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (uiState is ChannelUiState.Success) {
                ChannelTopBar(
                    channelName = uiState.details.name,
                    scrollProgress = (scrollOffset / bannerHeightPx).coerceIn(0f, 1f),
                    onBack = onBack
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (uiState) {
                is ChannelUiState.Loading -> {
                    com.arslandaim.playtube.ui.components.ChannelMetadataSkeleton()
                }
                is ChannelUiState.Success -> {
                    val details = uiState.details
                    
                    InfiniteScrollGridEffect(
                        gridState = gridState,
                        enabled = details.nextVideosPage != null && !uiState.isFetchingNextPage,
                        onLoadMore = onLoadMore
                    )
                    
                    // Immersive Ambient Mode
                    if (!details.bannerUrl.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(360.dp)
                                .graphicsLayer {
                                    translationY = -scrollOffset * 0.4f
                                    alpha = ((1f - (scrollOffset / (bannerHeightPx * 1.5f))) * bannerProgress).coerceIn(0f, 0.6f)
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                            80f, 80f, android.graphics.Shader.TileMode.CLAMP
                                        ).asComposeRenderEffect()
                                    }
                                }
                                .then(
                                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                        Modifier.blur(60.dp)
                                    } else Modifier
                                )
                        ) {
                            AsyncImage(
                                model = details.bannerUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.Low
                            )
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                        }
                    }

                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(bannerHeight)
                            .graphicsLayer {
                                translationY = -scrollOffset * 0.5f
                                alpha = bannerProgress
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
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        state = gridState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item(span = { GridItemSpan(columns) }) {
                            Spacer(modifier = Modifier.height(bannerHeight - 30.dp))
                        }
                            
                        item(span = { GridItemSpan(columns) }) {
                            ChannelHeader(
                                details = details,
                                isSubscribed = isSubscribed,
                                onToggleSubscription = onToggleSubscription
                            )
                        }
                        
                        item(span = { GridItemSpan(columns) }) {
                            Column {
                                PrimaryTabRow(
                                    selectedTabIndex = selectedTabIndex,
                                    containerColor = Color.Transparent,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    divider = {},
                                    indicator = {
                                        TabRowDefaults.PrimaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(selectedTabIndex),
                                            width = 32.dp,
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                    }
                                ) {
                                    tabs.forEachIndexed { index, title ->
                                        Tab(
                                            selected = selectedTabIndex == index,
                                            onClick = { selectedTabIndex = index },
                                            text = { 
                                                Text(
                                                    text = title,
                                                    fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Bold,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        if (selectedTabIndex == 0) {
                            items(details.videos, key = { it.id }) { video ->
                                Box(modifier = Modifier.padding(bottom = 4.dp)) {
                                    VideoItemRow(
                                        video = video,
                                        isDownloaded = downloadedIds.contains(video.id),
                                        isFavorite = favoriteIds.contains(video.id),
                                        isSaved = savedVideoIds.contains(video.id),
                                        onFavoriteClick = { onFavoriteClick(video) },
                                        onDownloadClick = { onDownloadClick(video) },
                                        onAddToPlaylistClick = { onAddToPlaylistClick(video) },
                                        onClick = { onVideoClick(video) }
                                    )
                                }
                            }
                            
                            if (details.nextVideosPage != null) {
                                item(span = { GridItemSpan(columns) }) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
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
