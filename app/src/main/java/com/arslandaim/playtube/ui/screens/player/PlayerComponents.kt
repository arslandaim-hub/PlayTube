/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.utils.VideoUtils

@Composable
fun UnifiedMetadataHub(
    title: String,
    viewCount: Long,
    uploadDate: String?,
    description: String?,
    uploaderName: String,
    uploaderThumbnailUrl: String?,
    uploaderUrl: String?,
    subscriberCount: Long?,
    isSubscribed: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onToggleSubscription: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onChannelClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.7f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Title & Micro-stats
            VideoHeaderSection(
                title = title,
                viewCount = viewCount,
                uploadDate = uploadDate,
                description = description
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Channel Section
            ChannelInfoSection(
                uploaderName = uploaderName,
                uploaderThumbnailUrl = uploaderThumbnailUrl,
                uploaderUrl = uploaderUrl,
                subscriberCount = subscriberCount,
                isSubscribed = isSubscribed,
                onToggleSubscription = onToggleSubscription,
                onChannelClick = onChannelClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Action Row
            PlayerActionRow(
                isFavorite = isFavorite,
                isDownloaded = isDownloaded,
                onToggleFavorite = onToggleFavorite,
                onDownloadClick = onDownloadClick,
                onShareClick = onShareClick
            )
        }
    }
}

@Composable
fun VerticalGestureHUD(
    visible: Boolean,
    progress: Float,
    icon: ImageVector,
    isRightSide: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = if (isRightSide) Alignment.End else Alignment.Start),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = if (isRightSide) Alignment.End else Alignment.Start),
        modifier = modifier
            .fillMaxHeight(0.4f)
            .width(48.dp)
            .padding(horizontal = 12.dp)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.45f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(4.dp)
                        .padding(vertical = 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress.coerceIn(0f, 1f))
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@Composable
fun VideoHeaderSection(
    title: String,
    viewCount: Long,
    uploadDate: String?,
    description: String? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            maxLines = if (isExpanded) 10 else 2,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${VideoUtils.formatViewCount(viewCount)} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = VideoUtils.formatUploadDate(uploadDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isExpanded) "less" else "...more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        if (isExpanded && !description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
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
            .clickable { uploaderUrl?.let { onChannelClick(it) } },
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = uploaderThumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uploaderName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subscriberCount != null && subscriberCount > 0) {
                Text(
                    text = "${VideoUtils.formatNumber(subscriberCount)} subs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(36.dp)
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
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerActionPill(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
            active = isFavorite,
            onClick = onToggleFavorite,
            modifier = Modifier.weight(1f)
        )
        
        PlayerActionPill(
            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
            label = if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download),
            active = isDownloaded,
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f)
        )

        PlayerActionPill(
            icon = Icons.Default.Share,
            label = stringResource(R.string.share),
            active = false,
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PlayerActionPill(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Surface(
        onClick = {
            // Subtle Haptic Feedback
            (context as? android.app.Activity)?.window?.decorView?.performHapticFeedback(
                android.view.HapticFeedbackConstants.VIRTUAL_KEY
            )
            onClick()
        },
        shape = RoundedCornerShape(12.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = modifier.height(38.dp)
            .animateContentSize() // Smooth layout transition
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
