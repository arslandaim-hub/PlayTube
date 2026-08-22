/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.services.PlaybackService
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.ui.components.main.OfflineBottomBanner
import com.arslandaim.playtube.ui.components.main.PlayTubeTopAppBar
import com.arslandaim.playtube.ui.components.main.RestorationBanner
import com.arslandaim.playtube.ui.navigation.NavGraph
import com.arslandaim.playtube.ui.navigation.Destination
import com.arslandaim.playtube.ui.navigation.toDestination
import com.arslandaim.playtube.ui.screens.player.MiniPlayerManager
import com.arslandaim.playtube.ui.screens.player.PlayerOverlay
import com.arslandaim.playtube.ui.screens.settings.UpdateViewModel
import com.arslandaim.playtube.ui.theme.IncognitoPurple
import com.arslandaim.playtube.ui.theme.PlayTubeTheme
import com.arslandaim.playtube.utils.ConnectivityObserver
import com.arslandaim.playtube.utils.PTLog
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    companion object {
        val BOTTOM_BAR_HEIGHT = 64.dp
    }

    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var miniPlayerManager: MiniPlayerManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Handle permission result if needed
    }

    private val mainViewModel: MainViewModel by viewModels()
    @OptIn(androidx.media3.common.util.UnstableApi::class)
    private val playerViewModel: com.arslandaim.playtube.ui.screens.player.PlayerViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private val pendingDeepLink = MutableStateFlow<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingDeepLink.value = intent

        // Safety timeout for SplashScreen: don't hang for more than 3 seconds
        val startTime = System.currentTimeMillis()
        splashScreen.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            val statusKnown = mainViewModel.isOnboardingCompleted.value != null
            !statusKnown && elapsed < 3000
        }

        // Local Network Protection check (Android 16+ / API 36+)
        if (Build.VERSION.SDK_INT >= 36) {
            val permission = if (Build.VERSION.SDK_INT >= 37) {
                "android.permission.ACCESS_LOCAL_NETWORK"
            } else {
                Manifest.permission.NEARBY_WIFI_DEVICES
            }

            try {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    // Only request if not previously denied in this session or check rationale
                    if (shouldShowRequestPermissionRationale(permission)) {
                        PTLog.d("MainActivity", "Local Network permission rationale needed.")
                        // For simplicity in startup, we just launch, but a dialog would be better
                    }
                    requestPermissionLauncher.launch(permission)
                }
            } catch (e: Exception) {
                PTLog.e("MainActivity", "Failed to request local network permission", e)
            }
        }

        // Observe critical events
        lifecycleScope.launch {
            playerViewModel.sleepTimerManager.timerFinishedEvent.collectLatest {
                if (playerViewModel.sleepTimerManager.shouldCloseApp.value) {
                    finishAndRemoveTask()
                }
            }
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val isBackgroundPlayEnabled by mainViewModel.isBackgroundPlayEnabled.collectAsStateWithLifecycle()
            val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsStateWithLifecycle()

            if (isOnboardingCompleted == null) return@setContent
            
            val startDestination = if (isOnboardingCompleted == true) Destination.Home else Destination.Onboarding

            // Background Play MediaSession Connection
            if (isBackgroundPlayEnabled) {
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                DisposableEffect(Unit) {
                    val sessionToken = SessionToken(this@MainActivity, android.content.ComponentName(this@MainActivity, PlaybackService::class.java))
                    val controllerFuture = MediaController.Builder(this@MainActivity, sessionToken).buildAsync()
                    controllerFuture.addListener({}, MoreExecutors.directExecutor())
                    onDispose {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }
            }

            PlayTubeTheme(
                darkTheme = darkTheme,
                isDynamicColorEnabled = mainViewModel.isDynamicColorEnabled.collectAsStateWithLifecycle().value
            ) {
                val navController = rememberNavController()
                val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(navController, isOnboardingCompleted) {
                    if (isOnboardingCompleted == true) {
                        pendingDeepLink.collectLatest { intent ->
                            if (intent != null) {
                                handleDeepLink(intent, navController)
                                pendingDeepLink.value = null
                            }
                        }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val playerVisibility by miniPlayerManager.visibilityState.collectAsStateWithLifecycle()
                val isExpanded = playerVisibility == com.arslandaim.playtube.ui.screens.player.MiniPlayerVisibility.Expanded

                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                LaunchedEffect(isExpanded) {
                    if (isExpanded) {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    }
                }

                val currentVideo by miniPlayerManager.currentVideo.collectAsStateWithLifecycle()
                val isIncognitoMode by mainViewModel.isIncognitoMode.collectAsStateWithLifecycle()
                val isOffline by mainViewModel.isOffline.collectAsStateWithLifecycle()

                val currentScreen = remember<Destination?>(currentRoute) { currentRoute.toDestination() }
                val isMainRoute = currentScreen?.isTopLevel == true
                val isOnboarding = currentScreen is Destination.Onboarding
                
                LaunchedEffect(currentRoute) {
                    val isPlayer = currentRoute?.contains("Player") == true
                    mainViewModel.setPlayerScreen(isPlayer)
                    
                    if (isMainRoute) {
                        mainViewModel.setBarsVisibility(true)
                    } else if (isOnboarding || isPlayer) {
                        mainViewModel.setBarsVisibility(false)
                    }
                }

                val showBars by remember(isMainRoute, isOnboarding, uiState.isInPipMode) {
                    derivedStateOf { isMainRoute && !uiState.isInPipMode && !isOnboarding }
                }

                val showTopBarActual by remember(showBars, currentScreen) {
                    derivedStateOf { showBars && currentScreen !is Destination.Search }
                }
                
                val barsVisibilityProgress by animateFloatAsState(
                    targetValue = if (showBars && uiState.isBarsVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "BarsVisibility"
                )

                DisposableEffect(Unit) {
                    val consumer = Consumer<Configuration> {
                        mainViewModel.setPipMode(isInPictureInPictureMode)
                    }
                    addOnConfigurationChangedListener(consumer)
                    onDispose {
                        removeOnConfigurationChangedListener(consumer)
                    }
                }

                DisposableEffect(playerViewModel.player) {
                    val listener = object : androidx.media3.common.Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (isPlaying) {
                                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            } else {
                                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            }
                        }
                    }
                    playerViewModel.player.addListener(listener)
                    if (playerViewModel.player.isPlaying) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onDispose {
                        playerViewModel.player.removeListener(listener)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Scaffold(
                    topBar = {
                        if (showTopBarActual) {
                            GlassSurface(
                                tonalElevation = 3.dp,
                                border = null,
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                                    RestorationBanner(isOffline)
                                    val isTopBarVisible = uiState.isBarsVisible
                                    val barsProgress = barsVisibilityProgress
                                    if (isTopBarVisible || barsProgress > 0f) {
                                        Box(modifier = Modifier
                                            .fillMaxWidth()
                                            .height(BOTTOM_BAR_HEIGHT * barsProgress)
                                            .clipToBounds()
                                        ) {
                                            PlayTubeTopAppBar(
                                                isIncognitoMode = isIncognitoMode,
                                                currentRoute = currentRoute,
                                                navController = navController,
                                                mainViewModel = mainViewModel,
                                                updateViewModel = updateViewModel,
                                                modifier = Modifier.graphicsLayer {
                                                    translationY = -BOTTOM_BAR_HEIGHT.toPx() * (1f - barsProgress)
                                                    alpha = if (isTopBarVisible) barsProgress else 0f
                                                }
                                            )
                                        }
                                    }
                                    if (isTopBarVisible) HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            Box(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
                                NavGraph(
                                    navController = navController,
                                    startDestination = startDestination,
                                    onBarsVisibilityChange = { mainViewModel.setBarsVisibility(it) }
                                )
                            }
                        }

                        if (showBars || barsVisibilityProgress > 0f) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                                    .navigationBarsPadding()
                                    .graphicsLayer {
                                        translationY = 100.dp.toPx() * (1f - barsVisibilityProgress)
                                        alpha = barsVisibilityProgress
                                    }
                            ) {
                                GlassSurface(
                                    modifier = Modifier.fillMaxWidth().height(BOTTOM_BAR_HEIGHT),
                                    shape = RoundedCornerShape(20.dp),
                                    tonalElevation = 8.dp,
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                ) {
                                    PlayTubeBottomBar(navController = navController)
                                }
                            }
                        }
                    }
                }

                PlayerOverlay(
                    isExpanded = isExpanded,
                    currentVideo = currentVideo,
                    bottomBarHeight = if (showBars) BOTTOM_BAR_HEIGHT * barsVisibilityProgress else 0.dp,
                    isIncognito = isIncognitoMode,
                    viewModel = playerViewModel,
                    navController = navController,
                    onClose = { miniPlayerManager.close { playerViewModel.stopPlayback() } },
                    onMaximize = { miniPlayerManager.maximize() },
                    onMinimize = { currentVideo?.let { miniPlayerManager.minimize(it) } },
                    onChannelClick = { url ->
                        currentVideo?.let { miniPlayerManager.minimize(it) }
                        navController.navigate(Destination.Channel(url))
                    },
                    onVideoClick = { playerViewModel.loadVideo(it) },
                    onAddToPlaylistClick = { mainViewModel.showPlaylistSelection(it) },
                    content = {}
                )

                // Global Offline Notification
                var showBanner by remember { mutableStateOf(false) }
                LaunchedEffect(isOffline) {
                    if (isOffline) {
                        showBanner = true
                        kotlinx.coroutines.delay(5000L)
                        showBanner = false
                    } else showBanner = false
                }

                val isMiniPlayerActive = playerVisibility == com.arslandaim.playtube.ui.screens.player.MiniPlayerVisibility.Minimized
                Box(modifier = Modifier.fillMaxSize().zIndex(200f)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = if (showBars) BOTTOM_BAR_HEIGHT + 24.dp else 16.dp)
                            .then(if (isMiniPlayerActive) Modifier.padding(bottom = 80.dp) else Modifier)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        OfflineBottomBanner(visible = showBanner, onNavigateToDownloads = {
                            navController.navigate(Destination.Downloads) { launchSingleTop = true }
                        })
                    }
                }

                // Global Offline Dialog
                val showOfflineDialog by mainViewModel.showOfflineDialog.collectAsStateWithLifecycle()
                if (showOfflineDialog) {
                    AlertDialog(
                        onDismissRequest = { mainViewModel.dismissOfflineDialog() },
                        icon = { Icon(Icons.Default.WifiOff, null) },
                        title = { Text(stringResource(R.string.no_internet)) },
                        text = { Text(stringResource(R.string.offline_dialog_text)) },
                        confirmButton = {
                            Button(onClick = { 
                                mainViewModel.dismissOfflineDialog()
                                navController.navigate(Destination.Downloads) { launchSingleTop = true }
                            }) { Text(stringResource(R.string.go_to_downloads)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { mainViewModel.dismissOfflineDialog() }) { Text(stringResource(R.string.close)) }
                        }
                    )
                }

                // Global Playlist Selection Sheet
                val playlistSelectionState by mainViewModel.playlistSelectionState.collectAsStateWithLifecycle()
                val localPlaylists by mainViewModel.localPlaylists.collectAsStateWithLifecycle()
                
                if (playlistSelectionState.isVisible) {
                    var showCreateDialog by remember { mutableStateOf(false) }
                    
                    com.arslandaim.playtube.ui.components.AddToPlaylistSheet(
                        playlists = localPlaylists,
                        playlistsWithVideo = playlistSelectionState.playlistsWithVideo,
                        onDismiss = { mainViewModel.hidePlaylistSelection() },
                        onPlaylistSelected = { playlist ->
                            playlistSelectionState.video?.let { video ->
                                mainViewModel.addVideoToPlaylist(playlist.id, video)
                            }
                        },
                        onCreateNewPlaylist = { showCreateDialog = true }
                    )

                    if (showCreateDialog) {
                        var name by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showCreateDialog = false },
                            title = { Text("Create New Playlist") },
                            text = {
                                TextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    placeholder = { Text("Playlist name") },
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (name.isNotBlank()) mainViewModel.createLocalPlaylist(name)
                                        showCreateDialog = false
                                    }
                                ) { Text("Create") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val isInPictureInPictureMode = isInPictureInPictureMode
        val isBackgroundPlayEnabled = mainViewModel.isBackgroundPlayEnabled.value
        if (!isInPictureInPictureMode && !isChangingConfigurations && !isBackgroundPlayEnabled) {
            playerViewModel.player.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            playerViewModel.player.stop()
            playerViewModel.player.clearMediaItems()
            miniPlayerManager.clear()
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        mainViewModel.setPipMode(isInPictureInPictureMode)
        if (!isInPictureInPictureMode && lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED) {
            playerViewModel.player.pause()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (mainViewModel.isPipEnabled.value && playerViewModel.player.isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = intent
    }

    private fun handleDeepLink(intent: Intent, navController: NavHostController) {
        if (intent.getBooleanExtra("OPEN_PLAYER", false)) {
            miniPlayerManager.maximize()
            intent.removeExtra("OPEN_PLAYER")
        }

        val data: Uri = intent.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        val url = data.toString()
        val videoId = com.arslandaim.playtube.utils.VideoUtils.extractVideoId(url)
        val playlistId = com.arslandaim.playtube.utils.VideoUtils.extractPlaylistId(url)
        when {
            url.contains("/playlist?list=") || url.contains("&list=") -> {
                if (playlistId.isNotBlank()) navController.navigate(Destination.Playlist(playlistId)) { launchSingleTop = true }
            }
            url.contains("/channel/") || url.contains("/c/") || url.contains("/user/") || url.contains("/@") -> {
                navController.navigate(Destination.Channel(url)) { launchSingleTop = true }
            }
            url.contains("/results?search_query=") || url.contains("/results?q=") -> {
                val query = data.getQueryParameter("search_query") ?: data.getQueryParameter("q")
                if (!query.isNullOrBlank()) navController.navigate(Destination.Search(query)) { launchSingleTop = true }
            }
            videoId.isNotBlank() -> playerViewModel.loadVideo(VideoItem(id = videoId, title = "Loading...", thumbnailUrl = "", uploaderName = "", uploaderUrl = null, viewCount = 0, uploadDate = null, duration = 0))
        }
        intent.action = null
    }
}

@Composable
fun PlayTubeBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Triple(Destination.Home, Icons.Default.Home, stringResource(R.string.tab_for_you)),
        Triple(Destination.Subscriptions, Icons.Default.Subscriptions, stringResource(R.string.subscriptions)),
        // Provide an empty string as the default argument for the Search route base
        Triple(Destination.Search(""), Icons.Default.Search, stringResource(R.string.search)),
        Triple(Destination.Library, Icons.Default.LibraryMusic, stringResource(R.string.library))
    )
    
    NavigationBar(
        modifier = Modifier.height(MainActivity.BOTTOM_BAR_HEIGHT),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val currentScreen = remember(currentRoute) { currentRoute?.toDestination() }
        
        items.forEach { (destination, icon, label) ->
            // Match the base class to highlight the active tab accurately
            val isSelected = currentScreen != null && currentScreen::class == destination::class
            
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).offset(y = 2.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.offset(y = (-2).dp)) },
                selected = isSelected,
                onClick = {
                    navController.navigate(destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
