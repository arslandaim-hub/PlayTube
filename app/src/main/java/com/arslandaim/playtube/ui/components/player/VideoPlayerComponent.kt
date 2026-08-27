/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components.player

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Composable
fun VideoPlayerView(
    player: Player,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> playerView?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> playerView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                this.player = player
                this.keepScreenOn = true
                this.resizeMode = resizeMode
                useController = false
                setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                subtitleView?.visibility = android.view.View.GONE
                playerView = this
            }
        },
        update = { view ->
            if (view.player != player) {
                view.player = player
            }
            if (view.resizeMode != resizeMode) {
                view.resizeMode = resizeMode
            }
            view.subtitleView?.visibility = android.view.View.GONE
        },
        onRelease = { view ->
            view.player = null
            playerView = null
        },
        modifier = modifier
    )
}
