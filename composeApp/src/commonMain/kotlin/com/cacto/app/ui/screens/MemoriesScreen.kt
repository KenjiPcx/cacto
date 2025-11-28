package com.cacto.app.ui.screens

/**
 * Memories Screen
 * ===============
 *
 * PURPOSE:
 * Displays list of extracted memories with filtering capabilities.
 *
 * WHERE USED:
 * - Navigated to from HomeScreen
 *
 * REDESIGN:
 * - Minimalist glassmorphism
 * - Nothing Phone inspired aesthetics
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.data.model.Memory
import com.cacto.app.ui.components.GlassCard
import com.cacto.app.ui.components.MonoText
import com.cacto.app.ui.components.NeonButton
import com.cacto.app.ui.components.TechDivider
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink
import com.cacto.app.ui.theme.CactoPurple

@Composable
fun MemoriesScreen(
    memories: List<Memory>,
    onSearch: (String) -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

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
                text = "MEMORY BANK",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Search Bar
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color.White.copy(alpha = 0.05f)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    onSearch(it)
                },
                placeholder = { 
                    MonoText(
                        text = "SEARCH_MEMORIES...",
                        color = Color.White.copy(alpha = 0.3f)
                    ) 
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = CactoGreen,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = CactoGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        TechDivider(color = CactoGreen.copy(alpha = 0.3f))
        
        Spacer(modifier = Modifier.height(16.dp))

        // List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (memories.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MonoText(
                            text = if (searchQuery.isEmpty()) "NO MEMORIES YET" else "NO MATCHES FOUND",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            items(memories) { memory ->
                MemoryGlassItem(memory)
            }
        }
    }
}

@Composable
fun MemoryGlassItem(memory: Memory) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when(memory.memoryType.name.lowercase()) {
            "fact" -> CactoGreen.copy(alpha = 0.3f)
            "preference" -> CactoPink.copy(alpha = 0.3f)
            "insight" -> CactoPurple.copy(alpha = 0.3f)
            else -> Color.White.copy(alpha = 0.1f)
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonoText(
                    text = memory.memoryType.name.uppercase(),
                    fontSize = 10.sp,
                    color = when(memory.memoryType.name.lowercase()) {
                        "fact" -> CactoGreen
                        "preference" -> CactoPink
                        "insight" -> CactoPurple
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Bold
                )
                
                MonoText(
                    text = "ID: ${memory.id.toString().takeLast(4)}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = memory.content,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (!memory.context.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                MonoText(
                    text = "> ${memory.context}",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
