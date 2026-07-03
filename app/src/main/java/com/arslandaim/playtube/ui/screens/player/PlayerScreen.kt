package com.arslandaim.playtube.ui.screens.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.screens.search.VideoItemRow
import com.arslandaim.playtube.utils.VideoUtils
import kotlinx.coroutines.delay
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoId: String,
    initialTitle: String? = null,
    initialThumbnail: String? = null,
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isSubscribed by viewModel.isSubscribed.collectAsState()
    val currentQuality by viewModel.currentQuality.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val isMinimized by viewModel.miniPlayerManager.isMinimized.collectAsState()
    val downloadedIds by viewModel.downloadedVideoIds.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }

    val isDownloaded = downloadedIds.contains(videoId)

    // Auto-pop backstack when minimized
    LaunchedEffect(isMinimized) {
        if (isMinimized) {
            onBack()
        }
    }

    // Gesture states
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    var brightnessOverlayVisible by remember { mutableStateOf(false) }
    var volumeOverlayVisible by remember { mutableStateOf(false) }
    var brightnessLevel by remember { mutableFloatStateOf(0f) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Initialize brightnessLevel
    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val layoutParams = activity?.window?.attributes
        brightnessLevel = if (layoutParams?.screenBrightness ?: -1f < 0) {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
        } else {
            layoutParams?.screenBrightness ?: 0.5f
        }
    }

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // If the video is NOT minimized, it means the user hit back to close it.
            // We only keep the player running if mini-player mode is active.
            if (!viewModel.miniPlayerManager.isMinimized.value) {
                viewModel.stopPlayback()
            }

            // Reset orientation on dispose
            val activity = context as? Activity
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (showDownloadDialog) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            DownloadQualityDialog(
                videoStreams = it.bundle.videoStreams,
                audioStreams = it.bundle.audioStreams,
                onDismiss = { showDownloadDialog = false },
                onDownload = { stream ->
                    viewModel.download(stream.url, stream.quality, stream.format, stream.isAdaptive)
                    showDownloadDialog = false
                }
            )
        }
    }

    if (showQualityDialog) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            QualitySelectionDialog(
                videoStreams = it.bundle.videoStreams,
                currentQuality = currentQuality,
                onDismiss = { showQualityDialog = false },
                onQualitySelected = { stream ->
                    viewModel.setQuality(stream)
                    showQualityDialog = false
                }
            )
        }
    }

    if (showDescriptionSheet) {
        val state = uiState as? PlayerUiState.Success
        state?.let {
            ModalBottomSheet(
                onDismissRequest = { showDescriptionSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it.bundle.description ?: "No description available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false }
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text("Quality") },
                    supportingContent = { Text(currentQuality ?: "Auto") },
                    leadingContent = { Icon(Icons.Default.Settings, null) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showQualityDialog = true
                    }
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.surface
                        ),
                        startY = 0f,
                        endY = 500f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    when (val state = uiState) {
                        is PlayerUiState.Loading, is PlayerUiState.Error -> {
                            // Show placeholder during loading or error
                            AsyncImage(
                                model = initialThumbnail,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.Medium
                            )
                            if (state is PlayerUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    color = Color.White
                                )
                            }
                        }
                        is PlayerUiState.Success -> {
                            VideoPlayerGestureDetector(
                                onDoubleTapLeft = { viewModel.seekBackward() },
                                onDoubleTapRight = { viewModel.seekForward() },
                                onSingleTap = { /* PlayerView handles its own controls usually */ },
                                onSwipeDown = {
                                    viewModel.minimize()
                                },
                                onSwipeUp = {
                                    val activity = context as? Activity
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                },
                                onDragStart = {
                                    isDragging = true
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    volumeLevel = currentVolume.toFloat() / maxVolume
                                },
                                onVerticalSwipeLeft = { dragPercentage ->
                                    brightnessLevel = (brightnessLevel + dragPercentage).coerceIn(0f, 1f)
                                    val activity = context as? Activity
                                    val layoutParams = activity?.window?.attributes
                                    layoutParams?.screenBrightness = brightnessLevel
                                    activity?.window?.attributes = layoutParams
                                    
                                    brightnessOverlayVisible = true
                                    volumeOverlayVisible = false
                                },
                                onVerticalSwipeRight = { dragPercentage ->
                                    volumeLevel = (volumeLevel + dragPercentage).coerceIn(0f, 1f)
                                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val newVolume = (volumeLevel * maxVolume).toInt()
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                                    
                                    volumeOverlayVisible = true
                                    brightnessOverlayVisible = false
                                },
                                onDragEnd = { isDragging = false },
                                onDragCancel = { isDragging = false }
                            ) {
                                AndroidView(
                                    factory = {
                                        PlayerView(context).apply {
                                            player = viewModel.player
                                            useController = true
                                            @androidx.media3.common.util.UnstableApi
                                            setShowPreviousButton(false)
                                            @androidx.media3.common.util.UnstableApi
                                            setShowNextButton(false)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            GestureOverlay(
                                visible = brightnessOverlayVisible,
                                icon = Icons.Default.BrightnessLow,
                                text = "${(brightnessLevel * 100).toInt()}%"
                            )
                            
                            GestureOverlay(
                                visible = volumeOverlayVisible,
                                icon = Icons.Default.VolumeUp,
                                text = "${(volumeLevel * 100).toInt()}%"
                            )

                            LaunchedEffect(brightnessOverlayVisible, isDragging) {
                                if (brightnessOverlayVisible && !isDragging) {
                                    delay(1500)
                                    brightnessOverlayVisible = false
                                }
                            }

                            LaunchedEffect(volumeOverlayVisible, isDragging) {
                                if (volumeOverlayVisible && !isDragging) {
                                    delay(1500)
                                    volumeOverlayVisible = false
                                }
                            }

                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(48.dp),
                                    color = Color.White,
                                    strokeWidth = 4.dp
                                )
                            }
                        }
                    }
                }

                // Metadata Area
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (val state = uiState) {
                        is PlayerUiState.Loading -> {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = initialTitle ?: "Loading...",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        is PlayerUiState.Success -> {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = state.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = VideoUtils.formatViewCount(state.bundle.viewCount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = " • ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = VideoUtils.formatUploadDate(state.bundle.uploadDate),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { state.bundle.uploaderUrl?.let { onChannelClick(it) } },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = state.bundle.uploaderThumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                            filterQuality = FilterQuality.Medium
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = state.uploader,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.toggleSubscription() },
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
                                            Text(text = if (isSubscribed) "Subscribed" else "Subscribe")
                                        }
                                    }
                                }
                            }

                            @OptIn(ExperimentalFoundationApi::class)
                            stickyHeader {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color.Transparent // Changed to transparent to show the background glow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ActionChip(
                                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            label = if (isFavorite) "Liked" else "Like",
                                            onClick = { viewModel.toggleFavorite() },
                                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        ActionChip(
                                            icon = if (isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                            label = if (isDownloaded) "Downloaded" else "Download",
                                            onClick = { if (!isDownloaded) showDownloadDialog = true },
                                            tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        ActionChip(
                                            icon = Icons.Default.Settings,
                                            label = "Settings",
                                            onClick = { showSettingsSheet = true }
                                        )
                                        ActionChip(
                                            icon = Icons.Default.Description,
                                            label = "Description",
                                            onClick = { showDescriptionSheet = true }
                                        )
                                    }
                                }
                            }

                            item {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "Related Videos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            items(state.bundle.relatedVideos) { relatedVideo ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    VideoItemRow(
                                        video = relatedVideo,
                                        isDownloaded = downloadedIds.contains(relatedVideo.id),
                                        onClick = { onVideoClick(relatedVideo) }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                        is PlayerUiState.Error -> {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = state.message,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(onClick = { viewModel.loadVideo(videoId) }) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun DownloadQualityDialog(
    videoStreams: List<StreamItem>,
    audioStreams: List<StreamItem>,
    onDismiss: () -> Unit,
    onDownload: (StreamItem) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Quality") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Video", style = MaterialTheme.typography.labelLarge)
                videoStreams.forEach { stream ->
                    TextButton(onClick = { onDownload(stream) }) {
                        Text("${stream.quality} (${stream.format})")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Audio", style = MaterialTheme.typography.labelLarge)
                audioStreams.forEach { stream ->
                    TextButton(onClick = { onDownload(stream) }) {
                        Text("${stream.quality} (${stream.format})")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
