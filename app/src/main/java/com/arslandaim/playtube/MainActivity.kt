/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arslandaim.playtube.ui.navigation.NavGraph
import com.arslandaim.playtube.ui.navigation.Screen
import com.arslandaim.playtube.ui.theme.PlayTubeTheme
import dagger.hilt.android.AndroidEntryPoint
import android.app.PictureInPictureParams
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import android.util.Rational
import androidx.core.util.Consumer
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.collectAsState
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.arslandaim.playtube.services.PlaybackService
import com.google.common.util.concurrent.MoreExecutors
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import com.arslandaim.playtube.R
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.screens.player.MiniPlayerManager
import com.arslandaim.playtube.ui.screens.player.PlayerOverlay
import com.arslandaim.playtube.ui.screens.settings.UpdateViewModel
import com.arslandaim.playtube.ui.components.GlassSurface
import com.arslandaim.playtube.utils.ConnectivityObserver
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
    private val playerViewModel: com.arslandaim.playtube.ui.screens.player.PlayerViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()

    private var isPlayerScreen = false
    private var isPipEnabledBySetting = true
    private var isBackgroundPlayEnabledBySetting = false
    private var wasInPip = false
    private var isEnteringPip = false
    private val isInPipModeState = mutableStateOf(value = false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Keep the splash screen on-screen until onboarding completion status is known
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.isOnboardingCompleted.value == null
        }

        // Local Network Protection check (Android 16+ Opt-in, Android 17+ Mandatory)
        if (Build.VERSION.SDK_INT >= 36) {
            val permission = if (Build.VERSION.SDK_INT >= 37) {
                "android.permission.ACCESS_LOCAL_NETWORK"
            } else {
                // In Android 16, this is gated behind NEARBY_WIFI_DEVICES for some implementations
                Manifest.permission.NEARBY_WIFI_DEVICES
            }

            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }

        // Observe Settings from MainViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.isPipEnabled.collect {
                        isPipEnabledBySetting = it
                    }
                }
                launch {
                    mainViewModel.isBackgroundPlayEnabled.collect {
                        isBackgroundPlayEnabledBySetting = it
                    }
                }
                launch {
                    playerViewModel.sleepTimerManager.timerFinishedEvent.collectLatest {
                        if (playerViewModel.sleepTimerManager.shouldCloseApp.value) {
                            finishAndRemoveTask()
                        }
                    }
                }
            }
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()
            val isBackgroundPlayEnabled by mainViewModel.isBackgroundPlayEnabled.collectAsState()
            val isOnboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
            val isOffline by mainViewModel.isOffline.collectAsState()

            if (isOnboardingCompleted == null) return@setContent
            
            // Startup Redirection: If offline on launch, default to Library/Downloads
            val startDestination = remember(isOnboardingCompleted, isOffline) {
                if (isOnboardingCompleted == false) Screen.Onboarding.route
                else if (isOffline) Screen.Library.route
                else Screen.Home.route
            }

            // Connect to MediaSession ONLY if background play is enabled
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
                    controllerFuture.addListener(
                        {
                            // Controller is connected
                        },
                        MoreExecutors.directExecutor()
                    )
                    onDispose {
                        MediaController.releaseFuture(controllerFuture)
                    }
                }
            }

            PlayTubeTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val connectivityStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val playerVisibility by miniPlayerManager.visibilityState.collectAsState()
                val isExpanded = playerVisibility == com.arslandaim.playtube.ui.screens.player.MiniPlayerVisibility.Expanded
                val currentVideo by miniPlayerManager.currentVideo.collectAsState()
                val isIncognitoMode by mainViewModel.isIncognitoMode.collectAsState()

                isPlayerScreen = currentRoute?.startsWith("player") == true
                var isBarsVisible by remember { mutableStateOf(true) }

                val mainRoutes = remember { listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Search.route, Screen.Library.route) }
                val currentBaseRoute = currentRoute?.split("/")?.firstOrNull()
                val isMainRoute = currentBaseRoute in mainRoutes
                val isOnboarding = currentRoute == Screen.Onboarding.route
                
                LaunchedEffect(currentRoute) {
                    if (isMainRoute) {
                        isBarsVisible = true
                    } else if (isOnboarding || (currentRoute?.startsWith("player") == true)) {
                        isBarsVisible = false
                    }
                }

                val showBars = isMainRoute && !isInPipModeState.value && !isOnboarding
                
                // Visual feedback for Incognito Mode
                val incognitoTint = Color(0xFF673AB7).copy(alpha = 0.25f) // Stronger purple tint
                val baseSurfaceColor = MaterialTheme.colorScheme.surface
                val glassColor = if (isIncognitoMode) {
                    baseSurfaceColor.copy(alpha = 0.95f).run {
                        Color(
                            red = (red + incognitoTint.red * incognitoTint.alpha) / (1 + incognitoTint.alpha),
                            green = (green + incognitoTint.green * incognitoTint.alpha) / (1 + incognitoTint.alpha),
                            blue = (blue + incognitoTint.blue * incognitoTint.alpha) / (1 + incognitoTint.alpha),
                            alpha = alpha
                        )
                    }
                } else {
                    baseSurfaceColor.copy(alpha = com.arslandaim.playtube.ui.theme.GlassAlpha)
                }


                // Animate visibility with a natural spring
                val barsVisibilityProgress by animateFloatAsState(
                    targetValue = if (showBars && isBarsVisible) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "BarsVisibility"
                )

                DisposableEffect(Unit) {
                    val consumer = Consumer<Configuration> {
                        isInPipModeState.value = isInPictureInPictureMode
                    }
                    addOnConfigurationChangedListener(consumer)
                    onDispose {
                        removeOnConfigurationChangedListener(consumer)
                    }
                }

                // Handle Screen Wake Lock (Keep screen on while playing)
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
                    
                    // Set initial state
                    if (playerViewModel.player.isPlaying) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }

                    onDispose {
                        playerViewModel.player.removeListener(listener)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Scaffold(
                    topBar = {
                        GlassSurface(
                            tonalElevation = 3.dp,
                            border = null,
                            containerColor = if (isIncognitoMode) glassColor.copy(alpha = 1f) else MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                                val hideBannerRoutes = listOf(
                                    Screen.Home.route,
                                    Screen.Subscriptions.route,
                                    Screen.Search.route
                                )
                                if (currentRoute !in hideBannerRoutes) {
                                    OfflineStatusBar(
                                        status = connectivityStatus,
                                        onNavigateToDownloads = {
                                            navController.navigate(Screen.Downloads.route) {
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                                if (showBars || barsVisibilityProgress > 0f) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BOTTOM_BAR_HEIGHT * barsVisibilityProgress)
                                        .clipToBounds()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    translationY = -BOTTOM_BAR_HEIGHT.toPx() * (1f - barsVisibilityProgress)
                                                    alpha = barsVisibilityProgress
                                                }
                                        ) {
                                            TopAppBar(
                                                title = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(28.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = buildAnnotatedString {
                                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                                    append("Play")
                                                                }
                                                                append("Tube")
                                                            },
                                                            style = MaterialTheme.typography.titleLarge.copy(
                                                                letterSpacing = (-0.5).sp
                                                            ),
                                                            fontWeight = FontWeight.ExtraBold
                                                        )

                                                        if (isIncognitoMode) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Surface(
                                                                color = Color(0xFF9C27B0).copy(alpha = 0.8f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = "INCOGNITO",
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Black,
                                                                        letterSpacing = 1.sp
                                                                    ),
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                },
                                                actions = {
                                                    if (currentRoute != Screen.Search.route) {
                                                        IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Search,
                                                                contentDescription = stringResource(R.string.search),
                                                                tint = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                    
                                                    val updateInfo by updateViewModel.updateInfo.collectAsState()
                                                    val isAutoUpdateEnabled by updateViewModel.isAutoUpdateEnabled.collectAsState()

                                                    IconButton(onClick = { mainViewModel.toggleIncognitoMode() }) {
                                                        Icon(
                                                            imageVector = if (isIncognitoMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                            contentDescription = "Incognito Mode",
                                                            tint = if (isIncognitoMode) Color(0xFF9C27B0) else MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                                        BadgedBox(
                                                            badge = {
                                                                if (isAutoUpdateEnabled && updateInfo.hasUpdate) {
                                                                    Badge(
                                                                        containerColor = MaterialTheme.colorScheme.error,
                                                                        contentColor = MaterialTheme.colorScheme.onError
                                                                    ) {
                                                                        Text("!")
                                                                    }
                                                                }
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Settings,
                                                                contentDescription = stringResource(R.string.settings),
                                                                tint = MaterialTheme.colorScheme.onSurface,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = Color.Transparent,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Content Area
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // Only apply top padding from scaffold, content flows to the bottom edge
                            Box(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
                                NavGraph(
                                    navController = navController,
                                    startDestination = startDestination,
                                    onBarsVisibilityChange = { isBarsVisible = it }
                                )
                            }
                        }

                        // Modern Floating Glass Bottom Bar
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(BOTTOM_BAR_HEIGHT),
                                    shape = RoundedCornerShape(20.dp),
                                    tonalElevation = 8.dp,
                                    shadowElevation = 12.dp,
                                    containerColor = glassColor
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
                    onClose = {
                        miniPlayerManager.close {
                            playerViewModel.stopPlayback()
                        }
                    },
                    onMaximize = { miniPlayerManager.maximize() },
                    onMinimize = { miniPlayerManager.minimize(currentVideo!!) },
                    onChannelClick = { channelUrl ->
                        miniPlayerManager.minimize(currentVideo!!)
                        navController.navigate(Screen.Channel.createRoute(channelUrl))
                    },
                    onVideoClick = { video ->
                        playerViewModel.loadVideo(video)
                    },
                    content = { /* PlayerView is handled inside PlayerOverlay/PlayerScreen for now(I will do something later) */ }
                )

                // Global Offline Dialog for Startup
                val showOfflineDialog by mainViewModel.showOfflineDialog.collectAsState()
                if (showOfflineDialog) {
                    AlertDialog(
                        onDismissRequest = { mainViewModel.dismissOfflineDialog() },
                        icon = { Icon(Icons.Default.WifiOff, null, tint = MaterialTheme.colorScheme.primary) },
                        title = { Text(stringResource(R.string.no_internet)) },
                        text = { Text("You are currently offline. Checkout your downloads.") },
                        confirmButton = {
                            Button(onClick = { 
                                mainViewModel.dismissOfflineDialog()
                                navController.navigate(Screen.Downloads.route) {
                                    launchSingleTop = true
                                }
                            }) {
                                Text("Go to Downloads")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { mainViewModel.dismissOfflineDialog() }) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Determine if we should pause.
        // We pause IF:
        // 1. We are NOT in PiP mode
        // 2. We are NOT rotating (changing configs)
        // 3. Background Play is DISABLED
        // 4. We are NOT currently entering PiP
        val isInPictureInPictureMode = isInPictureInPictureMode
        val shouldPause = !isInPictureInPictureMode && !isChangingConfigurations && 
                         !isBackgroundPlayEnabledBySetting && !isEnteringPip
        
        if (shouldPause) {
            playerViewModel.player.pause()
        }
        isEnteringPip = false // Reset after handling pause
    }

    override fun onStop() {
        super.onStop()
        // If the activity is finishing, stop the player and clear media.
        if (isFinishing) {
            playerViewModel.player.stop()
            playerViewModel.player.clearMediaItems()
            miniPlayerManager.clear()
            stopService(Intent(this, PlaybackService::class.java))
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        android.util.Log.d("PiP", "onPictureInPictureModeChanged: isInPip=$isInPictureInPictureMode")
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            wasInPip = true
        } else {
            // If we were in PiP and the app is currently in the background (CREATED state),
            // it means the user explicitly closed the PiP window.
            if (wasInPip && lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED) {
                android.util.Log.d("PiP", "PiP closed by user, pausing playback")
                playerViewModel.player.pause()
            }
            wasInPip = false
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        android.util.Log.d("PiP", "onUserLeaveHint: isPipEnabledBySetting=$isPipEnabledBySetting, isPlaying=${playerViewModel.player.isPlaying}")
        
        if (isPipEnabledBySetting && playerViewModel.player.isPlaying) {
            isEnteringPip = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                
                android.util.Log.d("PiP", "Entering Picture-in-Picture mode")
                enterPictureInPictureMode(params)
            }
        }
    }
}

@Composable
fun PlayTubeBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Triple(Screen.Home, Icons.Default.Home, stringResource(R.string.tab_for_you)),
        Triple(Screen.Subscriptions, Icons.Default.Subscriptions, stringResource(R.string.subscriptions)),
        Triple(Screen.Search, Icons.Default.Search, stringResource(R.string.search)),
        Triple(Screen.Library, Icons.Default.LibraryMusic, stringResource(R.string.library))
    )
    NavigationBar(
        modifier = Modifier.height(MainActivity.BOTTOM_BAR_HEIGHT),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { (screen, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).offset(y = 2.dp)) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.offset(y = (-2).dp)) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
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

// Legacy MiniPlayer removed

@Composable
fun OfflineStatusBar(
    status: ConnectivityObserver.Status,
    onNavigateToDownloads: () -> Unit = {}
) {
    val isOffline = status == ConnectivityObserver.Status.Lost || status == ConnectivityObserver.Status.Unavailable
    
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable { onNavigateToDownloads() }
                .padding(vertical = 6.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.no_internet) + ". Tap to view downloads.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
