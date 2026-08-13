/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.utils.PTLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val player: ExoPlayer,
    private val bandwidthMeter: BandwidthMeter,
    @Named("HttpDataSourceFactory") private val httpDataSourceFactory: DataSource.Factory,
    private val dataSourceFactory: DataSource.Factory,
    private val trackManager: PlayerTrackManager
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _playbackError = MutableSharedFlow<PlaybackException>()
    val playbackError: SharedFlow<PlaybackException> = _playbackError.asSharedFlow()

    private val _mediaItemTransition = MutableSharedFlow<String?>(replay = 1)
    val mediaItemTransition: SharedFlow<String?> = _mediaItemTransition.asSharedFlow()

    private val _playbackEnded = MutableSharedFlow<Unit>()
    val playbackEnded: SharedFlow<Unit> = _playbackEnded.asSharedFlow()

    private val _recoveryRequired = MutableSharedFlow<Long>()
    val recoveryRequired: SharedFlow<Long> = _recoveryRequired.asSharedFlow()

    private val _onSponsorSkipped = MutableSharedFlow<com.arslandaim.playtube.domain.model.SponsorSegment>()
    val onSponsorSkipped: SharedFlow<com.arslandaim.playtube.domain.model.SponsorSegment> = _onSponsorSkipped.asSharedFlow()

    private val _playbackStats = MutableStateFlow(PlaybackStats())
    val playbackStats: StateFlow<PlaybackStats> = _playbackStats.asStateFlow()

    private var _lastPauseTimestamp = 0L
    val lastPauseTimestamp: Long get() = _lastPauseTimestamp

    private var currentExtractedAt = 0L
    private var sponsorSegments: List<com.arslandaim.playtube.domain.model.SponsorSegment> = emptyList()

    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null

    fun getBandwidthEstimate(): Long {
        return bandwidthMeter.bitrateEstimate
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
            val rawDuration = player.duration
            _duration.value = if (rawDuration == C.TIME_UNSET) 0L else rawDuration.coerceAtLeast(0L)
            
            if (playbackState == Player.STATE_READY) {
                startProgressUpdate()
            } else {
                stopProgressUpdate()
            }

            if (playbackState == Player.STATE_ENDED) {
                managerScope.launch { _playbackEnded.emit(Unit) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val cause = error.cause
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS &&
                cause is HttpDataSource.InvalidResponseCodeException &&
                cause.responseCode == 403) {
                PTLog.w("PlaybackManager", "403 Forbidden detected. Triggering immediate recovery.")
                val pos = player.currentPosition
                managerScope.launch { _recoveryRequired.emit(pos) }
            } else {
                managerScope.launch { _playbackError.emit(error) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (!isPlaying) {
                _lastPauseTimestamp = System.currentTimeMillis()
            } else {
                _lastPauseTimestamp = 0L
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPosition.value = newPosition.positionMs
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            managerScope.launch { 
                _mediaItemTransition.emit(mediaItem?.mediaId) 
            }
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            _playbackStats.update { it.copy(width = videoSize.width, height = videoSize.height) }
        }

        override fun onMetadata(metadata: Metadata) {
            // Handle metadata if needed
        }
    }

    init {
        player.addListener(playerListener)
        player.repeatMode = Player.REPEAT_MODE_OFF
        
        // Emit current media item if already exists (e.g. Service restoration)
        player.currentMediaItem?.mediaId?.let { id ->
            managerScope.launch { _mediaItemTransition.emit(id) }
        }
    }

    fun play(videoId: String, bundle: StreamBundle, stream: StreamItem, startPosition: Long = 0) {
        currentExtractedAt = bundle.extractedAt
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle.title)
            .setArtist(bundle.uploaderName)
            .setArtworkUri(bundle.thumbnailUrl?.let { Uri.parse(it) })
            .build()

        val subtitleConfigs = trackManager.createSubtitleConfigs(bundle)

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)
            
        val isManifest = stream.format == "m3u8" || stream.format == "mpd"

        if (bundle.isLive || isManifest) {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            mediaItemBuilder.setMimeType(mimeType)
            mediaItemBuilder.setLiveConfiguration(
                MediaItem.LiveConfiguration.Builder()
                    .setTargetOffsetMs(10000)
                    .setMaxPlaybackSpeed(1.1f)
                    .setMinPlaybackSpeed(0.9f)
                    .build()
            )
        }

        val mediaItem = mediaItemBuilder.build()
        val effectiveDataSourceFactory = if (isManifest) httpDataSourceFactory else dataSourceFactory
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(effectiveDataSourceFactory)
        
        player.stop()
        player.clearMediaItems()
        
        if (stream.isAdaptive) {
            val audioUrl = bundle.bestAudioStreamUrl
            if (audioUrl != null) {
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                player.setMediaSource(MergingMediaSource(videoSource, audioSource))
            } else {
                // Fallback: If adaptive is requested but no audio stream is found, 
                // try to find a non-adaptive stream (standard MPEG-4) to avoid silent playback.
                val fallbackStream = bundle.videoStreams.find { !it.isAdaptive }
                if (fallbackStream != null) {
                    PTLog.w("PlaybackManager", "Adaptive stream requested but audio URL missing. Falling back to standard stream: ${fallbackStream.quality}")
                    val fallbackMediaItem = mediaItem.buildUpon().setUri(fallbackStream.url).build()
                    player.setMediaSource(mediaSourceFactory.createMediaSource(fallbackMediaItem))
                } else {
                    player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
                }
            }
        } else {
            player.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        }
        
        player.prepare()
        if (startPosition > 0 && !bundle.isLive) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
    }

    /**
     * Switches the video quality without stopping the player or resetting position.
     */
    fun switchQualitySeamlessly(videoId: String, bundle: StreamBundle, stream: StreamItem) {
        val currentPosition = player.currentPosition
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle.title)
            .setArtist(bundle.uploaderName)
            .setArtworkUri(bundle.thumbnailUrl?.let { Uri.parse(it) })
            .build()

        val subtitleConfigs = trackManager.createSubtitleConfigs(bundle)
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)

        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        if (bundle.isLive || isManifest) {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            mediaItemBuilder.setMimeType(mimeType)
        }

        val mediaItem = mediaItemBuilder.build()
        val effectiveDataSourceFactory = if (isManifest) httpDataSourceFactory else dataSourceFactory
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(effectiveDataSourceFactory)

        val newSource = if (stream.isAdaptive) {
            val audioUrl = bundle.bestAudioStreamUrl
            if (audioUrl != null) {
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                MergingMediaSource(videoSource, audioSource)
            } else {
                mediaSourceFactory.createMediaSource(mediaItem)
            }
        } else {
            mediaSourceFactory.createMediaSource(mediaItem)
        }

        player.setMediaSource(newSource, false) // false = don't reset position
        player.prepare()
        // No need to call play() if it was already playing
    }

    fun playLocal(videoId: String, file: java.io.File, title: String, uploader: String, thumbnail: String?) {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(uploader)
            .setArtworkUri(thumbnail?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .build()

        player.stop()
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentExtractedAt = 0L
        stopProgressUpdate()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        val now = System.currentTimeMillis()
        val isPausedTooLong = _lastPauseTimestamp > 0 && now - _lastPauseTimestamp > 3 * 60 * 1000
        val isLinkExpired = currentExtractedAt > 0 && now - currentExtractedAt > 5.5 * 60 * 60 * 1000
        
        if (isPausedTooLong || isLinkExpired) {
            val pos = player.currentPosition
            PTLog.d("PlaybackManager", "Resume check: isPausedTooLong=$isPausedTooLong, isLinkExpired=$isLinkExpired. Requesting recovery.")
            managerScope.launch { _recoveryRequired.emit(pos) }
            _lastPauseTimestamp = 0L // Reset to prevent multiple emissions
        }
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(position: Long) {
        player.seekTo(position)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackParameters(PlaybackParameters(speed, player.playbackParameters.pitch))
    }

    fun setPitch(pitch: Float) {
        player.setPlaybackParameters(PlaybackParameters(player.playbackParameters.speed, pitch))
    }

    fun updateCcState(enabled: Boolean, preferredLang: String?) {
        trackManager.updateCcState(player, enabled, preferredLang)
    }

    fun setSponsorSegments(segments: List<com.arslandaim.playtube.domain.model.SponsorSegment>) {
        sponsorSegments = segments
    }

    @OptIn(UnstableApi::class)
    fun prepareNextSource(video: com.arslandaim.playtube.domain.model.VideoItem, bundle: StreamBundle) {
        if (player.mediaItemCount > 1) return
        
        val stream = bundle.videoStreams.find { it.quality.contains("360") }
            ?: bundle.videoStreams.find { it.quality.contains("480") }
            ?: bundle.videoStreams.firstOrNull() ?: return
            
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle.title)
            .setArtist(bundle.uploaderName)
            .setArtworkUri(bundle.thumbnailUrl?.let { Uri.parse(it) })
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(video.id)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(trackManager.createSubtitleConfigs(bundle))

        if (stream.format == "m3u8" || stream.format == "mpd") {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            mediaItemBuilder.setMimeType(mimeType)
        }

        val mediaItem = mediaItemBuilder.build()
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
        val finalSource = if (stream.isAdaptive) {
            val audioUrl = bundle.bestAudioStreamUrl
            if (audioUrl != null) {
                val videoSource = mediaSourceFactory.createMediaSource(mediaItem)
                val audioSource = mediaSourceFactory.createMediaSource(MediaItem.fromUri(audioUrl))
                MergingMediaSource(videoSource, audioSource)
            } else {
                mediaSourceFactory.createMediaSource(mediaItem)
            }
        } else {
            mediaSourceFactory.createMediaSource(mediaItem)
        }

        player.addMediaSource(1, finalSource)
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = managerScope.launch {
            while (true) {
                val currentPos = player.currentPosition
                _currentPosition.value = currentPos
                _duration.value = if (player.duration == C.TIME_UNSET) 0L else player.duration
                _bufferedPosition.value = player.bufferedPosition

                // Update Stats
                val videoFormat = player.videoFormat
                _playbackStats.update { stats ->
                    stats.copy(
                        videoFormat = videoFormat?.sampleMimeType,
                        bitrate = videoFormat?.bitrate ?: 0,
                        resolution = videoFormat?.let { "${it.width}x${it.height}" } ?: "",
                        droppedFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0,
                        bandwidthEstimate = bandwidthMeter.bitrateEstimate
                    )
                }

                // SponsorBlock Skipping Logic
                if (sponsorSegments.isNotEmpty()) {
                    val matchingSegment = sponsorSegments.find { 
                        currentPos >= it.startMs && currentPos < it.endMs 
                    }
                    if (matchingSegment != null) {
                        PTLog.d("PlaybackManager", "Skipping sponsor segment: ${matchingSegment.category}")
                        player.seekTo(matchingSegment.endMs)
                        _onSponsorSkipped.emit(matchingSegment)
                    }
                }

                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    fun cleanUp() {
        player.removeListener(playerListener)
        managerScope.cancel()
        // Re-initialize scope so the singleton can be used again if the process lives on
        managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

data class PlaybackStats(
    val width: Int = 0,
    val height: Int = 0,
    val videoFormat: String? = null,
    val bitrate: Int = 0,
    val resolution: String = "",
    val droppedFrames: Int = 0,
    val bandwidthEstimate: Long = 0
)
