/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    LibraryDashboard(
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
    onDeleteDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    onSeeAllSubscriptions: () -> Unit,
    onSeeAllDownloads: () -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    LazyColumn(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollVisibilityConnection),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Header & Stats
        item {
            ProfileStatsHeader(
                downloadCount = downloads.size,
                subscriptionCount = subscriptions.size,
                favoriteCount = favorites.size
            )
        }

        // 2. History Section (Horizontal Carousel)
        if (history.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = stringResource(R.string.history),
                    icon = Icons.Default.History,
                    onSeeAllClick = onSeeAllHistory
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(history.take(15)) { item ->
                        ModernHistoryCard(item = item, onClick = { onVideoClick(item.toVideoItem()) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 3. Subscriptions Section (Horizontal Carousel)
        if (subscriptions.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = stringResource(R.string.subscriptions),
                    icon = Icons.Default.Subscriptions,
                    onSeeAllClick = onSeeAllSubscriptions
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(subscriptions.take(20)) { sub ->
                        ModernSubscriptionItem(sub = sub, onClick = { onChannelClick(sub.channelId) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 4. Playlists Section (Horizontal Carousel)
        if (playlists.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = stringResource(R.string.playlists),
                    icon = Icons.Default.PlaylistPlay,
                    showSeeAll = false
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(playlists) { playlist ->
                        ModernPlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.playlistId) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 5. Downloads Section (Horizontal Carousel)
        if (downloads.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = stringResource(R.string.downloads),
                    icon = Icons.Default.Download,
                    onSeeAllClick = onSeeAllDownloads
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(downloads.take(15)) { download ->
                        ModernDownloadCard(download = download, onClick = { onVideoClick(download.toVideoItem()) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 6. Favorites Section
        item {
            ModernSectionHeader(
                title = stringResource(R.string.favorites),
                icon = Icons.Default.Favorite,
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
                    onRemoveClick = { onRemoveFavorite(favorite) }
                )
            }
        }
    }
}
