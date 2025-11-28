package com.cacto.app.ai

/**
 * Vector Search
 * =============
 *
 * PURPOSE:
 * Provides vector similarity search functionality using cosine similarity.
 * Finds relevant memories based on embedding vectors. Supports text-to-vector
 * search and various vector operations (normalization, averaging).
 *
 * WHERE USED:
 * - Imported by: CactoPipeline
 * - Called from: CactoPipeline.processAction() (finding relevant memories)
 * - Used in workflows: Response generation (context retrieval)
 *
 * RELATIONSHIPS:
 * - Operates on: Memory objects with embeddings
 * - Uses: Cosine similarity algorithm
 * - Provides: Similarity scoring and ranking
 *
 * USAGE IN VECTOR SEARCH:
 * - Called during action generation to find relevant context
 * - Compares query embedding (from screenshot description) with memory embeddings
 * - Returns top-K most similar memories above similarity threshold
 * - Supports text queries via embedding generation
 *
 * DESIGN PHILOSOPHY:
 * Pure utility object with no side effects. Provides efficient cosine similarity
 * calculation. Filters and ranks results in a single pass. Supports configurable
 * similarity thresholds and result limits. Stateless design for easy testing.
 */

import com.cacto.app.data.model.Memory
import kotlin.math.sqrt

object VectorSearch {
    
    /**
     * Calculate cosine similarity between two vectors
     */
    fun cosineSimilarity(a: List<Double>, b: List<Double>): Double {
        if (a.isEmpty() || b.isEmpty() || a.size != b.size) return 0.0
        
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }
    
    /**
     * Find the most similar memories to a query embedding
     */
    fun findSimilarMemories(
        queryEmbedding: List<Double>,
        memories: List<Memory>,
        topK: Int = 5,
        minSimilarity: Double = 0.0
    ): List<Pair<Memory, Double>> {
        if (queryEmbedding.isEmpty()) return emptyList()
        
        return memories
            .filter { it.embedding.isNotEmpty() }
            .map { memory ->
                memory to cosineSimilarity(queryEmbedding, memory.embedding)
            }
            .filter { it.second >= minSimilarity }
            .sortedByDescending { it.second }
            .take(topK)
    }
    
    /**
     * Find memories similar to a text query (requires embedding generation)
     */
    suspend fun findSimilarMemoriesByText(
        query: String,
        memories: List<Memory>,
        embedder: suspend (String) -> List<Double>,
        topK: Int = 5,
        minSimilarity: Double = 0.0
    ): List<Pair<Memory, Double>> {
        val queryEmbedding = embedder(query)
        return findSimilarMemories(queryEmbedding, memories, topK, minSimilarity)
    }
    
    /**
     * Calculate average embedding from multiple embeddings
     */
    fun averageEmbedding(embeddings: List<List<Double>>): List<Double> {
        if (embeddings.isEmpty()) return emptyList()
        
        val size = embeddings.first().size
        if (embeddings.any { it.size != size }) return emptyList()
        
        return (0 until size).map { i ->
            embeddings.sumOf { it[i] } / embeddings.size
        }
    }
    
    /**
     * Normalize a vector to unit length
     */
    fun normalize(vector: List<Double>): List<Double> {
        val magnitude = sqrt(vector.sumOf { it * it })
        return if (magnitude == 0.0) vector else vector.map { it / magnitude }
    }
}

