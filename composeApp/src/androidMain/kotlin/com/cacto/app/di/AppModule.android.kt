package com.cacto.app.di

/**
 * Dependency Injection Module (Android)
 * =====================================
 *
 * PURPOSE:
 * Android-specific dependency injection module. Provides platform-specific
 * dependencies including database driver, clipboard service, and screenshot observer.
 *
 * WHERE USED:
 * - Loaded by: CactoApplication.onCreate() via platformModule()
 * - Used in: Koin dependency injection setup
 */

import com.cacto.app.ClipboardService
import com.cacto.app.ScreenshotObserver
import com.cacto.app.data.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { ClipboardService(androidContext()) }
    single { ScreenshotObserver(androidContext()) }
}
