package com.cacto.app.ai

/**
 * Memory Extractor
 * =================
 *
 * PURPOSE:
 * Extracts structured information from screenshots using AI vision models.
 * Handles action classification, memory extraction, entity/relation extraction,
 * and embedding generation. Uses sophisticated parsing and filtering logic.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline
 * - Called from: CactoPipeline.processScreenshot(), CactoPipeline.processMemories()
 *
 * DESIGN PHILOSOPHY:
 * Separates AI interaction from business logic. Handles parsing of AI responses
 * with fallback strategies for malformed JSON. Uses Result types for error handling.
 * Implements strict filtering to only extract high-quality, personal memories.
 */

import com.cacto.app.data.model.ActionType
import com.cacto.app.data.model.AnalysisResult
import com.cacto.app.data.model.BatchEntityExtraction
import com.cacto.app.data.model.EntityMatchResult
import com.cacto.app.data.model.EntityType
import com.cacto.app.data.model.ExtractedEntity
import com.cacto.app.data.model.ExtractedKnowledge
import com.cacto.app.data.model.ExtractedMemory
import com.cacto.app.data.model.ExtractedRelation
import com.cacto.app.data.model.Importance
import com.cacto.app.data.model.MemoryType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

class MemoryExtractor(private val cactusService: CactusService) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    // ============================================
    // Action Classification
    // ============================================
    
    suspend fun classifyAction(imagePath: String): Result<AnalysisResult> {
        return try {
            val response = cactusService.analyzeImage(
                imagePath = imagePath,
                prompt = Prompts.CLASSIFY_ACTION_PROMPT,
                systemPrompt = Prompts.CLASSIFY_ACTION_SYSTEM,
                maxTokens = 200
            ).getOrThrow()
            
            val result = parseActionClassification(response)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // Memory Extraction (Sophisticated)
    // ============================================
    
    suspend fun extractMemories(imagePath: String): Result<List<ExtractedMemory>> {
        return try {
            val response = cactusService.analyzeImage(
                imagePath = imagePath,
                prompt = Prompts.MEMORY_EXTRACTION_PROMPT,
                systemPrompt = Prompts.MEMORY_EXTRACTION_SYSTEM,
                maxTokens = 1500
            ).getOrThrow()
            
            val memories = parseMemoriesResponse(response)
            
            // Filter out low-quality memories
            val filtered = memories.filter { !Prompts.isLowQualityMemory(it.content) }
            
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // Batch Entity Extraction
    // ============================================
    
    suspend fun extractEntitiesFromBatch(memories: List<ExtractedMemory>): Result<BatchEntityExtraction> {
        return try {
            if (memories.isEmpty()) {
                return Result.success(BatchEntityExtraction(emptyList(), emptyList()))
            }
            
            // Format memories for prompt
            val memoriesText = memories.mapIndexed { index, mem ->
                "[$index] ${mem.content}" + 
                    (mem.context?.let { "\n    Context: $it" } ?: "")
            }.joinToString("\n")
            
            val response = cactusService.generateCompletion(
                prompt = Prompts.batchEntityExtractionPrompt(memoriesText),
                systemPrompt = Prompts.BATCH_ENTITY_EXTRACTION_SYSTEM,
                maxTokens = 1500
            ).getOrThrow()
            
            val extraction = parseBatchEntityExtraction(response)
            Result.success(extraction)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // Entity Resolution
    // ============================================
    
    suspend fun resolveEntity(
        newName: String,
        newType: String,
        newDescription: String?,
        memoryContext: String?,
        candidatesDescription: String
    ): Result<EntityMatchResult> {
        return try {
            val response = cactusService.generateCompletion(
                prompt = Prompts.entityResolutionPrompt(
                    newName, newType, newDescription, memoryContext, candidatesDescription
                ),
                systemPrompt = Prompts.ENTITY_RESOLUTION_SYSTEM,
                maxTokens = 300
            ).getOrThrow()
            
            val result = parseEntityMatchResult(response)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // Screenshot Description
    // ============================================
    
    suspend fun describeScreenshot(imagePath: String): Result<String> {
        return try {
            val response = cactusService.analyzeImage(
                imagePath = imagePath,
                prompt = Prompts.DESCRIBE_SCREENSHOT_PROMPT,
                systemPrompt = Prompts.DESCRIBE_SCREENSHOT_SYSTEM,
                maxTokens = 400
            ).getOrThrow()
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // Embedding Generation
    // ============================================
    
    suspend fun generateEmbedding(text: String): Result<List<Double>> {
        return cactusService.generateEmbedding(text)
    }
    
    // ============================================
    // Parsing Functions
    // ============================================
    
    private fun parseActionClassification(response: String): AnalysisResult {
        val parts = response.split("|").map { it.trim() }
        
        val actionType = when {
            parts[0].contains("BOTH", ignoreCase = true) -> ActionType.BOTH
            parts[0].contains("TAKE_ACTION", ignoreCase = true) -> ActionType.TAKE_ACTION
            parts[0].contains("SAVE_MEMORY", ignoreCase = true) -> ActionType.SAVE_MEMORY
            else -> ActionType.SAVE_MEMORY // Default
        }
        
        val context = parts.getOrElse(1) { response }
        
        return AnalysisResult(
            actionType = actionType,
            context = context
        )
    }
    
    private fun parseMemoriesResponse(response: String): List<ExtractedMemory> {
        return try {
            // Try to find JSON object in response
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
                
                val memoriesArray = jsonObj["memories"]?.jsonArray ?: return emptyList()
                
                memoriesArray.mapNotNull { element ->
                    val obj = element.jsonObject
                    val content = obj["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    
                    ExtractedMemory(
                        content = content,
                        memoryType = obj["memory_type"]?.jsonPrimitive?.content ?: "fact",
                        importance = obj["importance"]?.jsonPrimitive?.content ?: "medium",
                        context = obj["context"]?.jsonPrimitive?.content,
                        sourceIdx = obj["source_idx"]?.jsonPrimitive?.intOrNull,
                        structuredData = obj["structured_data"]?.jsonPrimitive?.content
                    )
                }
            } else {
                // Fallback: try to find JSON array
                val arrayStart = response.indexOf('[')
                val arrayEnd = response.lastIndexOf(']') + 1
                
                if (arrayStart >= 0 && arrayEnd > arrayStart) {
                    val jsonStr = response.substring(arrayStart, arrayEnd)
                    val jsonArray = json.parseToJsonElement(jsonStr).jsonArray
                    
                    jsonArray.mapNotNull { element ->
                        val obj = element.jsonObject
                        val content = obj["content"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        
                        ExtractedMemory(
                            content = content,
                            memoryType = obj["memory_type"]?.jsonPrimitive?.content ?: "fact",
                            importance = obj["importance"]?.jsonPrimitive?.content ?: "medium",
                            context = obj["context"]?.jsonPrimitive?.content,
                            sourceIdx = null,
                            structuredData = obj["structured_data"]?.jsonPrimitive?.content
                        )
                    }
                } else {
                    // Last resort: treat as plain text memories
                    response.lines()
                        .filter { it.isNotBlank() && it.length > 20 }
                        .map { 
                            ExtractedMemory(
                                content = it.trim().removePrefix("-").removePrefix("•").trim(),
                                memoryType = "fact",
                                importance = "medium",
                                context = null,
                                sourceIdx = null,
                                structuredData = null
                            )
                        }
                }
            }
        } catch (e: Exception) {
            // Fallback: treat whole response as single memory
            if (response.length > 20) {
                listOf(ExtractedMemory(
                    content = response.trim(),
                    memoryType = "fact",
                    importance = "medium",
                    context = null,
                    sourceIdx = null,
                    structuredData = null
                ))
            } else {
                emptyList()
            }
        }
    }
    
    private fun parseBatchEntityExtraction(response: String): BatchEntityExtraction {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
                
                val entities = (jsonObj["entities"] as? JsonArray)?.mapNotNull { element ->
                    val obj = element.jsonObject
                    val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val type = obj["entity_type"]?.jsonPrimitive?.content ?: "other"
                    val description = obj["description"]?.jsonPrimitive?.content
                    val indices = (obj["mentioned_in_memory_indices"] as? JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.intOrNull }
                        ?: emptyList()
                    
                    ExtractedEntity(
                        name = name,
                        entityType = type,
                        description = description,
                        mentionedInMemoryIndices = indices
                    )
                } ?: emptyList()
                
                val relations = (jsonObj["relations"] as? JsonArray)?.mapNotNull { element ->
                    val obj = element.jsonObject
                    val source = obj["source_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val target = obj["target_name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val relationType = obj["relation_type"]?.jsonPrimitive?.content ?: "related_to"
                    val description = obj["description"]?.jsonPrimitive?.content
                    
                    ExtractedRelation(
                        sourceName = source,
                        targetName = target,
                        relationType = relationType,
                        description = description
                    )
                } ?: emptyList()
                
                BatchEntityExtraction(entities, relations)
            } else {
                BatchEntityExtraction(emptyList(), emptyList())
            }
        } catch (e: Exception) {
            BatchEntityExtraction(emptyList(), emptyList())
        }
    }
    
    private fun parseEntityMatchResult(response: String): EntityMatchResult {
        return try {
            val jsonStart = response.indexOf('{')
            val jsonEnd = response.lastIndexOf('}') + 1
            
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd)
                val jsonObj = json.parseToJsonElement(jsonStr).jsonObject
                
                EntityMatchResult(
                    isMatch = jsonObj["is_match"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                    targetId = jsonObj["target_id"]?.jsonPrimitive?.content?.toLongOrNull(),
                    reasoning = jsonObj["reasoning"]?.jsonPrimitive?.content ?: "No reasoning provided"
                )
            } else {
                EntityMatchResult(false, null, "Failed to parse response")
            }
        } catch (e: Exception) {
            EntityMatchResult(false, null, "Parse error: ${e.message}")
        }
    }
    
    // ============================================
    // Utility Functions
    // ============================================
    
    fun parseMemoryType(typeString: String): MemoryType {
        return try {
            MemoryType.valueOf(typeString.uppercase())
        } catch (e: Exception) {
            MemoryType.FACT
        }
    }
    
    fun parseImportance(importanceString: String): Importance {
        return try {
            Importance.valueOf(importanceString.uppercase())
        } catch (e: Exception) {
            Importance.MEDIUM
        }
    }
    
    fun parseEntityType(typeString: String): EntityType {
        return try {
            EntityType.valueOf(typeString.uppercase())
        } catch (e: Exception) {
            EntityType.OTHER
        }
    }
}
