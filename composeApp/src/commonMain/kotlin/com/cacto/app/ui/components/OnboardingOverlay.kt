package com.cacto.app.ui.components

/**
 * Onboarding Overlay
 * ==================
 *
 * PURPOSE:
 * Displays a multi-step onboarding guide using the glassmorphism aesthetic.
 * Explains how to use the app (Snapshot -> Share -> Cacto).
 *
 * WHERE USED:
 * - Used in: App.kt (overlaid on top of HomeScreen on first launch)
 *
 * DESIGN PHILOSOPHY:
 * - Fun, engaging, and clear
 * - "Nothing Phone" aesthetic (dot matrix, neon, glass)
 * - Modal interaction
 */

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: String
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit
) {
    var currentStepIndex by remember { mutableStateOf(0) }
    
    val steps = listOf(
        OnboardingStep(
            title = "WELCOME TO CACTO",
            description = "Your second brain for the digital desert. I remember what you see and help you act on it.",
            icon = "🌵"
        ),
        OnboardingStep(
            title = "SNAP IT",
            description = "See something interesting? A conversation, a product, a memory? Just take a screenshot.",
            icon = "📸"
        ),
        OnboardingStep(
            title = "SHARE IT",
            description = "Share that screenshot directly to Cacto. I'll analyze it instantly.",
            icon = "🚀"
        ),
        OnboardingStep(
            title = "MAGIC HAPPENS",
            description = "I'll extract memories for your knowledge graph or generate a witty reply for you to paste.",
            icon = "✨"
        )
    )

    // Dark overlay background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .clickable(enabled = false) {} // Block clicks
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF121212).copy(alpha = 0.9f),
            borderColor = CactoGreen.copy(alpha = 0.3f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                // Progress Dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(8.dp)
                                .background(
                                    color = if (index == currentStepIndex) CactoGreen else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Content with animation
                AnimatedContent(
                    targetState = currentStepIndex,
                    transitionSpec = {
                        fadeIn() + slideInHorizontally { it } with fadeOut() + slideOutHorizontally { -it }
                    }
                ) { stepIndex ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = steps[stepIndex].icon,
                            fontSize = 64.sp
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        MonoText(
                            text = steps[stepIndex].title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = CactoGreen
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MonoText(
                            text = steps[stepIndex].description,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Center text manually since MonoText wraps it
                        Text(
                            text = "", // Empty text just to force layout update if needed, but actually MonoText modifiers work
                            modifier = Modifier.height(0.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStepIndex < steps.size - 1) {
                        // Skip button
                        Text(
                            text = "SKIP",
                            color = Color.White.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(16.dp)
                        )
                        
                        // Next button
                        NeonButton(
                            text = "NEXT ->",
                            onClick = { currentStepIndex++ }
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        // Finish button
                        NeonButton(
                            text = "START_OS",
                            onClick = { onDismiss() },
                            color = CactoPink
                        )
                    }
                }
            }
        }
    }
}

