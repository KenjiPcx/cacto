package com.cacto.app.di

/**
 * Dependency Injection Module (Android)
 * =====================================
 *
 * PURPOSE:
 * Android-specific dependency injection module. Provides platform-specific dependencies
 * including Android database driver factory and clipboard service. Actual implementation
 * of platform module expected in common code.
 *
 * WHERE USED:
 * - Loaded by: CactoApplication.onCreate() via platformModule()
 * - Referenced from: AppModule.kt (expect declaration)
 * - Used in: Koin dependency injection setup
 *
 * RELATIONSHIPS:
 * - Implements: platformModule() expect function
 * - Provides: DatabaseDriverFactory (Android), ClipboardService (Android)
 * - Uses: Android context from Koin
 * - Complements: sharedModule for complete dependency graph
 *
 * USAGE IN ANDROID DEPENDENCY INJECTION:
 * - Provides Android-specific implementations
 * - Creates database driver using Android SQLite
 * - Provides clipboard service using Android ClipboardManager
 * - Makes Android context available to dependencies
 *
 * DESIGN PHILOSOPHY:
 * Platform-specific dependency definitions. Uses Android context from Koin.
 * Provides actual implementations for expect declarations. Single responsibility:
 * Android platform dependencies only.
 */

import com.cacto.app.ClipboardService
import com.cacto.app.data.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { ClipboardService(androidContext()) }
}

