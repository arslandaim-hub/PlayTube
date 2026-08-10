/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    downloadCount: Int,
    subscriptionCount: Int,
    favoriteCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
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
}

@Composable
private fun StatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (showSeeAll) {
            TextButton(
                onClick = onSeeAllClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernHistoryCard(item: HistoryEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            ThumbnailImage(
                videoId = item.videoId,
                thumbnailUrl = item.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            // Watch Progress Bar (Minimal)
            if (item.durationMs > 0) {
                val progress = item.progressMs.toFloat() / item.durationMs
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
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = item.uploaderName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ModernSubscriptionItem(sub: SubscriptionEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = sub.thumbnailUrl,
            contentDescription = sub.name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = sub.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                .clip(RoundedCornerShape(16.dp))
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
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun ModernDownloadCard(
    download: DownloadEntity,
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
                .clip(RoundedCornerShape(16.dp))
        ) {
            ThumbnailImage(
                videoId = download.videoId,
                thumbnailUrl = download.thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            if (download.status != com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { if (download.totalSize > 0) download.downloadedSize.toFloat() / download.totalSize else 0f },
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp)
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
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 68.dp)
                .clip(RoundedCornerShape(12.dp))
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
                text = playlist.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.uploaderName} • ${playlist.streamCount} videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HistoryItemRow(item: HistoryEntity, onClick: () -> Unit) {
    VideoRow(
        videoId = item.videoId,
        title = item.title,
        uploader = item.uploaderName,
        thumbnailUrl = item.thumbnailUrl,
        progress = if (item.durationMs > 0) item.progressMs.toFloat() / item.durationMs else null,
        onClick = onClick
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
    onDeleteClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    onRetryClick: (() -> Unit)? = null,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    metadata: @Composable (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 140.dp, height = 80.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            ThumbnailImage(
                videoId = videoId,
                thumbnailUrl = thumbnailUrl,
                quality = com.arslandaim.playtube.ui.components.ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            // Duration Badge
            if (duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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
                    if (viewCount != null && viewCount >= 0) {
                        append(" • ")
                        append(VideoUtils.formatViewCount(viewCount))
                        if (viewCount >= 0) append(" views")
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
        
        if (trailing != null) {
            Box(modifier = Modifier.minimumInteractiveComponentSize()) {
                trailing()
            }
        } else if (onFavoriteClick != null || onDownloadClick != null) {
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
                                showMenu = false
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
                                showMenu = false
                                onFavoriteClick()
                            }
                        )
                    }
                }
            }
        } else {
            // Default Download controls if not provided via trailing
            Row {
                onRetryClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                onCancelClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Close, null)
                    }
                }
                onDeleteClick?.let {
                    IconButton(onClick = it) {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
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
        AsyncImage(
            model = channel.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (channel.subscriberCount != null && channel.subscriberCount >= 0) {
                Text(
                    text = "${VideoUtils.formatNumber(channel.subscriberCount)} subscribers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (channel.isSubscribed) "Subscribed" else "Subscribe",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
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
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    VideoRow(
        videoId = download.videoId,
        title = download.title,
        uploader = download.uploaderName,
        thumbnailUrl = download.thumbnailUrl,
        progress = if (download.status != com.arslandaim.playtube.data.local.DownloadStatus.COMPLETED) {
            if (download.totalSize > 0) download.downloadedSize.toFloat() / download.totalSize else 0f
        } else null,
        onClick = onClick,
        modifier = modifier,
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
        },
        trailing = {
            Row {
                when (download.status) {
                    com.arslandaim.playtube.data.local.DownloadStatus.FAILED -> {
                        IconButton(onClick = onRetryClick) {
                            Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    com.arslandaim.playtube.data.local.DownloadStatus.DOWNLOADING,
                    com.arslandaim.playtube.data.local.DownloadStatus.PENDING,
                    com.arslandaim.playtube.data.local.DownloadStatus.WAITING -> {
                        IconButton(onClick = onCancelClick) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                    else -> {}
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
fun FavoriteItemRow(
    favorite: FavoriteEntity,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    VideoRow(
        videoId = favorite.videoId,
        title = favorite.title,
        uploader = favorite.uploaderName,
        thumbnailUrl = favorite.thumbnailUrl,
        onClick = onClick,
        trailing = {
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Default.Favorite, null, tint = Color.Red)
            }
        }
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
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
