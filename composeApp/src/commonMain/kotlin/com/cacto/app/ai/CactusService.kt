package com.cacto.app.ai

/**
 * Cactus Service
 * ==============
 *
 * PURPOSE:
 * Wrapper service for the Cactus SDK that provides a unified interface for AI operations.
 * Manages two models: vision model (lfm2-vl-450m) for screenshot analysis and
 * text model (qwen3-0.6) for completions and embeddings.
 *
 * WHERE USED:
 * - Imported by: MemoryExtractor, ActionGenerator
 * - Called from: CactoPipeline (indirectly through extractors)
 *
 * DESIGN PHILOSOPHY:
 * Manages model lifecycle with separate vision and text models. Downloads models
 * on first use. Provides Result-based error handling.
 */

import com.cactus.CactusLM
import com.cactus.CactusInitParams
import com.cactus.CactusCompletionParams
import com.cactus.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class ModelDownloadState(
    val isDownloading: Boolean = false,
    val currentModel: String = "",
    val progress: String = "",
    val visionModelReady: Boolean = false,
    val textModelReady: Boolean = false,
    val error: String? = null
)

class CactusService {
    
    private var visionLM: CactusLM? = null
    private var textLM: CactusLM? = null
    
    private var visionModelLoaded = false
    private var textModelLoaded = false
    
    private val _downloadState = MutableStateFlow(ModelDownloadState())
    val downloadState: StateFlow<ModelDownloadState> = _downloadState.asStateFlow()
    
    companion object {
        // Vision model for screenshot analysis
        const val VISION_MODEL = "lfm2-vl-450m"
        
        // Text model for completions, entity extraction, embeddings
        const val TEXT_MODEL = "qwen3-0.6"
        
        const val CONTEXT_SIZE = 2048
    }
    
    /**
     * Check if models are already downloaded on disk and update state accordingly.
     * This should be called on app startup to avoid showing download screen unnecessarily.
     */
    suspend fun checkModelsDownloaded(): Unit = withContext(Dispatchers.IO) {
        try {
            // Try to download vision model - if it already exists, this will be a no-op
            if (visionLM == null) {
                visionLM = CactusLM()
            }
            visionLM?.downloadModel(VISION_MODEL)
            
            // Try to download text model - if it already exists, this will be a no-op
            if (textLM == null) {
                textLM = CactusLM()
            }
            textLM?.downloadModel(TEXT_MODEL)
            
            // If we got here without exceptions, models are ready
            _downloadState.value = ModelDownloadState(
                visionModelReady = true,
                textModelReady = true,
                progress = "Models ready"
            )
        } catch (e: Exception) {
            // Models not downloaded yet, state remains default (both false)
            // This is fine - user will need to download them
        }
    }
    
