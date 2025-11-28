package com.cacto.app

/**
 * Cacto Application
 * =================
 *
 * PURPOSE:
 * Application class that initializes dependency injection (Koin) for the entire app.
 * Sets up Koin modules (shared and platform-specific) and provides Android context.
 *
 * WHERE USED:
 * - Declared in: AndroidManifest.xml as application class
 * - Initialized by: Android system at app startup
 * - Entry point: First class instantiated when app launches
 *
 * RELATIONSHIPS:
 * - Initializes: Koin dependency injection framework
 * - Loads: sharedModule (common dependencies) and platformModule() (Android-specific)
 * - Provides: Android context to dependency injection
 * - Sets up: Logging level for Koin
 *
 * USAGE IN DEPENDENCY INJECTION:
 * - Called automatically by Android system at app startup
 * - Initializes Koin with all modules
 * - Makes dependencies available throughout app lifecycle
 * - Provides Android context to platform-specific modules
 *
 * DESIGN PHILOSOPHY:
 * Single responsibility: dependency injection setup. Minimal application class.
 * Initializes Koin early in app lifecycle. Loads both shared and platform modules.
 * Sets appropriate logging level for production use.
 */

import android.app.Application
import com.cacto.app.di.platformModule
import com.cacto.app.di.sharedModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class CactoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@CactoApplication)
            modules(platformModule(), sharedModule)
        }
    }
}

