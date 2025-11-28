package com.cacto.app.di

/**
 * Dependency Injection Module (Common)
 * =====================================
 *
 * PURPOSE:
 * Defines shared dependency injection module using Koin. Provides all common
 * dependencies including database, repositories, AI services, and pipeline.
 *
 * WHERE USED:
 * - Loaded by: CactoApplication.onCreate()
 * - Referenced from: Platform-specific AppModule (Android)
 *
 * DESIGN PHILOSOPHY:
 * Centralized dependency definitions. Uses singleton scope for most dependencies.
 * Separates shared and platform-specific concerns via expect/actual pattern.
 */

import com.cacto.app.ai.ActionGenerator
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.CactusService
import com.cacto.app.ai.EntityResolutionService
import com.cacto.app.ai.MemoryExtractor
import com.cacto.app.data.DatabaseDriverFactory
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.HistoryRepository
import com.cacto.app.data.repository.MemoryRepository
import com.cacto.app.db.CactoDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformModule(): Module

val sharedModule = module {
    // Database
    single { get<DatabaseDriverFactory>().createDriver() }
    single { CactoDatabase(get()) }
    
    // Repositories
    single { MemoryRepository(get()) }
    single { EntityRepository(get()) }
    single { HistoryRepository(get()) }
    
    // AI Services
    single { CactusService() }
    single { MemoryExtractor(get()) }
    single { ActionGenerator(get()) }
    
    // Entity Resolution Service
    single { 
        EntityResolutionService(
            entityRepository = get(),
            memoryExtractor = get()
        )
    }
    
    // Pipeline
    single { 
        CactoPipeline(
            cactusService = get(),
            memoryExtractor = get(),
            actionGenerator = get(),
            memoryRepository = get(),
            entityRepository = get(),
            historyRepository = get()
        )
    }
}
