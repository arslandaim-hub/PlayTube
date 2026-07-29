/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

/**
 * Shared infinite transition for all shimmer effects to ensure synchronization
 * and reduce CPU overhead from multiple independent animation timers.
 */
@Composable
fun rememberSyncShimmerTransition(): InfiniteTransition {
    return rememberInfiniteTransition(label = "shimmerSync")
}

fun Modifier.shimmerEffect(
    transition: InfiniteTransition? = null
): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    
    // Use provided transition or fallback to a local one (less efficient)
    val actualTransition = transition ?: rememberInfiniteTransition(label = "shimmerLocal")
    
    val startOffsetX by actualTransition.animateFloat(
        initialValue = -2 * size.width,
        targetValue = 2 * size.width,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width, size.height)
        )
    ).onGloballyPositioned {
        size = it.size.toSize()
    }
}

@Composable
fun VideoCardSkeleton(transition: InfiniteTransition? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp))
                .shimmerEffect(transition)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .shimmerEffect(transition)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Title placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(transition)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Metadata placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(transition)
                )
            }
        }
    }
}

@Composable
fun VideoListSkeleton() {
    val transition = rememberSyncShimmerTransition()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        items(5) {
            VideoCardSkeleton(transition)
        }
    }
}

@Composable
fun PlayerMetadataSkeleton(transition: InfiniteTransition? = null) {
    val actualTransition = transition ?: rememberSyncShimmerTransition()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.7f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect(actualTransition)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Micro-stats row
            Row {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect(actualTransition)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(
                thickness = 0.5.dp, 
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Channel Section
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .shimmerEffect(actualTransition)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(actualTransition)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect(actualTransition)
                    )
                }
                
                // Subscribe Button Placeholder
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .shimmerEffect(actualTransition)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action Row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .shimmerEffect(actualTransition)
                    )
                }
            }
        }
    }
}
