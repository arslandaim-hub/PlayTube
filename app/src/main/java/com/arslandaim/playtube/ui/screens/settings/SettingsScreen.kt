/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Recommend
import com.arslandaim.playtube.ui.theme.AppThemePalettes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import androidx.core.net.toUri
import com.arslandaim.playtube.BuildConfig
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.LanguageSelectionSheet
import com.arslandaim.playtube.ui.screens.library.GlobalGlassAlpha

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onViewHistory: () -> Unit,
    onDataManagement: () -> Unit,
    onAppearanceCustomization: () -> Unit,
    onRecommendationSettings: () -> Unit,
    onBack: () -> Unit
) {
    val isSearchHistoryPaused by viewModel.isSearchHistoryPaused.collectAsStateWithLifecycle()
    val isPipEnabled by viewModel.isPipEnabled.collectAsStateWithLifecycle()
    val isBackgroundPlayEnabled by viewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val availableLocales = viewModel.availableLocales
    val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsStateWithLifecycle()
    val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()

    SettingsContent(
        isSearchHistoryPaused = isSearchHistoryPaused,
        isPipEnabled = isPipEnabled,
        isBackgroundPlayEnabled = isBackgroundPlayEnabled,
        appLanguage = appLanguage,
        availableLocales = availableLocales,
        isAutoUpdateEnabled = isAutoUpdateEnabled,
        updateInfo = updateInfo,
        onSetSearchHistoryPaused = viewModel::setSearchHistoryPaused,
        onSetPipEnabled = viewModel::setPipEnabled,
        onSetBackgroundPlayEnabled = viewModel::setBackgroundPlayEnabled,
        onSetAppLanguage = viewModel::setAppLanguage,
        onSetAutoUpdateEnabled = updateViewModel::setAutoUpdateEnabled,
        onClearAllDownloads = viewModel::clearAllDownloads,
        onViewHistory = onViewHistory,
        onDataManagement = onDataManagement,
        onAppearanceCustomization = onAppearanceCustomization,
        onRecommendationSettings = onRecommendationSettings,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    isSearchHistoryPaused: Boolean,
    isPipEnabled: Boolean,
    isBackgroundPlayEnabled: Boolean,
    appLanguage: String?,
    availableLocales: List<java.util.Locale>,
    isAutoUpdateEnabled: Boolean,
    updateInfo: com.arslandaim.playtube.domain.repository.UpdateInfo,
    onSetSearchHistoryPaused: (Boolean) -> Unit,
    onSetPipEnabled: (Boolean) -> Unit,
    onSetBackgroundPlayEnabled: (Boolean) -> Unit,
    onSetAppLanguage: (String?) -> Unit,
    onSetAutoUpdateEnabled: (Boolean) -> Unit,
    onClearAllDownloads: () -> Unit,
    onViewHistory: () -> Unit,
    onDataManagement: () -> Unit,
    onAppearanceCustomization: () -> Unit,
    onRecommendationSettings: () -> Unit,
    onBack: () -> Unit
) {
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isPipSupported = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showDeveloperDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperDialog = false },
            title = { Text(stringResource(R.string.about_developer)) },
            text = { Text(stringResource(R.string.developed_by)) },
            confirmButton = {
                TextButton(onClick = { showDeveloperDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    if (showClearDownloadsDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.clear_all_downloads_title),
            message = stringResource(R.string.clear_all_downloads_desc),
            onDismiss = { showClearDownloadsDialog = false },
            onConfirm = {
                onClearAllDownloads()
                showClearDownloadsDialog = false
            }
        )
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            availableLocales = availableLocales,
            currentLanguageTag = appLanguage,
            onDismiss = { showLanguageSheet = false },
            onLanguageSelected = { tag ->
                onSetAppLanguage(tag)
                showLanguageSheet = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            GlassSurface(tonalElevation = 0.dp) {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
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
            // Update Category
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_update)) {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_update),
                        subtitle = stringResource(R.string.settings_auto_update_desc),
                        icon = Icons.Default.Info,
                        checked = isAutoUpdateEnabled,
                        onCheckedChange = onSetAutoUpdateEnabled
                    )
                    if (isAutoUpdateEnabled && updateInfo.hasUpdate) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.update_available, updateInfo.latestVersion),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = updateInfo.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, updateInfo.updateUrl.toUri())
                                    context.startActivity(intent)
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(stringResource(R.string.download_from_github), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // History & Privacy Category
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_history_privacy)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_view_history),
                        subtitle = stringResource(R.string.settings_view_history_desc),
                        icon = Icons.Default.History,
                        onClick = onViewHistory,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_pause_search_history),
                        subtitle = stringResource(R.string.settings_pause_search_history_desc),
                        icon = Icons.Default.Pause,
                        checked = isSearchHistoryPaused,
                        onCheckedChange = onSetSearchHistoryPaused
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    if (isPipSupported) {
                        SettingsSwitchItem(
                            title = stringResource(R.string.settings_pip),
                            subtitle = stringResource(R.string.settings_pip_desc),
                            icon = Icons.Default.PictureInPicture,
                            checked = isPipEnabled,
                            onCheckedChange = onSetPipEnabled
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsSwitchItem(
                        title = stringResource(R.string.background_play),
                        subtitle = stringResource(R.string.background_play_desc),
                        icon = Icons.Default.PlayArrow,
                        checked = isBackgroundPlayEnabled,
                        onCheckedChange = onSetBackgroundPlayEnabled
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        title = stringResource(R.string.settings_data_backup),
                        subtitle = stringResource(R.string.settings_data_backup_desc),
                        icon = Icons.Default.Info,
                        onClick = onDataManagement,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        title = stringResource(R.string.playtube_engine),
                        subtitle = stringResource(R.string.playtube_engine_desc),
                        icon = Icons.Default.AutoAwesome,
                        onClick = onRecommendationSettings,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                    )
                }
            }

            // Appearance & Playback Category
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_appearance_playback)) {
                    SettingsItem(
                        title = stringResource(R.string.appearance_customization),
                        subtitle = stringResource(R.string.appearance_customization_desc),
                        icon = Icons.Default.Palette,
                        onClick = onAppearanceCustomization,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        title = stringResource(R.string.app_language),
                        subtitle = appLanguage?.let { tag ->
                            val locale = java.util.Locale.forLanguageTag(tag)
                            locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
                        } ?: stringResource(R.string.system_default),
                        icon = Icons.Default.Language,
                        onClick = { showLanguageSheet = true }
                    )
                }
            }

            // Content Category
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_content)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_clear_downloads),
                        subtitle = stringResource(R.string.settings_clear_downloads_desc),
                        icon = Icons.Default.Delete,
                        onClick = { showClearDownloadsDialog = true },
                        titleColor = MaterialTheme.colorScheme.error
                    )
                }
            }

            // About Category
            item {
                SettingsGroup(title = stringResource(R.string.settings_group_about)) {
                    SettingsItem(
                        title = stringResource(R.string.settings_about_developer),
                        subtitle = stringResource(R.string.settings_about_developer_desc),
                        icon = Icons.Default.Person,
                        onClick = { showDeveloperDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        title = stringResource(R.string.settings_view_source),
                        subtitle = stringResource(R.string.settings_view_source_desc),
                        icon = Icons.Default.Code,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, "https://github.com/arslandaim-hub/PlayTube".toUri())
                            context.startActivity(intent)
                        },
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsItem(
                        title = stringResource(R.string.settings_app_version),
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        icon = Icons.Default.Info,
                        onClick = null
                    )
                }
            }
            
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp), // Consistent 20dp corners
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    trailingIcon: ImageVector? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val isError = titleColor == MaterialTheme.colorScheme.error

    Surface(
        modifier = modifier,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun settingsSwitchColors(): SwitchColors = com.arslandaim.playtube.ui.theme.settingsSwitchColors()

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = settingsSwitchColors()
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
