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
import com.arslandaim.playtube.utils.ConnectivityObserver
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var miniPlayerManager: MiniPlayerManager
    
    private val mainViewModel: MainViewModel by viewModels()
    private val playerViewModel: com.arslandaim.playtube.ui.screens.player.PlayerViewModel by viewModels()

    private var isPlayerScreen = false
    private var isPipEnabledBySetting = true
    private var isBackgroundPlayEnabledBySetting = false
    private var wasInPip = false
    private var isEnteringPip = false
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            }
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()

            PlayTubeTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                val connectivityStatus by connectivityObserver.observe().collectAsState(initial = ConnectivityObserver.Status.Available)

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isExpanded by miniPlayerManager.isExpanded.collectAsState()
                val currentVideo by miniPlayerManager.currentVideo.collectAsState()

                isPlayerScreen = currentRoute?.startsWith("player") == true
                var isBarsVisible by remember { mutableStateOf(true) }

                val mainRoutes = remember { listOf(Screen.Home.route, Screen.Subscriptions.route, Screen.Library.route) }
                val isMainRoute = currentRoute in mainRoutes
                
                LaunchedEffect(currentRoute) {
                    if (isMainRoute) {
                        isBarsVisible = true
                    }
                }

                val showBars = isMainRoute && !isInPipModeState.value
                
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            tonalElevation = 0.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
                                OfflineStatusBar(status = connectivityStatus)
                                if (showBars || barsVisibilityProgress > 0f) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp * barsVisibilityProgress)
                                        .clipToBounds()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    translationY = -64.dp.toPx() * (1f - barsVisibilityProgress)
                                                    alpha = barsVisibilityProgress
                                                }
                                        ) {
                                            TopAppBar(
                                                title = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = Color.Red,
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
                                                    }
                                                },
                                                actions = {
                                                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = stringResource(R.string.search),
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = stringResource(R.string.settings),
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(24.dp)
                                                        )
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
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(20.dp),
                                    tonalElevation = 8.dp,
                                    shadowElevation = 12.dp,
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
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
                    bottomBarHeight = 64.dp * barsVisibilityProgress,
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
        // We removed the !isInPictureInPictureMode check here because it was too aggressive,
        // causing playback to reset when the app was simply minimized.
        if (isFinishing) {
            playerViewModel.player.stop()
            playerViewModel.player.clearMediaItems()
            miniPlayerManager.clear()
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
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                
                android.util.Log.d("PiP", "Entering Picture-in-Picture mode")
                enterPictureInPictureMode(params)
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.util.Log.d("PiP", "Entering Picture-in-Picture mode (Legacy)")
                @Suppress("DEPRECATION")
                enterPictureInPictureMode()
            }
        }
    }
}

@Composable
fun PlayTubeBottomBar(navController: androidx.navigation.NavHostController) {
    val items = listOf(
        Screen.Home to Icons.Default.Home,
        Screen.Subscriptions to Icons.Default.Subscriptions,
        Screen.Library to Icons.Default.LibraryMusic
    )
    NavigationBar(
        modifier = Modifier.height(64.dp),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { (screen, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp).offset(y = 2.dp)) },
                label = { Text(screen.route, style = MaterialTheme.typography.labelSmall, modifier = Modifier.offset(y = (-2).dp)) },
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
fun OfflineStatusBar(status: ConnectivityObserver.Status) {
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
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_internet),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
