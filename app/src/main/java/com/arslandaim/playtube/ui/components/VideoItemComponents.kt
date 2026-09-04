/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.PlaylistItem
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.arslandaim.playtube.utils.VideoUtils
import com.arslandaim.playtube.utils.Constants
import android.content.res.Configuration
import coil3.size.Precision

enum class ThumbnailQuality {
    Low, Medium, High, Ultra
}

@Composable
fun ThumbnailImage(
    videoId: String,
    thumbnailUrl: String,
    modifier: Modifier = Modifier,
    quality: ThumbnailQuality = ThumbnailQuality.Medium,
    contentScale: ContentScale = ContentScale.Crop,
    filterQuality: FilterQuality = FilterQuality.High
) {
    val context = LocalContext.current

    val effectiveId = remember(videoId, thumbnailUrl) {
        val extractedId = if (thumbnailUrl.contains("ytimg.com") || thumbnailUrl.contains("youtube.com")) {
            VideoUtils.extractVideoId(thumbnailUrl)
        } else null
        videoId.ifBlank { extractedId ?: "" }
    }

    // Structured fallback chain based on requested quality
    val sourceUrls = remember(videoId, thumbnailUrl, quality, effectiveId) {
        if (effectiveId.isBlank()) {
            listOf(thumbnailUrl)
        } else {
            when (quality) {
                ThumbnailQuality.Ultra -> listOf(
                    VideoUtils.getMaxResThumbnail(effectiveId),
                    VideoUtils.getHq720ThumbnailUrl(effectiveId),
                    VideoUtils.getSdResThumbnailUrl(effectiveId),
                    VideoUtils.getHighResThumbnail(effectiveId)
                )
                ThumbnailQuality.High -> listOf(
                    VideoUtils.getMaxResThumbnail(effectiveId),
                    VideoUtils.getHq720ThumbnailUrl(effectiveId),
                    VideoUtils.getSdResThumbnailUrl(effectiveId),
                    VideoUtils.getHighResThumbnail(effectiveId)
                )
                ThumbnailQuality.Medium -> listOf(
                    VideoUtils.getSdResThumbnailUrl(effectiveId),
                    VideoUtils.getHighResThumbnail(effectiveId),
                    VideoUtils.getMediumResThumbnail(effectiveId)
                )
                ThumbnailQuality.Low -> listOf(
                    VideoUtils.getMediumResThumbnail(effectiveId),
                    VideoUtils.getLowResThumbnail(effectiveId)
                )
            }
        }
    }
    
    var currentUrlIndex by remember(videoId, thumbnailUrl, quality) { mutableIntStateOf(0) }
    var isLoaded by remember(videoId, thumbnailUrl, quality) { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        if (!isLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )
        }

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(sourceUrls.getOrNull(currentUrlIndex))
                .size(width, height)
                .precision(coil3.size.Precision.INEXACT)
                .allowHardware(true)
                .build(),
            onSuccess = {
                isLoaded = true
            },
            onError = {
                if (currentUrlIndex < sourceUrls.size - 1) {
                    currentUrlIndex++
                } else {
                    isLoaded = true
                }
            },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            filterQuality = filterQuality
        )
    }
}

