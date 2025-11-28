package com.cacto.app.ui.components

/**
 * Orb Background
 * ==============
 *
 * PURPOSE:
 * Renders an animated, shifting gradient background ("lava lamp" effect) with a
 * dot matrix overlay to achieve the "Nothing Phone" + "Palantir" aesthetic.
 *
 * WHERE USED:
 * - Used in: App.kt (wraps the entire screen content)
 *
 * DESIGN PHILOSOPHY:
 * - Minimalist but alive
 * - Dark theme focused
 * - Uses standard Canvas for performance
 */

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OrbBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_animation")

    // Animate positions of 3 orbs
    val orb1Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1_angle"
    )

    val orb2Angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2_angle"
    )
    
    val orb3Scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb3_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050505)) // Deep black background
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Orb 1 - Primary Accent (Cyan/Green)
            val x1 = width * 0.5f + cos(Math.toRadians(orb1Angle.toDouble())).toFloat() * width * 0.3f
            val y1 = height * 0.4f + sin(Math.toRadians(orb1Angle.toDouble())).toFloat() * height * 0.2f
            
            drawOrb(
                center = Offset(x1, y1),
                radius = width * 0.6f,
                color = Color(0xFF4ECDC4).copy(alpha = 0.15f) // CactoGreen
            )

            // Orb 2 - Secondary Accent (Purple/Pink)
            val x2 = width * 0.5f + cos(Math.toRadians(orb2Angle.toDouble() + 180)).toFloat() * width * 0.3f
            val y2 = height * 0.6f + sin(Math.toRadians(orb2Angle.toDouble() + 180)).toFloat() * height * 0.2f
            
            drawOrb(
                center = Offset(x2, y2),
                radius = width * 0.5f,
                color = Color(0xFF7B2CBF).copy(alpha = 0.12f) // CactoPurple
            )
            
            // Orb 3 - Pulsing Center (Orange/Red)
            drawOrb(
                center = Offset(width * 0.5f, height * 0.5f),
                radius = width * 0.4f * orb3Scale,
                color = Color(0xFFFF6B35).copy(alpha = 0.08f) // CactoOrange
            )
            
            // Dot Matrix Overlay
            drawDotMatrix(width, height)
        }
        
        // Content goes on top
        content()
    }
}

private fun DrawScope.drawOrb(center: Offset, radius: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawDotMatrix(width: Float, height: Float) {
    val dotColor = Color.White.copy(alpha = 0.05f)
    val spacing = 40f // Spacing between dots
    val radius = 1.5f // Dot radius
    
    var x = 0f
    while (x < width) {
        var y = 0f
        while (y < height) {
            drawCircle(
                color = dotColor,
                radius = radius,
                center = Offset(x, y)
            )
            y += spacing
        }
        x += spacing
    }
}

