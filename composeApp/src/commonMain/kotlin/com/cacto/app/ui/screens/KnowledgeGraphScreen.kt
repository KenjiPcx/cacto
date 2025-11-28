package com.cacto.app.ui.screens

/**
 * Knowledge Graph Screen
 * ======================
 *
 * PURPOSE:
 * Displays visualized knowledge graph of entities and relations.
 *
 * WHERE USED:
 * - Navigated to from HomeScreen
 *
 * REDESIGN:
 * - Minimalist glassmorphism
 * - Nothing Phone inspired aesthetics
 */

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.data.model.KnowledgeGraph
import com.cacto.app.ui.components.GlassCard
import com.cacto.app.ui.components.MonoText
import com.cacto.app.ui.components.NeonButton
import com.cacto.app.ui.theme.CactoGreen

@Composable
fun KnowledgeGraphScreen(
    graph: KnowledgeGraph,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonButton(
                text = "<- BACK",
                onClick = onBack,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(16.dp))
            MonoText(
                text = "ENTITY GRAPH",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Graph Container
        KnowledgeGraphWebView(
            graph = graph,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LegendItem("PERSON", Color(0xFF4ECDC4)) // CactoGreen
                LegendItem("PLACE", Color(0xFFE94560))  // CactoPink
                LegendItem("ORG", Color(0xFF7B2CBF))    // CactoPurple
                LegendItem("OTHER", Color(0xFFF7D060))  // CactoYellow
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(8.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = color,
            content = {}
        )
        Spacer(modifier = Modifier.width(8.dp))
        MonoText(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
