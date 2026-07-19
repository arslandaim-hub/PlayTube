/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.domain.model.StreamItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    videoStreams: List<StreamItem>,
    currentQuality: String?,
    onDismiss: () -> Unit,
    onQualitySelected: (StreamItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Video Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(videoStreams) { stream ->
                    ListItem(
                        headlineContent = { Text(text = "${stream.quality} (${stream.format})") },
                        leadingContent = { 
                            RadioButton(
                                selected = stream.quality == currentQuality,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onQualitySelected(stream) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSelectionSheet(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(speeds) { speed ->
                    ListItem(
                        headlineContent = { Text(text = if (speed == 1.0f) "Normal" else "${speed}x") },
                        leadingContent = { 
                            RadioButton(
                                selected = speed == currentSpeed,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onSpeedSelected(speed) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSelectionSheet(
    videoStreams: List<StreamItem>,
    audioStreams: List<StreamItem>,
    onDismiss: () -> Unit,
    onDownload: (StreamItem) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Select Download Quality",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            if (videoStreams.isNotEmpty()) {
                item {
                    SectionHeader(title = "Video", icon = Icons.Default.VideoFile)
                }
                items(videoStreams) { stream ->
                    StreamListItem(stream = stream, onDownload = onDownload)
                }
            }
            
            if (audioStreams.isNotEmpty()) {
                item {
                    SectionHeader(title = "Audio Only", icon = Icons.Default.AudioFile)
                }
                items(audioStreams) { stream ->
                    StreamListItem(stream = stream, onDownload = onDownload)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StreamListItem(stream: StreamItem, onDownload: (StreamItem) -> Unit) {
    ListItem(
        headlineContent = { Text(text = stream.quality) },
        supportingContent = { Text(text = stream.format) },
        trailingContent = {
            if (stream.quality.contains("1080") || stream.quality.contains("4K")) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = "High Quality",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = Modifier.clickable { onDownload(stream) }
    )
}
