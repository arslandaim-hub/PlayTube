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
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.SubtitleItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    videoStreams: List<StreamItem>,
    currentQuality: String?,
    preferredQuality: String,
    onDismiss: () -> Unit,
    onQualitySelected: (StreamItem?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.video_quality),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = if (preferredQuality == "Auto" && currentQuality != null) 
                                    "Auto ($currentQuality)" 
                                else "Auto"
                            ) 
                        },
                        leadingContent = { 
                            RadioButton(
                                selected = preferredQuality == "Auto",
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onQualitySelected(null) }
                    )
                }

                items(videoStreams) { stream ->
                    ListItem(
                        headlineContent = { Text(text = "${stream.quality} (${stream.format})") },
                        leadingContent = { 
                            RadioButton(
                                selected = preferredQuality == stream.quality,
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
                text = stringResource(R.string.playback_speed),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(speeds) { speed ->
                    ListItem(
                        headlineContent = { Text(text = if (speed == 1.0f) stringResource(R.string.normal_speed) else "${speed}x") },
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
fun PitchSelectionSheet(
    currentPitch: Float,
    onDismiss: () -> Unit,
    onPitchSelected: (Float) -> Unit
) {
    val pitches = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Audio Pitch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(pitches) { pitch ->
                    ListItem(
                        headlineContent = { Text(text = if (pitch == 1.0f) "Normal" else "${pitch}x") },
                        leadingContent = { 
                            RadioButton(
                                selected = pitch == currentPitch,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onPitchSelected(pitch) }
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
                    text = stringResource(R.string.download_quality),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            if (videoStreams.isNotEmpty()) {
                item {
                    SectionHeader(title = stringResource(R.string.video), icon = Icons.Default.VideoFile)
                }
                items(videoStreams) { stream ->
                    StreamListItem(stream = stream, onDownload = onDownload)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSelectionSheet(
    subtitles: List<SubtitleItem>,
    currentLanguage: String?,
    isCcEnabled: Boolean,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.subtitles),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(text = stringResource(R.string.off)) },
                        leadingContent = { 
                            RadioButton(
                                selected = !isCcEnabled,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onLanguageSelected(null) }
                    )
                }
                
                items(subtitles) { subtitle ->
                    val locale = java.util.Locale.forLanguageTag(subtitle.languageTag)
                    val languageName = locale.displayLanguage.replaceFirstChar { it.uppercase() }
                    ListItem(
                        headlineContent = { 
                            Text(text = if (subtitle.isAutoGenerated) stringResource(R.string.language_auto_generated, languageName, stringResource(R.string.auto_generated)) else languageName) 
                        },
                        supportingContent = { Text(text = subtitle.languageTag) },
                        leadingContent = { 
                            RadioButton(
                                selected = isCcEnabled && subtitle.languageTag == currentLanguage,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onLanguageSelected(subtitle.languageTag) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<com.arslandaim.playtube.data.local.LocalPlaylistEntity>,
    playlistsWithVideo: Set<Int> = emptySet(),
    onDismiss: () -> Unit,
    onPlaylistSelected: (com.arslandaim.playtube.data.local.LocalPlaylistEntity) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                headlineContent = { Text("Create New Playlist") },
                leadingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.clickable { onCreateNewPlaylist() }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn {
                items(playlists) { playlist ->
                    val isAdded = playlistsWithVideo.contains(playlist.id)
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = playlist.name,
                                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        leadingContent = { 
                            Icon(
                                imageVector = if (isAdded) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistPlay, 
                                contentDescription = null,
                                tint = if (isAdded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            ) 
                        },
                        trailingContent = {
                            if (isAdded) {
                                Text(
                                    text = "Added",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isAdded) { onPlaylistSelected(playlist) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDownloadSelectionSheet(
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    val options = listOf("4K", "1080p", "720p", "480p", "360p")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.playlist_download_quality),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(options) { quality ->
                    ListItem(
                        headlineContent = { Text(text = quality) },
                        leadingContent = { 
                            Icon(
                                imageVector = if (quality == "1080p" || quality == "4K") Icons.Default.HighQuality else Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = if (quality == "1080p" || quality == "4K") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onDownload(quality) }
                    )
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
