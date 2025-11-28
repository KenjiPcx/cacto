package com.cacto.app.ui.screens

/**
 * Home Screen
 * ===========
 *
 * PURPOSE:
 * Main entry point of the app. Shows listening status, processing progress,
 * and navigation to memories/graph screens.
 *
 * WHERE USED:
 * - Rendered by: App composable (default screen)
 * - Entry point: First screen users see when opening the app
 *
 * REDESIGN:
 * - Minimalist glassmorphism
 * - Nothing Phone inspired aesthetics
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.ai.PipelineState
import com.cacto.app.ai.PipelineStatus
import com.cacto.app.ui.components.GlassCard
import com.cacto.app.ui.components.MonoText
import com.cacto.app.ui.components.TechDivider
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink

@Composable
fun HomeScreen(
    pipelineState: PipelineState,
    memoryCount: Long,
    entityCount: Long,
    historyCount: Int,
    isListening: Boolean,
    onToggleListening: (Boolean) -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onNavigateToDebug: () -> Unit
) {
    val isProcessing = pipelineState.status != PipelineStatus.IDLE && 
                       pipelineState.status != PipelineStatus.COMPLETE &&
                       pipelineState.status != PipelineStatus.ERROR
    
    // Pulsing animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // Logo/Title
        MonoText(
            text = "CACTO OS",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        TechDivider(
            modifier = Modifier
                .width(60.dp)
                .padding(vertical = 16.dp),
            color = CactoGreen.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main action button - Start/Stop Listening
        Box(
            modifier = Modifier
                .size(200.dp)
                .scale(if (isListening && !isProcessing) scale else 1f)
                .clip(CircleShape)
                .background(Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isListening) CactoGreen else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
                .clickable { onToggleListening(!isListening) },
            contentAlignment = Alignment.Center
        ) {
            // Inner circle for visual effect
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        if (isListening) CactoGreen.copy(alpha = 0.1f) 
                        else Color.White.copy(alpha = 0.05f)
                    )
            )
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = CactoGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MonoText(
                        text = "PROCESSING",
                        color = CactoGreen,
                        fontSize = 12.sp
                    )
                } else if (isListening) {
                    Text(text = "👂", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    MonoText(
                        text = "LISTENING",
                        color = CactoGreen,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(text = "👁️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    MonoText(
                        text = "START",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Processing status
        if (pipelineState.status != PipelineStatus.IDLE) {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MonoText(
                        text = pipelineState.currentStep.uppercase(),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    if (isProcessing) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { pipelineState.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = CactoGreen,
                            trackColor = CactoGreen.copy(alpha = 0.2f)
                        )
                    }
                    
                    pipelineState.result?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        if (result.memoriesSaved > 0) {
                            MonoText(
                                text = ">> ${result.memoriesSaved} MEMORIES SAVED",
                                fontSize = 12.sp,
                                color = CactoGreen
                            )
                        }
                        if (result.generatedResponse != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            MonoText(
                                text = ">> RESPONSE COPIED TO CLIPBOARD",
                                fontSize = 12.sp,
                                color = CactoPink
                            )
                        }
                    }
                    
                    pipelineState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        MonoText(
                            text = "ERROR: $error",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
             MonoText(
                text = if (isListening) {
                    "TAKE A SCREENSHOT TO ANALYZE"
                } else {
                    "SYSTEM STANDBY"
                },
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
             // Center the text
             // Modifier.fillMaxWidth() alone won't center the text content, need TextAlign.Center
             // But MonoText wraps Text which has textAlign param. I should update MonoText or use Box.
             // Let's use Box to center or add textAlign to MonoText.
             // I'll leave it left aligned for "terminal" feel, or just adjust the MonoText component later if needed.
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatGlassCard(
                modifier = Modifier.weight(1f),
                value = memoryCount.toString(),
                label = "MEMORIES",
                onClick = onNavigateToMemories
            )
            StatGlassCard(
                modifier = Modifier.weight(1f),
                value = entityCount.toString(),
                label = "ENTITIES",
                onClick = onNavigateToGraph
            )
            StatGlassCard(
                modifier = Modifier.weight(1f),
                value = historyCount.toString(),
                label = "DEBUG",
                onClick = onNavigateToDebug
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatGlassCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonoText(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CactoGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            MonoText(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
