package com.cacto.app.data.repository

/**
 * History Repository
 *
 * PURPOSE:
 * Provides data access for processing history and debug steps.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline
 * - Called from: DebugScreen
 *
 * RELATIONSHIPS:
 * - Uses: CactoDatabase (SQLDelight)
 * - Maps: Database rows -> ProcessingHistory/ProcessingStep models
 */

import com.cacto.app.data.model.ProcessingHistory
import com.cacto.app.data.model.ProcessingStatus
import com.cacto.app.data.model.ProcessingStep
import com.cacto.app.data.model.StepStatus
import com.cacto.app.db.CactoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryRepository(private val db: CactoDatabase) {
    private val queries = db.cactoDatabaseQueries

    suspend fun getAllHistory(): List<ProcessingHistory> = withContext(Dispatchers.IO) {
        try {
            queries.getAllProcessingHistory().executeAsList().map { row ->
                ProcessingHistory(
                    id = row.id,
                    screenshotPath = row.screenshot_path,
                    startedAt = row.started_at,
                    completedAt = row.completed_at,
                    status = try {
                        ProcessingStatus.valueOf(row.status)
                    } catch (e: Exception) {
                        ProcessingStatus.PROCESSING
                    },
                    actionType = row.action_type,
                    screenshotDescription = row.screenshot_description,
                    memoriesSaved = row.memories_saved?.toInt() ?: 0,
                    entitiesCreated = row.entities_created?.toInt() ?: 0,
                    relationsCreated = row.relations_created?.toInt() ?: 0,
                    generatedResponse = row.generated_response,
                    errorMessage = row.error_message
                )
            }
        } catch (e: Exception) {
            // Table might not exist if migration failed or old DB version
            println("Error fetching history: ${e.message}")
            emptyList()
        }
    }

    suspend fun getStepsForHistory(historyId: Long): List<ProcessingStep> = withContext(Dispatchers.IO) {
        try {
            queries.getStepsForHistory(historyId).executeAsList().map { row ->
                ProcessingStep(
                    id = row.id,
                    historyId = row.history_id,
                    stepName = row.step_name,
                    stepStatus = try {
                        StepStatus.valueOf(row.step_status)
                    } catch (e: Exception) {
                        StepStatus.PENDING
                    },
                    startedAt = row.started_at,
                    completedAt = row.completed_at,
                    details = row.details,
                    errorMessage = row.error_message
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun startProcessing(screenshotPath: String): Long = withContext(Dispatchers.IO) {
        try {
            queries.insertProcessingHistory(
                screenshot_path = screenshotPath,
                started_at = System.currentTimeMillis()
            )
            queries.getLastInsertedHistoryId().executeAsOne()
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun updateProcessing(
        id: Long,
        status: String,
        actionType: String? = null,
        description: String? = null,
        memoriesSaved: Int = 0,
        entitiesCreated: Int = 0,
        relationsCreated: Int = 0,
        response: String? = null,
        error: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            queries.updateProcessingHistory(
                completed_at = if (status != "processing") System.currentTimeMillis() else null,
                status = status,
                action_type = actionType,
                screenshot_description = description,
                memories_saved = memoriesSaved.toLong(),
                entities_created = entitiesCreated.toLong(),
                relations_created = relationsCreated.toLong(),
                generated_response = response,
                error_message = error,
                id = id
            )
        } catch (e: Exception) {
            println("Error updating history: ${e.message}")
        }
    }

    suspend fun addStep(
        historyId: Long,
        name: String,
        details: String? = null
    ): Long = withContext(Dispatchers.IO) {
        try {
            queries.insertProcessingStep(
                history_id = historyId,
                step_name = name,
                step_status = "processing",
                started_at = System.currentTimeMillis(),
                details = details
            )
            queries.getLastInsertedStepId().executeAsOne()
        } catch (e: Exception) {
            -1L
        }
    }

    suspend fun updateStep(
        stepId: Long,
        status: String,
        details: String? = null,
        error: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            queries.updateProcessingStep(
                step_status = status,
                completed_at = System.currentTimeMillis(),
                details = details,
                error_message = error,
                id = stepId
            )
        } catch (e: Exception) {
            println("Error updating step: ${e.message}")
        }
    }
}
