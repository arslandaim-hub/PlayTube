/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.arslandaim.playtube.MainViewModel
import com.arslandaim.playtube.R
import com.arslandaim.playtube.ui.navigation.Screen
import com.arslandaim.playtube.ui.screens.settings.UpdateViewModel
import com.arslandaim.playtube.ui.theme.IncognitoPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayTubeTopAppBar(
    isIncognitoMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
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
                        color = IncognitoPurple.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.incognito_label),
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
                IconButton(onClick = { navController.navigate(Screen.Search.navigationRoute) }) {
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
                    tint = MaterialTheme.colorScheme.onSurface,
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
