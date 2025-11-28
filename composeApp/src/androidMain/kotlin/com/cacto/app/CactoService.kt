package com.cacto.app

/**
 * Cacto Foreground Service
 * ========================
 *
 * PURPOSE:
 * Background service that listens for screenshots and processes them automatically.
 * Runs as a foreground service with a persistent notification.
 *
 * WHERE USED:
 * - Started by: MainActivity when user enables auto-capture
 * - Runs in: Background, survives app being minimized
 *
 * DESIGN PHILOSOPHY:
 * Foreground service for reliable screenshot detection. Shows persistent
 * notification so user knows Cacto is active. Processes screenshots
 * automatically and shows results via notification.
 */

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.PipelineStatus
import com.cactus.CactusContextInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CactoService : Service() {
    
    private val pipeline: CactoPipeline by inject()
    private val clipboardService: ClipboardService by inject()
    
    private var screenshotObserver: ScreenshotObserver? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var observerJob: Job? = null
    
    companion object {
        private const val TAG = "CactoService"
        private const val CHANNEL_ID = "cacto_service"
        private const val NOTIFICATION_ID = 1
        private const val RESULT_NOTIFICATION_ID = 2
        
        const val ACTION_START = "com.cacto.app.START"
        const val ACTION_STOP = "com.cacto.app.STOP"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Initialize Cactus context
        CactusContextInitializer.initialize(this)
        
        createNotificationChannel()
        screenshotObserver = ScreenshotObserver(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification())
                startObserving()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopObserving()
        Log.d(TAG, "Service destroyed")
    }
    
    private fun startObserving() {
        screenshotObserver?.startObserving()
        
        observerJob = serviceScope.launch {
            screenshotObserver?.screenshots?.collectLatest { screenshotPath ->
                Log.d(TAG, "Processing screenshot: $screenshotPath")
                processScreenshot(screenshotPath)
            }
        }
    }
    
    private fun stopObserving() {
        observerJob?.cancel()
        screenshotObserver?.stopObserving()
    }
    
    private suspend fun processScreenshot(path: String) {
        // Show floating overlay
        val overlayIntent = Intent(this, FloatingOverlayService::class.java).apply {
            putExtra("screenshot_path", path)
        }
        startService(overlayIntent)
        
        // Update notification to show processing
        updateNotification("Processing screenshot...")
        
        try {
            val result = pipeline.processScreenshot(path)
            
            result.onSuccess { pipelineResult ->
                // Show result notification
                val message = buildString {
                    if (pipelineResult.memoriesSaved > 0) {
                        append("💾 ${pipelineResult.memoriesSaved} memories saved")
                    }
                    if (pipelineResult.generatedResponse != null) {
                        if (isNotEmpty()) append("\n")
                        append("💬 Response ready - tap to copy")
                        
                        // Auto-copy to clipboard
                        clipboardService.copyToClipboard(pipelineResult.generatedResponse)
                    }
                    if (isEmpty()) {
                        append("✅ Screenshot analyzed")
                    }
                }
                
                showResultNotification("Cacto Ready!", message, pipelineResult.generatedResponse)
                updateNotification("Listening for screenshots...")
            }
            
            result.onFailure { error ->
                showResultNotification("Processing failed", error.message ?: "Unknown error", null)
                updateNotification("Listening for screenshots...")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing screenshot", e)
            updateNotification("Listening for screenshots...")
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cacto Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Cacto is listening for screenshots"
                setShowBadge(false)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(message: String = "Listening for screenshots..."): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, CactoService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🌵 Cacto Active")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }
    
    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(message))
    }
    
    private fun showResultNotification(title: String, message: String, response: String?) {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        // If there's a response, add copy action
        if (response != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\n\nResponse: ${response.take(100)}${if (response.length > 100) "..." else ""}"
            ))
        }
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(RESULT_NOTIFICATION_ID, builder.build())
    }
}

