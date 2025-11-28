package com.cacto.app

/**
 * App Composable
 * ==============
 *
 * PURPOSE:
 * Root composable that orchestrates the entire application UI. Manages navigation,
 * state observation, and data loading. Coordinates between pipeline processing,
 * repository data, and screen composables.
 *
 * WHERE USED:
 * - Rendered by: MainActivity.setContent()
 * - Entry point: Main application composable
 * - Contains: Navigation logic and screen routing
 *
 * RELATIONSHIPS:
 * - Observes: CactoPipeline.state for processing status
 * - Uses: MemoryRepository, EntityRepository for data access
 * - Renders: HomeScreen, MemoriesScreen, KnowledgeGraphScreen
 * - Manages: Navigation state and screen routing
 *
 * USAGE IN APPLICATION FLOW:
 * - Entry point for all UI rendering
 * - Observes pipeline state for real-time processing updates
 * - Loads and refreshes data when pipeline completes
 * - Handles navigation between screens
 * - Manages search state for memories screen
 *
 * DESIGN PHILOSOPHY:
 * Central coordinator for UI state and navigation. Uses LaunchedEffect for
 * data loading and pipeline state observation. Separates screen logic into
 * dedicated composables. Manages search state locally. Refreshes data reactively
 * when pipeline completes processing.
 */

import androidx.compose.runtime.*
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.PipelineState
import com.cacto.app.data.model.KnowledgeGraph
import com.cacto.app.data.model.Memory
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.MemoryRepository
import com.cacto.app.ui.navigation.Screen
import com.cacto.app.ui.screens.HomeScreen
import com.cacto.app.ui.screens.KnowledgeGraphScreen
import com.cacto.app.ui.screens.MemoriesScreen
import com.cacto.app.ui.theme.CactoTheme
import kotlinx.coroutines.launch

@Composable
fun App(
    pipeline: CactoPipeline,
    memoryRepository: MemoryRepository,
    entityRepository: EntityRepository
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val scope = rememberCoroutineScope()
    
    // Pipeline state
    val pipelineState by pipeline.state.collectAsState()
    
    // Data states
    var memories by remember { mutableStateOf<List<Memory>>(emptyList()) }
    var memoryCount by remember { mutableStateOf(0L) }
    var entityCount by remember { mutableStateOf(0L) }
    var knowledgeGraph by remember { mutableStateOf(KnowledgeGraph(emptyList(), emptyList())) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Load data
    LaunchedEffect(Unit) {
        loadData(memoryRepository, entityRepository) { m, mc, ec, kg ->
            memories = m
            memoryCount = mc
            entityCount = ec
            knowledgeGraph = kg
        }
    }
    
    // Refresh data when pipeline completes
    LaunchedEffect(pipelineState) {
        if (pipelineState.status == com.cacto.app.ai.PipelineStatus.COMPLETE) {
            loadData(memoryRepository, entityRepository) { m, mc, ec, kg ->
                memories = m
                memoryCount = mc
                entityCount = ec
                knowledgeGraph = kg
            }
        }
    }
    
    CactoTheme {
        when (currentScreen) {
            Screen.HOME -> {
                HomeScreen(
                    pipelineState = pipelineState,
                    memoryCount = memoryCount,
                    entityCount = entityCount,
                    onNavigateToMemories = { currentScreen = Screen.MEMORIES },
                    onNavigateToGraph = { currentScreen = Screen.KNOWLEDGE_GRAPH }
                )
            }
            
            Screen.MEMORIES -> {
                val displayedMemories = if (searchQuery.isBlank()) {
                    memories
                } else {
                    memories.filter { it.content.contains(searchQuery, ignoreCase = true) }
                }
                
                MemoriesScreen(
                    memories = displayedMemories,
                    onSearch = { query ->
                        searchQuery = query
                        if (query.isNotBlank()) {
                            scope.launch {
                                memories = memoryRepository.searchMemories(query)
                            }
                        } else {
                            scope.launch {
                                memories = memoryRepository.getAllMemories()
                            }
                        }
                    },
                    onBack = { 
                        currentScreen = Screen.HOME
                        searchQuery = ""
                    }
                )
            }
            
            Screen.KNOWLEDGE_GRAPH -> {
                KnowledgeGraphScreen(
                    graph = knowledgeGraph,
                    onBack = { currentScreen = Screen.HOME }
                )
            }
        }
    }
}

private suspend fun loadData(
    memoryRepository: MemoryRepository,
    entityRepository: EntityRepository,
    onDataLoaded: (List<Memory>, Long, Long, KnowledgeGraph) -> Unit
) {
    val memories = memoryRepository.getAllMemories()
    val memoryCount = memoryRepository.getMemoryCount()
    val entityCount = entityRepository.getEntityCount()
    val knowledgeGraph = entityRepository.getKnowledgeGraph()
    onDataLoaded(memories, memoryCount, entityCount, knowledgeGraph)
}

