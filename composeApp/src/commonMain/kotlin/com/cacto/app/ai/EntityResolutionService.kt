package com.cacto.app.ai

/**
 * Entity Resolution Service
 * =========================
 *
 * PURPOSE:
 * Handles "Smart Upsert" logic for entities to prevent duplicates across different sources.
 * Uses a 3-step resolution process: exact name match (fastest), vector similarity search
 * (semantic candidate generation), and LLM verification (judge) for high-similarity candidates.
 * Caches resolved entities to avoid redundant processing.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline
 * - Called from: CactoPipeline.processMemories() during batch entity extraction
 * - Used in: Knowledge graph building to prevent duplicate entities
 *
 * RELATIONSHIPS:
 * - Uses: EntityRepository for database operations
 * - Uses: MemoryExtractor for embedding generation and LLM verification
 * - Uses: VectorSearch for cosine similarity calculations
 * - Produces: Resolved Entity objects (existing or newly created)
 *
 * USAGE IN ENTITY RESOLUTION:
 * - Called for each extracted entity during batch processing
 * - Checks cache first for recently resolved entities
 * - Performs exact name match (case-insensitive) for fast path
 * - Uses vector similarity search to find semantic candidates
 * - LLM verifies high-similarity candidates before merging
 * - Creates new entity if no match is confirmed
 *
 * DESIGN PHILOSOPHY:
 * Conservative resolution - when unsure, create new entity rather than merge.
 * Uses vector embeddings for semantic similarity. LLM judge provides final
 * verification for ambiguous cases. Caches resolved entities to avoid re-processing.
 * Three-tier approach balances speed (exact match) with accuracy (vector + LLM).
 * Similarity threshold (default 0.75) filters low-confidence candidates early.
 */

import com.cacto.app.data.model.Entity
import com.cacto.app.data.model.EntityType
import com.cacto.app.data.repository.EntityRepository

class EntityResolutionService(
    private val entityRepository: EntityRepository,
    private val memoryExtractor: MemoryExtractor,
    private val similarityThreshold: Double = 0.75
) {
    
    // Cache of resolved entities (name.lowercase -> entity id)
    private val resolvedCache = mutableMapOf<String, Long>()
    
    /**
     * Resolve an entity: Find existing match or create new.
     * 
     * Process:
     * 1. Check cache first
     * 2. Exact Name Match (Fastest)
     * 3. Vector Search (Semantic Candidate Generation)
     * 4. LLM Verification (Judge) if similarity score is high enough
     * 5. Create or Return Existing
     */
    suspend fun resolveEntity(
        name: String,
        entityType: EntityType,
        description: String? = null,
        memoryContext: String? = null
    ): Entity {
        val normalizedName = name.lowercase().trim()
        
        // 1. Check cache
        resolvedCache[normalizedName]?.let { cachedId ->
            entityRepository.getEntityById(cachedId)?.let { entity ->
                return entity
            }
        }
        
        // 2. Exact Match Check (case-insensitive)
        entityRepository.getEntityByNameCaseInsensitive(name, entityType)?.let { exactMatch ->
            resolvedCache[normalizedName] = exactMatch.id
            return exactMatch
        }
        
        // 3. Vector Similarity Search
        val candidates = findSimilarEntities(name, entityType, description)
        
        if (candidates.isEmpty()) {
            // No similar candidates - create new entity
            return createAndCacheEntity(name, entityType, description)
        }
        
        // 4. LLM Judge for high-similarity candidates
        val match = verifyMatchWithLLM(name, entityType.name, description, memoryContext, candidates)
        
        if (match != null && match.first) {
            // Match confirmed - return existing entity
            val matchedEntity = candidates.find { it.first.id == match.second }?.first
            if (matchedEntity != null) {
                resolvedCache[normalizedName] = matchedEntity.id
                return matchedEntity
            }
        }
        
        // 5. No match confirmed - create new entity
        return createAndCacheEntity(name, entityType, description)
    }
    
    /**
     * Find entities with similar embeddings.
     * Returns list of (Entity, similarity score) pairs.
     */
    private suspend fun findSimilarEntities(
        name: String,
        entityType: EntityType,
        description: String?
    ): List<Pair<Entity, Double>> {
        // Create query text for embedding
        val queryText = buildString {
            append("$name ($entityType)")
            description?.let { append(": $it") }
        }
        
        // Generate query embedding
        val queryEmbedding = memoryExtractor.generateEmbedding(queryText).getOrNull()
            ?: return emptyList()
        
        // Get all entities with embeddings
        val allEntities = entityRepository.getEntitiesWithEmbeddings()
        
        // Calculate similarity and filter by threshold
        return allEntities
            .filter { it.embedding.isNotEmpty() }
            .map { entity ->
                val similarity = VectorSearch.cosineSimilarity(queryEmbedding, entity.embedding)
                entity to similarity
            }
            .filter { it.second >= similarityThreshold }
            .sortedByDescending { it.second }
            .take(3) // Top 3 candidates
    }
    
    /**
     * Use LLM to verify if entity matches any candidate.
     * Returns (isMatch, matchedEntityId) or null if no match.
     */
    private suspend fun verifyMatchWithLLM(
        newName: String,
        newType: String,
        newDescription: String?,
        memoryContext: String?,
        candidates: List<Pair<Entity, Double>>
    ): Pair<Boolean, Long?>? {
        if (candidates.isEmpty()) return null
        
        // Format candidates for prompt
        val candidatesDescription = candidates.joinToString("\n") { (entity, score) ->
            "- ID: ${entity.id} | Name: ${entity.name} | Type: ${entity.type} | " +
                "Score: ${String.format("%.2f", score)} | Desc: ${entity.description ?: "N/A"}"
        }
        
        val result = memoryExtractor.resolveEntity(
            newName = newName,
            newType = newType,
            newDescription = newDescription,
            memoryContext = memoryContext,
            candidatesDescription = candidatesDescription
        ).getOrNull()
        
        return result?.let { Pair(it.isMatch, it.targetId) }
    }
    
    /**
     * Create a new entity with embedding and cache it.
     */
    private suspend fun createAndCacheEntity(
        name: String,
        entityType: EntityType,
        description: String?
    ): Entity {
        // Generate embedding for the entity
        val embeddingText = buildString {
            append("$name ($entityType)")
            description?.let { append(": $it") }
        }
        val embedding = memoryExtractor.generateEmbedding(embeddingText).getOrNull() ?: emptyList()
        
        // Create entity
        val entity = Entity(
            name = name,
            type = entityType,
            description = description,
            embedding = embedding,
            createdAt = System.currentTimeMillis()
        )
        
        val entityId = entityRepository.insertEntity(entity)
        val savedEntity = entity.copy(id = entityId)
        
        // Cache it
        resolvedCache[name.lowercase().trim()] = entityId
        
        return savedEntity
    }
    
    /**
     * Get or create entity by name and type (simple deduplication).
     * Uses exact match or creates new.
     */
    suspend fun getOrCreateEntity(name: String, entityType: EntityType): Long {
        return entityRepository.getOrCreateEntity(name, entityType)
    }
    
    /**
     * Clear the resolution cache (e.g., after batch processing).
     */
    fun clearCache() {
        resolvedCache.clear()
    }
}

