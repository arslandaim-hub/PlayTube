/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.EmptyState
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlaylistPlay

import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val history by viewModel.history.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    LibraryContent(
        downloads = downloads,
        favorites = favorites,
        history = history,
        subscriptions = subscriptions,
        playlists = playlists,
        onDeleteDownload = viewModel::deleteDownload,
        onCancelDownload = viewModel::cancelDownload,
        onResumeDownload = viewModel::resumeDownload,
        onRemoveFavorite = viewModel::removeFavorite,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onPlaylistClick = onPlaylistClick,
        onSeeAllHistory = onSeeAllHistory,
        onSeeAllSubscriptions = onSeeAllSubscriptions
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryContent(
    downloads: List<com.arslandaim.playtube.data.local.DownloadEntity>,
    favorites: List<com.arslandaim.playtube.data.local.FavoriteEntity>,
    history: List<com.arslandaim.playtube.data.local.HistoryEntity>,
    subscriptions: List<com.arslandaim.playtube.data.local.SubscriptionEntity>,
    playlists: List<com.arslandaim.playtube.data.local.PlaylistFavoriteEntity>,
    onDeleteDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRemoveFavorite: (com.arslandaim.playtube.data.local.FavoriteEntity) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.downloads), stringResource(R.string.favorites))
    
    var videoIdToDelete by remember { mutableStateOf<String?>(null) }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    if (videoIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoIdToDelete = null },
            title = { Text(stringResource(R.string.delete_download_title)) },
            text = { Text(stringResource(R.string.delete_download_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoIdToDelete?.let { onDeleteDownload(it) }
                        videoIdToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollVisibilityConnection),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.library), 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }

        // History Section
        item {
            SectionHeader(
                title = stringResource(R.string.history),
                onSeeAllClick = onSeeAllHistory,
                showSeeAll = history.isNotEmpty()
            )
            
            if (history.isEmpty()) {
                EmptySectionPlaceholder(stringResource(R.string.no_history))
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    history.take(10).forEach { item ->
                        HistoryCard(item = item, onClick = { onVideoClick(item.toVideoItem()) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Playlists Section
        item {
            SectionHeader(
                title = stringResource(R.string.playlists),
                showSeeAll = false
            )
            
            if (playlists.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_playlists),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    playlists.take(10).forEach { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.playlistId) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Subscriptions Section
        item {
            SectionHeader(
                title = stringResource(R.string.subscriptions),
                onSeeAllClick = onSeeAllSubscriptions,
                showSeeAll = subscriptions.isNotEmpty()
            )
            
            if (subscriptions.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_subscriptions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    subscriptions.take(15).forEach { sub ->
                        SubscriptionCircle(sub = sub, onClick = { onChannelClick(sub.channelId) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Sticky Tabs for Downloads and Favorites
        stickyHeader {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // Glass Effect
                tonalElevation = 0.dp
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent, // Let Surface handle background
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { 
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }

        if (selectedTabIndex == 0) {
            // Downloads Tab
            if (downloads.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.no_downloads),
                        description = stringResource(R.string.no_downloads_desc),
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            } else {
                // Group downloads by playlistId
                val groupedDownloads = downloads.groupBy { it.playlistId }
                
                // Single videos (no playlistId)
                val singleVideos = groupedDownloads[null] ?: emptyList()
                
                // Playlists
                val playlists = groupedDownloads.filterKeys { it != null }

                // Show Playlists first
                playlists.forEach { (playlistId, playlistVideos) ->
                    item {
                        val title = playlistVideos.firstOrNull()?.playlistTitle ?: "Playlist"
                        val isExpanded = expandedPlaylistId == playlistId
                        
                        PlaylistDownloadRow(
                            title = title,
                            videoCount = playlistVideos.size,
                            thumbnailUrl = playlistVideos.firstOrNull()?.thumbnailUrl ?: "",
                            isExpanded = isExpanded,
                            onClick = {
                                expandedPlaylistId = if (isExpanded) null else playlistId
                            }
                        )
                    }
                    
                    if (expandedPlaylistId == playlistId) {
                        items(playlistVideos) { download ->
                            DownloadItemRow(
                                download = download,
                                onClick = { onVideoClick(download.toVideoItem()) },
                                onDeleteClick = { videoIdToDelete = download.videoId },
                                onCancelClick = { onCancelDownload(download.videoId) },
                                onRetryClick = { onResumeDownload(download.videoId) },
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                }
                
                // Then show single videos
                items(singleVideos) { download ->
                    DownloadItemRow(
                        download = download,
                        onClick = { onVideoClick(download.toVideoItem()) },
                        onDeleteClick = { videoIdToDelete = download.videoId },
                        onCancelClick = { onCancelDownload(download.videoId) },
                        onRetryClick = { onResumeDownload(download.videoId) }
                    )
                }
            }
        } else {
            // Favorites Tab
            if (favorites.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Favorite,
                        title = stringResource(R.string.no_favorites),
                        description = stringResource(R.string.no_favorites_desc),
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
            } else {
                items(favorites) { favorite ->
                    FavoriteItemRow(
                        favorite = favorite,
                        onClick = { onVideoClick(favorite.toVideoItem()) },
                        onRemoveClick = { onRemoveFavorite(favorite) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlaylistDownloadRow(
    title: String,
    videoCount: Int,
    thumbnailUrl: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp, 56.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium
                )
                // Overlay for playlist indicator
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$videoCount videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    showSeeAll: Boolean = true,
    onSeeAllClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (showSeeAll) {
            TextButton(onClick = onSeeAllClick) {
                Text(stringResource(R.string.see_all))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun EmptySectionPlaceholder(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Composable
fun HistoryCard(
    item: HistoryEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: com.arslandaim.playtube.data.local.PlaylistFavoriteEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = playlist.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High
            )
            // Playlist Overlay
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .align(Alignment.CenterEnd),
                color = Color.Black.copy(alpha = 0.6f)
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
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = playlist.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SubscriptionCircle(
    sub: SubscriptionEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = sub.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = sub.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SubscriptionItemRow(
    sub: SubscriptionEntity,
    onClick: () -> Unit = {},
    onUnsubscribeClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = sub.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (sub.subscriberCount != null && sub.subscriberCount > 0) {
                    Text(
                        text = "${com.arslandaim.playtube.utils.VideoUtils.formatNumber(sub.subscriberCount)} subscribers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            TextButton(
                onClick = onUnsubscribeClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.subscribed),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    item: HistoryEntity,
    onClick: () -> Unit = {}
) {
    VideoRow(
        title = item.title,
        uploader = item.uploaderName,
        thumbnailUrl = item.thumbnailUrl,
        onClick = onClick
    )
}

@Composable
fun FavoriteItemRow(
    favorite: FavoriteEntity,
    onClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    VideoRow(
        title = favorite.title,
        uploader = favorite.uploaderName,
        thumbnailUrl = favorite.thumbnailUrl,
        onClick = onClick,
        trailingContent = {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove from favorites",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun VideoRow(
    title: String,
    uploader: String,
    thumbnailUrl: String,
    onClick: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp, 56.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uploader,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
fun DownloadItemRow(
    download: DownloadEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    val isFailed = download.status == com.arslandaim.playtube.data.local.DownloadStatus.FAILED
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isFailed, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (isFailed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                             else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .alpha(if (isFailed) 0.6f else 1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = download.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(100.dp, 56.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.uploaderName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val progressText = if (download.totalSize > 0) {
                        "${formatBytes(download.downloadedSize)} / ${formatBytes(download.totalSize)}"
                    } else {
                        formatBytes(download.downloadedSize)
                    }
                    
                    Text(
                        text = "${download.status} • $progressText • ${download.quality}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                when (download.status) {
                    com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING,
                    com.arslandaim.playtube.data.local.DownloadStatus.WAITING,
                    com.arslandaim.playtube.data.local.DownloadStatus.PENDING -> {
                        IconButton(onClick = onCancelClick) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                    com.arslandaim.playtube.data.local.DownloadStatus.FAILED -> {
                        Row {
                            IconButton(onClick = onRetryClick) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                            }
                            IconButton(onClick = onDeleteClick) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                    else -> {
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
            
            if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING ||
                download.status == com.arslandaim.playtube.data.local.DownloadStatus.WAITING ||
                download.status == com.arslandaim.playtube.data.local.DownloadStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING && download.totalSize > 0) {
                    val progress = download.downloadedSize.toFloat() / download.totalSize.toFloat()
                    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = progress,
                        animationSpec = androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessVeryLow
                        ),
                        label = "DownloadProgress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return String.format(java.util.Locale.getDefault(), "%.1f MB", mb)
}
