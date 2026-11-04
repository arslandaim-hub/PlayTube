/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerGestureDetector(
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onSingleTap: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onVerticalSwipeLeft: (Float) -> Unit = {},
    onVerticalSwipeRight: (Float) -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    areVolumeBrightnessGesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val doubleTapTimeout = 300L
    
    val currentOnDoubleTapLeft by rememberUpdatedState(onDoubleTapLeft)
    val currentOnDoubleTapRight by rememberUpdatedState(onDoubleTapRight)
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnVerticalSwipeLeft by rememberUpdatedState(onVerticalSwipeLeft)
    val currentOnVerticalSwipeRight by rememberUpdatedState(onVerticalSwipeRight)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    var tapCount = 0
                    var tapJob: kotlinx.coroutines.Job? = null
                    var longPressJob: Job? = null
                    var isLongPressActive = false

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.find { it.changedToDownIgnoreConsumed() }

                            if (down != null) {
                                tapCount++
                                tapJob?.cancel()
                                longPressJob?.cancel()
                                isLongPressActive = false

                                val isLeftSide = (down.position.x < size.width / 2)

                                if (tapCount >= 2) {
                                    down.consume()
                                    if (isLeftSide) currentOnDoubleTapLeft() else currentOnDoubleTapRight()
                                    tapCount = 0
                                } else {
                                    longPressJob = launch {
                                        delay(450L)
                                        isLongPressActive = true
                                        currentOnLongPressStart()
                                    }

                                    tapJob = launch {
                                        delay(doubleTapTimeout)
                                        if (tapCount == 1 && !isLongPressActive) {
                                            currentOnSingleTap()
                                        }
                                        tapCount = 0
                                    }
                                }
                            }

                            val up = event.changes.find { !it.pressed }
                            if (up != null) {
                                longPressJob?.cancel()
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    currentOnLongPressEnd()
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(areVolumeBrightnessGesturesEnabled) {
                var totalDrag = 0f
                var dragStartX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        totalDrag = 0f
                        dragStartX = offset.x
                        if (areVolumeBrightnessGesturesEnabled) {
                            currentOnDragStart()
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        
                        if (areVolumeBrightnessGesturesEnabled) {
                            val screenHeight = size.height
                            val screenWidth = size.width
                            val dragPercentage = -dragAmount / screenHeight
                            val sideMargin = screenWidth * 0.30f // 30% from each side
                            
                            if (change.position.x < sideMargin) {
                                currentOnVerticalSwipeLeft(dragPercentage)
                            } else if (change.position.x > screenWidth - sideMargin) {
                                currentOnVerticalSwipeRight(dragPercentage)
                            }
                        }
                    },
                    onDragEnd = {
                        val screenWidth = size.width
                        val sideMargin = screenWidth * 0.30f
                        
                        // Only trigger minimize/maximize if the drag started in the center "dead zone"
                        // to avoid conflicts with volume/brightness adjustments on the sides.
                        val startedInCenter = dragStartX in sideMargin..(screenWidth - sideMargin)

                        if (startedInCenter) {
                            if (totalDrag > 150) {
                                currentOnSwipeDown()
                            } else if (totalDrag < -150) {
                                currentOnSwipeUp()
                            }
                        }
                        currentOnDragEnd()
                    },
                    onDragCancel = {
                        currentOnDragCancel()
                    }
                )
            }
    ) {
        content()
    }
}
