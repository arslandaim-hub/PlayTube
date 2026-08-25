/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arslandaim.playtube.ui.components.ThumbnailImage
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.data.local.DownloadEntity
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.PlaylistFavoriteEntity
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.utils.VideoUtils

const val GlobalGlassAlpha = 0.75f

@Composable
fun ProfileStatsHeader(
    modifier: Modifier = Modifier,
    downloadCount: Int,
    subscriptionCount: Int,
    favoriteCount: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large Fluid Avatar
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(
                2.dp, 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Downloads", count = downloadCount)
            StatItem(label = "Subscribed", count = subscriptionCount)
            StatItem(label = "Favorites", count = favoriteCount)
        }
    }
}

@Composable
private fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ModernSectionHeader(
    title: String,
    icon: ImageVector,
    showSeeAll: Boolean = true,
    onSeeAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            )
        }
        
        if (showSeeAll) {
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), 
                        CircleShape
                    )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ModernHistoryCard(
    item: HistoryEntity,
    onClick: () -> Unit,
    isSaved: Boolean = false,
    onAddToPlaylist: () -> Unit = {},
    onRemoveClick: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            ThumbnailImage(
                videoId = item.videoId,
                thumbnailUrl = item.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            // Premium Menu Button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isSaved) "Saved" else "Add to Local Playlist") },
                        leadingIcon = { 
                            Icon(
                                imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd, 
                                contentDescription = null,
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            ) 
                        },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                    onRemoveClick?.let {
                        DropdownMenuItem(
                            text = { Text("Remove from History") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = {
                                showMenu = false
                                it()
                            }
                        )
                    }
                }
            }

            // Enhanced Watch Progress Bar
            if (item.durationMs > 0) {
                val progress = item.progressMs.toFloat() / item.durationMs
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Red, Color.Red.copy(alpha = 0.8f))
                                )
                            )
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
            letterSpacing = 0.1.sp
        )
        Text(
            text = item.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModernSubscriptionItem(sub: SubscriptionEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(86.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(68.dp),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(
                2.dp, 
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
            ),
            color = Color.Transparent
        ) {
            AsyncImage(
                model = sub.thumbnailUrl,
                contentDescription = sub.name,
                modifier = Modifier
                    .padding(3.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sub.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ModernPlaylistCard(
    playlist: PlaylistFavoriteEntity,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            ThumbnailImage(
                videoId = "",
                thumbnailUrl = playlist.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = playlist.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModernLocalPlaylistCard(
    playlist: com.arslandaim.playtube.data.local.LocalPlaylistEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete '${playlist.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            if (playlist.thumbnailUrl != null) {
                ThumbnailImage(
                    videoId = "",
                    thumbnailUrl = playlist.thumbnailUrl,
                    quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
            
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
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // More Menu Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete Playlist") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "Local Playlist",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModernDownloadCard(
    download: DownloadEntity,
    onClick: () -> Unit,
    isSaved: Boolean = false,
    onAddToPlaylist: () -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            ThumbnailImage(
                videoId = download.videoId,
                thumbnailUrl = download.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )

            // More menu for download card
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (isSaved) "Saved" else "Add to Local Playlist") },
                        leadingIcon = { 
                            Icon(
                                imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd, 
                                contentDescription = null,
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            ) 
                        },
                        onClick = {
                            showMenu = false
                            onAddToPlaylist()
                        }
                    )
                    onDelete?.let {
                        DropdownMenuItem(
                            text = { Text("Delete Download") },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                it()
                            }
                        )
                    }
                }
            }
            
            if (download.status != com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { if (download.totalSize > 0) download.downloadedSize.toFloat() / download.totalSize else 0f },
                            modifier = Modifier.size(36.dp),
                            color = Color.White,
                            strokeWidth = 3.5.dp,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = download.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = download.uploaderName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModernPlaylistRow(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 130.dp, height = 74.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            ThumbnailImage(
                videoId = "",
                thumbnailUrl = playlist.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp)
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
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.1.sp
            )
            Text(
                text = "${playlist.uploaderName} • ${playlist.streamCount} videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HistoryItemRow(
    item: HistoryEntity,
    onClick: () -> Unit,
    isSaved: Boolean = false,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onRemoveClick: (() -> Unit)? = null
) {
    VideoRow(
        videoId = item.videoId,
        title = item.title,
        uploader = item.uploaderName,
        thumbnailUrl = item.thumbnailUrl,
        progress = if (item.durationMs > 0) item.progressMs.toFloat() / item.durationMs else null,
        isSaved = isSaved,
        onClick = onClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
        onDeleteClick = onRemoveClick,
        deleteText = "Remove from History"
    )
}

@Composable
fun SubscriptionItemRow(sub: SubscriptionEntity, onClick: () -> Unit, onUnsubscribe: () -> Unit = {}, onUnsubscribeClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = sub.thumbnailUrl,
            contentDescription = sub.name,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = sub.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onUnsubscribeClick ?: onUnsubscribe) {
            Text("Unsubscribe", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VideoRow(
    videoId: String,
    title: String,
    uploader: String,
    thumbnailUrl: String,
    duration: Long = 0,
    viewCount: Long? = null,
    uploadDate: String? = null,
    progress: Float? = null,
    watchProgress: Float? = null,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    onDeleteClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onResumeClick: (() -> Unit)? = null,
    onSaveToDeviceClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onRowClickEnabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    deleteText: String = "Delete",
    trailing: @Composable (() -> Unit)? = null,
    metadata: @Composable (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = onRowClickEnabled,
                onClick = onClick
            )
            .padding(vertical = 10.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 150.dp, height = 84.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            ThumbnailImage(
                videoId = videoId,
                thumbnailUrl = thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            // Premium Duration Badge
            if (duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            val actualProgress = progress ?: watchProgress
            actualProgress?.let {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(it.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            if (isDownloaded) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        modifier = Modifier.padding(4.dp).size(14.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            val metaText = remember(uploader, viewCount, uploadDate) {
                buildString {
                    append(uploader)
                    if (viewCount != null) {
                        append(" • ")
                        append(VideoUtils.formatViewCount(viewCount))
                        append(" views")
                    }
                    if (!uploadDate.isNullOrBlank()) {
                        append(" • ")
                        append(VideoUtils.formatUploadDate(uploadDate))
                    }
                }
            }

            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            metadata?.invoke()
        }
        
        val hasAnyAction = onFavoriteClick != null || onDownloadClick != null || 
                          onAddToPlaylistClick != null || onRemoveFromPlaylistClick != null || 
                          onDeleteClick != null || onRetryClick != null || onCancelClick != null

        if (trailing != null || hasAnyAction) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trailing != null) {
                    Box(modifier = Modifier.minimumInteractiveComponentSize()) {
                        trailing()
                    }
                }
                
                if (hasAnyAction) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            VideoRowMenuContent(
                                isDownloaded = isDownloaded,
                                isFavorite = isFavorite,
                                isSaved = isSaved,
                                onDownloadClick = onDownloadClick,
                                onFavoriteClick = onFavoriteClick,
                                onAddToPlaylistClick = onAddToPlaylistClick,
                                onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
                                onDeleteClick = onDeleteClick,
                                onRetryClick = onRetryClick,
                                onPauseClick = onPauseClick,
                                onResumeClick = onResumeClick,
                                onSaveToDeviceClick = onSaveToDeviceClick,
                                onCancelClick = onCancelClick,
                                deleteText = deleteText,
                                dismissMenu = { showMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoRowMenuContent(
    isDownloaded: Boolean,
    isFavorite: Boolean,
    isSaved: Boolean,
    onDownloadClick: (() -> Unit)?,
    onFavoriteClick: (() -> Unit)?,
    onAddToPlaylistClick: (() -> Unit)?,
    onRemoveFromPlaylistClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    onPauseClick: (() -> Unit)? = null,
    onResumeClick: (() -> Unit)? = null,
    onSaveToDeviceClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    deleteText: String = "Delete",
    dismissMenu: () -> Unit
) {
    if (onSaveToDeviceClick != null) {
        DropdownMenuItem(
            text = { Text("Save to Device") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Save, 
                    contentDescription = null
                ) 
            },
            onClick = {
                dismissMenu()
                onSaveToDeviceClick()
            }
        )
    }
    if (onPauseClick != null) {
        DropdownMenuItem(
            text = { Text("Pause Download") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Pause, 
                    contentDescription = null
                ) 
            },
            onClick = {
                dismissMenu()
                onPauseClick()
            }
        )
    }
    if (onResumeClick != null) {
        DropdownMenuItem(
            text = { Text("Resume Download") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.PlayArrow, 
                    contentDescription = null
                ) 
            },
            onClick = {
                dismissMenu()
                onResumeClick()
            }
        )
    }
    if (onDownloadClick != null) {
        DropdownMenuItem(
            text = { Text(if (isDownloaded) "Downloaded" else "Download") },
            leadingIcon = { 
                Icon(
                    imageVector = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download, 
                    contentDescription = null,
                    tint = if (isDownloaded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                ) 
            },
            onClick = {
                dismissMenu()
                if (!isDownloaded) onDownloadClick()
            },
            enabled = !isDownloaded
        )
    }
    if (onFavoriteClick != null) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
            leadingIcon = { 
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, 
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else LocalContentColor.current
                ) 
            },
            onClick = {
                dismissMenu()
                onFavoriteClick()
            }
        )
    }
    if (onAddToPlaylistClick != null) {
        DropdownMenuItem(
            text = { Text(if (isSaved) "Saved" else "Add to Local Playlist") },
            leadingIcon = { 
                Icon(
                    imageVector = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd, 
                    contentDescription = null,
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else LocalContentColor.current
                ) 
            },
            onClick = {
                dismissMenu()
                onAddToPlaylistClick()
            }
        )
    }
    if (onRemoveFromPlaylistClick != null) {
        DropdownMenuItem(
            text = { Text("Remove from Playlist") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.PlaylistRemove, 
                    contentDescription = null
                ) 
            },
            onClick = {
                dismissMenu()
                onRemoveFromPlaylistClick()
            }
        )
    }
    if (onRetryClick != null) {
        DropdownMenuItem(
            text = { Text("Retry Download") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Refresh, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            onClick = {
                dismissMenu()
                onRetryClick()
            }
        )
    }
    if (onCancelClick != null) {
        DropdownMenuItem(
            text = { Text("Cancel Download") },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Close, 
                    contentDescription = null
                ) 
            },
            onClick = {
                dismissMenu()
                onCancelClick()
            }
        )
    }
    if (onDeleteClick != null) {
        DropdownMenuItem(
            text = { Text(deleteText) },
            leadingIcon = { 
                Icon(
                    imageVector = Icons.Default.Delete, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                ) 
            },
            onClick = {
                dismissMenu()
                onDeleteClick()
            }
        )
    }
}

@Composable
fun ModernChannelCard(
    channel: SearchItem.Channel,
    onToggleSubscription: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(
                2.dp, 
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer))
            ),
            color = Color.Transparent
        ) {
            AsyncImage(
                model = channel.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .padding(3.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = (-0.2).sp
            )
            
            if (channel.subscriberCount != null && channel.subscriberCount >= 0) {
                Text(
                    text = "${VideoUtils.formatNumber(channel.subscriberCount)} subscribers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Button(
            onClick = onToggleSubscription,
            colors = if (channel.isSubscribed) {
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
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (channel.isSubscribed) "Subscribed" else "Subscribe",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black
            )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 68.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(36.dp)
                    .align(Alignment.CenterEnd),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$videoCount videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DownloadItemRow(
    download: DownloadEntity,
    modifier: Modifier = Modifier,
    isSaved: Boolean = false,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onSaveToDeviceClick: () -> Unit,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null
) {
    val isCompleted = download.status == com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED

    VideoRow(
        videoId = download.videoId,
        title = download.title,
        uploader = download.uploaderName,
        thumbnailUrl = download.thumbnailUrl,
        progress = if (!isCompleted) {
            if (download.totalSize > 0) download.downloadedSize.toFloat() / download.totalSize else 0f
        } else null,
        isSaved = isSaved,
        onClick = onClick,
        onRowClickEnabled = isCompleted,
        modifier = modifier.graphicsLayer { 
            alpha = if (isCompleted) 1f else 0.7f 
        },
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
        onDeleteClick = onDeleteClick,
        onRetryClick = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.FAILED) onResumeClick else null,
        onCancelClick = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING || 
                           download.status == com.arslandaim.playtube.data.local.DownloadStatus.PENDING || 
                           download.status == com.arslandaim.playtube.data.local.DownloadStatus.WAITING) onCancelClick else null,
        onPauseClick = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING || 
                          download.status == com.arslandaim.playtube.data.local.DownloadStatus.PENDING || 
                          download.status == com.arslandaim.playtube.data.local.DownloadStatus.WAITING) onPauseClick else null,
        onResumeClick = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.PAUSED) onResumeClick else null,
        onSaveToDeviceClick = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) onSaveToDeviceClick else null,
        deleteText = "Delete Download",
        metadata = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when (download.status) {
                    com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED -> "Completed"
                    com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING -> "Downloading"
                    com.arslandaim.playtube.data.local.DownloadStatus.FAILED -> "Failed"
                    com.arslandaim.playtube.data.local.DownloadStatus.PAUSED -> "Paused"
                    com.arslandaim.playtube.data.local.DownloadStatus.PENDING, 
                    com.arslandaim.playtube.data.local.DownloadStatus.WAITING -> "Waiting"
                }
                
                val sizeText = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) {
                    formatBytes(download.totalSize)
                } else {
                    "${formatBytes(download.downloadedSize)} / ${formatBytes(download.totalSize)}"
                }

                val qualityText = download.quality?.let { " • $it" } ?: ""

                // Status: Truncates if space is tight
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                    color = if (download.status == com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) 
                        MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Metrics: Always visible
                Text(
                    text = " • $sizeText$qualityText",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun FavoriteItemRow(
    favorite: FavoriteEntity,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    isSaved: Boolean = false,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null
) {
    VideoRow(
        videoId = favorite.videoId,
        title = favorite.title,
        uploader = favorite.uploaderName,
        thumbnailUrl = favorite.thumbnailUrl,
        isSaved = isSaved,
        onClick = onClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
        onFavoriteClick = onRemoveClick,
        isFavorite = true
    )
}

@Composable
fun EmptySectionPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(java.util.Locale.US, "%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