    /**
     * Download all required models. Call this on app launch.
     */
    suspend fun downloadModels(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Download vision model
            _downloadState.value = ModelDownloadState(
                isDownloading = true,
                currentModel = VISION_MODEL,
                progress = "Downloading vision model (1/2)..."
            )
            
            if (visionLM == null) {
                visionLM = CactusLM()
            }
            visionLM?.downloadModel(VISION_MODEL)
            
            _downloadState.value = _downloadState.value.copy(
                visionModelReady = true,
                currentModel = TEXT_MODEL,
                progress = "Downloading text model (2/2)..."
            )
            
            // Download text model
            if (textLM == null) {
                textLM = CactusLM()
            }
            textLM?.downloadModel(TEXT_MODEL)
            
            _downloadState.value = _downloadState.value.copy(
                isDownloading = false,
                textModelReady = true,
                progress = "All models ready!"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            _downloadState.value = _downloadState.value.copy(
                isDownloading = false,
                error = "Download failed: ${e.message}"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Check if models are downloaded (doesn't initialize them).
     */
    fun areModelsDownloaded(): Boolean {
        return _downloadState.value.visionModelReady && _downloadState.value.textModelReady
    }
    
    /**
     * Initialize the vision model for image analysis.
     */
    suspend fun initializeVisionModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (visionModelLoaded) return@withContext Result.success(Unit)
            
            if (visionLM == null) {
                visionLM = CactusLM()
                // Ensure model is downloaded before init
                visionLM?.downloadModel(VISION_MODEL)
            }
            
            visionLM?.initializeModel(
                CactusInitParams(
                    model = VISION_MODEL,
                    contextSize = CONTEXT_SIZE
                )
            )
            
            visionModelLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Initialize the text model for completions and embeddings.
     */
    suspend fun initializeTextModel(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (textModelLoaded) return@withContext Result.success(Unit)
            
            if (textLM == null) {
                textLM = CactusLM()
                // Ensure model is downloaded before init
                textLM?.downloadModel(TEXT_MODEL)
            }
            
            textLM?.initializeModel(
                CactusInitParams(
                    model = TEXT_MODEL,
                    contextSize = CONTEXT_SIZE
                )
            )
            
            textModelLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Analyze an image using the vision model.
     */
    suspend fun analyzeImage(
        imagePath: String,
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 500
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Ensure vision model is ready
            if (!visionModelLoaded) {
                // Use getOrThrow to propagate exception if init fails
                val initResult = initializeVisionModel()
                if (initResult.isFailure) return@withContext Result.failure(initResult.exceptionOrNull()!!)
            }
            
            val messages = buildList {
                systemPrompt?.let { add(ChatMessage(content = it, role = "system")) }
                add(ChatMessage(
                    content = prompt,
                    role = "user",
                    images = listOf(imagePath)
                ))
            }
            
            val result = visionLM?.generateCompletion(
                messages = messages,
                params = CactusCompletionParams(maxTokens = maxTokens)
            )
            
            if (result?.success == true && result.response != null) {
                Result.success(result.response!!)
            } else {
                Result.failure(Exception("Vision analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate text completion using the text model.
     */
    suspend fun generateCompletion(
        prompt: String,
        systemPrompt: String? = null,
        maxTokens: Int = 500,
        onToken: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Ensure text model is ready
            if (!textModelLoaded) {
                val initResult = initializeTextModel()
                if (initResult.isFailure) return@withContext Result.failure(initResult.exceptionOrNull()!!)
            }
            
            val messages = buildList {
                systemPrompt?.let { add(ChatMessage(content = it, role = "system")) }
                add(ChatMessage(content = prompt, role = "user"))
            }
            
            val result = textLM?.generateCompletion(
                messages = messages,
                params = CactusCompletionParams(maxTokens = maxTokens),
                onToken = if (onToken != null) { token, _ -> onToken(token) } else null
            )
            
            if (result?.success == true && result.response != null) {
                Result.success(result.response!!)
            } else {
                Result.failure(Exception("Completion failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate embedding using the text model.
     */
    suspend fun generateEmbedding(text: String): Result<List<Double>> = withContext(Dispatchers.IO) {
        try {
            // Ensure text model is ready
            if (!textModelLoaded) {
                val initResult = initializeTextModel()
                if (initResult.isFailure) return@withContext Result.failure(initResult.exceptionOrNull()!!)
            }
            
            val result = textLM?.generateEmbedding(text)
            
            if (result?.success == true) {
                Result.success(result.embeddings)
            } else {
                Result.failure(Exception(result?.errorMessage ?: "Embedding failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if vision model is loaded and ready.
     */
    fun isVisionModelLoaded(): Boolean = visionModelLoaded
    
    /**
     * Check if text model is loaded and ready.
     */
    fun isTextModelLoaded(): Boolean = textModelLoaded
    
    /**
     * Check if any model is loaded (for backward compatibility).
     */
    fun isModelLoaded(): Boolean = visionModelLoaded || textModelLoaded
    
    /**
     * Unload all models to free memory.
     */
    fun unload() {
        visionLM?.unload()
        textLM?.unload()
        visionModelLoaded = false
        textModelLoaded = false
    }
}
