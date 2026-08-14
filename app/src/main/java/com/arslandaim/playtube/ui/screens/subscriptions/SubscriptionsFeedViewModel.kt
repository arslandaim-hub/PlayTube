/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arslandaim.playtube.data.local.FavoriteEntity
import com.arslandaim.playtube.data.local.HistoryEntity
import com.arslandaim.playtube.data.local.SubscriptionEntity
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.domain.repository.LibraryRepository
import com.arslandaim.playtube.domain.repository.VideoRepository
import com.arslandaim.playtube.domain.usecase.DownloadVideoUseCase
import com.arslandaim.playtube.domain.usecase.GetVideoStreamsUseCase
import com.arslandaim.playtube.domain.usecase.ToggleFavoriteUseCase
import com.arslandaim.playtube.ui.components.DownloadDialogState
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.HistoryUtils.applyHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.schabi.newpipe.extractor.Page

@HiltViewModel
class SubscriptionsFeedViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val videoRepository: VideoRepository,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val getVideoStreamsUseCase: GetVideoStreamsUseCase,
    private val downloadVideoUseCase: DownloadVideoUseCase
) : ViewModel() {

    companion object {
        private var isThoroughSearchDone = false
    }

    // Isolated States
    private val _subscriptions = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
    private val _selectedChannelId = MutableStateFlow<String?>(null)
    
    private val _allFeedState = MutableStateFlow(FeedState())
    private val _channelFeedsCache = ConcurrentHashMap<String, MutableStateFlow<FeedState>>()
    
    private val _isThoroughSearching = MutableStateFlow(false)
    private val _thoroughSearchProgress = MutableStateFlow(0f)

    // Current displayed feed state based on selection
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _activeFeedState = _selectedChannelId.flatMapLatest { id ->
        if (id == null) _allFeedState else {
            _channelFeedsCache.getOrPut(id) { MutableStateFlow(FeedState()) }
        }
    }

    val uiState: StateFlow<SubscriptionsFeedUIState> = combine(
        _selectedChannelId,
        _subscriptions,
        _activeFeedState,
        _isThoroughSearching,
        _thoroughSearchProgress,
        libraryRepository.getHistory()
    ) { args: Array<Any?> ->
        val selectedId = args[0] as String?
        val subs = args[1] as List<SubscriptionEntity>
        val activeFeed = args[2] as FeedState
        val isSearching = args[3] as Boolean
        val progress = args[4] as Float
        val history = args[5] as List<HistoryEntity>

        SubscriptionsFeedUIState(
            selectedChannelId = selectedId,
            subscriptions = subs,
            activeFeed = activeFeed.copy(
                videos = activeFeed.videos.applyHistory(history)
            ),
            isThoroughSearching = isSearching,
            thoroughSearchProgress = progress
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SubscriptionsFeedUIState()
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _downloadState = MutableStateFlow<DownloadDialogState>(DownloadDialogState.Idle)
    val downloadState: StateFlow<DownloadDialogState> = _downloadState.asStateFlow()

    // Independent Job Management
    private var thoroughSearchJob: Job? = null
    private var channelFetchJob: Job? = null
    private var fetchedChannelsIndex = 0
    private val CHUNK_SIZE = 10

    init {
        viewModelScope.launch {
            libraryRepository.getSubscriptions().collect { subs ->
                _subscriptions.value = subs
                
                if (!isThoroughSearchDone && subs.isNotEmpty() && _selectedChannelId.value == null) {
                    runThoroughSearch(subs)
                }
            }
        }

        // Phase 1: Load cached "All" feed immediately
        viewModelScope.launch {
            libraryRepository.getCachedFeed("subs_all").collect { cache ->
                if (cache != null && _allFeedState.value.videos.isEmpty()) {
                    _allFeedState.update { it.copy(videos = cache.videos) }
                }
            }
        }
    }

    fun onChannelSelected(channelId: String?) {
        if (_selectedChannelId.value == channelId) return
        
        // Cancel only the specific channel fetch, NOT the global thorough search
        channelFetchJob?.cancel()
        _selectedChannelId.value = channelId

        if (channelId != null) {
            // Check cache or initiate fetch
            if (!_channelFeedsCache.containsKey(channelId) && _channelFeedsCache.size >= 20) {
                // Remove oldest/first entry to bound memory
                _channelFeedsCache.keys().asSequence().firstOrNull()?.let { 
                    _channelFeedsCache.remove(it) 
                }
            }
            val cachedState = _channelFeedsCache.getOrPut(channelId) { MutableStateFlow(FeedState()) }
            
            // Phase 1: Load channel cache immediately
            viewModelScope.launch {
                libraryRepository.getCachedFeed("subs_channel_$channelId").firstOrNull()?.let { cache ->
                    if (cachedState.value.videos.isEmpty()) {
                        cachedState.update { it.copy(videos = cache.videos) }
                    }
                }
            }

            if (cachedState.value.videos.isEmpty()) {
                loadSubscriptionsFeed()
            }
        } else {
            // Returning to "All" - the thorough search might still be running or finished.
            // We don't clear anything, just let uiState observe _allFeedState.
            if (_allFeedState.value.videos.isEmpty()) {
                loadSubscriptionsFeed()
            }
        }
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            
            val currentSubs = _subscriptions.value
            val selectedId = _selectedChannelId.value

            if (selectedId == null && currentSubs.isNotEmpty()) {
                runThoroughSearch(currentSubs, force = true)
            } else {
                fetchSubscriptionsFeed(isRefresh = true)
            }
            
            _isRefreshing.value = false
        }
    }

    fun loadSubscriptionsFeed() {
        val selectedId = _selectedChannelId.value
        
        if (selectedId == null) {
            // Handle All Feed
            if (_isThoroughSearching.value && thoroughSearchJob?.isActive == true) return
            
            val subs = _subscriptions.value
            if (!isThoroughSearchDone && subs.isNotEmpty()) {
                runThoroughSearch(subs)
            } else if (isThoroughSearchDone && _allFeedState.value.videos.isEmpty()) {
                // Rare case where session says done but list is empty, re-trigger
                runThoroughSearch(subs, force = true)
            }
        } else {
            // Handle Specific Channel Feed
            val currentFeedState = _channelFeedsCache.getOrPut(selectedId) { MutableStateFlow(FeedState()) }
            if (currentFeedState.value.isLoading) return

            channelFetchJob?.cancel()
            channelFetchJob = viewModelScope.launch {
                currentFeedState.update { it.copy(isLoading = true, error = null) }
                fetchSubscriptionsFeed(isRefresh = true)
                currentFeedState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun runThoroughSearch(allSubscriptions: List<SubscriptionEntity>, force: Boolean = false) {
        if (isThoroughSearchDone && !force) return
        
        thoroughSearchJob?.cancel()
        thoroughSearchJob = viewModelScope.launch {
            _isThoroughSearching.value = true
            _thoroughSearchProgress.value = 0f
            
            try {
                val limit = allSubscriptions.take(if (force) 50 else 1000) 
                
                limit.chunked(5).forEachIndexed { chunkIndex, chunk ->
                    ensureActive()

                    val chunkVideos = coroutineScope {
                        val deferred = chunk.map { sub ->
                            async(Dispatchers.IO) {
                                try {
                                    videoRepository.getChannelDetails(sub.channelId).videos.take(10)
                                } catch (e: Exception) {
                                    emptyList()
                                }
                            }
                        }
                        deferred.awaitAll().flatten()
                    }
                    
                    _allFeedState.update { state ->
                        val updated = (state.videos + chunkVideos)
                            .distinctBy { it.id }
                            .sortedByDescending { it.rawUploadDate ?: 0L }
                        
                        state.copy(videos = updated.take(1000))
                    }

                    // Phase 1: Silent Cache Update
                    libraryRepository.updateCachedFeed("subs_all", _allFeedState.value.videos)

                    val progress = ((chunkIndex + 1) * 5).toFloat() / limit.size
                    _thoroughSearchProgress.value = progress.coerceAtMost(1f)
                    fetchedChannelsIndex = (chunkIndex + 1) * 5
                    
                    delay(50) 
                }

                _isThoroughSearching.value = false
                if (!force) isThoroughSearchDone = true
                fetchedChannelsIndex = limit.size
            } catch (e: CancellationException) {
                _isThoroughSearching.value = false
                throw e
            } catch (e: Exception) {
                _isThoroughSearching.value = false
                _snackbarMessage.emit("Failed to sync subscriptions: ${PlayTubeError.fromThrowable(e).getMessage()}")
                _allFeedState.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
            }
        }
    }

    fun loadMore() {
        val selectedId = _selectedChannelId.value
        val totalSubs = _subscriptions.value.size
        
        if (selectedId == null) {
            if (fetchedChannelsIndex >= totalSubs || _isThoroughSearching.value) return
        } else {
            val feedState = _channelFeedsCache[selectedId]?.value ?: return
            if (feedState.isLoadingMore || feedState.nextPage == null) return
        }

        channelFetchJob?.cancel()
        channelFetchJob = viewModelScope.launch {
            val stateFlow = if (selectedId == null) _allFeedState else _channelFeedsCache[selectedId]!!
            stateFlow.update { it.copy(isLoadingMore = true) }
            fetchSubscriptionsFeed(isRefresh = false)
            stateFlow.update { it.copy(isLoadingMore = false) }
        }
    }

    private suspend fun fetchSubscriptionsFeed(isRefresh: Boolean) {
        val selectedId = _selectedChannelId.value
        val stateFlow = if (selectedId == null) _allFeedState else _channelFeedsCache.getOrPut(selectedId) { MutableStateFlow(FeedState()) }
        
        try {
            val allSubscriptions = _subscriptions.value
            if (allSubscriptions.isEmpty()) {
                stateFlow.update { it.copy(videos = emptyList()) }
                return
            }

            val newVideos = mutableListOf<VideoItem>()
            var updatedNextPage: Page? = null
            
            if (selectedId != null) {
                val currentNextPage = if (isRefresh) null else stateFlow.value.nextPage
                
                if (isRefresh || currentNextPage != null) {
                    if (isRefresh) {
                        val details = videoRepository.getChannelDetails(selectedId)
                        updatedNextPage = details.nextVideosPage
                        newVideos.addAll(details.videos)
                    } else {
                        val result = videoRepository.fetchNextChannelVideosPage(selectedId, currentNextPage!!)
                        updatedNextPage = result.nextPage
                        newVideos.addAll(result.items)
                    }
                }
            } else {
                val channelsToFetch = if (isRefresh) {
                    allSubscriptions.take(CHUNK_SIZE.coerceAtMost(50))
                } else {
                    val chunk = allSubscriptions.drop(fetchedChannelsIndex).take(CHUNK_SIZE)
                    if (chunk.isEmpty()) return
                    fetchedChannelsIndex += CHUNK_SIZE
                    chunk
                }

                coroutineScope {
                    val deferredVideos = channelsToFetch.map { sub ->
                        async(Dispatchers.IO) {
                            try {
                                videoRepository.getChannelDetails(sub.channelId).videos.take(10)
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }
                    }
                    newVideos.addAll(deferredVideos.awaitAll().flatten())
                }
            }

            stateFlow.update { state ->
                val updatedVideos = if (isRefresh) {
                    if (selectedId != null) newVideos else {
                        (state.videos + newVideos).distinctBy { it.id }.sortedByDescending { it.rawUploadDate ?: 0L }
                    }
                } else {
                    val sortedNew = newVideos
                        .filter { nv -> state.videos.none { it.id == nv.id } }
                        .sortedByDescending { it.rawUploadDate ?: 0L }
                    state.videos + sortedNew
                }
                
                state.copy(
                    videos = updatedVideos.take(1000),
                    nextPage = if (selectedId != null) updatedNextPage else state.nextPage
                )
            }

            // Phase 1: Silent Cache Update for specific channel or All
            val cacheKey = if (selectedId != null) "subs_channel_$selectedId" else "subs_all"
            libraryRepository.updateCachedFeed(cacheKey, stateFlow.value.videos)
            
        } catch (e: Exception) {
            stateFlow.update { it.copy(error = PlayTubeError.fromThrowable(e)) }
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            val isFavorite = libraryRepository.isFavorite(video.id).first()
            toggleFavoriteUseCase(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    thumbnailUrl = video.thumbnailUrl,
                    uploaderName = video.uploaderName
                )
            )
            _snackbarMessage.emit(if (isFavorite) "Removed from Favorites" else "Added to Favorites")
        }
    }

    fun prepareDownload(video: VideoItem) {
        viewModelScope.launch {
            // Optimistic Cache Check
            val cachedBundle = videoRepository.getCachedStreamBundle(video.id)
            if (cachedBundle != null && !cachedBundle.videoStreams.isEmpty()) {
                _downloadState.value = DownloadDialogState.ShowDialog(video, cachedBundle)
                return@launch
            }

            _downloadState.value = DownloadDialogState.Loading(video)
            getVideoStreamsUseCase(video.id)
                .onSuccess { bundle ->
                    _downloadState.value = DownloadDialogState.ShowDialog(video, bundle)
                }
                .onFailure {
                    _downloadState.value = DownloadDialogState.Idle
                }
        }
    }

    fun download(video: VideoItem, bundle: StreamBundle, url: String?, quality: String?, format: String?, isAdaptive: Boolean) {
        viewModelScope.launch {
            val audioUrl = if (isAdaptive) {
                val isWebm = format?.contains("webm", ignoreCase = true) == true
                val compatibleStreams = bundle.audioStreams.filter { audio ->
                    if (isWebm) {
                        audio.format.contains("webm", ignoreCase = true) || 
                        audio.format.contains("opus", ignoreCase = true)
                    } else {
                        audio.format.contains("m4a", ignoreCase = true) || 
                        audio.format.contains("aac", ignoreCase = true)
                    }
                }

                compatibleStreams.filter { it.trackType == "ORIGINAL" }
                    .maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
                    ?.url ?: compatibleStreams.maxByOrNull { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }?.url
            } else null

            downloadVideoUseCase(
                videoId = video.id,
                url = url,
                title = video.title,
                thumbnailUrl = video.thumbnailUrl,
                uploaderName = video.uploaderName,
                quality = quality,
                format = format,
                audioUrl = audioUrl
            )
            _snackbarMessage.emit("Downloading started")
            _downloadState.value = DownloadDialogState.Idle
        }
    }

    fun dismissDownloadDialog() {
        _downloadState.value = DownloadDialogState.Idle
    }
}

data class FeedState(
    val videos: List<VideoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: PlayTubeError? = null,
    val nextPage: Page? = null
)

data class SubscriptionsFeedUIState(
    val selectedChannelId: String? = null,
    val subscriptions: List<SubscriptionEntity> = emptyList(),
    val activeFeed: FeedState = FeedState(),
    val isThoroughSearching: Boolean = false,
    val thoroughSearchProgress: Float = 0f
)
