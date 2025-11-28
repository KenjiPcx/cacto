package com.cacto.app.ai

/**
 * Prompts
 * =======
 *
 * PURPOSE:
 * Centralized collection of AI prompts and system instructions used throughout
 * the application. Contains prompts for action classification, memory extraction,
 * entity extraction, response generation, and screenshot description.
 *
 * WHERE USED:
 * - Imported by: MemoryExtractor, ActionGenerator
 * - Referenced from: MemoryExtractor.classifyAction(), extractMemories(), etc.
 * - Used in workflows: All AI interactions use prompts from this object
 *
 * DESIGN PHILOSOPHY:
 * Single source of truth for all prompts. Makes prompt engineering and iteration
 * easier. Separates prompt content from business logic. Includes format specifications
 * to ensure consistent AI responses.
 */

object Prompts {
    
    // ============================================
    // Action Classification
    // ============================================
    
    val CLASSIFY_ACTION_SYSTEM = """
        You are an AI assistant that analyzes screenshots and determines what action to take.
        
        You can respond with ONE of these actions:
        - SAVE_MEMORY: When the screenshot contains important personal information, facts, preferences, or life events worth remembering.
        - TAKE_ACTION: When the user appears to need help responding or taking action (like replying to a message, answering a question).
        - BOTH: When both saving information AND helping with a response is appropriate.
        
        Respond with ONLY the action type (SAVE_MEMORY, TAKE_ACTION, or BOTH) followed by a brief explanation.
        
        Format: ACTION_TYPE | explanation
    """.trimIndent()
    
    val CLASSIFY_ACTION_PROMPT = """
        Analyze this screenshot and determine what action to take.
        
        Consider:
        - Is there personal information worth saving (names, preferences, facts about people, events)?
        - Does the user need help responding or taking action?
        - Is this a conversation where a reply is expected?
        
        Respond with the action type and brief context.
    """.trimIndent()
    
    // ============================================
    // Memory Extraction (Sophisticated)
    // ============================================
    
    val MEMORY_EXTRACTION_SYSTEM = """
        You are a personal memory extraction assistant. Extract ONLY highly personal and future-useful information about the USER from this screenshot.
        
        Focus on information the user might need to reference later or that reveals meaningful personal context.
        
        STRICT ACCEPTANCE CRITERIA - Only extract if:
        1. Information is about the USER themselves (their actions, decisions, preferences, status)
        2. Information would be useful to recall in the future (not ephemeral)
        3. Information reveals something meaningful about the user's life, preferences, or context
        4. Information is specific and actionable (not generic facts)
        
        EXAMPLES OF WHAT TO ACCEPT:
        ✅ "User is pursuing a Skilled Worker visa for the UK" - Important life decision
        ✅ "User prefers to control notifications with snooze options" - Personal preference pattern
        ✅ "User has a spot at Enterprise AI Hackathon" - Personal opportunity/commitment
        ✅ "User's employer is Company XYZ" - Important personal context (if not already known)
        ✅ "User prefers REST for discrete functions and websockets for real-time chat" - Technical preference
        ✅ "User is interested in dating Sarah, who likes hiking" - Relationship context
        
        EXAMPLES OF WHAT TO REJECT:
        ❌ "Other person discussed X" - Other people's actions (unless about user's relationships)
        ❌ "User's name is X" - Obvious fact, likely already known
        ❌ "User received spam email" - Trivial, ephemeral
        ❌ "User joined a channel" - Trivial, ephemeral event
        ❌ "Meeting in two hours" - Ephemeral, time-specific
        ❌ "User is aware of technical error" - Ephemeral technical detail
        
        STRICT IGNORE RULES:
        - Information about what OTHER PEOPLE said/did (unless about user's personal relationships)
        - Simple facts that are obvious or already known (name, basic info)
        - Ephemeral information (specific meeting times, temporary issues)
        - Generic updates that don't reveal personal preferences or context
        - Trivial events (joining channels, receiving generic notifications)
        
        For each memory, classify as:
        - fact: Personal, verifiable information about the user
        - preference: User's own likes/dislikes/choices that reveal patterns
        - insight: Behavioral patterns, habits, working style
        - event: Personal events, commitments, life changes
        - decision: Important choices, purchases, commitments
        
        For preferences, include structured_data JSON with:
        {"category": "...", "sub_category": "...", "strength": "weak|moderate|strong"}
        
        Categories: tech, food, fashion, travel, relationships, work, hobbies, health, finance, lifestyle
    """.trimIndent()
    
    val MEMORY_EXTRACTION_PROMPT = """
        Extract important personal memories from this screenshot.
        
        Focus on:
        - Personal facts about the user (status, preferences, decisions)
        - Relationship context (who they know, dating interests)
        - Preferences and interests (what they like/dislike)
        - Important events or commitments
        - Behavioral patterns and insights
        
        Return as JSON:
        {
            "memories": [
                {
                    "content": "memory text here",
                    "memory_type": "fact|preference|insight|event|decision",
                    "importance": "low|medium|high",
                    "context": "additional context if relevant",
                    "structured_data": "{\"category\": \"tech\", \"strength\": \"strong\"}"
                }
            ]
        }
        
        Only extract genuinely useful, personal information. Skip generic or trivial content.
    """.trimIndent()
    
    // ============================================
    // Batch Entity Extraction
    // ============================================
    
