/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arslandaim.playtube.R
import com.arslandaim.playtube.ui.components.GlassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val isRecommendationsPaused by viewModel.isRecommendationsPaused.collectAsStateWithLifecycle()
    val userInterests by viewModel.userInterests.collectAsStateWithLifecycle()
    val totalWatchTime by viewModel.totalWatchTimeFormatted.collectAsStateWithLifecycle()
    val totalVideosWatched by viewModel.totalVideosWatched.collectAsStateWithLifecycle()
    val topWatchedChannel by viewModel.topWatchedChannel.collectAsStateWithLifecycle()
    val topChannelsList by viewModel.topChannelsList.collectAsStateWithLifecycle()

    var isTopicsExpanded by remember { mutableStateOf(false) }
    var isCreatorsExpanded by remember { mutableStateOf(false) }
    var showClearInterestsDialog by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showClearInterestsDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_learned_data_title),
            message = stringResource(R.string.clear_learned_data_desc),
            onDismiss = { showClearInterestsDialog = false },
            onConfirm = {
                viewModel.clearLearnedInterests()
                showClearInterestsDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassSurface(tonalElevation = 0.dp) {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.playtube_engine),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Statistics Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            title = "Watch Time",
                            value = totalWatchTime,
                            icon = Icons.Default.AccessTime,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Videos Watched",
                            value = "$totalVideosWatched",
                            icon = Icons.Default.PlayCircle,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatMetricCard(
                            title = stringResource(R.string.top_channel),
                            value = topWatchedChannel ?: "-",
                            icon = Icons.Default.Person,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = stringResource(R.string.algorithm_status),
                            value = if (isRecommendationsPaused) stringResource(R.string.status_paused) else stringResource(R.string.status_active),
                            icon = Icons.Default.Timeline,
                            accentColor = if (isRecommendationsPaused) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Collapsible Boxy Learned Topic Profile Section
            item {
                CollapsibleSettingsGroup(
                    title = "Learned Topic Profile",
                    subtitle = if (userInterests.isNotEmpty()) "${userInterests.size} topics tracked" else "No topics tracked",
                    icon = Icons.Default.Topic,
                    isExpanded = isTopicsExpanded,
                    onToggleExpand = { isTopicsExpanded = !isTopicsExpanded }
                ) {
                    if (userInterests.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No learned topics yet. Watch videos to build your recommendation profile.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val maxWeight = (userInterests.maxOfOrNull { it.weight } ?: 1f).coerceAtLeast(1f)
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            userInterests.take(10).forEach { interest ->
                                TopicStrengthRow(
                                    keyword = interest.keyword,
                                    weight = interest.weight,
                                    maxWeight = maxWeight
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Boxy Top Creators Breakdown Section
            if (topChannelsList.isNotEmpty()) {
                item {
                    CollapsibleSettingsGroup(
                        title = "Top Creators Affinity",
                        subtitle = "Top ${topChannelsList.size} channels watched",
                        icon = Icons.Default.Leaderboard,
                        isExpanded = isCreatorsExpanded,
                        onToggleExpand = { isCreatorsExpanded = !isCreatorsExpanded }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            topChannelsList.forEach { stat ->
                                ChannelAffinityRow(
                                    channelName = stat.channelName,
                                    count = stat.count,
                                    percentage = stat.percentage
                                )
                            }
                        }
                    }
                }
            }

            // Engine Controls Section
            item {
                SettingsGroup(title = "Engine Controls") {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_pause_recommendations),
                        subtitle = stringResource(R.string.settings_pause_recommendations_desc),
                        icon = Icons.Default.Recommend,
                        checked = isRecommendationsPaused,
                        onCheckedChange = viewModel::setRecommendationsPaused
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsItem(
                        title = stringResource(R.string.settings_clear_learned_data),
                        subtitle = stringResource(R.string.settings_clear_learned_data_desc_item),
                        icon = Icons.Default.AutoAwesome,
                        onClick = { showClearInterestsDialog = true },
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CollapsibleSettingsGroup(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Surface(
                onClick = onToggleExpand,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (icon != null) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                if (isExpanded) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = if (isExpanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun TopicStrengthRow(
    keyword: String,
    weight: Float,
    maxWeight: Float
) {
    val progress = (weight / maxWeight).coerceIn(0f, 1f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = keyword,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = String.format(java.util.Locale.US, "%.1f", weight),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun ChannelAffinityRow(
    channelName: String,
    count: Int,
    percentage: Float
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = channelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "$count videos",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { percentage.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
