package com.cacto.app.data.repository

/**
 * Entity Repository
 * =================
 *
 * PURPOSE:
 * Provides data access layer for Entity and Relation entities. Handles knowledge
 * graph operations including entity creation, relation linking, and graph construction.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline, EntityResolutionService, App composable
 * - Called from: CactoPipeline.processMemories(), EntityResolutionService
 * - Used in: Knowledge graph visualization, entity resolution
 *
 * DESIGN PHILOSOPHY:
 * All database operations wrapped in coroutines with Dispatchers.IO. Provides
 * getOrCreateEntity() for safe entity creation (prevents duplicates). Stores
 * entity embeddings for vector similarity search.
 */

import com.cacto.app.data.model.Entity
import com.cacto.app.data.model.EntityType
import com.cacto.app.data.model.GraphEdge
import com.cacto.app.data.model.GraphNode
import com.cacto.app.data.model.KnowledgeGraph
import com.cacto.app.data.model.Relation
import com.cacto.app.data.model.RelationSourceType
import com.cacto.app.db.CactoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EntityRepository(private val database: CactoDatabase) {
    
    private val queries = database.cactoDatabaseQueries
    private val json = Json { ignoreUnknownKeys = true }
    
    // ============================================
    // Entity Operations
    // ============================================
    
    suspend fun getAllEntities(): List<Entity> = withContext(Dispatchers.IO) {
        queries.getAllEntities().executeAsList().map { it.toEntity() }
    }
    
    suspend fun getEntityById(id: Long): Entity? = withContext(Dispatchers.IO) {
        queries.getEntityById(id).executeAsOneOrNull()?.toEntity()
    }
    
    suspend fun getEntityByNameAndType(name: String, type: EntityType): Entity? = withContext(Dispatchers.IO) {
        queries.getEntityByNameAndType(name, type.name.lowercase()).executeAsOneOrNull()?.toEntity()
    }
    
    suspend fun getEntityByNameCaseInsensitive(name: String, type: EntityType): Entity? = withContext(Dispatchers.IO) {
        queries.getEntityByNameCaseInsensitive(name, type.name.lowercase()).executeAsOneOrNull()?.toEntity()
    }
    
    suspend fun insertEntity(entity: Entity): Long = withContext(Dispatchers.IO) {
        val embeddingJson = if (entity.embedding.isNotEmpty()) {
            json.encodeToString(entity.embedding)
        } else null
        
        queries.insertEntity(
            name = entity.name,
            entity_type = entity.type.name.lowercase(),
            description = entity.description,
            embedding = embeddingJson,
            created_at = entity.createdAt
        )
        
        // Get the inserted entity ID (or existing if duplicate)
        val existing = queries.getEntityByNameAndType(entity.name, entity.type.name.lowercase())
            .executeAsOneOrNull()
        existing?.id ?: queries.getLastInsertedEntityId().executeAsOne()
    }
    
    suspend fun updateEntityEmbedding(entityId: Long, embedding: List<Double>) = withContext(Dispatchers.IO) {
        val embeddingJson = json.encodeToString(embedding)
        queries.updateEntityEmbedding(embeddingJson, entityId)
    }
    
    suspend fun getOrCreateEntity(name: String, type: EntityType): Long = withContext(Dispatchers.IO) {
        val existing = queries.getEntityByNameCaseInsensitive(name, type.name.lowercase())
            .executeAsOneOrNull()
        
        if (existing != null) {
            existing.id
        } else {
            queries.insertEntity(
                name = name,
                entity_type = type.name.lowercase(),
                description = null,
                embedding = null,
                created_at = System.currentTimeMillis()
            )
            queries.getLastInsertedEntityId().executeAsOne()
        }
    }
    
    suspend fun deleteEntity(id: Long) = withContext(Dispatchers.IO) {
        queries.deleteEntity(id)
    }
    
    suspend fun getEntitiesByType(type: EntityType): List<Entity> = withContext(Dispatchers.IO) {
        queries.getEntitiesByType(type.name.lowercase()).executeAsList().map { it.toEntity() }
    }
    
    suspend fun getEntitiesWithEmbeddings(): List<Entity> = withContext(Dispatchers.IO) {
        queries.getEntitiesWithEmbeddings().executeAsList().map { row ->
            val embeddingList: List<Double> = try {
                row.embedding?.let { json.decodeFromString(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            Entity(
                id = row.id,
                name = row.name,
                type = parseEntityType(row.entity_type),
                description = row.description,
                embedding = embeddingList,
                createdAt = 0L
            )
        }
    }
    
    suspend fun getEntityCount(): Long = withContext(Dispatchers.IO) {
        queries.getEntityCount().executeAsOne()
    }
    
    // ============================================
    // Relation Operations
    // ============================================
    
    suspend fun getAllRelations(): List<Relation> = withContext(Dispatchers.IO) {
        queries.getAllRelations().executeAsList().map { it.toRelation() }
    }
    
    suspend fun insertRelation(relation: Relation): Long = withContext(Dispatchers.IO) {
        queries.insertRelation(
            source_type = relation.sourceType.name.lowercase(),
            source_entity_id = relation.sourceEntityId,
            target_entity_id = relation.targetEntityId,
            relation_type = relation.relationType,
            description = relation.description,
            memory_id = relation.memoryId,
            created_at = relation.createdAt
        )
        queries.getRelationCount().executeAsOne()
    }
    
    suspend fun deleteRelation(id: Long) = withContext(Dispatchers.IO) {
        queries.deleteRelation(id)
    }
    
    suspend fun getRelationCount(): Long = withContext(Dispatchers.IO) {
        queries.getRelationCount().executeAsOne()
    }
    
    // ============================================
    // Memory-Entity Links
    // ============================================
    
    suspend fun linkMemoryToEntity(memoryId: Long, entityId: Long) = withContext(Dispatchers.IO) {
        queries.insertMemoryEntityLink(
            memory_id = memoryId,
            entity_id = entityId,
            created_at = System.currentTimeMillis()
        )
    }
    
    // ============================================
    // Knowledge Graph
    // ============================================
    
    suspend fun getKnowledgeGraph(): KnowledgeGraph = withContext(Dispatchers.IO) {
        val entities = queries.getAllEntities().executeAsList()
        val graphData = queries.getGraphData().executeAsList()
        
        val nodes = entities.map { entity ->
            GraphNode(
                id = entity.id,
                name = entity.name,
                type = entity.entity_type,
                description = entity.description
            )
        }
        
        val edges = graphData.map { row ->
            GraphEdge(
                id = row.relation_id,
                source = row.source_id,
                target = row.target_id,
                label = row.relation_type,
                description = row.relation_description
            )
        }
        
        KnowledgeGraph(nodes = nodes, edges = edges)
    }
    
    // ============================================
    // Conversion Functions
    // ============================================
    
    private fun com.cacto.app.db.Entities.toEntity(): Entity {
        val embeddingList: List<Double> = try {
            embedding?.let { json.decodeFromString(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return Entity(
            id = id,
            name = name,
            type = parseEntityType(entity_type),
            description = description,
            embedding = embeddingList,
            createdAt = created_at
        )
    }
    
    private fun com.cacto.app.db.Relations.toRelation(): Relation {
        return Relation(
            id = id,
            sourceType = parseRelationSourceType(source_type),
            sourceEntityId = source_entity_id,
            targetEntityId = target_entity_id,
            relationType = relation_type,
            description = description,
            memoryId = memory_id,
            createdAt = created_at
        )
    }
    
    private fun parseEntityType(type: String): EntityType {
        return try {
            EntityType.valueOf(type.uppercase())
        } catch (e: Exception) {
            EntityType.OTHER
        }
    }
    
    private fun parseRelationSourceType(type: String): RelationSourceType {
        return try {
            RelationSourceType.valueOf(type.uppercase())
        } catch (e: Exception) {
            RelationSourceType.ENTITY
        }
    }
}
