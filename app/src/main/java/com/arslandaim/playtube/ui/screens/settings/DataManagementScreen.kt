/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.repository.ImportProgress
import com.arslandaim.playtube.ui.screens.library.GlobalGlassAlpha

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.importSnackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.localSnackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // SAF Launchers
    val historyPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importHistory(it) }
    }

    val subscriptionsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importSubscriptions(it) }
    }

    val backupCreator = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = GlobalGlassAlpha),
                tonalElevation = 0.dp
            ) {
                TopAppBar(
                    title = { Text("Data & Backup", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
            // YouTube Takeout Section
            item {
                SettingsGroup(title = "YouTube Takeout Import") {
                    SettingsItem(
                        title = "Import Watch History",
                        subtitle = "Select your watch-history.json or Takeout ZIP",
                        icon = Icons.Default.History,
                        onClick = { historyPicker.launch(arrayOf("application/json", "application/zip")) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingsItem(
                        title = "Import Subscriptions",
                        subtitle = "Select your subscriptions.csv or Takeout ZIP",
                        icon = Icons.Default.Subscriptions,
                        onClick = { subscriptionsPicker.launch(arrayOf("text/comma-separated-values", "text/csv", "application/zip")) }
                    )
                }
            }

            // Native Backup Section
            item {
                SettingsGroup(title = "PlayTube Native Backup") {
                    SettingsItem(
                        title = "Create Backup",
                        subtitle = "Save all your PlayTube data to a file",
                        icon = Icons.Default.Backup,
                        onClick = { backupCreator.launch("PlayTubeBackup_${System.currentTimeMillis()}.zip") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    SettingsItem(
                        title = "Restore Backup",
                        subtitle = "Restore your data from a previous backup",
                        icon = Icons.Default.Restore,
                        onClick = { backupPicker.launch(arrayOf("application/zip")) }
                    )
                }
            }

            // Progress Dialog Overlay
            if (isProcessing || importProgress != null) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (val progress = importProgress) {
                                is ImportProgress.Loading -> {
                                    Text(progress.status, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "You can safely leave this page. The import will continue in the background.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                                is ImportProgress.Success -> {
                                    Icon(Icons.Default.CheckCircle, "Success", tint = Color.Green)
                                    Text("Import Complete!", fontWeight = FontWeight.Bold)
                                    Text("${progress.importedCount} items processed")
                                    TextButton(onClick = { viewModel.clearProgress() }) {
                                        Text("Dismiss")
                                    }
                                }
                                is ImportProgress.Error -> {
                                    Icon(Icons.Default.Error, "Error", tint = MaterialTheme.colorScheme.error)
                                    Text("Import Failed", fontWeight = FontWeight.Bold)
                                    Text(progress.message, color = MaterialTheme.colorScheme.error)
                                    TextButton(onClick = { viewModel.clearProgress() }) {
                                        Text("Dismiss")
                                    }
                                }
                                else -> {
                                    if (isProcessing) {
                                        Text("Processing...")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
