package com.cacto.app.ui.screens

/**
 * Home Screen
 * ===========
 *
 * PURPOSE:
 * Main entry point of the app. Shows processing progress and navigation.
 *
 * WHERE USED:
 * - Rendered by: App composable (default screen)
 * - Entry point: First screen users see when opening the app
 *
 * REDESIGN:
 * - Minimalist glassmorphism
 * - Nothing Phone inspired aesthetics (Dot matrix, technical grid)
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Header - Dot Matrix Style
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(CactoGreen, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MonoText(
                text = "CACTO OS",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            MonoText(
                text = "v1.0",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        
        TechDivider(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 32.dp),
            color = Color.White.copy(alpha = 0.1f)
        )
        
        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Central Status Display
            StatusOrb(
                isProcessing = isProcessing,
                status = pipelineState.status
            )
        }
        
        // Processing Status Overlay
        if (pipelineState.status != PipelineStatus.IDLE) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                backgroundColor = Color(0xFF0A0A0A).copy(alpha = 0.8f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                   Column(modifier = Modifier.weight(1f)) {
                       MonoText(
                           text = pipelineState.currentStep.uppercase(),
                           fontSize = 10.sp,
                           color = CactoGreen
                       )
                       Spacer(modifier = Modifier.height(4.dp))
                       if (isProcessing) {
                           LinearProgressIndicator(
                               progress = { pipelineState.progress },
                               modifier = Modifier.fillMaxWidth(),
                               color = CactoGreen,
                               trackColor = CactoGreen.copy(alpha = 0.2f),
                               strokeCap = androidx.compose.ui.graphics.StrokeCap.Square
                           )
                       } else {
                           MonoText(
                               text = when(pipelineState.status) {
                                   PipelineStatus.COMPLETE -> "PROCESSING COMPLETE"
                                   PipelineStatus.ERROR -> "ERROR OCCURRED"
                                   else -> ""
                               },
                               fontSize = 12.sp,
                               color = Color.White
                           )
                       }
                   }
                }
            }
        } else {
            // Instructions when idle
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                backgroundColor = Color(0xFF0A0A0A).copy(alpha = 0.5f)
            ) {
                MonoText(
                    text = "SHARE A SCREENSHOT TO ANALYZE",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Bottom Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatWidget(
                modifier = Modifier.weight(1f),
                value = memoryCount.toString(),
                label = "MEMORIES",
                icon = null,
                color = CactoGreen,
                onClick = onNavigateToMemories
            )
            StatWidget(
                modifier = Modifier.weight(1f),
                value = entityCount.toString(),
                label = "ENTITIES",
                icon = Icons.Filled.Share,
                color = CactoPink,
                onClick = onNavigateToGraph
            )
            StatWidget(
                modifier = Modifier.weight(1f),
                value = historyCount.toString(),
                label = "DEBUG",
                icon = Icons.Filled.Settings,
                color = Color.White,
                onClick = onNavigateToDebug
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatusOrb(
    isProcessing: Boolean,
    status: PipelineStatus
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer dashed ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val radius = size.minDimension / 2 - strokeWidth
            val center = Offset(size.width / 2, size.height / 2)
            
            if (isProcessing) {
                rotate(rotation, center) {
                    drawCircle(
                        color = CactoGreen.copy(alpha = 0.3f),
                        radius = radius,
                        center = center,
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                        )
                    )
                }
            } else {
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
        
        // Inner pulsing orb
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isProcessing) CactoGreen.copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .border(
                    width = 1.dp,
                    color = if (isProcessing) CactoGreen else Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
             when {
                 isProcessing -> Icon(
                     imageVector = Icons.Filled.Refresh,
                     contentDescription = "Processing",
                     tint = CactoGreen,
                     modifier = Modifier.size(48.dp)
                 )
                 status == PipelineStatus.COMPLETE -> Icon(
                     imageVector = Icons.Filled.CheckCircle,
                     contentDescription = "Complete",
                     tint = CactoGreen,
                     modifier = Modifier.size(48.dp)
                 )
                 status == PipelineStatus.ERROR -> Icon(
                     imageVector = Icons.Filled.Warning,
                     contentDescription = "Error",
                     tint = Color.White.copy(alpha = 0.5f),
                     modifier = Modifier.size(48.dp)
                 )
                 else -> {
                     // Use text instead of icon for standby
                     MonoText(
                         text = "●",
                         fontSize = 48.sp,
                         color = Color.White.copy(alpha = 0.3f)
                     )
                 }
             }
        }
        
        // Status Text
        Box(
            modifier = Modifier.offset(y = 100.dp)
        ) {
            MonoText(
                text = when {
                    isProcessing -> "PROCESSING"
                    status == PipelineStatus.COMPLETE -> "READY"
                    status == PipelineStatus.ERROR -> "ERROR"
                    else -> "STANDBY"
                },
                color = if (isProcessing) CactoGreen else Color.White.copy(alpha = 0.3f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatWidget(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector?,
    color: Color,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier.aspectRatio(1f),
        onClick = onClick,
        backgroundColor = Color(0xFF111111).copy(alpha = 0.5f),
        borderColor = Color.White.copy(alpha = 0.05f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Use a simple dot indicator for memories
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(color.copy(alpha = 0.8f), CircleShape)
                )
            }
            
            Column {
                MonoText(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                MonoText(
                    text = label,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}
