package com.cacto.app.ai

/**
 * Action Generator
 * ================
 *
 * PURPOSE:
 * Generates contextual responses and quick replies based on screenshot analysis
 * and relevant user memories. Uses AI to craft personalized responses that match
 * the user's communication style and personality.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline
 * - Called from: CactoPipeline.processAction()
 * - Used in workflows: Response generation during screenshot processing
 *
 * RELATIONSHIPS:
 * - Uses: CactusService for text generation
 * - Consumes: Memory objects for context
 * - Produces: ActionResponse with generated text
 * - Uses: Prompts for response generation instructions
 *
 * USAGE IN ACTION GENERATION:
 * - Called when screenshot requires a response (TAKE_ACTION or BOTH)
 * - Uses relevant memories to personalize responses
 * - Supports different response tones (friendly, professional, witty, etc.)
 * - Provides streaming support for real-time UI updates
 *
 * DESIGN PHILOSOPHY:
 * Focused on generating natural, authentic responses. Separates tone selection
 * from core generation logic. Returns structured ActionResponse with metadata
 * about which memories were used. Supports both full context and quick reply modes.
 */

import com.cacto.app.data.model.ActionResponse
import com.cacto.app.data.model.Memory

class ActionGenerator(private val cactusService: CactusService) {
    
    suspend fun generateResponse(
        screenshotDescription: String,
        relevantMemories: List<Memory>,
        additionalContext: String = "",
        onToken: ((String) -> Unit)? = null
    ): Result<ActionResponse> {
        return try {
            val memoriesText = if (relevantMemories.isEmpty()) {
                "No relevant memories found. Generate a generic helpful response."
            } else {
                relevantMemories.joinToString("\n") { "- ${it.content}" }
            }
            
            val prompt = Prompts.generateResponsePrompt(
                context = additionalContext,
                memories = memoriesText,
                screenshotDescription = screenshotDescription
            )
            
            val response = cactusService.generateCompletion(
                prompt = prompt,
                systemPrompt = Prompts.GENERATE_RESPONSE_SYSTEM,
                maxTokens = 300,
                onToken = onToken
            ).getOrThrow()
            
            Result.success(
                ActionResponse(
                    response = response.trim(),
                    memoriesUsed = relevantMemories
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun generateQuickReply(
        screenshotDescription: String,
        tone: ResponseTone = ResponseTone.FRIENDLY,
        onToken: ((String) -> Unit)? = null
    ): Result<String> {
        return try {
            val toneInstruction = when (tone) {
                ResponseTone.FRIENDLY -> "Be warm, friendly, and approachable."
                ResponseTone.PROFESSIONAL -> "Be professional and courteous."
                ResponseTone.WITTY -> "Be clever, witty, and engaging."
                ResponseTone.CASUAL -> "Be casual and relaxed."
                ResponseTone.FLIRTY -> "Be playful and subtly flirtatious."
            }
            
            val prompt = """
                Based on this situation: $screenshotDescription
                
                Generate a short, natural response. $toneInstruction
                
                Just output the response text, nothing else.
            """.trimIndent()
            
            val response = cactusService.generateCompletion(
                prompt = prompt,
                systemPrompt = "You are a helpful assistant that generates natural, human-like responses.",
                maxTokens = 150,
                onToken = onToken
            ).getOrThrow()
            
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

enum class ResponseTone {
    FRIENDLY,
    PROFESSIONAL,
    WITTY,
    CASUAL,
    FLIRTY
}

