/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.arslandaim.playtube.domain.model.CommentItem
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
    isSaved: Boolean,
    isDownloaded: Boolean,
    comments: List<CommentItem>,
    commentCount: Int?,
    onToggleSubscription: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSaveClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    onCommentsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Title & Micro-stats
        VideoHeaderSection(
            title = title,
            viewCount = viewCount,
            uploadDate = uploadDate,
            description = description
        )
        
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Action Row
        PlayerActionRow(
            isFavorite = isFavorite,
            isSaved = isSaved,
            isDownloaded = isDownloaded,
            onToggleFavorite = onToggleFavorite,
            onSaveClick = onSaveClick,
            onDownloadClick = onDownloadClick,
            onShareClick = onShareClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        // 4. Comments Preview
        CommentsPreviewCard(
            comments = comments,
            totalCount = commentCount,
            onClick = onCommentsClick
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(
            thickness = 0.5.dp, 
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        )
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
        enter = fadeIn() + expandHorizontally(expandFrom = if (isRightSide) Alignment.End else Alignment.Start),
        exit = fadeOut() + shrinkHorizontally(shrinkTowards = if (isRightSide) Alignment.End else Alignment.Start),
        modifier = modifier
            .fillMaxHeight(0.35f)
            .width(42.dp)
            .padding(horizontal = 8.dp)
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = CircleShape,
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .width(3.dp)
                        .padding(vertical = 10.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(progress.coerceIn(0f, 1f))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.White, Color.White.copy(alpha = 0.8f))
                                )
                            )
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
    var isExpanded by remember { mutableStateOf(value = false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { isExpanded = !isExpanded }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            maxLines = if (isExpanded) 15 else 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 28.sp,
            letterSpacing = (-0.5).sp
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${VideoUtils.formatViewCount(viewCount)} views",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "•",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = VideoUtils.formatUploadDate(uploadDate),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (isExpanded) "less" else "...more",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        if (isExpanded && !description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
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
            model = uploaderThumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
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
            if ((subscriberCount != null) && (subscriberCount > 0)) {
                Text(
                    text = "${VideoUtils.formatNumber(subscriberCount)} subscribers",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
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
            contentPadding = PaddingValues(horizontal = 18.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun PlayerActionRow(
    isFavorite: Boolean,
    isSaved: Boolean,
    isDownloaded: Boolean,
    onToggleFavorite: () -> Unit,
    onSaveClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerActionPill(
            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            label = if (isFavorite) stringResource(R.string.liked) else stringResource(R.string.like),
            active = isFavorite,
            onClick = onToggleFavorite
        )
        
        PlayerActionPill(
            icon = if (isSaved) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistAdd,
            label = if (isSaved) stringResource(R.string.saved) else stringResource(R.string.save),
            active = isSaved,
            onClick = onSaveClick
        )

        PlayerActionPill(
            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
            label = if (isDownloaded) stringResource(R.string.downloaded) else stringResource(R.string.download),
            active = isDownloaded,
            onClick = onDownloadClick
        )

        PlayerActionPill(
            icon = Icons.Default.Share,
            label = stringResource(R.string.share),
            active = false,
            onClick = onShareClick
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
    val haptic = LocalHapticFeedback.current
    
    val backgroundColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) 
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        label = "PillBackground"
    )
    
    val contentColor by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary 
                      else MaterialTheme.colorScheme.onSurface,
        label = "PillContent"
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

fun LazyListScope.relatedVideosSection(
    relatedVideos: List<VideoItem>,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    isAutoplayEnabled: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
) {
    item {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.related_videos),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = "Autoplay",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Switch(
                        checked = isAutoplayEnabled,
                        onCheckedChange = onAutoplayChange,
                        modifier = Modifier.scale(0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    items(relatedVideos, key = { it.id }) { relatedVideo ->
        VideoItemRow(
            video = relatedVideo,
            isDownloaded = downloadedIds.contains(relatedVideo.id),
            isFavorite = favoriteIds.contains(relatedVideo.id),
            onFavoriteClick = { onFavoriteClick(relatedVideo) },
            onAddToPlaylistClick = { onAddToPlaylistClick(relatedVideo) },
            onDownloadClick = { onDownloadClick(relatedVideo) },
            onChannelClick = { onChannelClick(relatedVideo.uploaderUrl ?: "") },
            onClick = { onVideoClick(relatedVideo) }
        )
    }
}
