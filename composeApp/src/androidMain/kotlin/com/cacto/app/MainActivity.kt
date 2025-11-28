package com.cacto.app

/**
 * Main Activity
 * =============
 *
 * PURPOSE:
 * Main entry point for the Android application. Initializes Cactus SDK context,
 * sets up Compose UI, and provides dependencies to App composable. Handles app lifecycle.
 *
 * WHERE USED:
 * - Launched by: Android system when app starts
 * - Entry point: Main activity declared in AndroidManifest.xml
 * - Contains: App composable rendering
 *
 * RELATIONSHIPS:
 * - Uses: Dependency injection (Koin) for pipeline and repositories
 * - Initializes: Cactus SDK context (required for Cactus SDK)
 * - Renders: App composable with dependencies
 * - Manages: Activity lifecycle
 *
 * USAGE IN APPLICATION STARTUP:
 * - First activity launched when user opens app
 * - Initializes Cactus SDK before any AI operations
 * - Sets up edge-to-edge display for modern Android UI
 * - Provides dependencies to App composable via injection
 *
 * DESIGN PHILOSOPHY:
 * Minimal activity that delegates to Compose. Uses dependency injection for
 * clean architecture. Initializes Cactus SDK context early (required for SDK).
 * Enables edge-to-edge for immersive UI. Single responsibility: app initialization.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.MemoryRepository
import com.cactus.CactusContextInitializer
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    private val pipeline: CactoPipeline by inject()
    private val memoryRepository: MemoryRepository by inject()
    private val entityRepository: EntityRepository by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Cactus context (required for Cactus SDK)
        CactusContextInitializer.initialize(this)
        
        enableEdgeToEdge()
        
        setContent {
            App(
                pipeline = pipeline,
                memoryRepository = memoryRepository,
                entityRepository = entityRepository
            )
        }
    }
}

