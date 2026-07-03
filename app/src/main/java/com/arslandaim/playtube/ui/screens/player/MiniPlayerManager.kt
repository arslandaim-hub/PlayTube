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

    fun minimize(video: VideoItem) {
        _currentVideo.value = video
        _isMinimized.value = true
    }

    fun onNewVideoSelected() {
        _isMinimized.value = false
        // We keep currentVideo until the new one is loaded to prevent a flicker,
        // or clear it if the player screen handles the transition.
    }

    fun toggleMinimize() {
        _isMinimized.value = !_isMinimized.value
    }

    fun maximize() {
        _isMinimized.value = false
    }

    fun close(onClose: () -> Unit = {}) {
        _currentVideo.value = null
        _isMinimized.value = false
        onClose()
    }

    fun clear() {
        _currentVideo.value = null
        _isMinimized.value = false
    }
}
