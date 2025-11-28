package com.cacto.app

/**
 * App Composable
 * ==============
 *
 * PURPOSE:
 * Root composable that orchestrates the entire application UI. Manages navigation,
 * model downloads, service state, and data loading.
 */

import androidx.compose.runtime.*
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.CactusService
import com.cacto.app.ai.PipelineStatus
import com.cacto.app.data.model.KnowledgeGraph
import com.cacto.app.data.model.Memory
import com.cacto.app.data.model.ProcessingHistory
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.HistoryRepository
import com.cacto.app.data.repository.MemoryRepository
import com.cacto.app.ui.navigation.Screen
import com.cacto.app.ui.screens.DebugScreen
import com.cacto.app.ui.screens.HomeScreen
import com.cacto.app.ui.screens.KnowledgeGraphScreen
import com.cacto.app.ui.screens.MemoriesScreen
import com.cacto.app.ui.screens.ModelDownloadScreen
import com.cacto.app.ui.theme.CactoTheme
import kotlinx.coroutines.launch

@Composable
fun App(
    cactusService: CactusService,
    pipeline: CactoPipeline,
    memoryRepository: MemoryRepository,
    entityRepository: EntityRepository,
    historyRepository: HistoryRepository,
    isServiceRunning: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val scope = rememberCoroutineScope()
    
    // Model download state
    val downloadState by cactusService.downloadState.collectAsState()
    var modelsReady by remember { mutableStateOf(false) }
    
    // Pipeline state
    val pipelineState by pipeline.state.collectAsState()
    
    // Data states
    var memories by remember { mutableStateOf<List<Memory>>(emptyList()) }
    var memoryCount by remember { mutableStateOf(0L) }
    var entityCount by remember { mutableStateOf(0L) }
    var knowledgeGraph by remember { mutableStateOf(KnowledgeGraph(emptyList(), emptyList())) }
    var processingHistory by remember { mutableStateOf<List<ProcessingHistory>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Check if models are already downloaded
    LaunchedEffect(downloadState) {
        if (downloadState.visionModelReady && downloadState.textModelReady) {
            modelsReady = true
        }
    }
    
    // Load data once models are ready
    LaunchedEffect(modelsReady) {
        if (modelsReady) {
            loadData(memoryRepository, entityRepository, historyRepository) { m, mc, ec, kg, h ->
                memories = m
                memoryCount = mc
                entityCount = ec
                knowledgeGraph = kg
                processingHistory = h
            }
        }
    }
    
    // Refresh data when pipeline completes
    LaunchedEffect(pipelineState) {
        if (pipelineState.status == PipelineStatus.COMPLETE || pipelineState.status == PipelineStatus.ERROR) {
            loadData(memoryRepository, entityRepository, historyRepository) { m, mc, ec, kg, h ->
                memories = m
                memoryCount = mc
                entityCount = ec
                knowledgeGraph = kg
                processingHistory = h
            }
        }
    }
    
    CactoTheme {
        if (!modelsReady) {
            ModelDownloadScreen(
                downloadState = downloadState,
                onStartDownload = {
                    scope.launch {
                        cactusService.downloadModels()
                    }
                },
                onRetry = {
                    scope.launch {
                        cactusService.downloadModels()
                    }
                }
            )
        } else {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        pipelineState = pipelineState,
                        memoryCount = memoryCount,
                        entityCount = entityCount,
                        historyCount = processingHistory.size,
                        isListening = isServiceRunning,
                        onToggleListening = { shouldListen ->
                            if (shouldListen) {
                                onStartService()
                            } else {
                                onStopService()
                            }
                        },
                        onNavigateToMemories = { currentScreen = Screen.MEMORIES },
                        onNavigateToGraph = { currentScreen = Screen.KNOWLEDGE_GRAPH },
                        onNavigateToDebug = { currentScreen = Screen.DEBUG }
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
                
                Screen.DEBUG -> {
                    DebugScreen(
                        history = processingHistory,
                        onRefresh = {
                            scope.launch {
                                processingHistory = historyRepository.getAllHistory()
                            }
                        },
                        onBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}

private suspend fun loadData(
    memoryRepository: MemoryRepository,
    entityRepository: EntityRepository,
    historyRepository: HistoryRepository,
    onDataLoaded: (List<Memory>, Long, Long, KnowledgeGraph, List<ProcessingHistory>) -> Unit
) {
    val memories = memoryRepository.getAllMemories()
    val memoryCount = memoryRepository.getMemoryCount()
    val entityCount = entityRepository.getEntityCount()
    val knowledgeGraph = entityRepository.getKnowledgeGraph()
    val history = historyRepository.getAllHistory()
    onDataLoaded(memories, memoryCount, entityCount, knowledgeGraph, history)
}
