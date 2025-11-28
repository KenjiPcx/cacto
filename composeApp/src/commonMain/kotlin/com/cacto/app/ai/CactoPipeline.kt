package com.cacto.app.ai

/**
 * Cacto Pipeline
 * ==============
 *
 * PURPOSE:
 * Main orchestration pipeline for processing screenshots. Implements a multi-step
 * extraction process with full logging to history for debugging.
 *
 * WHERE USED:
 * - Imported by: App composable, ShareReceiverActivity, CactoService
 * - Called from: ShareReceiverActivity (auto-processing), CactoService (screenshot detection)
 */

import com.cacto.app.data.model.ActionType
import com.cacto.app.data.model.Entity
import com.cacto.app.data.model.EntityType
import com.cacto.app.data.model.ExtractedMemory
import com.cacto.app.data.model.Importance
import com.cacto.app.data.model.Memory
import com.cacto.app.data.model.MemoryType
import com.cacto.app.data.model.ProcessingStatus
import com.cacto.app.data.model.Relation
import com.cacto.app.data.model.RelationSourceType
import com.cacto.app.data.model.StepStatus
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.HistoryRepository
import com.cacto.app.data.repository.MemoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PipelineState(
    val status: PipelineStatus = PipelineStatus.IDLE,
    val currentStep: String = "",
    val progress: Float = 0f,
    val error: String? = null,
    val result: PipelineResult? = null
)

data class PipelineResult(
    val actionType: ActionType,
    val memoriesSaved: Int = 0,
    val entitiesCreated: Int = 0,
    val relationsCreated: Int = 0,
    val generatedResponse: String? = null,
    val screenshotDescription: String = ""
)

enum class PipelineStatus {
    IDLE,
    INITIALIZING,
    ANALYZING,
    EXTRACTING_MEMORIES,
    GENERATING_EMBEDDINGS,
    EXTRACTING_ENTITIES,
    RESOLVING_ENTITIES,
    CREATING_RELATIONS,
    SAVING_DATA,
    GENERATING_RESPONSE,
    COMPLETE,
    ERROR
}

