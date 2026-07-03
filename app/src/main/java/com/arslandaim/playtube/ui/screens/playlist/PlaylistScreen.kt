package com.arslandaim.playtube.ui.screens.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.screens.search.VideoItemRow

import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    viewModel: PlaylistViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isPlaylistDownloaded = remember(uiState, downloadedIds) {
        val state = uiState as? PlaylistUiState.Success
        if (state != null) {
            state.details.videos.all { downloadedIds.contains(it.id) }
        } else false
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Playlist") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is PlaylistUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is PlaylistUiState.Success -> {
                    val details = state.details
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = details.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "By ${details.uploaderName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { if (!isPlaylistDownloaded) viewModel.downloadPlaylist() },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        colors = if (isPlaylistDownloaded) {
                                            ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else ButtonDefaults.buttonColors()
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaylistDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isPlaylistDownloaded) "Downloaded" else "Download All")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = { viewModel.toggleFavorite() },
                                        modifier = Modifier.weight(1f),
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        colors = if (isFavorite) {
                                            ButtonDefaults.outlinedButtonColors(
                                                contentColor = MaterialTheme.colorScheme.primary
                                            )
                                        } else ButtonDefaults.outlinedButtonColors()
                                    ) {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isFavorite) "Liked" else "Favorite")
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        items(details.videos) { video ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                VideoItemRow(
                                    video = video,
                                    isDownloaded = downloadedIds.contains(video.id),
                                    onClick = { onVideoClick(video) }
                                )
                            }
                        }
                    }
                }
                is PlaylistUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadPlaylist(playlistId) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
