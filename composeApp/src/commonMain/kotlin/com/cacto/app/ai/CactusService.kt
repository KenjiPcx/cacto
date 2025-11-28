package com.cacto.app.ai

/**
 * Cactus Service
 * ==============
 *
 * PURPOSE:
 * Wrapper service for the Cactus SDK that provides a unified interface for AI operations
 * including LLM text generation, vision model image analysis, and embedding generation.
 * Manages model initialization, loading, and lifecycle.
 *
 * WHERE USED:
 * - Imported by: MemoryExtractor, ActionGenerator
 * - Called from: CactoPipeline (indirectly through extractors)
 * - Used in workflows: Screenshot processing pipeline, memory extraction, response generation
 *
 * RELATIONSHIPS:
 * - Provides services to: MemoryExtractor, ActionGenerator
 * - Wraps: Cactus SDK (CactusLM)
 * - Manages: Model lifecycle and initialization
 *
 * USAGE IN AI PROCESSING:
 * - Initializes vision model for screenshot analysis
 * - Generates embeddings for vector search
 * - Provides text completion for memory extraction and response generation
 * - Handles streaming responses for real-time UI updates
 *
 * DESIGN PHILOSOPHY:
 * Single entry point for all Cactus SDK operations. Abstracts away SDK details
 * and provides Result-based error handling. Manages model state to avoid redundant
 * initializations. Supports both standard and vision-capable models.
 */

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.CactusCompletionParams
import com.cactus.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CactusService {
    
    private var lm: CactusLM? = null
    private var currentModel: String? = null
    private var isInitialized = false
    
    companion object {
        const val DEFAULT_MODEL = "qwen3-0.6"
        const val VISION_MODEL = "gemma3-4b"  // Vision-capable model
        const val CONTEXT_SIZE = 2048
    }
    
    suspend fun initialize(model: String = DEFAULT_MODEL): Result<Unit> {
        return try {
            if (lm == null) {
                lm = CactusLM()
            }
            
            // Download model if needed
            lm?.downloadModel(model)
            
            // Initialize model
            lm?.initializeModel(
                CactusInitParams(
                    model = model,
                    contextSize = CONTEXT_SIZE
                )
            )
            
            currentModel = model
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun initializeVisionModel(): Result<Unit> {
        return try {
            if (lm == null) {
                lm = CactusLM()
            }
            
            // Get available models and find a vision-capable one
            val models = lm?.getModels() ?: emptyList()
            val visionModel = models.firstOrNull { it.supports_vision }
            
            val modelSlug = visionModel?.slug ?: VISION_MODEL
            
            lm?.downloadModel(modelSlug)
            lm?.initializeModel(
                CactusInitParams(
                    model = modelSlug,
                    contextSize = CONTEXT_SIZE
                )
            )
            
            currentModel = modelSlug
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 500,
        onToken: ((String) -> Unit)? = null
    ): Result<String> {
        return try {
            val messages = buildList {
                systemPrompt?.let { add(ChatMessage(content = it, role = "system")) }
                add(ChatMessage(content = prompt, role = "user"))
            }
            
            val result = lm?.generateCompletion(
                messages = messages,
                params = CactusCompletionParams(maxTokens = maxTokens),
                onToken = if (onToken != null) { token, _ -> onToken(token) } else null
            )
            
            if (result?.success == true && result.response != null) {
                Result.success(result.response!!)
            } else {
                Result.failure(Exception("Failed to generate completion"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun generateCompletionStream(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 500
    ): Flow<String> = flow {
        val messages = buildList {
            systemPrompt?.let { add(ChatMessage(content = it, role = "system")) }
            add(ChatMessage(content = prompt, role = "user"))
        }
        
        val fullResponse = StringBuilder()
        
        lm?.generateCompletion(
            messages = messages,
            params = CactusCompletionParams(maxTokens = maxTokens),
            onToken = { token, _ ->
                fullResponse.append(token)
            }
        )
        
        emit(fullResponse.toString())
    }
    
    suspend fun analyzeImage(
        imagePath: String,
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 500
    ): Result<String> {
        return try {
            val messages = buildList {
                systemPrompt?.let { add(ChatMessage(content = it, role = "system")) }
                add(ChatMessage(
                    content = prompt,
                    role = "user",
                    images = listOf(imagePath)
                ))
            }
            
            val result = lm?.generateCompletion(
                messages = messages,
                params = CactusCompletionParams(maxTokens = maxTokens)
            )
            
            if (result?.success == true && result.response != null) {
                Result.success(result.response!!)
            } else {
                Result.failure(Exception("Failed to analyze image"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateEmbedding(text: String): Result<List<Double>> {
        return try {
            val result = lm?.generateEmbedding(text)
            
            if (result?.success == true) {
                Result.success(result.embeddings)
            } else {
                Result.failure(Exception(result?.errorMessage ?: "Failed to generate embedding"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAvailableModels(): List<String> {
        return try {
            lm?.getModels()?.map { it.slug } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun isModelLoaded(): Boolean = lm?.isLoaded() == true
    
    fun getCurrentModel(): String? = currentModel
    
    fun unload() {
        lm?.unload()
        isInitialized = false
        currentModel = null
    }
}

