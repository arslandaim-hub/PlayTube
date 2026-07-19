/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.channel

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import coil3.compose.AsyncImage
import com.arslandaim.playtube.domain.model.PlaylistItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.VideoItemRow
import com.arslandaim.playtube.utils.VideoUtils

import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@Composable
fun ChannelScreen(
    channelUrl: String,
    viewModel: ChannelViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()

    ChannelContent(
        channelUrl = channelUrl,
        uiState = uiState,
        isSubscribed = isSubscribed,
        onLoadChannel = viewModel::loadChannel,
        onToggleSubscription = viewModel::toggleSubscription,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onBack = onBack,
        onVideoClick = onVideoClick,
        onPlaylistClick = onPlaylistClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ChannelContent(
    channelUrl: String,
    uiState: ChannelUiState,
    isSubscribed: Boolean?,
    onLoadChannel: (String) -> Unit,
    onToggleSubscription: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.videos), stringResource(R.string.playlists))

    LaunchedEffect(channelUrl) {
        onLoadChannel(channelUrl)
    }

    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    Scaffold(modifier = Modifier.nestedScroll(scrollVisibilityConnection)) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is ChannelUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChannelUiState.Success -> {
                    val details = uiState.details
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // Banner with Back Button Overlay
                            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                AsyncImage(
                                    model = details.bannerUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    filterQuality = FilterQuality.Medium
                                )
                                // Back Button Overlay
                                IconButton(
                                    onClick = onBack,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            }
                            
                            // Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = details.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape),
                                    filterQuality = FilterQuality.Medium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = details.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        val subCountText = if (details.subscriberCount != null && details.subscriberCount < 0) {
                                            stringResource(R.string.subscribers_hidden)
                                        } else if (details.subscriberCount != null) {
                                            stringResource(R.string.subscribers_count, VideoUtils.formatNumber(details.subscriberCount))
                                        } else null
                                        
                                        if (subCountText != null) {
                                            Text(
                                                text = subCountText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    // Subscribe Button (Only show once the state is known to prevent flicker)
                                    if (isSubscribed != null) {
                                        Button(
                                            onClick = onToggleSubscription,
                                            colors = if (isSubscribed == true) {
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
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = if (isSubscribed == true) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                
                                details.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 12.dp)
                                    )
                                }
                            }
                        }
                        
                        // Sticky Tabs
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
                            items(details.videos) { video ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    VideoItemRow(video = video, onClick = { onVideoClick(video) })
                                }
                            }
                            if (details.videos.isEmpty()) {
                                item {
                                    EmptyChannelPlaceholder("No videos found")
                                }
                            }
                        } else {
                            items(details.playlists) { playlist ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    PlaylistItemRow(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
                                }
                            }
                            if (details.playlists.isEmpty()) {
                                item {
                                    EmptyChannelPlaceholder("No playlists found")
                                }
                            }
                        }
                    }
                }
                is ChannelUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { onLoadChannel(channelUrl) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistItemRow(
    playlist: PlaylistItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.Medium
                )
                // Playlist Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "${playlist.streamCount} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlist.uploaderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyChannelPlaceholder(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
