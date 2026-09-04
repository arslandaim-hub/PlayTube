/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.R
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.VideoItem
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.utils.VideoUtils

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onSeeAllDownloads: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.localPlaylists.collectAsStateWithLifecycle()
    val savedVideoIds by viewModel.savedVideoIds.collectAsStateWithLifecycle()

    LibraryDashboard(
        downloads = downloads,
        favorites = favorites,
        history = history,
        subscriptions = subscriptions,
        playlists = playlists,
        localPlaylists = localPlaylists,
        savedVideoIds = savedVideoIds,
        onCreateLocalPlaylist = viewModel::createLocalPlaylist,
        onDeleteLocalPlaylist = viewModel::deleteLocalPlaylist,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onDeleteDownload = viewModel::deleteDownload,
        onCancelDownload = viewModel::cancelDownload,
        onResumeDownload = viewModel::resumeDownload,
        onRemoveFavorite = viewModel::removeFavorite,
        onRemoveHistoryItem = viewModel::removeFromHistory,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onPlaylistClick = onPlaylistClick,
        onSeeAllHistory = onSeeAllHistory,
        onSeeAllSubscriptions = onSeeAllSubscriptions,
        onSeeAllDownloads = onSeeAllDownloads
    )
}

@Composable
private fun LibraryDashboard(
    downloads: List<DownloadEntity>,
    favorites: List<FavoriteEntity>,
    history: List<HistoryEntity>,
    subscriptions: List<SubscriptionEntity>,
    playlists: List<com.arslandaim.playtube.data.local.PlaylistFavoriteEntity>,
    localPlaylists: List<com.arslandaim.playtube.data.local.LocalPlaylistEntity>,
    savedVideoIds: Set<String>,
    onCreateLocalPlaylist: (String) -> Unit,
    onDeleteLocalPlaylist: (com.arslandaim.playtube.data.local.LocalPlaylistEntity) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onSeeAllDownloads: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollVisibilityConnection),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 2. Profile & Stats (Redesigned)
            item {
                ProfileStatsHeader(
                    downloadCount = downloads.size,
                    subscriptionCount = subscriptions.size,
                    favoriteCount = favorites.size
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 3. History Section (More spacious)
            if (history.isNotEmpty()) {
                item {
                    ModernSectionHeader(
                        title = stringResource(R.string.history),
                        icon = Icons.Default.History,
                        onSeeAllClick = onSeeAllHistory
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(history.take(15)) { item ->
                            ModernHistoryCard(
                                item = item, 
                                onClick = { onVideoClick(item.toVideoItem()) },
                                isSaved = savedVideoIds.contains(item.videoId),
                                onAddToPlaylist = { onAddToPlaylistClick(item.toVideoItem()) },
                                onRemoveClick = { onRemoveHistoryItem(item.videoId) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 4. Subscriptions Section (Story feel)
            if (subscriptions.isNotEmpty()) {
                item {
                    ModernSectionHeader(
                        title = stringResource(R.string.subscriptions),
                        icon = Icons.Default.Subscriptions,
                        onSeeAllClick = onSeeAllSubscriptions
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(subscriptions.take(20)) { sub ->
                            ModernSubscriptionItem(sub = sub, onClick = { onChannelClick(sub.channelId) })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 5. Playlists Section
            if (playlists.isNotEmpty() || localPlaylists.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.PlaylistPlay, 
                                null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = stringResource(R.string.playlists),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                        
                        var showCreateDialog by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }

                        if (showCreateDialog) {
                            var name by remember { mutableStateOf("") }
                            AlertDialog(
                                onDismissRequest = { showCreateDialog = false },
                                title = { Text("Create New Playlist") },
                                text = {
                                    TextField(
                                        value = name,
                                        onValueChange = { name = it },
                                        placeholder = { Text("Playlist name") },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            if (name.isNotBlank()) onCreateLocalPlaylist(name)
                                            showCreateDialog = false
                                        }
                                    ) { Text("Create") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                                }
                            )
                        }
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Local Playlists
                        items(localPlaylists) { playlist ->
                            ModernLocalPlaylistCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick("local:${playlist.id}") },
                                onDelete = { onDeleteLocalPlaylist(playlist) }
                            )
                        }
                        // Remote Playlists
                        items(playlists) { playlist ->
                            ModernPlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.playlistId) })
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 6. Downloads Section
            if (downloads.isNotEmpty()) {
                item {
                    ModernSectionHeader(
                        title = stringResource(R.string.downloads),
                        icon = Icons.Default.Download,
                        onSeeAllClick = onSeeAllDownloads
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(downloads.take(15)) { download ->
                            ModernDownloadCard(
                                download = download, 
                                onClick = { onVideoClick(download.toVideoItem()) },
                                isSaved = savedVideoIds.contains(download.videoId),
                                onAddToPlaylist = { onAddToPlaylistClick(download.toVideoItem()) },
                                onDelete = { onDeleteDownload(download.videoId) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // 7. Liked Videos Section
            item {
                ModernSectionHeader(
                    title = stringResource(R.string.favorites),
                    icon = Icons.Default.ThumbUp,
                    showSeeAll = false
                )
            }

            if (favorites.isEmpty()) {
                item {
                    EmptySectionPlaceholder(stringResource(R.string.no_favorites))
                }
            } else {
                items(favorites) { favorite ->
                    FavoriteItemRow(
                        favorite = favorite,
                        onClick = { onVideoClick(favorite.toVideoItem()) },
                        onRemoveClick = { onRemoveFavorite(favorite) },
                        isSaved = savedVideoIds.contains(favorite.videoId),
                        onAddToPlaylistClick = { onAddToPlaylistClick(favorite.toVideoItem()) }
                    )
                }
            }
        }
    }
}
