package com.cacto.app.ui.screens

/**
 * Model Download Screen
 * =====================
 *
 * PURPOSE:
 * Displays model download progress on first app launch. Shows download status
 * for both vision (lfm2-vl-450m) and text (qwen3-0.6) models.
 *
 * WHERE USED:
 * - Rendered by: App composable when models aren't downloaded
 * - Entry point: First screen on fresh install
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.ai.ModelDownloadState
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink

@Composable
fun ModelDownloadScreen(
    downloadState: ModelDownloadState,
    onStartDownload: () -> Unit,
    onRetry: () -> Unit
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
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
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated cactus
            Text(
                text = "🌵",
                fontSize = 80.sp,
                modifier = Modifier.scale(if (downloadState.isDownloading) scale else 1f)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Cacto",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = CactoGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Personal AI Memory Assistant",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Download status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!downloadState.isDownloading && !downloadState.visionModelReady && downloadState.error == null) {
                        // Initial state - show download button
                        Text(
                            text = "Download AI Models",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Cacto needs to download AI models to work offline.\nThis may take a few minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Model list
                        ModelItem(
                            name = "Vision Model",
                            description = "lfm2-vl-450m - Analyzes screenshots",
                            isReady = false
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ModelItem(
                            name = "Text Model", 
                            description = "qwen3-0.6 - Generates responses",
                            isReady = false
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = onStartDownload,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CactoGreen
                            )
                        ) {
                            Text("Download Models (~500MB)")
                        }
                        
                    } else if (downloadState.isDownloading) {
                        // Downloading state
                        Text(
                            text = "Downloading Models...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        CircularProgressIndicator(
                            color = CactoGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = downloadState.progress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ModelItem(
                            name = "Vision Model",
                            description = "lfm2-vl-450m",
                            isReady = downloadState.visionModelReady
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        ModelItem(
                            name = "Text Model",
                            description = "qwen3-0.6", 
                            isReady = downloadState.textModelReady
                        )
                        
                    } else if (downloadState.error != null) {
                        // Error state
                        Text(
                            text = "❌ Download Failed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = downloadState.error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CactoPink
                            )
                        ) {
                            Text("Retry Download")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Models run 100% on-device.\nNo internet needed after download.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ModelItem(
    name: String,
    description: String,
    isReady: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isReady) "✅" else "⏳",
            fontSize = 20.sp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isReady) CactoGreen else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

