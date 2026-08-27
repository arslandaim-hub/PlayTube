/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.arslandaim.playtube.ui.components.DownloadSelectionSheet
import com.arslandaim.playtube.ui.components.PlaylistDownloadSelectionSheet
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import androidx.compose.ui.platform.LocalContext
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.screens.library.VideoRow
import com.arslandaim.playtube.utils.PlayTubeError
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import kotlinx.coroutines.flow.map

@Composable
fun PlaylistScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val savedVideoIds by remember(viewModel) {
        viewModel.libraryRepository.getAllSavedVideoIds().map { it.toSet() }
    }.collectAsStateWithLifecycle(initialValue = emptySet())
    val favorites by viewModel.libraryRepository.getFavorites().collectAsStateWithLifecycle(initialValue = emptyList())
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val showPlaylistDownloadDialog by viewModel.showPlaylistDownloadDialog.collectAsStateWithLifecycle()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    PlaylistContent(
        playlistId = playlistId,
        uiState = uiState,
        isFavorite = isFavorite,
        downloadedIds = downloadedIds,
        savedVideoIds = savedVideoIds,
        favoriteIds = favoriteIds,
        downloadState = downloadState,
        showPlaylistDownloadDialog = showPlaylistDownloadDialog,
        snackbarMessage = viewModel.snackbarMessage,
        onLoadPlaylist = viewModel::loadPlaylist,
        onDownloadPlaylist = viewModel::showPlaylistDownloadDialog,
        onDownloadPlaylistConfirm = viewModel::downloadPlaylist,
        onDismissPlaylistDownload = viewModel::dismissPlaylistDownloadDialog,
        onTogglePlaylistFavorite = viewModel::togglePlaylistFavorite,
        onToggleVideoFavorite = viewModel::toggleVideoFavorite,
        onDownloadVideo = viewModel::prepareDownload,
        onDownloadConfirm = viewModel::download,
        onDismissDownload = viewModel::dismissDownloadDialog,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onNavigateToDownloads = onNavigateToDownloads,
        onBack = onBack,
        onVideoClick = onVideoClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRemoveFromPlaylistClick = viewModel::removeFromPlaylist
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTopBar(
    title: String,
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
                    text = title,
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
private fun PlaylistHeader(
    details: com.arslandaim.playtube.domain.model.PlaylistDetails,
    isFavorite: Boolean,
    isPlaylistDownloaded: Boolean,
    onDownloadPlaylist: () -> Unit,
    onTogglePlaylistFavorite: () -> Unit
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
        Text(
            text = details.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.by_author, details.uploaderName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { if (!isPlaylistDownloaded) onDownloadPlaylist() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = if (isPlaylistDownloaded) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                } else ButtonDefaults.buttonColors()
            ) {
                Icon(
                    imageVector = if (isPlaylistDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPlaylistDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download_all),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            OutlinedButton(
                onClick = onTogglePlaylistFavorite,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                colors = if (isFavorite) {
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                } else ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isFavorite) Color.Red else LocalContentColor.current
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistContent(
    playlistId: String,
    uiState: PlaylistUiState,
    isFavorite: Boolean,
    downloadedIds: Set<String>,
    savedVideoIds: Set<String>,
    favoriteIds: Set<String>,
    downloadState: com.arslandaim.playtube.ui.components.DownloadDialogState,
    showPlaylistDownloadDialog: Boolean,
    snackbarMessage: kotlinx.coroutines.flow.SharedFlow<String>,
    onLoadPlaylist: (String) -> Unit,
    onDownloadPlaylist: () -> Unit,
    onDownloadPlaylistConfirm: (String) -> Unit,
    onDismissPlaylistDownload: () -> Unit,
    onTogglePlaylistFavorite: () -> Unit,
    onToggleVideoFavorite: (VideoItem) -> Unit,
    onDownloadVideo: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, com.arslandaim.playtube.domain.model.StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onRemoveFromPlaylistClick: (VideoItem) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)
    val lifecycleOwner = LocalLifecycleOwner.current

    val listState = rememberLazyListState()
    
    // Parallax & Fade Calculation
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

    LaunchedEffect(playlistId) {
        onLoadPlaylist(playlistId)
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snackbarMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val isPlaylistDownloaded = remember(uiState, downloadedIds) {
        val state = uiState as? PlaylistUiState.Success
        state?.details?.videos?.all { downloadedIds.contains(it.id) } == true
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (uiState is PlaylistUiState.Success) {
                PlaylistTopBar(
                    title = uiState.details.title,
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
                is PlaylistUiState.Loading -> {
                    com.arslandaim.playtube.ui.components.PlaylistMetadataSkeleton()
                }
                is PlaylistUiState.Success -> {
                    val details = uiState.details
                    
                    // Immersive Ambient Mode
                    if (!details.thumbnailUrl.isBlank()) {
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
                                model = details.thumbnailUrl,
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
                                .data(details.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(bannerHeight - 30.dp))
                        }
                        
                        item {
                            PlaylistHeader(
                                details = details,
                                isFavorite = isFavorite,
                                isPlaylistDownloaded = isPlaylistDownloaded,
                                onDownloadPlaylist = onDownloadPlaylist,
                                onTogglePlaylistFavorite = onTogglePlaylistFavorite
                            )
                        }

                        items(
                            items = details.videos, 
                            key = { it.id },
                            contentType = { "playlist_video" }
                        ) { video ->
                            val isLocal = details.id.startsWith("local:")
                            Box(modifier = Modifier.padding(bottom = 4.dp)) {
                                VideoRow(
                                    videoId = video.id,
                                    title = video.title,
                                    uploader = video.uploaderName,
                                    thumbnailUrl = video.thumbnailUrl,
                                    duration = video.duration,
                                    viewCount = video.viewCount,
                                    uploadDate = video.uploadDate,
                                    watchProgress = video.watchProgress,
                                    isDownloaded = downloadedIds.contains(video.id),
                                    isFavorite = favoriteIds.contains(video.id),
                                    isSaved = savedVideoIds.contains(video.id),
                                    onFavoriteClick = { onToggleVideoFavorite(video) },
                                    onDownloadClick = { onDownloadVideo(video) },
                                    onAddToPlaylistClick = if (isLocal) null else { { onAddToPlaylistClick(video) } },
                                    onRemoveFromPlaylistClick = if (isLocal) { { onRemoveFromPlaylistClick(video) } } else null,
                                    onClick = { onVideoClick(video) }
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
                is PlaylistUiState.Error -> {
                    val isNetworkError = uiState.error is PlayTubeError.Network
                    EmptyState(
                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                        description = if (isNetworkError) "Your downloads are still available offline." else uiState.error.getMessage(),
                        actionText = if (isNetworkError) "Go to Offline Hub" else stringResource(R.string.retry),
                        onActionClick = { 
                            if (isNetworkError) onNavigateToDownloads() else onLoadPlaylist(playlistId)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Download Dialog for individual videos in playlist
        when (downloadState) {
            com.arslandaim.playtube.ui.components.DownloadDialogState.Idle -> {}
            is com.arslandaim.playtube.ui.components.DownloadDialogState.Loading -> {
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
            is com.arslandaim.playtube.ui.components.DownloadDialogState.ShowDialog -> {
                DownloadSelectionSheet(
                    videoStreams = downloadState.bundle.videoStreams,
                    onDismiss = { onDismissDownload() },
                    onDownload = { stream ->
                        onDownloadConfirm(
                            downloadState.video,
                            downloadState.bundle,
                            stream.url,
                            stream.quality,
                            stream.format,
                            stream.isAdaptive
                        )
                    }
                )
            }
        }

        if (showPlaylistDownloadDialog) {
            PlaylistDownloadSelectionSheet(
                onDismiss = onDismissPlaylistDownload,
                onDownload = onDownloadPlaylistConfirm
            )
        }
    }
}
