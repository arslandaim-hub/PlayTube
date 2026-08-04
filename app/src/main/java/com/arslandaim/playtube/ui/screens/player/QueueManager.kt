/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueManager @Inject constructor() {
    private val _skipToNextEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val skipToNextEvent = _skipToNextEvent.asSharedFlow()

    private val _skipToPreviousEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val skipToPreviousEvent = _skipToPreviousEvent.asSharedFlow()

    fun skipToNext() {
        _skipToNextEvent.tryEmit(Unit)
    }

    fun skipToPrevious() {
        _skipToPreviousEvent.tryEmit(Unit)
    }
}
