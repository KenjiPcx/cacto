package com.cacto.app.data.model

/**
 * Data Models
 * ===========
 *
 * PURPOSE:
 * Defines all data models used throughout the application. Includes domain models
 * (Memory, Entity, Relation), extraction models (ExtractedMemory, ExtractedEntity),
 * graph visualization models (KnowledgeGraph, GraphNode, GraphEdge), and workflow
 * models (AnalysisResult, ActionResponse, ActionType).
 *
 * WHERE USED:
 * - Imported by: All repositories, services, and UI composables
 * - Used in: Database operations, AI processing, UI display
 * - Referenced from: CactoPipeline, MemoryRepository, EntityRepository, UI screens
 *
 * RELATIONSHIPS:
 * - Memory: Links to Entity via Relation table (many-to-many)
 * - Entity: Connected via Relations (source/target entities)
 * - Relation: Links two entities with a relation type
 * - KnowledgeGraph: Aggregates entities and relations for visualization
 *
 * DESIGN PHILOSOPHY:
 * Uses Kotlinx Serialization for JSON encoding/decoding. Models are immutable
 * data classes. Embeddings stored as List<Double> for type safety. Entity types
 * use enum for type safety. Relations support flexible relation types via strings.
 */

import kotlinx.serialization.Serializable

/**
 * Memory Model
 * 
 * Stores extracted insights, facts, preferences, and contextual information.
 * Each memory has embeddings for vector search and metadata for filtering.
 * 
 * memory_type categories:
 * - fact: Personal, verifiable information (e.g., "User is pursuing UK visa")
 * - preference: Likes/dislikes/choices with optional structured_data
 * - insight: Behavioral patterns, habits, working style
 * - event: Personal events, commitments, life changes
 * - decision: Important choices, purchases, commitments
 * - interaction: Notable interactions with others
 */
@Serializable
data class Memory(
    val id: Long = 0,
    val content: String,
    val memoryType: MemoryType = MemoryType.FACT,
    val importance: Importance = Importance.MEDIUM,
    val context: String? = null,
    val embedding: List<Double>,
    val createdAt: Long,
    val sourceType: String = "screenshot",
    val imagePath: String? = null,
    val structuredData: String? = null // JSON string for preferences: category, sub_category, strength
)

@Serializable
enum class MemoryType {
    FACT,        // Personal, verifiable information
    PREFERENCE,  // Likes/dislikes/choices that reveal patterns
    INSIGHT,     // Behavioral patterns, habits, working style
    EVENT,       // Personal events, commitments, life changes
    DECISION,    // Important choices, purchases, commitments
    INTERACTION  // Notable interactions with others
}

@Serializable
enum class Importance {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Entity Model
 * 
 * Represents nodes in the knowledge graph - people, places, preferences, etc.
 * Includes description for better context and deduplication.
 */
@Serializable
data class Entity(
    val id: Long = 0,
    val name: String,
    val type: EntityType,
    val description: String? = null,
    val embedding: List<Double> = emptyList(), // For entity resolution
    val createdAt: Long
)

@Serializable
enum class EntityType {
    PERSON,
    PLACE,
    PREFERENCE,
    EVENT,
    TOPIC,
    PROJECT,
    ORGANIZATION,
    OTHER
}

/**
 * Relation Model
 * 
 * Represents edges in the knowledge graph.
 * Supports both memory→entity and entity→entity relations.
 */
@Serializable
data class Relation(
    val id: Long = 0,
    val sourceType: RelationSourceType = RelationSourceType.ENTITY,
    val sourceEntityId: Long,
    val targetEntityId: Long,
    val relationType: String,
    val description: String? = null,
    val memoryId: Long? = null,
    val createdAt: Long
)

@Serializable
enum class RelationSourceType {
    MEMORY,  // Memory "about" Entity
    ENTITY   // Entity related to Entity
}

@Serializable
data class GraphNode(
    val id: Long,
    val name: String,
    val type: String,
    val description: String? = null
)

@Serializable
data class GraphEdge(
    val id: Long,
    val source: Long,
    val target: Long,
    val label: String,
    val description: String? = null
)

@Serializable
data class KnowledgeGraph(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>
)

// ============================================
// Extraction Models (AI Pipeline Output)
// ============================================

/**
 * Extracted Memory
 * 
 * Output from AI memory extraction. Maps to Memory model after processing.
 * Includes source_idx for batch processing to track which document it came from.
 */
@Serializable
data class ExtractedMemory(
    val content: String,
    val memoryType: String, // "fact", "preference", "insight", "event", "decision"
    val importance: String = "medium", // "low", "medium", "high"
    val context: String? = null,
    val sourceIdx: Int? = null, // Index of source in batch (for multi-screenshot processing)
    val structuredData: String? = null // JSON for preferences: category, sub_category, strength
)

/**
 * Extracted Entity
 * 
 * Output from AI entity extraction. Includes memory indices for relation creation.
 */
@Serializable
data class ExtractedEntity(
    val name: String,
    val entityType: String, // "person", "place", "preference", "project", "topic", etc.
    val description: String? = null,
    val mentionedInMemoryIndices: List<Int> = emptyList() // Which memories mention this entity
)

/**
 * Extracted Relation
 * 
 * Output from AI relation extraction. Represents entity-entity connections.
 */
@Serializable
data class ExtractedRelation(
    val sourceName: String,
    val targetName: String,
    val relationType: String,
    val description: String? = null
)

/**
 * Batch Extraction Result
 * 
 * Combined output from batch entity extraction.
 */
@Serializable
data class BatchEntityExtraction(
    val entities: List<ExtractedEntity>,
    val relations: List<ExtractedRelation>
)

/**
 * Extracted Knowledge
 * 
 * Complete extraction output from a screenshot/document.
 */
@Serializable
data class ExtractedKnowledge(
    val memories: List<ExtractedMemory> = emptyList(),
    val entities: List<ExtractedEntity> = emptyList(),
    val relations: List<ExtractedRelation> = emptyList()
)

// ============================================
// Pipeline Models
// ============================================

enum class ActionType {
    SAVE_MEMORY,
    TAKE_ACTION,
    BOTH
}

data class AnalysisResult(
    val actionType: ActionType,
    val context: String,
    val suggestedAction: String? = null
)

data class ActionResponse(
    val response: String,
    val memoriesUsed: List<Memory>
)

/**
 * Entity Match Result
 * 
 * Output from LLM entity resolution judge.
 */
@Serializable
data class EntityMatchResult(
    val isMatch: Boolean,
    val targetId: Long? = null,
    val reasoning: String
)

/**
 * Preference Structured Data
 * 
 * Structure for preference memory types.
 */
@Serializable
data class PreferenceData(
    val category: String, // cars, fashion, food, travel, tech, etc.
    val subCategory: String? = null, // luxury, budget, organic, etc.
    val strength: String = "moderate", // weak, moderate, strong
    val spendingContext: SpendingContext? = null
)

@Serializable
data class SpendingContext(
    val typicalSpend: Int? = null,
    val frequency: String? = null // daily, weekly, monthly, yearly
)
