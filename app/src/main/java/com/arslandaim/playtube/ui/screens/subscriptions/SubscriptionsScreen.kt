/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.subscriptions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.ui.screens.library.LibraryViewModel
import com.arslandaim.playtube.ui.screens.library.SubscriptionItemRow
import com.arslandaim.playtube.ui.components.EmptyState
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R

import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.arslandaim.playtube.utils.rememberScrollVisibilityConnection

@Composable
fun SubscriptionsScreen(
    viewModel: LibraryViewModel,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit,
    showTopAppBar: Boolean = true
) {
    val subscriptions by viewModel.filteredSubscriptions.collectAsState()
    val searchQuery by viewModel.subscriptionSearchQuery.collectAsState()

    SubscriptionsContent(
        subscriptions = subscriptions,
        searchQuery = searchQuery,
        showTopAppBar = showTopAppBar,
        onSearchQueryChange = viewModel::onSubscriptionSearchQueryChange,
        onToggleSubscription = viewModel::toggleSubscription,
        onBarsVisibilityChange = onBarsVisibilityChange,
        onBackClick = onBackClick,
        onChannelClick = onChannelClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionsContent(
    subscriptions: List<com.arslandaim.playtube.data.local.SubscriptionEntity>,
    searchQuery: String,
    showTopAppBar: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSubscription: (com.arslandaim.playtube.data.local.SubscriptionEntity) -> Unit,
    onBarsVisibilityChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var channelToUnsubscribe by remember { mutableStateOf<com.arslandaim.playtube.data.local.SubscriptionEntity?>(null) }
    val focusManager = LocalFocusManager.current
    val scrollVisibilityConnection = rememberScrollVisibilityConnection(onBarsVisibilityChange)

    if (channelToUnsubscribe != null) {
        AlertDialog(
            onDismissRequest = { channelToUnsubscribe = null },
            title = {
                Text(
                    text = "Unsubscribe?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to unsubscribe from ${channelToUnsubscribe?.name}?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        channelToUnsubscribe?.let { onToggleSubscription(it) }
                        channelToUnsubscribe = null
                    }
                ) {
                    Text("Unsubscribe", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { channelToUnsubscribe = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollVisibilityConnection),
        topBar = {
            if (showTopAppBar) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), // Glass Effect
                    tonalElevation = 0.dp
                ) {
                    TopAppBar(
                        title = {
                            if (isSearchActive) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = onSearchQueryChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Search subscriptions") },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                                )
                            } else {
                                Text(text = "Subscriptions", fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                if (isSearchActive) {
                                    isSearchActive = false
                                    onSearchQueryChange("")
                                    focusManager.clearFocus()
                                } else {
                                    onBackClick()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            if (!isSearchActive) {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                }
                            } else if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        ),
                        windowInsets = WindowInsets(0, 0, 0, 0)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (subscriptions.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.People,
                    title = if (searchQuery.isEmpty()) stringResource(R.string.no_subscriptions) else "No matching channels",
                    description = if (searchQuery.isEmpty()) stringResource(R.string.no_subscriptions_desc) else "Try a different search term"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(subscriptions, key = { it.channelId }) { sub ->
                        SubscriptionItemRow(
                            sub = sub,
                            onClick = { onChannelClick(sub.channelId) },
                            onUnsubscribeClick = { channelToUnsubscribe = sub }
                        )
                    }
                }
            }
        }
    }
}
