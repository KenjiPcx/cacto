package com.cacto.app.data

/**
 * Database Driver Factory (Common)
 * =================================
 *
 * PURPOSE:
 * Expect declaration for platform-specific database driver creation.
 * Provides abstraction for SQLDelight driver initialization across platforms.
 *
 * WHERE USED:
 * - Imported by: AppModule (dependency injection)
 * - Implemented by: DatabaseDriverFactory.android.kt (Android)
 * - Used in: Koin module setup for database initialization
 *
 * RELATIONSHIPS:
 * - Expect/Actual pattern: Common expect, Android actual implementation
 * - Creates: SqlDriver for SQLDelight database access
 * - Used by: CactoDatabase initialization
 *
 * USAGE IN DATABASE SETUP:
 * - Called during app initialization via dependency injection
 * - Creates platform-specific SQLDelight driver
 * - Provides driver to CactoDatabase for query execution
 *
 * DESIGN PHILOSOPHY:
 * Uses Kotlin Multiplatform expect/actual pattern for platform abstraction.
 * Allows platform-specific driver implementations (Android SQLite, iOS SQLite, etc.).
 * Single responsibility: driver creation only.
 */

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

