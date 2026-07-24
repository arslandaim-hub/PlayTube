/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.search

import androidx.compose.animation.*
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.SearchItem
import com.arslandaim.playtube.domain.model.SearchSort
import com.arslandaim.playtube.domain.model.StreamBundle
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.components.*
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
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
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSort by viewModel.searchSort.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()
    val downloadedIds by libraryViewModel.downloadedVideoIds.collectAsState()
    val favorites by libraryViewModel.favorites.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()

    val favoriteIds = remember(favorites) {
        favorites.map { it.videoId }.toSet()
    }

    SearchContent(
        searchQuery = searchQuery,
        searchSort = searchSort,
        uiState = uiState,
        suggestions = suggestions,
        searchHistory = searchHistory,
        downloadedIds = downloadedIds,
        favoriteIds = favoriteIds,
        downloadState = downloadState,
        snackbarMessage = viewModel.snackbarMessage,
        onQueryChange = viewModel::onQueryChange,
        onSortChange = viewModel::onSortChange,
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
    downloadedIds: Set<String>,
    favoriteIds: Set<String>,
    downloadState: DownloadDialogState,
    snackbarMessage: SharedFlow<String>,
    onQueryChange: (String) -> Unit,
    onSortChange: (SearchSort) -> Unit,
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
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    val surfaceColor = MaterialTheme.colorScheme.surface
    val searchBarColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val snackbarHostState = remember { SnackbarHostState() }

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
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { onQueryChange(it) },
                    onSearch = { query ->
                        if (query.isNotBlank()) {
                            isSearchFocused = false // Hide suggestions immediately
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
                    },
                    containerColor = searchBarColor
                )
                
                // Sort Chips Row
                AnimatedVisibility(
                    visible = uiState is SearchUiState.Success && !isSearchFocused,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                        color = surfaceColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchSort.entries.forEach { sort ->
                                FilterChip(
                                    selected = searchSort == sort,
                                    onClick = { onSortChange(sort) },
                                    label = { 
                                        Text(
                                            when(sort) {
                                                SearchSort.RELEVANCE -> stringResource(R.string.sort_relevance)
                                                SearchSort.UPLOAD_DATE -> stringResource(R.string.sort_newest)
                                                SearchSort.VIEW_COUNT -> stringResource(R.string.sort_most_viewed)
                                                SearchSort.RATING -> stringResource(R.string.sort_top_rated)
                                            }
                                        ) 
                                    },
                                    shape = CircleShape,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (uiState) {
                is SearchUiState.Initial -> {
                    InitialSearchState()
                }
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Success -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val totalItemsCount = listState.layoutInfo.totalItemsCount
                            val lastVisibleItemIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItemIndex >= totalItemsCount - 5
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(uiState.items) { item ->
                            when (item) {
                                is SearchItem.Video -> {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        VideoItemRow(
                                            video = item.video,
                                            isDownloaded = downloadedIds.contains(item.video.id),
                                            isFavorite = favoriteIds.contains(item.video.id),
                                            onFavoriteClick = { onFavoriteClick(item.video) },
                                            onDownloadClick = { onDownloadClick(item.video) },
                                            onChannelClick = { item.video.uploaderUrl?.let { onChannelClick(it) } },
                                            onClick = { onVideoClick(item.video) }
                                        )
                                    }
                                }
                                is SearchItem.Channel -> {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        PremiumChannelCard(
                                            channel = item,
                                            onClick = { onChannelClick(item.id) },
                                            onToggleSubscription = { onToggleSubscription(item) }
                                        )
                                    }
                                }
                                is SearchItem.Playlist -> {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                        PremiumPlaylistCard(
                                            playlist = item.playlist,
                                            onClick = { onPlaylistClick(item.playlist.id) }
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.isLoadingMore) {
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
                is SearchUiState.Error -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Something went wrong",
                        description = uiState.message,
                        actionText = stringResource(R.string.retry),
                        onActionClick = { onSearch(searchQuery) }
                    )
                }
            }

            // Overlay suggestions when search is focused
            if (isSearchFocused && (searchQuery.isNotEmpty() || searchHistory.isNotEmpty())) {
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
                        audioStreams = currentDownloadState.bundle.audioStreams,
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
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = containerColor,
        shape = RoundedCornerShape(24.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
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
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (query.isEmpty() && history.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.recent_searches), style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.clear_all))
                    }
                }
            }
            items(history) { item ->
                SearchItem(
                    text = item.query,
                    icon = Icons.Default.History,
                    onDelete = { onDeleteHistory(item) },
                    onClick = { onSuggestionClick(item.query) }
                )
            }
        } else {
            items(suggestions) { suggestion ->
                SearchItem(
                    text = suggestion,
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SearchItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text, modifier = Modifier.weight(1f))
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            }
        } else {
            Icon(Icons.Default.NorthWest, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InitialSearchState() {
    EmptyState(
        icon = Icons.Default.Search,
        title = stringResource(R.string.discover_new),
        description = "Search for your favorite videos and channels"
    )
}
