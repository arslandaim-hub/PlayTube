/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import com.arslandaim.playtube.domain.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiniPlayerManager @Inject constructor() {
    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _isMinimized = MutableStateFlow(false)
    val isMinimized: StateFlow<Boolean> = _isMinimized.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _visibilityState = MutableStateFlow(MiniPlayerVisibility.Hidden)
    val visibilityState: StateFlow<MiniPlayerVisibility> = _visibilityState.asStateFlow()

    fun minimize(video: VideoItem) {
        _currentVideo.value = video
        _isMinimized.value = true
        _isExpanded.value = false
        _visibilityState.value = MiniPlayerVisibility.Minimized
    }

    fun onNewVideoSelected(video: VideoItem) {
        _currentVideo.value = video
        _isMinimized.value = false
        _isExpanded.value = true
        _visibilityState.value = MiniPlayerVisibility.Expanded
    }

    fun toggleMinimize() {
        _isMinimized.value = !_isMinimized.value
        _isExpanded.value = !_isMinimized.value
        _visibilityState.value = if (_isMinimized.value) MiniPlayerVisibility.Minimized else MiniPlayerVisibility.Expanded
    }

    fun maximize() {
        _isMinimized.value = false
        _isExpanded.value = true
        _visibilityState.value = MiniPlayerVisibility.Expanded
    }

    fun close(onClose: () -> Unit = {}) {
        _currentVideo.value = null
        _isMinimized.value = false
        _isExpanded.value = false
        _visibilityState.value = MiniPlayerVisibility.Hidden
        onClose()
    }

    fun clear() {
        _currentVideo.value = null
        _isMinimized.value = false
        _isExpanded.value = false
        _visibilityState.value = MiniPlayerVisibility.Hidden
    }
}

enum class MiniPlayerVisibility {
    Hidden, Minimized, Expanded
}
