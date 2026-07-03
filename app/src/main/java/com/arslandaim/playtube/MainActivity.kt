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
import coil3.compose.AsyncImage
import com.arslandaim.playtube.domain.model.VideoItem
import com.arslandaim.playtube.ui.screens.player.MiniPlayerManager
import com.arslandaim.playtube.utils.ConnectivityObserver
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectivityObserver: ConnectivityObserver
    @Inject lateinit var miniPlayerManager: MiniPlayerManager
    @Inject lateinit var preferencesManager: com.arslandaim.playtube.data.local.PreferencesManager
    
    private val playerViewModel: com.arslandaim.playtube.ui.screens.player.PlayerViewModel by viewModels()

    private var isPlayerScreen = false
    private var isPipEnabledBySetting = true
    private var wasInPip = false
    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe PiP setting
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                preferencesManager.isPipEnabled.collect {
                    isPipEnabledBySetting = it
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
                val isMinimized by miniPlayerManager.isMinimized.collectAsState()
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

                Scaffold(
                    topBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
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
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = buildAnnotatedString {
                                                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                                                                    append("Play")
                                                                }
                                                                append("Tube")
                                                            },
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                },
                                                actions = {
                                                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Search,
                                                            contentDescription = "Search",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Settings,
                                                            contentDescription = "Settings",
                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.size(26.dp)
                                                        )
                                                    }
                                                },
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = MaterialTheme.colorScheme.surface,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                                // MiniPlayer stays stable above navigation bars
                                val showMiniPlayer = isMinimized && currentVideo != null && !isInPipModeState.value && !isPlayerScreen
                                AnimatedVisibility(
                                    visible = showMiniPlayer,
                                    enter = slideInVertically(
                                        initialOffsetY = { it },
                                        animationSpec = tween(400)
                                    ) + fadeIn(animationSpec = tween(400)),
                                    exit = slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(400)
                                    ) + fadeOut(animationSpec = tween(400))
                                ) {
                                    Column {
                                        MiniPlayer(
                                            video = currentVideo ?: return@Column,
                                            onClick = {
                                                val video = currentVideo ?: return@MiniPlayer
                                                miniPlayerManager.maximize()
                                                navController.navigate(Screen.Player.createRoute(video.id, video.title, video.thumbnailUrl)) {
                                                    launchSingleTop = true
                                                }
                                            },
                                            onDismiss = { 
                                                miniPlayerManager.close {
                                                    // Stop playback when mini-player is closed
                                                    playerViewModel.player.stop()
                                                    playerViewModel.player.clearMediaItems()
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                                // Animated height Box for the bottom navigation bar
                                if (showBars || barsVisibilityProgress > 0f) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp * barsVisibilityProgress)
                                        .clipToBounds()
                                    ) {
                                        Box(modifier = Modifier.graphicsLayer {
                                            translationY = 64.dp.toPx() * (1f - barsVisibilityProgress)
                                            alpha = barsVisibilityProgress
                                        }) {
                                            PlayTubeBottomBar(navController = navController)
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // Scaffold's innerPadding will now animate smoothly as topBar and bottomBar heights change.
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        NavGraph(
                            navController = navController,
                            onBarsVisibilityChange = { isBarsVisible = it }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // If not in PiP mode, pause the player.
        // This stops audio from playing in the background when minimized.
        if (!isInPictureInPictureMode) {
            playerViewModel.player.pause()
        }
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
        isInPipModeState.value = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            wasInPip = true
        } else {
            // If we were in PiP and the app is currently in the background (CREATED state),
            // it means the user explicitly closed the PiP window.
            if (wasInPip && lifecycle.currentState == androidx.lifecycle.Lifecycle.State.CREATED) {
                // Pause instead of stop to allow resumption when reopening the app
                playerViewModel.player.pause()
            }
            wasInPip = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final safety cleanup
        if (isFinishing) {
            playerViewModel.player.stop()
            playerViewModel.player.clearMediaItems()
            miniPlayerManager.clear()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerScreen && isPipEnabledBySetting && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
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
        modifier = Modifier.height(64.dp)
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

@Composable
fun MiniPlayer(
    video: VideoItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(64.dp)
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetX > 200f || offsetX < -200f) {
                            onDismiss()
                        } else {
                            offsetX = 0f
                        }
                    },
                    onDragCancel = { offsetX = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Medium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.uploaderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

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
                text = "No Internet Connection",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
