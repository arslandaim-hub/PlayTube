/*
 * PlayTube Project Original (2026)
 * arslandaim-hub (GitHub.com/arslandaim-hub)
 * Licenced Under GPL-3.0+
*/
package com.arslandaim.playtube.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import com.arslandaim.playtube.ui.screens.library.GlobalGlassAlpha

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsSelectionScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.onboardingEvents) {
        viewModel.onboardingEvents.collectLatest { event ->
            when (event) {
                OnboardingViewModel.OnboardingEvent.NavigateToHome -> {
                    onComplete()
                }
            }
        }
    }

    val interests = remember {
        mapOf(
            "Technology" to listOf("Technology", "Programming", "Coding", "AI", "Machine Learning", "Linux", "Software", "Cybersecurity", "Android", "App development"),
            "Education" to listOf("English", "Spanish", "Study tips", "College", "Tutorials", "Philosophy", "University", "Finance", "Economics", "Marketing", "Business", "History"),
            "Health" to listOf("Mental Health", "Meditation", "Self Improvement", "Productivity", "Motivation", "Sports", "Football", "Soccer", "Fitness", "Gym"),
            "Science & Nature" to listOf("Animals", "Astronomy", "Nature", "Climate", "Geology", "Engineering", "Inventions"),
            "News" to listOf("World News", "Tech news", "Sports News", "Entertainment News", "Current events", "Debates", "Politics News")
        )
    }

    val selectedInterests = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = GlobalGlassAlpha)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome to PlayTube",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    TextButton(
                        onClick = {
                            isLoading = true
                            viewModel.skipOnboarding()
                        },
                        enabled = !isLoading
                    ) {
                        Text("Skip", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = GlobalGlassAlpha),
                tonalElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            isLoading = true
                            viewModel.saveInterests(selectedInterests.toList())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = selectedInterests.isNotEmpty() && !isLoading,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Continue", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Select your interests to personalize your feed",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            interests.forEach { (category, subcategories) ->
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subcategories.forEach { sub ->
                        val isSelected = selectedInterests.contains(sub)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedInterests.remove(sub)
                                else selectedInterests.add(sub)
                            },
                            label = { Text(sub) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