class CactoPipeline(
    private val cactusService: CactusService,
    private val memoryExtractor: MemoryExtractor,
    private val actionGenerator: ActionGenerator,
    private val memoryRepository: MemoryRepository,
    private val entityRepository: EntityRepository,
    private val historyRepository: HistoryRepository
) {
    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()
    
    private var entityResolutionService: EntityResolutionService? = null
    
    private fun getEntityResolutionService(): EntityResolutionService {
        if (entityResolutionService == null) {
            entityResolutionService = EntityResolutionService(
                entityRepository = entityRepository,
                memoryExtractor = memoryExtractor
            )
        }
        return entityResolutionService!!
    }
    
    suspend fun processScreenshot(
        imagePath: String,
        onResponseToken: ((String) -> Unit)? = null
    ): Result<PipelineResult> {
        // Start history tracking
        val historyId = historyRepository.startProcessing(imagePath)
        var currentStepId: Long = 0
        
        return try {
            // Step 1: Initialize models
            currentStepId = historyRepository.addStep(historyId, "Initialize AI Models")
            updateState(PipelineStatus.INITIALIZING, "Initializing AI models...", 0.05f)
            
            if (!cactusService.isVisionModelLoaded()) {
                cactusService.initializeVisionModel().getOrThrow()
            }
            if (!cactusService.isTextModelLoaded()) {
                cactusService.initializeTextModel().getOrThrow()
            }
            historyRepository.completeStep(currentStepId, StepStatus.COMPLETED, "Vision & Text models ready")
            
            // Step 2: Classify action
            currentStepId = historyRepository.addStep(historyId, "Classify Action")
            updateState(PipelineStatus.ANALYZING, "Analyzing screenshot...", 0.1f)
            val analysisResult = memoryExtractor.classifyAction(imagePath).getOrThrow()
            historyRepository.completeStep(currentStepId, StepStatus.COMPLETED, 
                "Action: ${analysisResult.actionType}, Context: ${analysisResult.context.take(100)}")
            
            var memoriesSaved = 0
            var entitiesCreated = 0
            var relationsCreated = 0
            var generatedResponse: String? = null
            var screenshotDescription = analysisResult.context
            
            // Step 3: Handle based on action type
            when (analysisResult.actionType) {
                ActionType.SAVE_MEMORY -> {
                    val (memories, entities, relations) = processMemoriesAndEntities(imagePath, historyId)
                    memoriesSaved = memories
                    entitiesCreated = entities
                    relationsCreated = relations
                }
                ActionType.TAKE_ACTION -> {
                    currentStepId = historyRepository.addStep(historyId, "Describe Screenshot")
                    screenshotDescription = memoryExtractor.describeScreenshot(imagePath)
                        .getOrElse { analysisResult.context }
                    historyRepository.completeStep(currentStepId, StepStatus.COMPLETED, 
                        screenshotDescription.take(200))
                    
                    generatedResponse = processAction(screenshotDescription, historyId, onResponseToken)
                }
                ActionType.BOTH -> {
                    val (memories, entities, relations) = processMemoriesAndEntities(imagePath, historyId)
                    memoriesSaved = memories
                    entitiesCreated = entities
                    relationsCreated = relations
                    
                    currentStepId = historyRepository.addStep(historyId, "Describe Screenshot")
                    screenshotDescription = memoryExtractor.describeScreenshot(imagePath)
                        .getOrElse { analysisResult.context }
                    historyRepository.completeStep(currentStepId, StepStatus.COMPLETED,
                        screenshotDescription.take(200))
                    
                    generatedResponse = processAction(screenshotDescription, historyId, onResponseToken)
                }
            }
            
            val result = PipelineResult(
                actionType = analysisResult.actionType,
                memoriesSaved = memoriesSaved,
                entitiesCreated = entitiesCreated,
                relationsCreated = relationsCreated,
                generatedResponse = generatedResponse,
                screenshotDescription = screenshotDescription
            )
            
            // Complete history
            historyRepository.completeProcessing(
                historyId = historyId,
                status = ProcessingStatus.COMPLETED,
                actionType = analysisResult.actionType.name,
                screenshotDescription = screenshotDescription.take(500),
                memoriesSaved = memoriesSaved,
                entitiesCreated = entitiesCreated,
                relationsCreated = relationsCreated,
                generatedResponse = generatedResponse,
                errorMessage = null
            )
            
            entityResolutionService?.clearCache()
            updateState(PipelineStatus.COMPLETE, "Done!", 1f, result = result)
            Result.success(result)
            
        } catch (e: Exception) {
            // Log error
            historyRepository.completeStep(currentStepId, StepStatus.ERROR, errorMessage = e.message)
            historyRepository.completeProcessing(
                historyId = historyId,
                status = ProcessingStatus.ERROR,
                actionType = null,
                screenshotDescription = null,
                memoriesSaved = 0,
                entitiesCreated = 0,
                relationsCreated = 0,
                generatedResponse = null,
                errorMessage = e.message
            )
            
            updateState(PipelineStatus.ERROR, "Error: ${e.message}", error = e.message)
            Result.failure(e)
        }
    }
    
    private suspend fun processMemoriesAndEntities(
        imagePath: String, 
        historyId: Long
    ): Triple<Int, Int, Int> {
        // Step: Extract memories
        var stepId = historyRepository.addStep(historyId, "Extract Memories")
        updateState(PipelineStatus.EXTRACTING_MEMORIES, "Extracting memories...", 0.2f)
        val extractedMemories = memoryExtractor.extractMemories(imagePath).getOrElse { emptyList() }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Found ${extractedMemories.size} memories: ${extractedMemories.map { it.content.take(50) }}")
        
        if (extractedMemories.isEmpty()) {
            return Triple(0, 0, 0)
        }
        
        var memoriesSaved = 0
        var entitiesCreated = 0
        var relationsCreated = 0
        
        val savedMemoryIds = mutableListOf<Long>()
        val savedMemories = mutableListOf<ExtractedMemory>()
        
        // Step: Generate embeddings and save
        stepId = historyRepository.addStep(historyId, "Generate Embeddings & Save Memories")
        for ((index, extractedMemory) in extractedMemories.withIndex()) {
            val progress = 0.2f + (0.3f * (index + 1) / extractedMemories.size)
            updateState(PipelineStatus.GENERATING_EMBEDDINGS, 
                "Embedding ${index + 1}/${extractedMemories.size}...", progress)
            
            val embedding = memoryExtractor.generateEmbedding(extractedMemory.content)
                .getOrElse { emptyList() }
            
            val memory = Memory(
                content = extractedMemory.content,
                memoryType = memoryExtractor.parseMemoryType(extractedMemory.memoryType),
                importance = memoryExtractor.parseImportance(extractedMemory.importance),
                context = extractedMemory.context,
                embedding = embedding,
                createdAt = System.currentTimeMillis(),
                sourceType = "screenshot",
                imagePath = imagePath,
                structuredData = extractedMemory.structuredData
            )
            
            val memoryId = memoryRepository.insertMemory(memory)
            savedMemoryIds.add(memoryId)
            savedMemories.add(extractedMemory)
            memoriesSaved++
        }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Saved $memoriesSaved memories with embeddings")
        
        // Step: Extract entities
        stepId = historyRepository.addStep(historyId, "Extract Entities")
        updateState(PipelineStatus.EXTRACTING_ENTITIES, "Extracting entities...", 0.55f)
        val batchExtraction = memoryExtractor.extractEntitiesFromBatch(savedMemories).getOrNull()
        
        if (batchExtraction == null || batchExtraction.entities.isEmpty()) {
            historyRepository.completeStep(stepId, StepStatus.COMPLETED, "No entities found")
            return Triple(memoriesSaved, 0, 0)
        }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Found ${batchExtraction.entities.size} entities: ${batchExtraction.entities.map { it.name }}")
        
        // Step: Resolve entities
        stepId = historyRepository.addStep(historyId, "Resolve Entities (Deduplication)")
        updateState(PipelineStatus.RESOLVING_ENTITIES, "Resolving entities...", 0.65f)
        val resolutionService = getEntityResolutionService()
        val resolvedEntityMap = mutableMapOf<String, Long>()
        
        for ((index, extractedEntity) in batchExtraction.entities.withIndex()) {
            val progress = 0.65f + (0.15f * (index + 1) / batchExtraction.entities.size)
            updateState(PipelineStatus.RESOLVING_ENTITIES, 
                "Resolving: ${extractedEntity.name}", progress)
            
            val memoryContexts = extractedEntity.mentionedInMemoryIndices
                .take(3)
                .mapNotNull { idx ->
                    if (idx < savedMemories.size) savedMemories[idx].content.take(200)
                    else null
                }
            val contextStr = memoryContexts.joinToString(" | ")
            
            val resolvedEntity = resolutionService.resolveEntity(
                name = extractedEntity.name,
                entityType = memoryExtractor.parseEntityType(extractedEntity.entityType),
                description = extractedEntity.description,
                memoryContext = contextStr.ifEmpty { null }
            )
            
            resolvedEntityMap[extractedEntity.name.lowercase()] = resolvedEntity.id
            entitiesCreated++
        }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Resolved $entitiesCreated entities: ${resolvedEntityMap.keys.toList()}")
        
        // Step: Create relations
        stepId = historyRepository.addStep(historyId, "Create Relations")
        updateState(PipelineStatus.CREATING_RELATIONS, "Creating relations...", 0.85f)
        
        // Memory-entity links
        for (extractedEntity in batchExtraction.entities) {
            val entityId = resolvedEntityMap[extractedEntity.name.lowercase()] ?: continue
            for (memoryIdx in extractedEntity.mentionedInMemoryIndices) {
                if (memoryIdx < savedMemoryIds.size) {
                    try {
                        entityRepository.linkMemoryToEntity(savedMemoryIds[memoryIdx], entityId)
                        relationsCreated++
                    } catch (e: Exception) { }
                }
            }
        }
        
        // Entity-entity relations
        for (relation in batchExtraction.relations) {
            val sourceId = resolvedEntityMap[relation.sourceName.lowercase()]
            val targetId = resolvedEntityMap[relation.targetName.lowercase()]
            
            if (sourceId != null && targetId != null && sourceId != targetId) {
                try {
                    entityRepository.insertRelation(
                        Relation(
                            sourceType = RelationSourceType.ENTITY,
                            sourceEntityId = sourceId,
                            targetEntityId = targetId,
                            relationType = relation.relationType.lowercase().replace(" ", "_"),
                            description = relation.description,
                            memoryId = null,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    relationsCreated++
                } catch (e: Exception) { }
            }
        }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Created $relationsCreated relations")
        
        return Triple(memoriesSaved, entitiesCreated, relationsCreated)
    }
    
    private suspend fun processAction(
        screenshotDescription: String,
        historyId: Long,
        onResponseToken: ((String) -> Unit)?
    ): String {
        // Step: Vector search
        var stepId = historyRepository.addStep(historyId, "Vector Search for Context")
        updateState(PipelineStatus.GENERATING_RESPONSE, "Finding relevant context...", 0.75f)
        
        val allMemories = memoryRepository.getAllMemories()
        val queryEmbedding = memoryExtractor.generateEmbedding(screenshotDescription)
            .getOrElse { emptyList() }
        
        val relevantMemories = if (queryEmbedding.isNotEmpty()) {
            VectorSearch.findSimilarMemories(queryEmbedding, allMemories, topK = 5, minSimilarity = 0.3)
                .map { it.first }
        } else {
            memoryRepository.getRecentMemories(5)
        }
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Found ${relevantMemories.size} relevant memories: ${relevantMemories.map { it.content.take(30) }}")
        
        // Step: Generate response
        stepId = historyRepository.addStep(historyId, "Generate Response")
        updateState(PipelineStatus.GENERATING_RESPONSE, "Generating response...", 0.85f)
        
        val response = actionGenerator.generateResponse(
            screenshotDescription = screenshotDescription,
            relevantMemories = relevantMemories,
            onToken = onResponseToken
        ).getOrThrow()
        
        historyRepository.completeStep(stepId, StepStatus.COMPLETED, 
            "Generated: ${response.response.take(100)}...")
        
        return response.response
    }
    
    private fun updateState(
        status: PipelineStatus,
        step: String,
        progress: Float = 0f,
        error: String? = null,
        result: PipelineResult? = null
    ) {
        _state.value = PipelineState(
            status = status,
            currentStep = step,
            progress = progress,
            error = error,
            result = result
        )
    }
    
    fun reset() {
        _state.value = PipelineState()
    }
}
