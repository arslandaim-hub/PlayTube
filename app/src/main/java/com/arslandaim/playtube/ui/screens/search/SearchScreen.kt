/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.search

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.*
import com.arslandaim.playtube.ui.components.InfiniteScrollEffect
import com.arslandaim.playtube.ui.components.InfiniteScrollGridEffect
import com.arslandaim.playtube.ui.components.*
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.utils.Constants
import com.arslandaim.playtube.MainViewModel
import com.arslandaim.playtube.domain.repository.UpdateInfo
import com.arslandaim.playtube.ui.screens.settings.UpdateViewModel
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.ui.screens.library.VideoRow
import com.arslandaim.playtube.ui.screens.library.ModernChannelCard
import com.arslandaim.playtube.ui.screens.library.ModernPlaylistRow
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    mainViewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    libraryViewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchSort by viewModel.searchSort.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridView.collectAsStateWithLifecycle()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsStateWithLifecycle()
    val favorites by libraryViewModel.favorites.collectAsStateWithLifecycle()
    val savedVideoIds by libraryViewModel.savedVideoIds.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val isSortingNewest by viewModel.isSortingNewest.collectAsStateWithLifecycle()
    
    val isIncognitoMode by mainViewModel.isIncognitoMode.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
    val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsStateWithLifecycle()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    SearchContent(
        searchQuery = searchQuery,
        searchSort = searchSort,
        uiState = uiState,
        suggestions = suggestions,
        searchHistory = searchHistory,
        isGridView = isGridView,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        savedVideoIds = savedVideoIds,
        downloadState = downloadState,
        isSortingNewest = isSortingNewest,
        isIncognitoMode = isIncognitoMode,
        updateInfo = updateInfo,
        isAutoUpdateEnabled = isAutoUpdateEnabled,
        onToggleIncognito = { mainViewModel.toggleIncognitoMode() },
        onNavigateToSettings = onNavigateToSettings,
        snackbarMessage = viewModel.snackbarMessage,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
        onToggleGrid = viewModel::toggleGridView,
        onSearch = viewModel::search,
        onLoadMore = viewModel::loadNextPage,
        onDeleteHistory = { viewModel.deleteSearchQuery(it.query) },
        onClearHistory = viewModel::clearSearchHistory,
        onFavoriteClick = viewModel::toggleFavorite,
        onDownloadClick = viewModel::prepareDownload,
        onDownloadConfirm = viewModel::download,
        onDismissDownload = viewModel::dismissDownloadDialog,
        onToggleSubscription = viewModel::toggleSubscription,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onVideoClick = onVideoClick,
        onChannelClick = onChannelClick,
        onPlaylistClick = onPlaylistClick,
        onAddToPlaylistClick = onAddToPlaylistClick,
        onNavigateToDownloads = onNavigateToDownloads,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchContent(
    searchQuery: String,
    searchSort: SearchSort,
    uiState: SearchUiState,
    suggestions: List<String>,
    searchHistory: List<com.arslandaim.playtube.data.local.SearchHistoryEntity>,
    isGridView: Boolean,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    savedVideoIds: Set<String>,
    downloadState: DownloadDialogState,
    isSortingNewest: Boolean,
    isIncognitoMode: Boolean,
    updateInfo: UpdateInfo,
    isAutoUpdateEnabled: Boolean,
    onToggleIncognito: () -> Unit,
    onNavigateToSettings: () -> Unit,
    snackbarMessage: SharedFlow<String>,
    onQueryChange: (String) -> Unit,
    onSortChange: (SearchSort) -> Unit,
    onToggleGrid: () -> Unit,
    onSearch: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteHistory: (com.arslandaim.playtube.data.local.SearchHistoryEntity) -> Unit,
    onClearHistory: () -> Unit,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onDownloadConfirm: (VideoItem, StreamBundle, String?, String?, String?, Boolean) -> Unit,
    onDismissDownload: () -> Unit,
    onToggleSubscription: (SearchItem.Channel) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onAddToPlaylistClick: (VideoItem) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Reset scroll state when a new search query is initiated
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            snackbarMessage.collect { message ->
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            GlassSurface(
                tonalElevation = 0.dp,
                border = null,
                containerColor = if (isSearchFocused) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                }
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernSearchBar(
                                query = searchQuery,
                                onQueryChange = { onQueryChange(it) },
                                onSearch = { query ->
                                    if (query.isNotBlank()) {
                                        isSearchFocused = false
                                        onSearch(query)
                                        focusManager.clearFocus()
                                    }
                                },
                                isFocused = isSearchFocused,
                                onFocusChange = { isSearchFocused = it },
                                onBack = {
                                    if (isSearchFocused || searchQuery.isNotEmpty()) {
                                        onQueryChange("")
                                        isSearchFocused = false
                                        focusManager.clearFocus()
                                    } else {
                                        onBack()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(bottom = 0.dp) // Removed bottom padding
                            )
                            
                            if (!isSearchFocused) {
                                IconButton(onClick = onToggleIncognito) {
                                    Icon(
                                        imageVector = if (isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Incognito Mode",
                                        tint = if (isIncognitoMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                IconButton(onClick = onNavigateToSettings) {
                                    BadgedBox(
                                        badge = {
                                            if (isAutoUpdateEnabled && updateInfo.hasUpdate) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ) { Text("!") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = stringResource(R.string.settings),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                        
                        // Sort Chips Row
                        AnimatedVisibility(
                            visible = uiState is SearchUiState.Success && !isSearchFocused,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp), // Reduced top padding
                                horizontalArrangement = Arrangement.spacedBy(8.dp), // Tighter spacing
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Better Layout Toggle
                                Surface(
                                    onClick = onToggleGrid,
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp) // Smaller size
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isGridView) Icons.Default.ViewStream else Icons.Default.GridView,
                                            contentDescription = "Toggle Layout",
                                            modifier = Modifier.size(18.dp), // Smaller icon
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                SearchSort.entries.forEach { sort ->
                                    val isSelected = searchSort == sort
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSortChange(sort) },
                                        label = { 
                                            Text(
                                                text = when(sort) {
                                                    SearchSort.RELEVANCE -> stringResource(R.string.sort_relevance)
                                                    SearchSort.UPLOAD_DATE -> stringResource(R.string.sort_newest)
                                                    SearchSort.VIEW_COUNT -> stringResource(R.string.sort_most_viewed)
                                                    SearchSort.RATING -> stringResource(R.string.sort_top_rated)
                                                },
                                                style = MaterialTheme.typography.labelMedium, // Smaller font
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                            ) 
                                        },
                                        shape = CircleShape,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        ),
                                        border = null,
                                        modifier = Modifier.height(32.dp) // Smaller height
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top-level Progress Indicator for "Newest" sort transition
                AnimatedVisibility(
                    visible = isSortingNewest,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(start = 16.dp, end = 16.dp, top = topPadding + 4.dp, bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Fetching newest videos...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = uiState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "SearchContentTransition",
                        contentKey = { it::class }
                    ) { state ->
                        when (state) {
                            is SearchUiState.Initial -> {
                                Box(modifier = Modifier.fillMaxSize().padding(top = topPadding)) {
                                    InitialSearchState()
                                }
                            }
                            is SearchUiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(top = topPadding), 
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is SearchUiState.Success -> {
                                if (state.items.isEmpty() && !state.isLoadingMore && !isSortingNewest) {
                                    EmptyState(
                                        icon = Icons.Default.SearchOff,
                                        title = "No results found",
                                        description = "Try searching for something else or check your spelling",
                                        actionText = "Clear Search",
                                        onActionClick = { onQueryChange("") }
                                    )
                                } else {
                                    val configuration = LocalConfiguration.current
                                    val screenWidth = configuration.screenWidthDp
                                    val gridColumns = Constants.calculateGridColumns(screenWidth)
                                    val finalColumns = if (isGridView) gridColumns else 1

                                    if (finalColumns > 1) {
                                        val gridState = rememberLazyGridState()
                                        InfiniteScrollGridEffect(
                                            gridState = gridState,
                                            enabled = !state.isLoadingMore,
                                            onLoadMore = onLoadMore
                                        )

                                        LazyVerticalGrid(
                                            state = gridState,
                                            columns = GridCells.Fixed(finalColumns),
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = topPadding, bottom = 100.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(
                                                items = state.items,
                                                key = { it.uniqueKey },
                                                span = { item ->
                                                    if (item is SearchItem.Video) GridItemSpan(1)
                                                    else GridItemSpan(finalColumns)
                                                },
                                                contentType = { item ->
                                                    when (item) {
                                                        is SearchItem.Video -> "type_video"
                                                        is SearchItem.Channel -> "type_channel"
                                                        is SearchItem.Playlist -> "type_playlist"
                                                    }
                                                }
                                            ) { item ->
                                                SearchItemRenderer(
                                                    item = item,
                                                    isGridView = true,
                                                    downloadedIds = downloadedIds,
                                                    favoriteIds = favoriteIds,
                                                    savedVideoIds = savedVideoIds,
                                                    onFavoriteClick = onFavoriteClick,
                                                    onDownloadClick = onDownloadClick,
                                                    onChannelClick = onChannelClick,
                                                    onVideoClick = onVideoClick,
                                                    onToggleSubscription = onToggleSubscription,
                                                    onPlaylistClick = onPlaylistClick
                                                )
                                            }

                                            if (state.isLoadingMore) {
                                                item(span = { GridItemSpan(finalColumns) }) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(24.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        InfiniteScrollEffect(
                                            listState = listState,
                                            enabled = !state.isLoadingMore,
                                            onLoadMore = onLoadMore
                                        )

                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(top = topPadding, bottom = 100.dp)
                                        ) {
                                            items(
                                                items = state.items,
                                                key = { it.uniqueKey },
                                                contentType = { item ->
                                                    when (item) {
                                                        is SearchItem.Video -> "type_video"
                                                        is SearchItem.Channel -> "type_channel"
                                                        is SearchItem.Playlist -> "type_playlist"
                                                    }
                                                }
                                            ) { item ->
                                                SearchItemRenderer(
                                                    item = item,
                                                    isGridView = isGridView,
                                                    downloadedIds = downloadedIds,
                                                    favoriteIds = favoriteIds,
                                                    savedVideoIds = savedVideoIds,
                                                    onFavoriteClick = onFavoriteClick,
                                                    onDownloadClick = onDownloadClick,
                                                    onChannelClick = onChannelClick,
                                                    onVideoClick = onVideoClick,
                                                    onToggleSubscription = onToggleSubscription,
                                                    onPlaylistClick = onPlaylistClick
                                                )
                                            }

                                            if (state.isLoadingMore) {
                                                item {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(24.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is SearchUiState.Error -> {
                                val isNetworkError = state.error is PlayTubeError.Network
                                Box(modifier = Modifier.fillMaxSize().padding(top = topPadding)) {
                                    EmptyState(
                                        icon = if (isNetworkError) Icons.Default.WifiOff else Icons.Default.ErrorOutline,
                                        title = if (isNetworkError) stringResource(R.string.no_internet) else "Something went wrong",
                                        description = if (isNetworkError) "Your downloads are still available offline." else state.error.getMessage(),
                                        actionText = if (isNetworkError) "Go to Offline Hub" else stringResource(R.string.retry),
                                        onActionClick = { 
                                            if (isNetworkError) onNavigateToDownloads() else onSearch(searchQuery)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Redesigned Overlay suggestions (Glassmorphic)
            AnimatedVisibility(
                visible = isSearchFocused && (searchQuery.isNotEmpty() || searchHistory.isNotEmpty()),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topPadding),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ) {
                    SuggestionsAndHistoryList(
                        query = searchQuery,
                        history = searchHistory,
                        suggestions = suggestions,
                        onSuggestionClick = { suggestion ->
                            onQueryChange(suggestion)
                            onSearch(suggestion)
                            focusManager.clearFocus()
                        },
                        onDeleteHistory = { onDeleteHistory(it) },
                        onClearHistory = onClearHistory
                    )
                }
            }

            // Download Dialogs
            when (val currentDownloadState = downloadState) {
                DownloadDialogState.Idle -> {}
                is DownloadDialogState.Loading -> {
                    AlertDialog(
                        onDismissRequest = { onDismissDownload() },
                        confirmButton = {},
                        title = { Text(stringResource(R.string.loading)) },
                        text = {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    )
                }
                is DownloadDialogState.ShowDialog -> {
                DownloadSelectionSheet(
                    videoStreams = currentDownloadState.bundle.videoStreams,
                    onDismiss = { onDismissDownload() },
                    onDownload = { stream ->
                        onDownloadConfirm(
                            currentDownloadState.video,
                            currentDownloadState.bundle,
                            stream.url,
                            stream.quality,
                            stream.format,
                            stream.isAdaptive
                        )
                    }
                )
            }
            }
        }
    }
}

@Composable
private fun SearchItemRenderer(
    item: SearchItem,
    isGridView: Boolean,
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    savedVideoIds: Set<String>,
    onFavoriteClick: (VideoItem) -> Unit,
    onDownloadClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onToggleSubscription: (SearchItem.Channel) -> Unit,
    onPlaylistClick: (String) -> Unit
) {
    when (item) {
        is SearchItem.Video -> {
            val video = item.video
            val currentOnFavoriteClick = remember(video.id, onFavoriteClick) { { onFavoriteClick(video) } }
            val currentOnDownloadClick = remember(video.id, onDownloadClick) { { onDownloadClick(video) } }
            val currentOnChannelClick = remember(video.id, onChannelClick) {
                video.uploaderUrl?.let { url -> { onChannelClick(url) } }
            }
            val currentOnClick = remember(video.id, onVideoClick) { { onVideoClick(video) } }

            Box {
                if (isGridView) {
                    VideoItemRow(
                        video = video,
                        isDownloaded = downloadedIds.contains(video.id),
                        isFavorite = favoriteIds.contains(video.id),
                        isSaved = savedVideoIds.contains(video.id),
                        onFavoriteClick = currentOnFavoriteClick,
                        onDownloadClick = currentOnDownloadClick,
                        onChannelClick = currentOnChannelClick,
                        onClick = currentOnClick
                    )
                } else {
                    VideoRow(
                        videoId = video.id,
                        title = video.title,
                        uploader = video.uploaderName,
                        thumbnailUrl = video.thumbnailUrl,
                        watchProgress = video.watchProgress,
                        isDownloaded = downloadedIds.contains(video.id),
                        isFavorite = favoriteIds.contains(video.id),
                        isSaved = savedVideoIds.contains(video.id),
                        onFavoriteClick = currentOnFavoriteClick,
                        onDownloadClick = currentOnDownloadClick,
                        onChannelClick = currentOnChannelClick,
                        onClick = currentOnClick
                    )
                }
            }
        }
        is SearchItem.Channel -> {
            val currentOnToggleSubscription = remember(item.id, onToggleSubscription) {
                { onToggleSubscription(item) }
            }
            val currentOnChannelClick = remember(item.id, onChannelClick) {
                { onChannelClick(item.id) }
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                ModernChannelCard(
                    channel = item,
                    onClick = currentOnChannelClick,
                    onToggleSubscription = currentOnToggleSubscription
                )
            }
        }
        is SearchItem.Playlist -> {
            val currentOnClick = remember(item.playlist.id, onPlaylistClick) {
                { onPlaylistClick(item.playlist.id) }
            }
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                ModernPlaylistRow(
                    playlist = item.playlist,
                    onClick = currentOnClick
                )
            }
        }
    }
}

@Composable
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val containerAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.85f else 0.4f, 
        label = "SearchContainerAlpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 0.dp)
            .height(52.dp)
            .animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                    else Color.White.copy(alpha = 0.1f)
        )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) },
            placeholder = { 
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                ) 
            },
            leadingIcon = {
                IconButton(
                    onClick = {
                        if (isFocused || query.isNotEmpty()) {
                            onBack()
                        } else {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onBack()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (!isFocused && query.isEmpty()) Icons.Default.Search 
                                      else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { 
                onSearch(query)
                focusManager.clearFocus()
            })
        )
    }
}

@Composable
fun SuggestionsAndHistoryList(
    query: String,
    history: List<com.arslandaim.playtube.data.local.SearchHistoryEntity>,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onDeleteHistory: (com.arslandaim.playtube.data.local.SearchHistoryEntity) -> Unit,
    onClearHistory: () -> Unit
) {
    val filteredHistory = remember(query, history) {
        if (query.isEmpty()) history
        else history.filter { it.query.contains(query, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        if (query.isEmpty() && history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.clear_all), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Show Matching History First
        items(filteredHistory) { item ->
            SearchItemRow(
                text = item.query,
                icon = Icons.Default.History,
                onDelete = { onDeleteHistory(item) },
                onClick = { onSuggestionClick(item.query) }
            )
        }

        // Show Remote Suggestions
        if (query.isNotEmpty()) {
            val suggestionsToDisplay = suggestions.filter { s -> filteredHistory.none { it.query.equals(s, true) } }
            items(suggestionsToDisplay) { suggestion ->
                SearchItemRow(
                    text = suggestion,
                    icon = Icons.Default.Search,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SearchItemRow(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (onDelete != null) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                   else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium
        )
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun InitialSearchState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.discover_new),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Search for your favorite videos and channels",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}