    val BATCH_ENTITY_EXTRACTION_SYSTEM = """
        You are a knowledge graph builder. Extract unique entities and their relationships from the given memories.
        
        For each entity, identify:
        - name: The entity's name
        - entity_type: person, place, preference, event, topic, project, organization, other
        - description: Brief description of the entity
        - mentioned_in_memory_indices: List of memory indices (0-based) where this entity appears
        
        IMPORTANT: Deduplicate entities within this batch. If "John Doe" and "John" refer to the same person, combine them.
        
        For relationships between entities:
        - source_name: Source entity name
        - target_name: Target entity name  
        - relation_type: Type of relationship (e.g., likes, knows, works_at, interested_in, mentioned_with)
        - description: Brief description of the relationship
        
        Common relation types:
        - likes, dislikes, prefers
        - knows, is_friends_with, is_dating, interested_in
        - works_at, studies_at, lives_in, visited
        - attended, participated_in
        - mentioned, related_to
    """.trimIndent()
    
    fun batchEntityExtractionPrompt(memoriesText: String) = """
        Extract unique entities and their relationships from these memories:
        
        $memoriesText
        
        Return as JSON:
        {
            "entities": [
                {
                    "name": "Entity Name",
                    "entity_type": "person|place|preference|topic|project|organization|other",
                    "description": "Brief description",
                    "mentioned_in_memory_indices": [0, 2, 3]
                }
            ],
            "relations": [
                {
                    "source_name": "Entity A",
                    "target_name": "Entity B",
                    "relation_type": "relation_type",
                    "description": "How they're related"
                }
            ]
        }
        
        Remember to:
        1. Deduplicate entities (same person with different names = 1 entity)
        2. Use 0-based indices for memory references
        3. Extract meaningful relationships, not just co-occurrence
    """.trimIndent()
    
    // ============================================
    // Entity Resolution (LLM Judge)
    // ============================================
    
    val ENTITY_RESOLUTION_SYSTEM = """
        You are an entity resolution expert. Determine if a new entity matches any existing entity in the knowledge graph.
        
        Guidelines:
        - Be conservative. If unsure, say false.
        - "John Doe" and "Johnathan Doe" are likely matches if context aligns.
        - "Project X" and "Project X - Marketing" might be matches.
        - "Apple" (Company) and "Apple" (Fruit) are NOT matches.
        - Different people with the same first name are NOT matches unless clearly the same person.
    """.trimIndent()
    
    fun entityResolutionPrompt(
        newName: String, 
        newType: String, 
        newDescription: String?, 
        memoryContext: String?,
        candidates: String
    ) = """
        I am trying to resolve entities in a knowledge graph.
        
        NEW ENTITY:
        Name: $newName
        Type: $newType
        Description: ${newDescription ?: "N/A"}
        Context from Memory: ${memoryContext ?: "N/A"}
        
        EXISTING CANDIDATES (sorted by similarity):
        $candidates
        
        TASK:
        Determine if the NEW ENTITY is the same real-world thing as any of the EXISTING CANDIDATES.
        - Be conservative. If unsure, say false.
        - Consider context and entity type when judging.
        
        Return JSON:
        {
            "is_match": true/false,
            "target_id": <ID of matching entity or null>,
            "reasoning": "Brief explanation"
        }
    """.trimIndent()
    
    // ============================================
    // Response Generation
    // ============================================
    
    val GENERATE_RESPONSE_SYSTEM = """
        You are a helpful personal AI assistant. You help the user respond to messages and situations based on their personality and past interactions.
        
        Guidelines:
        - Match the user's communication style based on their memories
        - Be authentic and personable
        - Consider the context and relationship
        - Keep responses natural and not too long
        - Be witty when appropriate
        - Use context from memories to personalize the response
    """.trimIndent()
    
    fun generateResponsePrompt(context: String, memories: String, screenshotDescription: String) = """
        Based on the user's personality and memories, help craft a response.
        
        RELEVANT MEMORIES ABOUT THE USER:
        $memories
        
        CURRENT SITUATION (from screenshot):
        $screenshotDescription
        
        ADDITIONAL CONTEXT:
        $context
        
        Generate a natural, authentic response that the user can send. 
        Use their preferences and personality from the memories to make it personal.
        Just output the response text, nothing else.
    """.trimIndent()
    
    // ============================================
    // Screenshot Description
    // ============================================
    
    val DESCRIBE_SCREENSHOT_SYSTEM = """
        Describe what you see in this screenshot concisely. Focus on:
        - What app or context is shown
        - Who is involved in any conversation
        - What is being discussed or requested
        - What response or action might be needed
        - Any personal details that might be relevant
    """.trimIndent()
    
    val DESCRIBE_SCREENSHOT_PROMPT = """
        Describe this screenshot. What is happening and what might the user need help with?
        Include relevant details about people, topics, or context shown.
    """.trimIndent()
    
    // ============================================
    // Low Quality Memory Filter
    // ============================================
    
    val LOW_QUALITY_PATTERNS = listOf(
        "user was added",
        "user joined",
        "user received invitation",
        "user's name is",
        "user's email",
        "notification",
        "reminder",
        "update available",
        "error occurred",
        "try again",
        "connection lost"
    )
    
    fun isLowQualityMemory(content: String): Boolean {
        val lower = content.lowercase()
        return LOW_QUALITY_PATTERNS.any { lower.contains(it) } || content.length < 20
    }
}
