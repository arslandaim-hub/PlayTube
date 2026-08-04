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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.InfiniteScrollEffect
import com.arslandaim.playtube.ui.components.*
import com.arslandaim.playtube.utils.PlayTubeError
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.ui.screens.library.VideoRow
import com.arslandaim.playtube.ui.screens.library.ModernChannelCard
import com.arslandaim.playtube.ui.screens.library.ModernPlaylistRow
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSort by viewModel.searchSort.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsState()
    val favorites by libraryViewModel.favorites.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val isSortingNewest by viewModel.isSortingNewest.collectAsState()

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
        downloadState = downloadState,
        isSortingNewest = isSortingNewest,
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
    downloadState: DownloadDialogState,
    isSortingNewest: Boolean,
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
    onNavigateToDownloads: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    
    // Reset scroll state when a new search query is initiated
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(Unit) {
        snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(surfaceColor)) {
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
                    onFocusChange = { isSearchFocused = it },
                    onBack = {
                        if (isSearchFocused || searchQuery.isNotEmpty()) {
                            onQueryChange("")
                            isSearchFocused = false
                            focusManager.clearFocus()
                        } else {
                            onBack()
                        }
                    }
                )
                
                // Sort Chips Row (Integrated with GlassSurface)
                AnimatedVisibility(
                    visible = uiState is SearchUiState.Success && !isSearchFocused,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Grid Toggle Button
                        IconButton(
                            onClick = onToggleGrid,
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewStream else Icons.Default.GridView,
                                contentDescription = "Toggle Layout",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(20.dp).padding(horizontal = 4.dp), thickness = 0.5.dp)

                        SearchSort.entries.forEach { sort ->
                            FilterChip(
                                selected = searchSort == sort,
                                onClick = { onSortChange(sort) },
                                label = { 
                                    Text(
                                        text = when(sort) {
                                            SearchSort.RELEVANCE -> stringResource(R.string.sort_relevance)
                                            SearchSort.UPLOAD_DATE -> stringResource(R.string.sort_newest)
                                            SearchSort.VIEW_COUNT -> stringResource(R.string.sort_most_viewed)
                                            SearchSort.RATING -> stringResource(R.string.sort_top_rated)
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    ) 
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
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
                            .background(surfaceColor)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                InitialSearchState()
                            }
                            is SearchUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                    InfiniteScrollEffect(
                                        listState = listState,
                                        enabled = !state.isLoadingMore,
                                        onLoadMore = onLoadMore
                                    )

                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        items(
                                            items = state.items,
                                            key = { it.uniqueKey }
                                        ) { item ->
                                            when (item) {
                                                is SearchItem.Video -> {
                                                    Box(modifier = Modifier.animateItem()) {
                                                        if (isGridView) {
                                                            VideoItemRow(
                                                                video = item.video,
                                                                isDownloaded = downloadedIds.contains(item.video.id),
                                                                isFavorite = favoriteIds.contains(item.video.id),
                                                                onFavoriteClick = { onFavoriteClick(item.video) },
                                                                onDownloadClick = { onDownloadClick(item.video) },
                                                                onChannelClick = { item.video.uploaderUrl?.let { onChannelClick(it) } },
                                                                onClick = { onVideoClick(item.video) }
                                                            )
                                                        } else {
                                                            VideoRow(
                                                                videoId = item.video.id,
                                                                title = item.video.title,
                                                                uploader = item.video.uploaderName,
                                                                thumbnailUrl = item.video.thumbnailUrl,
                                                                watchProgress = item.video.watchProgress,
                                                                isDownloaded = downloadedIds.contains(item.video.id),
                                                                isFavorite = favoriteIds.contains(item.video.id),
                                                                onFavoriteClick = { onFavoriteClick(item.video) },
                                                                onDownloadClick = { onDownloadClick(item.video) },
                                                                onChannelClick = { item.video.uploaderUrl?.let { onChannelClick(it) } },
                                                                onClick = { onVideoClick(item.video) }
                                                            )
                                                        }
                                                    }
                                                }
                                                is SearchItem.Channel -> {
                                                    Box(modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 2.dp)) {
                                                        ModernChannelCard(
                                                            channel = item,
                                                            onClick = { onChannelClick(item.id) },
                                                            onToggleSubscription = { onToggleSubscription(item) }
                                                        )
                                                    }
                                                }
                                                is SearchItem.Playlist -> {
                                                    Box(modifier = Modifier.animateItem().padding(horizontal = 16.dp, vertical = 2.dp)) {
                                                        ModernPlaylistRow(
                                                            playlist = item.playlist,
                                                            onClick = { onPlaylistClick(item.playlist.id) }
                                                        )
                                                    }
                                                }
                                            }
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
                            is SearchUiState.Error -> {
                                val isNetworkError = state.error is PlayTubeError.Network
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

            // Overlay suggestions when search is focused (Enhanced design)
            AnimatedVisibility(
                visible = isSearchFocused && (searchQuery.isNotEmpty() || searchHistory.isNotEmpty()),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
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
fun ModernSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(52.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(26.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                ) 
            },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (query.isEmpty()) Icons.Default.Search else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
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
            items(history) { item ->
                SearchItemRow(
                    text = item.query,
                    icon = Icons.Default.History,
                    onDelete = { onDeleteHistory(item) },
                    onClick = { onSuggestionClick(item.query) }
                )
            }
        } else {
            items(suggestions) { suggestion ->
                SearchItemRow(
                    text = suggestion,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Default.NorthWest,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
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
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.discover_new),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            text = "Search for your favorite videos and channels",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
