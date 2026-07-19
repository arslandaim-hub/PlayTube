/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import coil3.compose.AsyncImage
import com.arslandaim.playtube.domain.model.VideoItem
import java.util.Locale

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration

@Composable
fun VideoList(
    videos: List<VideoItem>,
    downloadedIds: Set<String> = emptySet(),
    favoriteIds: Set<String> = emptySet(),
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: ((String) -> Unit)? = null,
    onFavoriteClick: ((VideoItem) -> Unit)? = null,
    onDownloadClick: ((VideoItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columns = if (isLandscape) 2 else 1

    if (columns > 1) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = videos,
                key = { video -> video.id },
                contentType = { "video" }
            ) { video ->
                VideoItemRow(
                    video = video,
                    isDownloaded = downloadedIds.contains(video.id),
                    isFavorite = favoriteIds.contains(video.id),
                    onFavoriteClick = if (onFavoriteClick != null) { { onFavoriteClick(video) } } else null,
                    onDownloadClick = if (onDownloadClick != null) { { onDownloadClick(video) } } else null,
                    onChannelClick = if (onChannelClick != null && video.uploaderUrl != null) { { onChannelClick(video.uploaderUrl) } } else null,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = videos,
                key = { video -> video.id },
                contentType = { "video" }
            ) { video ->
                VideoItemRow(
                    video = video,
                    isDownloaded = downloadedIds.contains(video.id),
                    isFavorite = favoriteIds.contains(video.id),
                    onFavoriteClick = if (onFavoriteClick != null) { { onFavoriteClick(video) } } else null,
                    onDownloadClick = if (onDownloadClick != null) { { onDownloadClick(video) } } else null,
                    onChannelClick = if (onChannelClick != null && video.uploaderUrl != null) { { onChannelClick(video.uploaderUrl) } } else null,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}

@Composable
fun VideoItemRow(
    video: VideoItem,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onChannelClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "VideoScale")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)) // Added clip for modern rounded thumbnails
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium
            )
            
            // Duration Badge
            if (video.duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Downloaded Tag
            if (isDownloaded) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(bottomStart = 12.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Downloaded",
                        modifier = Modifier.padding(6.dp).size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = onChannelClick != null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { onChannelClick?.invoke() }
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = video.uploaderThumbnailUrl,
                    contentDescription = "Channel Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = null,
                    fallback = null
                )
                
                if (video.uploaderThumbnailUrl == null) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = video.uploaderName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (onFavoriteClick != null || onDownloadClick != null) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (onDownloadClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download)) },
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
                                        text = { Text(if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
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
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                VideoMetadata(
                    uploaderName = video.uploaderName,
                    viewCount = video.viewCount,
                    uploadDate = video.uploadDate,
                    onChannelClick = if (onChannelClick != null) { { onChannelClick() } } else null
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", m, s)
    }
}
