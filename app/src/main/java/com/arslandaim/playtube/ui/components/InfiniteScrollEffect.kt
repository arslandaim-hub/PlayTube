/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.*

/**
 * A reusable Effect that triggers [onLoadMore] when the user scrolls near the end of a [LazyListState].
 * 
 * @param listState The state of the LazyColumn/LazyRow to monitor.
 * @param buffer How many items from the end to trigger the load. Default is 5.
 * @param enabled Whether the scroll effect is active. Should be false if already loading or no more pages.
 * @param onLoadMore Lambda to trigger fetching next page.
 */
@Composable
fun InfiniteScrollEffect(
    listState: LazyListState,
    buffer: Int = 5,
    enabled: Boolean = true,
    onLoadMore: () -> Unit
) {
    val loadMoreTrigger = remember(listState, enabled) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            enabled && (totalItemsNumber > 0) && (lastVisibleItemIndex > (totalItemsNumber - buffer))
        }
    }

    LaunchedEffect(loadMoreTrigger.value) {
        if (loadMoreTrigger.value) {
            onLoadMore()
        }
    }
}

/**
 * A reusable Effect that triggers [onLoadMore] when the user scrolls near the end of a [LazyGridState].
 */
@Composable
fun InfiniteScrollGridEffect(
    gridState: LazyGridState,
    buffer: Int = 5,
    enabled: Boolean = true,
    onLoadMore: () -> Unit
) {
    val loadMoreTrigger = remember(gridState, enabled) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            enabled && (totalItemsNumber > 0) && (lastVisibleItemIndex > (totalItemsNumber - buffer))
        }
    }

    LaunchedEffect(loadMoreTrigger.value) {
        if (loadMoreTrigger.value) {
            onLoadMore()
        }
    }
}
