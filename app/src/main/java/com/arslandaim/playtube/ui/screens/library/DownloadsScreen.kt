/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    var videoIdToDelete by remember { mutableStateOf<String?>(null) }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

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
                TopAppBar(
                    title = { Text("Downloads", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                EmptySectionPlaceholder(stringResource(R.string.no_downloads))
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
