package com.cacto.app.ai

/**
 * Cacto Pipeline
 * ==============
 *
 * PURPOSE:
 * Main orchestration pipeline for processing screenshots. Implements a multi-step
 * extraction process:
 * 1. Action Classification (SAVE_MEMORY, TAKE_ACTION, BOTH)
 * 2. Memory Extraction (with sophisticated filtering)
 * 3. Batch Entity Extraction (with deduplication)
 * 4. Entity Resolution (vector similarity + LLM verification)
 * 5. Relation Creation (memory→entity and entity→entity)
 * 6. Response Generation (if needed)
 *
 * WHERE USED:
 * - Imported by: App composable, ShareReceiverActivity, MainActivity
 * - Called from: ShareReceiverActivity (auto-processing), App (state observation)
 *
 * DESIGN PHILOSOPHY:
 * Central coordinator that manages the entire processing workflow. Uses StateFlow
 * for reactive state management. Separates concerns into specialized extractors.
 * Provides detailed progress tracking for better UX.
 */

import com.cacto.app.data.model.ActionType
import com.cacto.app.data.model.Entity
import com.cacto.app.data.model.EntityType
import com.cacto.app.data.model.ExtractedMemory
import com.cacto.app.data.model.Importance
import com.cacto.app.data.model.Memory
import com.cacto.app.data.model.MemoryType
import com.cacto.app.data.model.Relation
import com.cacto.app.data.model.RelationSourceType
import com.cacto.app.data.repository.EntityRepository
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
    private val entityRepository: EntityRepository
) {
    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()
    
    // Entity resolution service (created lazily)
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
        return try {
            // Initialize model if needed
            updateState(PipelineStatus.INITIALIZING, "Initializing AI model...", 0.05f)
            if (!cactusService.isModelLoaded()) {
                cactusService.initializeVisionModel().getOrThrow()
            }
            
            // Step 1: Classify action
            updateState(PipelineStatus.ANALYZING, "Analyzing screenshot...", 0.1f)
            val analysisResult = memoryExtractor.classifyAction(imagePath).getOrThrow()
            
            var memoriesSaved = 0
            var entitiesCreated = 0
            var relationsCreated = 0
            var generatedResponse: String? = null
            var screenshotDescription = analysisResult.context
            
            // Step 2: Handle based on action type
            when (analysisResult.actionType) {
                ActionType.SAVE_MEMORY -> {
                    val (memories, entities, relations) = processMemoriesAndEntities(imagePath)
                    memoriesSaved = memories
                    entitiesCreated = entities
                    relationsCreated = relations
                }
                ActionType.TAKE_ACTION -> {
                    // Get better screenshot description for response generation
                    screenshotDescription = memoryExtractor.describeScreenshot(imagePath)
                        .getOrElse { analysisResult.context }
                    generatedResponse = processAction(screenshotDescription, onResponseToken)
                }
                ActionType.BOTH -> {
                    val (memories, entities, relations) = processMemoriesAndEntities(imagePath)
                    memoriesSaved = memories
                    entitiesCreated = entities
                    relationsCreated = relations
                    
                    screenshotDescription = memoryExtractor.describeScreenshot(imagePath)
                        .getOrElse { analysisResult.context }
                    generatedResponse = processAction(screenshotDescription, onResponseToken)
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
            
            // Clear entity resolution cache after processing
            entityResolutionService?.clearCache()
            
            updateState(PipelineStatus.COMPLETE, "Done!", 1f, result = result)
            Result.success(result)
            
        } catch (e: Exception) {
            updateState(PipelineStatus.ERROR, "Error: ${e.message}", error = e.message)
            Result.failure(e)
        }
    }
    
    /**
     * Process memories and entities from a screenshot.
     * 
     * Flow:
     * 1. Extract memories from screenshot
     * 2. Generate embeddings for memories
     * 3. Save memories to database
     * 4. Batch extract entities from memories
     * 5. Resolve entities (deduplication)
     * 6. Create memory→entity relations
     * 7. Create entity→entity relations
     */
    private suspend fun processMemoriesAndEntities(imagePath: String): Triple<Int, Int, Int> {
        // Step 1: Extract memories
        updateState(PipelineStatus.EXTRACTING_MEMORIES, "Extracting memories...", 0.2f)
        val extractedMemories = memoryExtractor.extractMemories(imagePath).getOrElse { emptyList() }
        
        if (extractedMemories.isEmpty()) {
            return Triple(0, 0, 0)
        }
        
        var memoriesSaved = 0
        var entitiesCreated = 0
        var relationsCreated = 0
        
        // Track saved memory IDs for relation creation
        val savedMemoryIds = mutableListOf<Long>()
        val savedMemories = mutableListOf<ExtractedMemory>()
        
        // Step 2 & 3: Generate embeddings and save memories
        for ((index, extractedMemory) in extractedMemories.withIndex()) {
            val progress = 0.2f + (0.3f * (index + 1) / extractedMemories.size)
            
            updateState(PipelineStatus.GENERATING_EMBEDDINGS, 
                "Generating embedding ${index + 1}/${extractedMemories.size}...", progress)
            
            val embedding = memoryExtractor.generateEmbedding(extractedMemory.content)
                .getOrElse { emptyList() }
            
            updateState(PipelineStatus.SAVING_DATA, 
                "Saving memory ${index + 1}/${extractedMemories.size}...", progress + 0.05f)
            
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
        
        // Step 4: Batch extract entities from memories
        updateState(PipelineStatus.EXTRACTING_ENTITIES, "Extracting entities...", 0.55f)
        val batchExtraction = memoryExtractor.extractEntitiesFromBatch(savedMemories)
            .getOrNull()
        
        if (batchExtraction == null || batchExtraction.entities.isEmpty()) {
            return Triple(memoriesSaved, 0, 0)
        }
        
        // Step 5: Resolve entities (deduplication with vector search + LLM)
        updateState(PipelineStatus.RESOLVING_ENTITIES, "Resolving entities...", 0.65f)
        val resolutionService = getEntityResolutionService()
        val resolvedEntityMap = mutableMapOf<String, Long>() // name.lowercase -> entity ID
        
        for ((index, extractedEntity) in batchExtraction.entities.withIndex()) {
            val progress = 0.65f + (0.15f * (index + 1) / batchExtraction.entities.size)
            updateState(PipelineStatus.RESOLVING_ENTITIES, 
                "Resolving entity ${index + 1}/${batchExtraction.entities.size}: ${extractedEntity.name}", progress)
            
            // Get memory context for better resolution
            val memoryContexts = extractedEntity.mentionedInMemoryIndices
                .take(3)
                .mapNotNull { idx ->
                    if (idx < savedMemories.size) savedMemories[idx].content.take(200)
                    else null
                }
            val contextStr = memoryContexts.joinToString(" | ")
            
            // Resolve entity (finds existing or creates new)
            val resolvedEntity = resolutionService.resolveEntity(
                name = extractedEntity.name,
                entityType = memoryExtractor.parseEntityType(extractedEntity.entityType),
                description = extractedEntity.description,
                memoryContext = contextStr.ifEmpty { null }
            )
            
            resolvedEntityMap[extractedEntity.name.lowercase()] = resolvedEntity.id
            entitiesCreated++
        }
        
        // Step 6: Create memory→entity relations
        updateState(PipelineStatus.CREATING_RELATIONS, "Creating memory-entity links...", 0.85f)
        for (extractedEntity in batchExtraction.entities) {
            val entityId = resolvedEntityMap[extractedEntity.name.lowercase()] ?: continue
            
            for (memoryIdx in extractedEntity.mentionedInMemoryIndices) {
                if (memoryIdx < savedMemoryIds.size) {
                    val memoryId = savedMemoryIds[memoryIdx]
                    try {
                        entityRepository.linkMemoryToEntity(memoryId, entityId)
                        relationsCreated++
                    } catch (e: Exception) {
                        // Link already exists, ignore
                    }
                }
            }
        }
        
        // Step 7: Create entity→entity relations
        updateState(PipelineStatus.CREATING_RELATIONS, "Creating entity-entity relations...", 0.9f)
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
                } catch (e: Exception) {
                    // Relation already exists or failed, ignore
                }
            }
        }
        
        return Triple(memoriesSaved, entitiesCreated, relationsCreated)
    }
    
    /**
     * Process action: generate contextual response.
     */
    private suspend fun processAction(
        screenshotDescription: String,
        onResponseToken: ((String) -> Unit)?
    ): String {
        updateState(PipelineStatus.GENERATING_RESPONSE, "Finding relevant context...", 0.75f)
        
        // Get all memories for vector search
        val allMemories = memoryRepository.getAllMemories()
        
        // Generate embedding for the screenshot description to find relevant memories
        val queryEmbedding = memoryExtractor.generateEmbedding(screenshotDescription)
            .getOrElse { emptyList() }
        
        val relevantMemories = if (queryEmbedding.isNotEmpty()) {
            VectorSearch.findSimilarMemories(queryEmbedding, allMemories, topK = 5, minSimilarity = 0.3)
                .map { it.first }
        } else {
            memoryRepository.getRecentMemories(5)
        }
        
        updateState(PipelineStatus.GENERATING_RESPONSE, "Generating response...", 0.85f)
        
        val response = actionGenerator.generateResponse(
            screenshotDescription = screenshotDescription,
            relevantMemories = relevantMemories,
            onToken = onResponseToken
        ).getOrThrow()
        
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
