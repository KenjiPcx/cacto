package com.cacto.app

/**
 * Screenshot Observer
 * ===================
 *
 * PURPOSE:
 * Listens for new screenshots using ContentObserver on MediaStore.
 * Automatically triggers Cacto pipeline when user takes a screenshot.
 *
 * WHERE USED:
 * - Started by: MainActivity when app is in foreground
 * - Or by: Foreground service for background monitoring
 *
 * DESIGN PHILOSOPHY:
 * Uses MediaStore.Images ContentObserver to detect new screenshots.
 * Filters by path containing "screenshot" (case-insensitive).
 * Debounces rapid events. No accessibility service needed.
 */

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ScreenshotObserver(
    private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    
    private var observer: ContentObserver? = null
    private var lastProcessedUri: String? = null
    private var lastProcessedTime: Long = 0
    
    // Emit screenshot paths when detected
    private val _screenshots = MutableSharedFlow<String>()
    val screenshots: SharedFlow<String> = _screenshots.asSharedFlow()
    
    companion object {
        private const val TAG = "ScreenshotObserver"
        private const val DEBOUNCE_MS = 1000L // Ignore duplicates within 1 second
    }
    
    fun startObserving() {
        if (observer != null) return
        
        observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let { handleNewMedia(it) }
            }
        }
        
        // Watch for new images
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer!!
        )
        
        Log.d(TAG, "Started observing screenshots")
    }
    
    fun stopObserving() {
        observer?.let {
            contentResolver.unregisterContentObserver(it)
            observer = null
        }
        Log.d(TAG, "Stopped observing screenshots")
    }
    
    private fun handleNewMedia(uri: Uri) {
        scope.launch {
            try {
                // Debounce - ignore if same URI within debounce period
                val now = System.currentTimeMillis()
                if (uri.toString() == lastProcessedUri && now - lastProcessedTime < DEBOUNCE_MS) {
                    return@launch
                }
                
                // Small delay to ensure file is written
                delay(300)
                
                // Query the media store for this URI
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATE_ADDED
                )
                
                // Try to get the latest screenshot
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )
                
                cursor?.use {
                    if (it.moveToFirst()) {
                        val pathIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                        val nameIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        
                        val path = it.getString(pathIndex)
                        val name = it.getString(nameIndex).lowercase()
                        
                        // Check if it's a screenshot
                        if (isScreenshot(path, name)) {
                            Log.d(TAG, "Screenshot detected: $path")
                            
                            lastProcessedUri = uri.toString()
                            lastProcessedTime = now
                            
                            _screenshots.emit(path)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling new media", e)
            }
        }
    }
    
    private fun isScreenshot(path: String?, name: String): Boolean {
        if (path == null) return false
        
        val pathLower = path.lowercase()
        
        // Common screenshot paths/names
        return pathLower.contains("screenshot") ||
               pathLower.contains("screen_shot") ||
               pathLower.contains("screen shot") ||
               pathLower.contains("/screenshots/") ||
               name.startsWith("screenshot") ||
               name.contains("screenshot")
    }
}

