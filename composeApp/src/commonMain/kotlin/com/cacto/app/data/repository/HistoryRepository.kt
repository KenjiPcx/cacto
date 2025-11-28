package com.cacto.app.data.repository

/**
 * History Repository
 * ==================
 *
 * PURPOSE:
 * Provides data access layer for processing history and debug information.
 * Tracks all screenshot processing runs with their steps and results.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline (logging), DebugScreen (display)
 * - Called from: Pipeline processing to log each step
 */

import com.cacto.app.data.model.ProcessingHistory
import com.cacto.app.data.model.ProcessingStatus
import com.cacto.app.data.model.ProcessingStep
import com.cacto.app.data.model.StepStatus
import com.cacto.app.db.CactoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class HistoryRepository(private val database: CactoDatabase) {
    
    private val queries = database.cactoDatabaseQueries
    
    // ============================================
    // Processing History
    // ============================================
    
    suspend fun startProcessing(screenshotPath: String): Long = withContext(Dispatchers.IO) {
        queries.insertProcessingHistory(screenshotPath, System.currentTimeMillis())
        queries.getLastInsertedHistoryId().executeAsOne()
    }
    
    suspend fun completeProcessing(
        historyId: Long,
        status: ProcessingStatus,
        actionType: String?,
        screenshotDescription: String?,
        memoriesSaved: Int,
        entitiesCreated: Int,
        relationsCreated: Int,
        generatedResponse: String?,
        errorMessage: String?
    ) = withContext(Dispatchers.IO) {
        queries.updateProcessingHistory(
            completed_at = System.currentTimeMillis(),
            status = status.name.lowercase(),
            action_type = actionType,
            screenshot_description = screenshotDescription,
            memories_saved = memoriesSaved.toLong(),
            entities_created = entitiesCreated.toLong(),
            relations_created = relationsCreated.toLong(),
            generated_response = generatedResponse,
            error_message = errorMessage,
            id = historyId
        )
    }
    
    suspend fun getAllHistory(): List<ProcessingHistory> = withContext(Dispatchers.IO) {
        queries.getAllProcessingHistory().executeAsList().map { row ->
            val steps = queries.getStepsForHistory(row.id).executeAsList().map { it.toStep() }
            row.toHistory(steps)
        }
    }
    
    suspend fun getHistoryById(id: Long): ProcessingHistory? = withContext(Dispatchers.IO) {
        queries.getProcessingHistoryById(id).executeAsOneOrNull()?.let { row ->
            val steps = queries.getStepsForHistory(id).executeAsList().map { it.toStep() }
            row.toHistory(steps)
        }
    }
    
    suspend fun getRecentHistory(limit: Long): List<ProcessingHistory> = withContext(Dispatchers.IO) {
        queries.getRecentProcessingHistory(limit).executeAsList().map { row ->
            val steps = queries.getStepsForHistory(row.id).executeAsList().map { it.toStep() }
            row.toHistory(steps)
        }
    }
    
    // ============================================
    // Processing Steps
    // ============================================
    
    suspend fun addStep(
        historyId: Long,
        stepName: String,
        details: String? = null
    ): Long = withContext(Dispatchers.IO) {
        queries.insertProcessingStep(
            history_id = historyId,
            step_name = stepName,
            step_status = StepStatus.RUNNING.name.lowercase(),
            started_at = System.currentTimeMillis(),
            details = details
        )
        queries.getLastInsertedStepId().executeAsOne()
    }
    
    suspend fun completeStep(
        stepId: Long,
        status: StepStatus,
        details: String? = null,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        queries.updateProcessingStep(
            step_status = status.name.lowercase(),
            completed_at = System.currentTimeMillis(),
            details = details,
            error_message = errorMessage,
            id = stepId
        )
    }
    
    // ============================================
    // Conversion
    // ============================================
    
    private fun com.cacto.app.db.Processing_history.toHistory(steps: List<ProcessingStep>): ProcessingHistory {
        return ProcessingHistory(
            id = id,
            screenshotPath = screenshot_path,
            startedAt = started_at,
            completedAt = completed_at,
            status = parseStatus(status),
            actionType = action_type,
            screenshotDescription = screenshot_description,
            memoriesSaved = memories_saved?.toInt() ?: 0,
            entitiesCreated = entities_created?.toInt() ?: 0,
            relationsCreated = relations_created?.toInt() ?: 0,
            generatedResponse = generated_response,
            errorMessage = error_message,
            steps = steps
        )
    }
    
    private fun com.cacto.app.db.Processing_steps.toStep(): ProcessingStep {
        return ProcessingStep(
            id = id,
            historyId = history_id,
            stepName = step_name,
            stepStatus = parseStepStatus(step_status),
            startedAt = started_at,
            completedAt = completed_at,
            details = details,
            errorMessage = error_message
        )
    }
    
    private fun parseStatus(status: String): ProcessingStatus {
        return try {
            ProcessingStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            ProcessingStatus.PROCESSING
        }
    }
    
    private fun parseStepStatus(status: String): StepStatus {
        return try {
            StepStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            StepStatus.PENDING
        }
    }
}

