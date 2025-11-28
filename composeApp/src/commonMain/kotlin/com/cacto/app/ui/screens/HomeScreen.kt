package com.cacto.app.ui.screens

/**
 * Home Screen
 * ===========
 *
 * PURPOSE:
 * Main entry point of the app. Displays processing status, statistics, and
 * navigation to other screens. Shows real-time pipeline processing state with
 * progress indicators and results.
 *
 * WHERE USED:
 * - Rendered by: App composable (default screen)
 * - Navigated to: From MemoriesScreen and KnowledgeGraphScreen
 * - Entry point: First screen users see when opening the app
 *
 * RELATIONSHIPS:
 * - Observes: CactoPipeline.state for processing status
 * - Displays: Memory and entity counts from repositories
 * - Navigates to: MemoriesScreen, KnowledgeGraphScreen
 * - Shows: Pipeline processing progress and results
 *
 * USAGE IN APPLICATION FLOW:
 * - Default screen when app launches
 * - Displays processing status during screenshot analysis
 * - Shows statistics (memory count, entity count) as clickable cards
 * - Provides visual feedback during pipeline execution
 * - Animated pulsing button indicates readiness for screenshots
 *
 * DESIGN PHILOSOPHY:
 * Central hub for app navigation and status display. Uses animated transitions
 * for better UX. Separates status display from navigation logic. Shows real-time
 * pipeline state for transparency. Stat cards provide quick access to other screens.
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.ai.PipelineState
import com.cacto.app.ai.PipelineStatus
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink

@Composable
fun HomeScreen(
    pipelineState: PipelineState,
    memoryCount: Long,
    entityCount: Long,
    onNavigateToMemories: () -> Unit,
    onNavigateToGraph: () -> Unit
) {
    val isProcessing = pipelineState.status != PipelineStatus.IDLE && 
                       pipelineState.status != PipelineStatus.COMPLETE &&
                       pipelineState.status != PipelineStatus.ERROR
    
    // Pulsing animation for the main button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Logo/Title
            Text(
                text = "🌵",
                fontSize = 64.sp
            )
            
            Text(
                text = "Cacto",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = CactoGreen
            )
            
            Text(
                text = "Your Personal AI Memory Assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Main action button (visual only - actual processing from share intent)
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(if (!isProcessing) scale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CactoGreen, CactoGreen.copy(alpha = 0.7f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing...",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Text(
                            text = "📸",
                            fontSize = 48.sp
                        )
                        Text(
                            text = "Share a\nScreenshot",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Take a screenshot and share it to Cacto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            
            // Status display
            if (pipelineState.status != PipelineStatus.IDLE) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = pipelineState.currentStep,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (isProcessing) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { pipelineState.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        
                        pipelineState.result?.let { result ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "✅ ${result.memoriesSaved} memories saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = CactoGreen
                            )
                            if (result.generatedResponse != null) {
                                Text(
                                    text = "💬 Response generated",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CactoPink
                                )
                            }
                        }
                        
                        pipelineState.error?.let { error ->
                            Text(
                                text = "❌ $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Stats cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🧠",
                    value = memoryCount.toString(),
                    label = "Memories",
                    onClick = onNavigateToMemories
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔗",
                    value = entityCount.toString(),
                    label = "Entities",
                    onClick = onNavigateToGraph
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: String,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

