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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    val downloads by viewModel.filteredDownloads.collectAsStateWithLifecycle()
    val allDownloads by viewModel.downloads.collectAsStateWithLifecycle()
    val searchQuery by viewModel.offlineSearchQuery.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    
    var videoIdToDelete by remember { mutableStateOf<String?>(null) }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCleanupConfirm by remember { mutableStateOf(false) }

    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    if (showCleanupConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanupConfirm = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Smart Cleanup") },
            text = { Text("This will remove all downloaded videos that you have already finished watching (>90%). This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearWatchedDownloads()
                        showCleanupConfirm = false
                    }
                ) {
                    Text("Clean Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (videoIdToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoIdToDelete = null },
            title = { Text(stringResource(R.string.delete_download_title)) },
            text = { Text(stringResource(R.string.delete_download_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoIdToDelete?.let { viewModel.deleteDownload(it) }
                        videoIdToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { videoIdToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = GlobalGlassAlpha),
                tonalElevation = 0.dp
            ) {
                Column {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.onOfflineSearchQueryChange(it) },
                                    placeholder = { Text("Search offline...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            } else {
                                Text("Downloads", fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    viewModel.onOfflineSearchQueryChange("")
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                                IconButton(onClick = { showCleanupConfirm = true }) {
                                    Icon(Icons.Default.CleaningServices, contentDescription = "Clean Watched")
                                }
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onOfflineSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                    
                    // Storage Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Using ${storageUsage.usedText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { 0.5f }, // Mock total device storage progress or just show a nice line
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    ) { padding ->
        if (downloads.isEmpty() && searchQuery.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptySectionPlaceholder(stringResource(R.string.no_downloads))
            }
        } else if (downloads.isEmpty() && searchQuery.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptySectionPlaceholder("No results found for \"$searchQuery\"")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = 100.dp
                )
            ) {
                val groupedDownloads = downloads.groupBy { it.playlistId }
                val singleVideos = groupedDownloads[null] ?: emptyList()
                val playlistsGroup = groupedDownloads.filterKeys { it != null }

                playlistsGroup.forEach { (playlistId, playlistVideos) ->
                    item {
                        val title = playlistVideos.firstOrNull()?.playlistTitle ?: "Playlist"
                        val isExpanded = expandedPlaylistId == playlistId
                        
                        PlaylistDownloadRow(
                            title = title,
                            videoCount = playlistVideos.size,
                            thumbnailUrl = playlistVideos.firstOrNull()?.thumbnailUrl ?: "",
                            isExpanded = isExpanded,
                            onClick = {
                                expandedPlaylistId = if (isExpanded) null else playlistId
                            }
                        )
                    }
                    
                    if (expandedPlaylistId == playlistId) {
                        items(playlistVideos) { download ->
                            DownloadItemRow(
                                download = download,
                                onClick = { onVideoClick(download.toVideoItem()) },
                                onDeleteClick = { videoIdToDelete = download.videoId },
                                onCancelClick = { viewModel.cancelDownload(download.videoId) },
                                onRetryClick = { viewModel.resumeDownload(download.videoId) },
                                modifier = Modifier.padding(start = 24.dp)
                            )
                        }
                    }
                }

                items(singleVideos) { download ->
                    DownloadItemRow(
                        download = download,
                        onClick = { onVideoClick(download.toVideoItem()) },
                        onDeleteClick = { videoIdToDelete = download.videoId },
                        onCancelClick = { viewModel.cancelDownload(download.videoId) },
                        onRetryClick = { viewModel.resumeDownload(download.videoId) }
                    )
                }
            }
        }
    }
}
