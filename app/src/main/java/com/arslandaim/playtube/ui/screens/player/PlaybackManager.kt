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
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.StreamItem
import com.arslandaim.playtube.domain.model.SubtitleItem
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
    private val libraryRepository: com.arslandaim.playtube.domain.repository.LibraryRepository,
    private val bandwidthMeter: BandwidthMeter,
    private val okHttpClient: okhttp3.OkHttpClient,
    @Named("HttpDataSourceFactory") private val httpDataSourceFactory: DataSource.Factory,
    private val dataSourceFactory: DataSource.Factory,
    private val trackManager: PlayerTrackManager,
    private val dashManifestGenerator: com.arslandaim.playtube.data.network.AdaptiveDashManifestGenerator
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

    private val _abLoopState = MutableStateFlow(ABLoopState())
    val abLoopState: StateFlow<ABLoopState> = _abLoopState.asStateFlow()

    private val audioEnhancerManager = com.arslandaim.playtube.utils.AudioEnhancerManager()
    private val _volumeBoostLevel = MutableStateFlow(com.arslandaim.playtube.utils.VolumeBoostLevel.OFF)
    val volumeBoostLevel: StateFlow<com.arslandaim.playtube.utils.VolumeBoostLevel> = _volumeBoostLevel.asStateFlow()

    private val _isAudioOnlyMode = MutableStateFlow(false)
    val isAudioOnlyMode: StateFlow<Boolean> = _isAudioOnlyMode.asStateFlow()

    private var _lastPauseTimestamp = 0L
    val lastPauseTimestamp: Long get() = _lastPauseTimestamp

    private var currentExtractedAt = 0L
    private var currentBundle: StreamBundle? = null
    private var currentStream: StreamItem? = null
    private var isAutoQualityEnabled = true
    private var lastQualitySwitchTime = 0L
    private var currentCcEnabled = false
    private var currentPreferredLang: String? = null
    private var currentSelectedSubtitle: SubtitleItem? = null

    private val _currentCues = MutableStateFlow<List<Cue>>(emptyList())
    val currentCues: StateFlow<List<Cue>> = _currentCues.asStateFlow()
    private var sponsorSegments: List<com.arslandaim.playtube.domain.model.SponsorSegment> = emptyList()
    private val _sponsorSegmentsFlow = MutableStateFlow<List<com.arslandaim.playtube.domain.model.SponsorSegment>>(emptyList())
    val sponsorSegmentsFlow: StateFlow<List<com.arslandaim.playtube.domain.model.SponsorSegment>> = _sponsorSegmentsFlow.asStateFlow()

    private var managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var heartbeatJob: Job? = null

    private val BITRATE_1080P_THRESHOLD = 5_000_000L // 5 Mbps
    private val BITRATE_720P_THRESHOLD = 2_500_000L // 2.5 Mbps

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
            val invalidResp = cause as? HttpDataSource.InvalidResponseCodeException
            val failedUri = invalidResp?.dataSpec?.uri?.toString() ?: ""
            val causeMsg = cause?.message ?: ""
            val causeClass = cause?.javaClass?.name ?: ""

            // Check if this error belongs to an auxiliary subtitle request or text renderer decoding
            val isSubtitleUri = failedUri.contains("/api/timedtext") ||
                                failedUri.endsWith(".vtt") ||
                                failedUri.endsWith(".srt") ||
                                failedUri.endsWith(".ttml") ||
                                failedUri.contains("sub_") ||
                                failedUri.contains("subtitles")

            val isExplicitSubtitleError = causeClass.contains("SubtitleDecoderException") ||
                                          causeMsg.contains("Subtitle", ignoreCase = true) ||
                                          causeMsg.contains("WEBVTT", ignoreCase = true)

            val isSubtitleError = isSubtitleUri || isExplicitSubtitleError ||
                    (currentCcEnabled && (causeMsg.contains("application/x-media3-cues") || causeClass.contains("Subtitle")))

            if (isSubtitleError) {
                PTLog.w("PlaybackManager", "Subtitle load/decode failed ($failedUri, cause: $causeMsg). Disabling text track gracefully and recovering video playback.")
                updateCcState(enabled = false, preferredLang = currentPreferredLang, selectedSubtitle = null)
                val pos = player.currentPosition
                val isPlaying = player.isPlaying
                val bundle = currentBundle
                val stream = currentStream
                val videoId = player.currentMediaItem?.mediaId
                if (bundle != null && stream != null && videoId != null) {
                    // Completely strip all subtitle tracks to guarantee safe recovery
                    val cleanBundle = bundle.copy(subtitles = emptyList())
                    currentBundle = cleanBundle
                    
                    val isManifest = stream.format == "m3u8" || stream.format == "mpd"
                    val effectiveDataSourceFactory = if (isManifest) {
                        androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
                    } else {
                        dataSourceFactory
                    }
                    val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(effectiveDataSourceFactory)
                        .setLoadOnlySelectedTracks(true)
                    val compositeSource = buildCompositeMediaSource(videoId, cleanBundle, stream, mediaSourceFactory)
                    
                    player.setMediaSource(compositeSource, false)
                    player.prepare()
                    if (pos > 0) player.seekTo(pos)
                    if (isPlaying) player.play()
                } else {
                    player.prepare()
                }
                return
            }

            val isNetworkError = error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE ||
                                error.errorCode == PlaybackException.ERROR_CODE_IO_UNSPECIFIED

            if (isNetworkError && (invalidResp != null && invalidResp.responseCode == 403)) {
                PTLog.w("PlaybackManager", "403 Forbidden on primary stream detected. Triggering immediate recovery.")
                val pos = player.currentPosition
                managerScope.launch { _recoveryRequired.emit(pos) }
            } else if (isNetworkError) {
                PTLog.w("PlaybackManager", "Transient network error on primary stream (${error.errorCode}). Attempting hot-swap recovery.")
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
                startHeartbeat()
            } else {
                _lastPauseTimestamp = 0L
                stopHeartbeat()
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

        override fun onRenderedFirstFrame() {
            val videoId = player.currentMediaItem?.mediaId
            if (videoId != null) {
                com.arslandaim.playtube.utils.InstantPlaybackTracker.onFirstFrameRendered(videoId)
            }
        }

        override fun onCues(cueGroup: CueGroup) {
            _currentCues.value = cueGroup.cues
        }

        override fun onTracksChanged(tracks: Tracks) {
            trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
            PTLog.d("PlaybackManager", "onTracksChanged: updated CC state (enabled=$currentCcEnabled, lang=$currentPreferredLang)")
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            audioEnhancerManager.attachToAudioSession(audioSessionId)
        }

        override fun onMetadata(metadata: Metadata) {
            // Handle metadata if needed
        }
    }

    init {
        ensureListenerRegistered()
        player.repeatMode = Player.REPEAT_MODE_OFF
        if (player.audioSessionId > 0) {
            audioEnhancerManager.attachToAudioSession(player.audioSessionId)
        }
        
        // Emit current media item if already exists (e.g. Service restoration)
        player.currentMediaItem?.mediaId?.let { id ->
            managerScope.launch { _mediaItemTransition.emit(id) }
        }
    }

    fun ensureListenerRegistered() {
        // Safe-guard to prevent duplicate listeners if called multiple times
        player.removeListener(playerListener)
        player.addListener(playerListener)
    }


    private fun createMediaSourceFactory(isManifest: Boolean): androidx.media3.exoplayer.source.DefaultMediaSourceFactory {
        val effectiveDataSourceFactory = if (isManifest) {
            androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)
        } else {
            dataSourceFactory
        }
        return androidx.media3.exoplayer.source.DefaultMediaSourceFactory(effectiveDataSourceFactory)
            .setLoadOnlySelectedTracks(true)
    }

    private fun mergeCachedSubtitles(incomingBundle: StreamBundle): StreamBundle {
        val existingSubs = currentBundle?.subtitles ?: return incomingBundle
        val localSubsById = existingSubs.filter {
            it.url.startsWith("file:") || it.url.startsWith("content:")
        }
        if (localSubsById.isEmpty()) return incomingBundle

        val merged = incomingBundle.subtitles.map { sub ->
            localSubsById.find {
                (it.id.isNotBlank() && it.id == sub.id) ||
                (it.languageTag.equals(sub.languageTag, ignoreCase = true) && it.isAutoGenerated == sub.isAutoGenerated)
            } ?: sub
        }
        return incomingBundle.copy(subtitles = merged)
    }

    private fun buildCompositeMediaSource(
        videoId: String,
        bundle: StreamBundle,
        stream: StreamItem,
        mediaSourceFactory: androidx.media3.exoplayer.source.DefaultMediaSourceFactory
    ): androidx.media3.exoplayer.source.MediaSource {
        val metadata = MediaMetadata.Builder()
            .setTitle(bundle.title)
            .setArtist(bundle.uploaderName)
            .setArtworkUri(bundle.thumbnailUrl?.let { Uri.parse(it) })
            .build()

        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        val subtitleConfigs = trackManager.createSubtitleConfigs(
            bundle = bundle,
            selectedSubtitle = currentSelectedSubtitle,
            preferredLang = currentPreferredLang
        )

        if (_isAudioOnlyMode.value && !bundle.isLive && !bundle.bestAudioStreamUrl.isNullOrBlank()) {
            val audioOnlyItem = MediaItem.Builder()
                .setUri(bundle.bestAudioStreamUrl)
                .setMediaId(videoId)
                .setMediaMetadata(metadata)
                .setSubtitleConfigurations(subtitleConfigs)
                .build()
            return mediaSourceFactory.createMediaSource(audioOnlyItem)
        }

        val videoMediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)

        if (bundle.isLive || isManifest) {
            val mimeType = if (stream.format == "mpd") MimeTypes.APPLICATION_MPD else MimeTypes.APPLICATION_M3U8
            videoMediaItemBuilder.setMimeType(mimeType)
            if (bundle.isLive) {
                videoMediaItemBuilder.setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setTargetOffsetMs(10000)
                        .setMaxPlaybackSpeed(1.1f)
                        .setMinPlaybackSpeed(0.9f)
                        .build()
                )
            }
        }

        val videoSource = mediaSourceFactory.createMediaSource(videoMediaItemBuilder.build())

        val audioSource = if (stream.isAdaptive) {
            bundle.bestAudioStreamUrl?.let { audioUrl ->
                val audioMediaItem = MediaItem.fromUri(audioUrl)
                mediaSourceFactory.createMediaSource(audioMediaItem)
            }
        } else null

        val allSources = buildList {
            add(videoSource)
            if (audioSource != null) add(audioSource)
        }

        return if (allSources.size == 1) {
            allSources.first()
        } else {
            MergingMediaSource(/* adjustPeriodTimeOffsets= */ true, /* clipDurations= */ true, *allSources.toTypedArray())
        }
    }

    fun play(videoId: String, bundle: StreamBundle, stream: StreamItem, startPosition: Long = 0) {
        currentExtractedAt = bundle.extractedAt
        currentBundle = bundle
        currentStream = stream
        lastQualitySwitchTime = System.currentTimeMillis()
        
        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        val mediaSourceFactory = createMediaSourceFactory(isManifest)
        
        stopHeartbeat()
        player.stop()
        player.clearMediaItems()
        
        val compositeSource = buildCompositeMediaSource(videoId, bundle, stream, mediaSourceFactory)
        player.setMediaSource(compositeSource)
        
        player.prepare()
        if (startPosition > 0 && !bundle.isLive) {
            player.seekTo(startPosition)
        }
        player.playWhenReady = true
        trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
    }

    /**
     * Switches the video quality without stopping the player or resetting position.
     */
    fun switchQualitySeamlessly(videoId: String, bundle: StreamBundle, stream: StreamItem) {
        val mergedBundle = mergeCachedSubtitles(bundle)
        currentBundle = mergedBundle
        currentStream = stream
        lastQualitySwitchTime = System.currentTimeMillis()
        val currentPosition = player.currentPosition
        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        val mediaSourceFactory = createMediaSourceFactory(isManifest)

        val compositeSource = buildCompositeMediaSource(videoId, mergedBundle, stream, mediaSourceFactory)

        player.setMediaSource(compositeSource, false) // false = don't reset position
        player.prepare()
        if (currentPosition > 0) {
            player.seekTo(currentPosition)
        }
        trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
    }

    fun playLocal(videoId: String, file: java.io.File, title: String, uploader: String, thumbnail: String?, bundle: StreamBundle? = null) {
        currentBundle = bundle
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(uploader)
            .setArtworkUri(thumbnail?.let { Uri.parse(it) })
            .build()

        val subtitleConfigs = if (bundle != null) trackManager.createSubtitleConfigs(bundle) else emptyList()

        val videoMediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId(videoId)
            .setMediaMetadata(metadata)
            .setSubtitleConfigurations(subtitleConfigs)
            .build()

        val mediaSourceFactory = createMediaSourceFactory(isManifest = false)
        val finalMediaSource = mediaSourceFactory.createMediaSource(videoMediaItem)

        player.stop()
        player.clearMediaItems()
        player.setMediaSource(finalMediaSource)
        player.prepare()
        player.playWhenReady = true

        if (bundle != null && bundle.subtitles.isNotEmpty()) {
            val activeSub = bundle.subtitles.first()
            updateCcState(true, activeSub.languageTag, activeSub)
        }
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        currentExtractedAt = 0L
        stopProgressUpdate()
        stopHeartbeat()
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        val now = System.currentTimeMillis()
        val pauseDuration = if (_lastPauseTimestamp > 0) now - _lastPauseTimestamp else 0L
        
        val isPausedTooLong = pauseDuration > 60 * 60 * 1000 // 60 minutes
        val isLinkExpired = currentExtractedAt > 0 && now - currentExtractedAt > 5.5 * 60 * 60 * 1000
        
        if (isPausedTooLong || isLinkExpired) {
            val pos = player.currentPosition
            PTLog.d("PlaybackManager", "Resume check: isPausedTooLong=$isPausedTooLong, isLinkExpired=$isLinkExpired. Requesting recovery.")
            managerScope.launch { _recoveryRequired.emit(pos) }
            _lastPauseTimestamp = 0L // Reset to prevent multiple emissions
        } else {
            // Phase 1: Contextual Rewind
            if (pauseDuration > 60_000L) {
                val rewindPos = (player.currentPosition - 3000L).coerceAtLeast(0L)
                PTLog.d("PlaybackManager", "Applying contextual rewind: -3s to $rewindPos")
                player.seekTo(rewindPos)
            }
            player.play()
        }
    }

    /**
     * Seamlessly swaps the current media source with a fresh one (e.g. after 403 recovery).
     * Retains the current position and state.
     */
    fun hotSwapSource(videoId: String, bundle: StreamBundle, stream: StreamItem, position: Long) {
        val mergedBundle = mergeCachedSubtitles(bundle)
        currentExtractedAt = mergedBundle.extractedAt
        currentBundle = mergedBundle
        currentStream = stream
        lastQualitySwitchTime = System.currentTimeMillis()

        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        val mediaSourceFactory = createMediaSourceFactory(isManifest)

        val compositeSource = buildCompositeMediaSource(videoId, mergedBundle, stream, mediaSourceFactory)

        PTLog.d("PlaybackManager", "Performing hot-swap for $videoId at position $position")
        player.setMediaSource(compositeSource, false) // false = don't reset position
        player.prepare()
        if (position > 0) {
            player.seekTo(position)
        }
        player.play()
        trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
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

    fun switchAudioTrack(videoId: String, bundle: StreamBundle, audioStream: StreamItem) {
        val position = player.currentPosition
        val isPlaying = player.isPlaying
        
        val videoStream = currentStream ?: bundle.videoStreams.firstOrNull() ?: return
        currentStream = videoStream

        val customBundle = mergeCachedSubtitles(bundle).copy(bestAudioStreamUrl = audioStream.url)
        currentBundle = customBundle
        val isManifest = videoStream.format == "m3u8" || videoStream.format == "mpd"
        val mediaSourceFactory = createMediaSourceFactory(isManifest)
        val compositeSource = buildCompositeMediaSource(videoId, customBundle, videoStream, mediaSourceFactory)

        PTLog.d("PlaybackManager", "Switching audio track for $videoId to ${audioStream.quality}")
        player.setMediaSource(compositeSource, false)
        player.prepare()
        if (position > 0) {
            player.seekTo(position)
        }
        if (isPlaying) {
            player.play()
        }
        trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
    }

    fun attachLocalSubtitleTrack(subtitleItem: SubtitleItem, enableCc: Boolean = true) {
        val bundle = currentBundle ?: return
        val stream = currentStream ?: return
        val videoId = player.currentMediaItem?.mediaId ?: return

        val existingSubtitles = bundle.subtitles.toMutableList()
        val index = if (subtitleItem.id.isNotBlank()) {
            existingSubtitles.indexOfFirst { it.id == subtitleItem.id }.takeIf { it != -1 }
                ?: existingSubtitles.indexOfFirst { it.languageTag.equals(subtitleItem.languageTag, ignoreCase = true) }
        } else {
            existingSubtitles.indexOfFirst { it.languageTag.equals(subtitleItem.languageTag, ignoreCase = true) }
        }

        val hasMatchingPlayerTrack = player.currentTracks.groups.any { group ->
            group.type == C.TRACK_TYPE_TEXT && group.isSupported && (0 until group.length).any { i ->
                val format = group.getTrackFormat(i)
                val formatId = format.id ?: ""
                (subtitleItem.id.isNotBlank() && (formatId.equals(subtitleItem.id, ignoreCase = true) || formatId.endsWith(":${subtitleItem.id}", ignoreCase = true))) ||
                    format.language.equals(subtitleItem.languageTag, ignoreCase = true)
            }
        }
        val alreadyAttached = index != -1 && existingSubtitles[index].url == subtitleItem.url && hasMatchingPlayerTrack
        if (index != -1) {
            existingSubtitles[index] = subtitleItem
        } else {
            existingSubtitles.add(subtitleItem)
        }

        val updatedBundle = bundle.copy(subtitles = existingSubtitles)
        currentBundle = updatedBundle
        currentSelectedSubtitle = subtitleItem
        currentPreferredLang = subtitleItem.languageTag
        currentCcEnabled = enableCc

        if (!alreadyAttached) {
            val currentPosition = player.currentPosition
            val isPlaying = player.isPlaying
            val isManifest = stream.format == "m3u8" || stream.format == "mpd"
            val mediaSourceFactory = createMediaSourceFactory(isManifest)

            val compositeSource = buildCompositeMediaSource(videoId, updatedBundle, stream, mediaSourceFactory)

            PTLog.d("PlaybackManager", "Seamlessly attaching subtitle track (${subtitleItem.languageTag}) for $videoId")
            player.setMediaSource(compositeSource, false)
            player.prepare()
            if (currentPosition > 0) {
                player.seekTo(currentPosition)
            }
            if (isPlaying) {
                player.play()
            }
        }
        trackManager.updateCcState(player, enableCc, subtitleItem, subtitleItem.languageTag)
    }

    fun updateCcState(enabled: Boolean, preferredLang: String?, selectedSubtitle: SubtitleItem? = null) {
        currentCcEnabled = enabled
        currentPreferredLang = preferredLang
        currentSelectedSubtitle = selectedSubtitle
        trackManager.updateCcState(player, enabled, selectedSubtitle, preferredLang)
    }

    fun updateCcState(enabled: Boolean, preferredLang: String?) {
        updateCcState(enabled = enabled, preferredLang = preferredLang, selectedSubtitle = null)
    }

    fun setABLoopPointA(pointA: Long?) {
        _abLoopState.update { it.copy(pointA = pointA) }
    }

    fun setABLoopPointB(pointB: Long?) {
        _abLoopState.update { it.copy(pointB = pointB) }
    }

    fun clearABLoop() {
        _abLoopState.value = ABLoopState()
    }

    fun setSponsorSegments(segments: List<com.arslandaim.playtube.domain.model.SponsorSegment>) {
        sponsorSegments = segments
        _sponsorSegmentsFlow.value = segments
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

    fun prepareNextLocalSource(video: com.arslandaim.playtube.domain.model.VideoItem, file: java.io.File) {
        if (player.mediaItemCount > 1) return

        val metadata = MediaMetadata.Builder()
            .setTitle(video.title)
            .setArtist(video.uploaderName)
            .setArtworkUri(video.thumbnailUrl.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId(video.id)
            .setMediaMetadata(metadata)
            .build()

        player.addMediaItem(1, mediaItem)
    }

    fun setAutoQualityEnabled(enabled: Boolean) {
        isAutoQualityEnabled = enabled
    }

    fun setVolumeBoostLevel(level: com.arslandaim.playtube.utils.VolumeBoostLevel) {
        _volumeBoostLevel.value = level
        if (player.audioSessionId > 0) {
            audioEnhancerManager.attachToAudioSession(player.audioSessionId)
        }
        audioEnhancerManager.setBoostLevel(level)
    }

    fun setAudioOnlyMode(enabled: Boolean) {
        if (_isAudioOnlyMode.value == enabled) return
        _isAudioOnlyMode.value = enabled
        val bundle = currentBundle ?: return
        val stream = currentStream ?: return
        val videoId = player.currentMediaItem?.mediaId ?: return
        if (bundle.isLive || bundle.videoStreams.isEmpty()) return

        val pos = player.currentPosition
        val wasPlaying = player.isPlaying
        val isManifest = stream.format == "m3u8" || stream.format == "mpd"
        val mediaSourceFactory = createMediaSourceFactory(isManifest)
        val compositeSource = buildCompositeMediaSource(videoId, bundle, stream, mediaSourceFactory)

        player.setMediaSource(compositeSource, false)
        player.prepare()
        if (pos > 0) player.seekTo(pos)
        if (wasPlaying) player.play()
        trackManager.updateCcState(player, currentCcEnabled, currentSelectedSubtitle, currentPreferredLang)
    }

    private fun dropQuality() {
        val bundle = currentBundle ?: return
        val currentVideoId = player.currentMediaItem?.mediaId ?: return
        
        // Find next lower quality
        val qualities = listOf("1080", "720", "480", "360")
        val currentQual = currentStream?.quality ?: ""
        val currentIndex = qualities.indexOfFirst { currentQual.contains(it) }
        
        if (currentIndex != -1 && currentIndex < qualities.size - 1) {
            val nextQual = qualities[currentIndex + 1]
            val nextStream = bundle.videoStreams.find { it.quality.contains(nextQual) }
            if (nextStream != null) {
                lastQualitySwitchTime = System.currentTimeMillis()
                switchQualitySeamlessly(currentVideoId, bundle, nextStream)
            }
        }
    }

    private fun upgradeQuality() {
        val bundle = currentBundle ?: return
        val currentVideoId = player.currentMediaItem?.mediaId ?: return

        val qualities = listOf("1080", "720", "480", "360")
        val currentQual = currentStream?.quality ?: ""
        val currentIndex = qualities.indexOfFirst { currentQual.contains(it) }

        if (currentIndex > 0) {
            val higherQual = qualities[currentIndex - 1]
            val higherStream = bundle.videoStreams.find { it.quality.contains(higherQual) }
            if (higherStream != null) {
                lastQualitySwitchTime = System.currentTimeMillis()
                switchQualitySeamlessly(currentVideoId, bundle, higherStream)
            }
        }
    }

    private fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = managerScope.launch {
            while (true) {
                val currentPos = player.currentPosition
                val dur = if (player.duration == C.TIME_UNSET) 0L else player.duration
                val bufPos = player.bufferedPosition
                
                _currentPosition.value = currentPos
                _duration.value = dur
                _bufferedPosition.value = bufPos

                // Dynamic Bitrate Downgrade on severe buffer depletion (after initial startup window)
                val now = System.currentTimeMillis()
                if (isAutoQualityEnabled && !_isAudioOnlyMode.value && !player.isCurrentMediaItemLive &&
                    currentPos > 8_000L && now - lastQualitySwitchTime >= 10_000L
                ) {
                    val estimate = bandwidthMeter.bitrateEstimate
                    val bufferedAheadMs = (bufPos - currentPos).coerceAtLeast(0L)
                    val currentQual = currentStream?.quality ?: ""

                    if (bufferedAheadMs < 2_000L) {
                        if (currentQual.contains("1080") && estimate in 1 until BITRATE_1080P_THRESHOLD) {
                            PTLog.w("PlaybackManager", "Low buffer (${bufferedAheadMs}ms) & bandwidth $estimate < 1080p threshold. Downgrading.")
                            dropQuality()
                        } else if (currentQual.contains("720") && estimate in 1 until BITRATE_720P_THRESHOLD) {
                            PTLog.w("PlaybackManager", "Low buffer (${bufferedAheadMs}ms) & bandwidth $estimate < 720p threshold. Downgrading.")
                            dropQuality()
                        }
                    }
                }

                // Update Stats
                val videoFormat = player.videoFormat
                val bandwidth = bandwidthMeter.bitrateEstimate
                _playbackStats.update { stats ->
                    stats.copy(
                        videoFormat = videoFormat?.sampleMimeType,
                        bitrate = videoFormat?.bitrate ?: 0,
                        resolution = videoFormat?.let { "${it.width}x${it.height}" } ?: "",
                        droppedFrames = player.videoDecoderCounters?.droppedBufferCount ?: 0,
                        bandwidthEstimate = bandwidth
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

                // AB Loop Enforcement
                val abState = _abLoopState.value
                if (abState.isActive) {
                    val start = abState.pointA!!
                    val end = abState.pointB!!
                    if (currentPos >= end || currentPos < start) {
                        player.seekTo(start)
                    }
                }

                delay(500)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        val url = currentStream?.url ?: return
        if (url.isBlank() || !url.contains("googlevideo.com")) return

        heartbeatJob = managerScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(90_000L) // Ping every 90 seconds
                try {
                    val request = okhttp3.Request.Builder()
                        .url(url)
                        .header("Range", "bytes=0-1")
                        .build()
                    
                    okHttpClient.newCall(request).execute().use { response ->
                        PTLog.d("PlaybackManager", "Heartbeat sent to stream server. Status: ${response.code}")
                    }
                } catch (e: Exception) {
                    PTLog.w("PlaybackManager", "Heartbeat failed: ${e.message}")
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun cleanUp() {
        stopProgressUpdate()
        stopHeartbeat()
        // We don't remove the listener here because this is a Singleton.
        // If we remove it, subsequent uses of this instance (e.g. re-entering the app) 
        // will not receive updates. The listener is managed by process lifecycle.
        managerScope.coroutineContext.cancelChildren()
    }

    fun release() {
        cleanUp()
        // DO NOT call player.release() here. 
        // Both PlaybackManager and ExoPlayer are Singletons. 
        // Releasing the singleton player makes it unusable for the rest of the process lifetime.
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

data class ABLoopState(
    val pointA: Long? = null,
    val pointB: Long? = null
) {
    val isActive: Boolean get() = pointA != null && pointB != null && pointB > pointA
}
