/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.ui.screens.library.HistoryItemRow
import com.arslandaim.playtube.ui.components.EmptyState
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.screens.settings.SettingsViewModel
import com.arslandaim.playtube.domain.model.VideoItem
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R

import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@Composable
fun HistoryScreen(
    settingsViewModel: SettingsViewModel,
    historyViewModel: com.arslandaim.playtube.ui.screens.library.LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onDiscoverVideos: () -> Unit
) {
    val history by historyViewModel.history.collectAsStateWithLifecycle()
    val savedVideoIds by historyViewModel.savedVideoIds.collectAsStateWithLifecycle()
    val isHistoryEnabled by settingsViewModel.isHistoryEnabled.collectAsStateWithLifecycle()

    HistoryContent(
        history = history,
        savedVideoIds = savedVideoIds,
        isHistoryEnabled = isHistoryEnabled,
        onSetHistoryEnabled = settingsViewModel::setHistoryEnabled,
        onClearHistory = settingsViewModel::clearHistory,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onBack = onBack,
        onVideoClick = onVideoClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onRemoveHistoryItem = historyViewModel::removeFromHistory,
        onDiscoverVideos = onDiscoverVideos
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    history: List<com.arslandaim.playtube.data.local.HistoryEntity>,
    savedVideoIds: Set<String>,
    isHistoryEnabled: Boolean,
    onSetHistoryEnabled: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onDiscoverVideos: () -> Unit
) {
    val historyClearedMessage = stringResource(R.string.history_cleared)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GlassSurface(tonalElevation = 0.dp) {
                TopAppBar(
                    title = { Text(stringResource(R.string.history), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showClearHistoryDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = stringResource(R.string.pause_watch_history), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = !isHistoryEnabled,
                    onCheckedChange = { paused ->
                        onSetHistoryEnabled(!paused)
                    }
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (history.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.no_history),
                    description = stringResource(R.string.no_history_desc),
                    actionText = stringResource(R.string.discover_videos),
                    onActionClick = onDiscoverVideos
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), 
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(history, key = { it.videoId + it.timestamp }) { item ->
                        val currentOnClick = remember(item.videoId, onVideoClick) {
                            { onVideoClick(item.toVideoItem()) }
                        }
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            HistoryItemRow(
                                item = item,
                                onClick = currentOnClick,
                                isSaved = savedVideoIds.contains(item.videoId),
                                onAddToPlaylistClick = { onAddToPlaylistClick(item.toVideoItem()) },
                                onRemoveClick = { onRemoveHistoryItem(item.videoId) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(historyClearedMessage)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
