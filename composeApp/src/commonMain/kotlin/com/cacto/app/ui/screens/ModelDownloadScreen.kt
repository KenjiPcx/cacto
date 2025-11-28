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
import com.cacto.app.ui.components.GlassCard
import com.cacto.app.ui.components.MonoText
import com.cacto.app.ui.components.NeonButton
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
            .background(Color.Black)
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
            
            MonoText(
                text = "CACTO OS",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CactoGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            MonoText(
                text = "PERSONAL AI MEMORY ASSISTANT",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Download status card
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!downloadState.isDownloading && !downloadState.visionModelReady && downloadState.error == null) {
                        // Initial state - show download button
                        MonoText(
                            text = "DOWNLOAD AI MODELS",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MonoText(
                            text = "Cacto runs 100% on-device. Download models once to run offline forever.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Model list
                        ModelItem(
                            name = "VISION MODEL",
                            description = "lfm2-vl-450m (~1.5GB)",
                            isReady = false
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ModelItem(
                            name = "TEXT MODEL", 
                            description = "qwen3-0.6 (~500MB)",
                            isReady = false
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        NeonButton(
                            text = "START DOWNLOAD (~2GB)",
                            onClick = onStartDownload,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                    } else if (downloadState.isDownloading) {
                        // Downloading state
                        MonoText(
                            text = "DOWNLOADING MODELS...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CactoGreen
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LinearProgressIndicator(
                            color = CactoGreen,
                            trackColor = CactoGreen.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        MonoText(
                            text = downloadState.progress.uppercase(),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        ModelItem(
                            name = "VISION MODEL",
                            description = "lfm2-vl-450m",
                            isReady = downloadState.visionModelReady,
                            isActive = downloadState.currentModel == com.cacto.app.ai.CactusService.VISION_MODEL
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        ModelItem(
                            name = "TEXT MODEL",
                            description = "qwen3-0.6", 
                            isReady = downloadState.textModelReady,
                            isActive = downloadState.currentModel == com.cacto.app.ai.CactusService.TEXT_MODEL
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MonoText(
                            text = "PLEASE KEEP APP OPEN",
                            fontSize = 10.sp,
                            color = CactoPink.copy(alpha = 0.8f)
                        )
                        
                    } else if (downloadState.error != null) {
                        // Error state
                        MonoText(
                            text = "❌ DOWNLOAD FAILED",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        MonoText(
                            text = downloadState.error,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        NeonButton(
                            text = "RETRY DOWNLOAD",
                            onClick = onRetry,
                            color = CactoPink,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    name: String,
    description: String,
    isReady: Boolean,
    isActive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isReady) "✅" else if (isActive) "⏳" else "⭕",
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            MonoText(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReady) CactoGreen else if (isActive) CactoGreen.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.5f)
            )
            MonoText(
                text = description,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}
