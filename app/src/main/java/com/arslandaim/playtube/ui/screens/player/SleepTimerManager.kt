/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SleepTimerManager @Inject constructor(
    private val player: ExoPlayer
) {
    private val _remainingTime = MutableStateFlow<Int?>(null) // null = off, -1 = end of video
    val remainingTime = _remainingTime.asStateFlow()

    private val _shouldCloseApp = MutableStateFlow(false)
    val shouldCloseApp = _shouldCloseApp.asStateFlow()

    private val _timerFinishedEvent = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinishedEvent = _timerFinishedEvent.asSharedFlow()

    fun isTimerActive(): Boolean = _remainingTime.value != null

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _remainingTime.value == -1) {
                stopPlayback()
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    fun startTimer(minutes: Int) {
        cancelTimerInternal()
        _remainingTime.value = minutes
        timerJob = scope.launch {
            while ((_remainingTime.value ?: 0) > 0) {
                delay(60000)
                if (isActive) {
                    _remainingTime.value = (_remainingTime.value ?: 1) - 1
                    if (_remainingTime.value == 0) {
                        stopPlayback()
                    }
                }
            }
        }
    }

    fun setEndOfVideo() {
        cancelTimerInternal()
        _remainingTime.value = -1
    }

    fun setShouldCloseApp(close: Boolean) {
        _shouldCloseApp.value = close
    }

    fun cancelTimer() {
        cancelTimerInternal()
    }

    private fun cancelTimerInternal() {
        timerJob?.cancel()
        timerJob = null
        _remainingTime.value = null
    }

    private fun stopPlayback() {
        player.pause()
        scope.launch { _timerFinishedEvent.emit(Unit) }
        cancelTimerInternal()
    }
}