@Composable
fun PremiumChannelCard(
    channel: SearchItem.Channel,
    onToggleSubscription: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = channel.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.Medium
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
                    text = stringResource(R.string.subscribers_count, VideoUtils.formatNumber(channel.subscriberCount)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            channel.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (channel.isSubscribed) {
            FilledTonalButton(
                onClick = onToggleSubscription,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscribed),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Button(
                onClick = onToggleSubscription,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = stringResource(R.string.subscribe),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PremiumPlaylistCard(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            // Stack Effect Layer 1 (Bottom)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.85f)
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp)
                    .graphicsLayer { alpha = 0.4f },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 2.dp
            ) {}

            // Stack Effect Layer 2 (Middle)
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.92f)
                    .align(Alignment.TopCenter)
                    .offset(y = 4.dp)
                    .graphicsLayer { alpha = 0.7f },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp
            ) {}

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            ThumbnailImage(
                videoId = "",
                thumbnailUrl = playlist.thumbnailUrl,
                quality = ThumbnailQuality.High,
                modifier = Modifier.fillMaxSize()
            )
            
            // Right Side Overlay (Playlist Info)
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .align(Alignment.CenterEnd),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.streamCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.videos).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Playlist Icon Placeholder
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${playlist.uploaderName} • Playlist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun VideoList(
    videos: List<VideoItem>,
    downloadedIds: Set<String> = emptySet(),
    favoriteIds: Set<String> = emptySet(),
    savedVideoIds: Set<String> = emptySet(),
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: ((String) -> Unit)? = null,
    onFavoriteClick: ((VideoItem) -> Unit)? = null,
    onNotInterestedClick: ((VideoItem) -> Unit)? = null,
    onDownloadClick: ((VideoItem) -> Unit)? = null,
    onAddToPlaylistClick: ((VideoItem) -> Unit)? = null,
    onRemoveFromPlaylistClick: ((VideoItem) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    isLoadingMore: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 100.dp)
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val columns = Constants.calculateGridColumns(screenWidth)

    if (columns > 1) {
        val gridState = rememberLazyGridState()
        InfiniteScrollGridEffect(
            gridState = gridState,
            enabled = onLoadMore != null && videos.isNotEmpty() && !isLoadingMore,
            onLoadMore = { onLoadMore?.invoke() }
        )

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(columns),
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (header != null) {
                item(span = { GridItemSpan(columns) }) {
                    header()
                }
            }

            items(
                items = videos,
                key = { video -> video.id },
                contentType = { "video" }
            ) { video ->
                val currentOnFavoriteClick = remember(video, onFavoriteClick) {
                    if (onFavoriteClick != null) { { onFavoriteClick(video) } } else null
                }
                val currentOnNotInterestedClick = remember(video, onNotInterestedClick) {
                    if (onNotInterestedClick != null) { { onNotInterestedClick(video) } } else null
                }
                val currentOnDownloadClick = remember(video, onDownloadClick) {
                    if (onDownloadClick != null) { { onDownloadClick(video) } } else null
                }
                val currentOnAddToPlaylistClick = remember(video, onAddToPlaylistClick) {
                    if (onAddToPlaylistClick != null) { { onAddToPlaylistClick(video) } } else null
                }
                val currentOnRemoveFromPlaylistClick = remember(video, onRemoveFromPlaylistClick) {
                    if (onRemoveFromPlaylistClick != null) { { onRemoveFromPlaylistClick(video) } } else null
                }
                val currentOnChannelClick = remember(video, onChannelClick) {
                    if (onChannelClick != null && video.uploaderUrl != null) { { onChannelClick(video.uploaderUrl) } } else null
                }
                val currentOnClick = remember(video, onVideoClick) {
                    { onVideoClick(video) }
                }

                VideoItemRow(
                    video = video,
                    isDownloaded = downloadedIds.contains(video.id),
                    isFavorite = favoriteIds.contains(video.id),
                    isSaved = savedVideoIds.contains(video.id),
                    onFavoriteClick = currentOnFavoriteClick,
                    onNotInterestedClick = currentOnNotInterestedClick,
                    onDownloadClick = currentOnDownloadClick,
                    onAddToPlaylistClick = currentOnAddToPlaylistClick,
                    onRemoveFromPlaylistClick = currentOnRemoveFromPlaylistClick,
                    onChannelClick = currentOnChannelClick,
                    onClick = currentOnClick
                )
            }
            
            if (isLoadingMore) {
                item(span = { GridItemSpan(columns) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    } else {
        val listState = rememberLazyListState()
        InfiniteScrollEffect(
            listState = listState,
            enabled = onLoadMore != null && videos.isNotEmpty() && !isLoadingMore,
            onLoadMore = { onLoadMore?.invoke() }
        )

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (header != null) {
                item {
                    header()
                }
            }

            items(
                items = videos,
                key = { video -> video.id },
                contentType = { "video" }
            ) { video ->
                val currentOnFavoriteClick = remember(video, onFavoriteClick) {
                    if (onFavoriteClick != null) { { onFavoriteClick(video) } } else null
                }
                val currentOnNotInterestedClick = remember(video, onNotInterestedClick) {
                    if (onNotInterestedClick != null) { { onNotInterestedClick(video) } } else null
                }
                val currentOnDownloadClick = remember(video, onDownloadClick) {
                    if (onDownloadClick != null) { { onDownloadClick(video) } } else null
                }
                val currentOnAddToPlaylistClick = remember(video, onAddToPlaylistClick) {
                    if (onAddToPlaylistClick != null) { { onAddToPlaylistClick(video) } } else null
                }
                val currentOnRemoveFromPlaylistClick = remember(video, onRemoveFromPlaylistClick) {
                    if (onRemoveFromPlaylistClick != null) { { onRemoveFromPlaylistClick(video) } } else null
                }
                val currentOnChannelClick = remember(video, onChannelClick) {
                    if (onChannelClick != null && video.uploaderUrl != null) { { onChannelClick(video.uploaderUrl) } } else null
                }
                val currentOnClick = remember(video, onVideoClick) {
                    { onVideoClick(video) }
                }

                VideoItemRow(
                    video = video,
                    isDownloaded = downloadedIds.contains(video.id),
                    isFavorite = favoriteIds.contains(video.id),
                    onFavoriteClick = currentOnFavoriteClick,
                    onNotInterestedClick = currentOnNotInterestedClick,
                    onDownloadClick = currentOnDownloadClick,
                    onAddToPlaylistClick = currentOnAddToPlaylistClick,
                    onRemoveFromPlaylistClick = currentOnRemoveFromPlaylistClick,
                    onChannelClick = currentOnChannelClick,
                    onClick = currentOnClick
                )
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun VideoItemRow(
    video: VideoItem,
    isDownloaded: Boolean = false,
    isFavorite: Boolean = false,
    isSaved: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
    onNotInterestedClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = 0.5.dp, 
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), 
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            ThumbnailImage(
                videoId = video.id,
                thumbnailUrl = video.thumbnailUrl,
                quality = ThumbnailQuality.High,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            
            // High-End Duration Badge
            if (video.duration > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = VideoUtils.formatDuration(video.duration),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                        modifier = Modifier.padding(8.dp).size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Watch Progress Bar
            video.watchProgress?.let { progress ->
                if (progress > 0.001f) {
                    WatchProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(
                        enabled = onChannelClick != null,
                        onClick = { onChannelClick?.invoke() }
                    ),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.2).sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (onFavoriteClick != null || onDownloadClick != null || onNotInterestedClick != null || onAddToPlaylistClick != null || onRemoveFromPlaylistClick != null) {
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(x = 8.dp, y = (-4).dp)
                            ) {
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
                                    imageVector = if (isFavorite) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt, 
                                    contentDescription = null,
                                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                ) 
                            },
                                        onClick = {
                                            showMenu = false
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
                                            showMenu = false
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
                                            showMenu = false
                                            onRemoveFromPlaylistClick()
                                        }
                                    )
                                }
                                if (onNotInterestedClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.not_interested)) },
                                        leadingIcon = { 
                                            Icon(
                                                imageVector = Icons.Default.Block, 
                                                contentDescription = null
                                            ) 
                                        },
                                        onClick = {
                                            showMenu = false
                                            onNotInterestedClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
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

@Composable
fun WatchProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "WatchProgressAnimation"
    )

    Box(
        modifier = modifier
            .height(3.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
