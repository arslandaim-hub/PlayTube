/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components.main

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arslandaim.playtube.R
import com.arslandaim.playtube.ui.navigation.Screen

@Composable
fun PlayTubeBottomBar(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple(Screen.Home, Icons.Default.Home, stringResource(R.string.tab_for_you)),
        Triple(Screen.Subscriptions, Icons.Default.Subscriptions, stringResource(R.string.subscriptions)),
        Triple(Screen.Search, Icons.Default.Search, stringResource(R.string.search)),
        Triple(Screen.Library, Icons.Default.LibraryMusic, stringResource(R.string.library))
    )
    NavigationBar(
        modifier = modifier,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { (screen, icon, label) ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = icon, 
                        contentDescription = null, 
                        modifier = Modifier.size(20.dp).offset(y = 2.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = label, 
                        style = MaterialTheme.typography.labelSmall, 
                        modifier = Modifier.offset(y = (-2).dp)
                    ) 
                },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    if (currentDestination?.route != screen.route) {
                        navController.navigate(screen.navigationRoute) {
                            try {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            } catch (e: Exception) {
                                // Fallback if graph is not yet available or corrupted
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
