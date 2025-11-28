package com.cacto.app.data.repository

/**
 * Memory Repository
 * =================
 *
 * PURPOSE:
 * Provides data access layer for Memory entities. Handles all database operations
 * for storing and retrieving memories with their embeddings and metadata.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline, App composable
 * - Called from: CactoPipeline.processMemories() (insertion)
 * - Used in: App composable (retrieval for UI), VectorSearch (loading memories)
 *
 * DESIGN PHILOSOPHY:
 * All database operations wrapped in coroutines with Dispatchers.IO for thread safety.
 * Embeddings serialized as JSON for SQLite storage. Provides both suspend functions
 * and Flow-based reactive queries.
 */

import com.cacto.app.data.model.Importance
import com.cacto.app.data.model.Memory
import com.cacto.app.data.model.MemoryType
import com.cacto.app.db.CactoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MemoryRepository(private val database: CactoDatabase) {
    
    private val queries = database.cactoDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }
    
    suspend fun getAllMemories(): List<Memory> = withContext(Dispatchers.IO) {
        queries.getAllMemories().executeAsList().map { it.toMemory() }
    }
    
    fun getAllMemoriesFlow(): Flow<List<Memory>> = flow {
        emit(getAllMemories())
    }
    
    suspend fun getMemoryById(id: Long): Memory? = withContext(Dispatchers.IO) {
        queries.getMemoryById(id).executeAsOneOrNull()?.toMemory()
    }
    
    suspend fun insertMemory(memory: Memory): Long = withContext(Dispatchers.IO) {
        val embeddingJson = json.encodeToString(memory.embedding)
        queries.insertMemory(
            content = memory.content,
            memory_type = memory.memoryType.name.lowercase(),
            importance = memory.importance.name.lowercase(),
            context = memory.context,
            embedding = embeddingJson,
            created_at = memory.createdAt,
            source_type = memory.sourceType,
            image_path = memory.imagePath,
            structured_data = memory.structuredData
        )
        queries.getLastInsertedMemoryId().executeAsOne()
    }
    
    suspend fun deleteMemory(id: Long) = withContext(Dispatchers.IO) {
        queries.deleteMemory(id)
    }
    
    suspend fun searchMemories(query: String): List<Memory> = withContext(Dispatchers.IO) {
        queries.searchMemoriesByContent(query).executeAsList().map { it.toMemory() }
    }
    
    suspend fun getRecentMemories(limit: Long): List<Memory> = withContext(Dispatchers.IO) {
        queries.getRecentMemories(limit).executeAsList().map { it.toMemory() }
    }
    
    suspend fun getMemoriesByType(type: MemoryType): List<Memory> = withContext(Dispatchers.IO) {
        queries.getMemoriesByType(type.name.lowercase()).executeAsList().map { it.toMemory() }
    }
    
    suspend fun getImportantMemories(): List<Memory> = withContext(Dispatchers.IO) {
        queries.getImportantMemories().executeAsList().map { it.toMemory() }
    }
    
    suspend fun getMemoriesWithEmbeddings(): List<Memory> = withContext(Dispatchers.IO) {
        queries.getMemoriesWithEmbeddings().executeAsList().map { row ->
            val embeddingList: List<Double> = try {
                json.decodeFromString(row.embedding)
            } catch (e: Exception) {
                emptyList()
            }
            Memory(
                id = row.id,
                content = row.content,
                embedding = embeddingList,
                memoryType = MemoryType.FACT,
                importance = Importance.MEDIUM,
                createdAt = 0L
            )
        }
    }
    
    suspend fun getMemoryCount(): Long = withContext(Dispatchers.IO) {
        queries.getMemoryCount().executeAsOne()
    }
    
    private fun com.cacto.app.db.Memories.toMemory(): Memory {
        val embeddingList: List<Double> = try {
            json.decodeFromString(embedding)
        } catch (e: Exception) {
            emptyList()
        }
        return Memory(
            id = id,
            content = content,
            memoryType = parseMemoryType(memory_type),
            importance = parseImportance(importance),
            context = context,
            embedding = embeddingList,
            createdAt = created_at,
            sourceType = source_type,
            imagePath = image_path,
            structuredData = structured_data
        )
    }
    
    private fun parseMemoryType(type: String): MemoryType {
        return try {
            MemoryType.valueOf(type.uppercase())
        } catch (e: Exception) {
            MemoryType.FACT
        }
    }
    
    private fun parseImportance(importance: String): Importance {
        return try {
            Importance.valueOf(importance.uppercase())
        } catch (e: Exception) {
            Importance.MEDIUM
        }
    }
}
