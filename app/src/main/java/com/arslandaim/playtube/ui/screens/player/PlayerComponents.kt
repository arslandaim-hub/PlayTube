/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.utils.VideoUtils

@Composable
fun VideoHeaderSection(
    title: String,
    viewCount: Long,
    uploadDate: String?,
    onShowDescription: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${VideoUtils.formatViewCount(viewCount)} • ${VideoUtils.formatUploadDate(uploadDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.more),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowDescription() }
            )
        }
    }
}

@Composable
fun ChannelInfoSection(
    uploaderName: String,
    uploaderThumbnailUrl: String?,
    uploaderUrl: String?,
    subscriberCount: Long?,
    isSubscribed: Boolean,
    onToggleSubscription: () -> Unit,
    onChannelClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uploaderUrl?.let { onChannelClick(it) } }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uploaderThumbnailUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.High
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uploaderName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subscriberCount != null && subscriberCount > 0) {
                Text(
                    text = "${VideoUtils.formatNumber(subscriberCount)} subscribers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Button(
            onClick = onToggleSubscription,
            colors = if (isSubscribed) {
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PlayerActionRow(
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onDownloadClick: () -> Unit,
    onShowDescription: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerActionItem(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
            onClick = onToggleFavorite,
            active = isFavorite
        )
        PlayerActionItem(
            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
            label = if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download),
            onClick = onDownloadClick,
            active = isDownloaded
        )
        PlayerActionItem(
            icon = Icons.Default.Description,
            label = stringResource(R.string.info),
            onClick = onShowDescription
        )
    }
}

@Composable
fun PlayerActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
        )
    }
}

fun LazyListScope.relatedVideosSection(
    relatedVideos: List<VideoItem>,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit
) {
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.related_videos),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    items(relatedVideos, key = { it.id }) { relatedVideo ->
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            VideoItemRow(
                video = relatedVideo,
                isDownloaded = downloadedIds.contains(relatedVideo.id),
                isFavorite = favoriteIds.contains(relatedVideo.id),
                onFavoriteClick = { onFavoriteClick(relatedVideo) },
                onDownloadClick = { onDownloadClick(relatedVideo) },
                onChannelClick = { onChannelClick(relatedVideo.uploaderUrl ?: "") },
                onClick = { onVideoClick(relatedVideo) }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
