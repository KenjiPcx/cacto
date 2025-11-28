package com.cacto.app.data

/**
 * Database Driver Factory (Android)
 * ==================================
 *
 * PURPOSE:
 * Android-specific implementation of DatabaseDriverFactory. Creates SQLDelight
 * Android SQLite driver using application context.
 *
 * WHERE USED:
 * - Actual implementation of: DatabaseDriverFactory (expect)
 * - Called from: AppModule.android.kt (dependency injection)
 * - Used in: Database initialization during app startup
 *
 * RELATIONSHIPS:
 * - Implements: DatabaseDriverFactory expect class
 * - Uses: Android Context for database file location
 * - Creates: AndroidSqliteDriver for SQLDelight
 *
 * USAGE IN ANDROID DATABASE SETUP:
 * - Called during Koin module initialization
 * - Creates Android-specific SQLite driver
 * - Database file stored in app's private data directory
 *
 * DESIGN PHILOSOPHY:
 * Platform-specific implementation using Android SQLite driver. Database file
 * name is "cacto.db". Uses application context for database file location.
 * Single responsibility: Android driver creation.
 */

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.cacto.app.db.CactoDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(CactoDatabase.Schema, context, "cacto.db")
    }
}

