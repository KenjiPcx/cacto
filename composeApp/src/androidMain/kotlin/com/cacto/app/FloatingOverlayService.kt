package com.cacto.app

/**
 * Floating Overlay Service
 * ========================
 *
 * PURPOSE:
 * Shows a floating overlay widget when a screenshot is detected, similar to
 * "Hey Google" style. Displays processing status and quick actions.
 *
 * WHERE USED:
 * - Started by: CactoService when screenshot is detected
 * - Shows: Floating bubble with processing status
 *
 * DESIGN PHILOSOPHY:
 * Uses TYPE_APPLICATION_OVERLAY for floating window. Appears on top of all apps.
 * Shows minimal UI with processing status. Auto-dismisses when complete.
 */

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.PipelineStatus
import com.cacto.app.ui.components.MonoText
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoTheme
import org.koin.android.ext.android.inject

class FloatingOverlayService : Service() {
    
    private val pipeline: CactoPipeline by inject()
    private val clipboardService: ClipboardService by inject()
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val screenshotPath = intent?.getStringExtra("screenshot_path")
        if (screenshotPath != null && canDrawOverlays()) {
            showOverlay(screenshotPath)
        } else if (!canDrawOverlays()) {
            Toast.makeText(this, "Please enable overlay permission for Cacto", Toast.LENGTH_LONG).show()
        }
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }
    
    private fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    private fun showOverlay(screenshotPath: String) {
        if (overlayView != null) return // Already showing
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }
        
        overlayView = ComposeView(this).apply {
            setContent {
                CactoTheme {
                    FloatingOverlayContent(
                        screenshotPath = screenshotPath,
                        pipeline = pipeline,
                        clipboardService = clipboardService,
                        onDismiss = { hideOverlay() }
                    )
                }
            }
        }
        
        windowManager?.addView(overlayView, params)
    }
    
    private fun hideOverlay() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        stopSelf()
    }
}

@Composable
fun FloatingOverlayContent(
    screenshotPath: String,
    pipeline: CactoPipeline,
    clipboardService: ClipboardService,
    onDismiss: () -> Unit
) {
    val state by pipeline.state.collectAsState()
    var isExpanded by remember { mutableStateOf(true) }
    
    LaunchedEffect(screenshotPath) {
        pipeline.processScreenshot(screenshotPath)
    }
    
    LaunchedEffect(state.status) {
        if (state.status == PipelineStatus.COMPLETE || state.status == PipelineStatus.ERROR) {
            // Auto-dismiss after 5 seconds
            kotlinx.coroutines.delay(5000)
            onDismiss()
        }
    }
    
    Card(
        modifier = Modifier
            .width(320.dp)
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoText(
                    text = "CACTO",
                    fontSize = 14.sp,
                    color = CactoGreen
                )
                TextButton(onClick = onDismiss) {
                    MonoText(text = "✕", fontSize = 12.sp, color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status
            MonoText(
                text = when (state.status) {
                    PipelineStatus.IDLE -> "PROCESSING..."
                    PipelineStatus.INITIALIZING -> "LOADING AI..."
                    PipelineStatus.ANALYZING -> "ANALYZING..."
                    PipelineStatus.EXTRACTING_MEMORIES -> "EXTRACTING..."
                    PipelineStatus.GENERATING_RESPONSE -> "GENERATING..."
                    PipelineStatus.COMPLETE -> "COMPLETE"
                    PipelineStatus.ERROR -> "ERROR"
                    else -> state.currentStep.uppercase()
                },
                fontSize = 12.sp,
                color = Color.White
            )
            
            if (state.status != PipelineStatus.COMPLETE && state.status != PipelineStatus.ERROR) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = CactoGreen,
                    trackColor = CactoGreen.copy(alpha = 0.2f)
                )
            }
            
            // Results
            state.result?.let { result ->
                Spacer(modifier = Modifier.height(12.dp))
                
                if (result.memoriesSaved > 0) {
                    MonoText(
                        text = "✓ ${result.memoriesSaved} MEMORIES SAVED",
                        fontSize = 10.sp,
                        color = CactoGreen
                    )
                }
                
                result.generatedResponse?.let { response ->
                    Spacer(modifier = Modifier.height(8.dp))
                    MonoText(
                        text = response.take(100) + if (response.length > 100) "..." else "",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            clipboardService.copyToClipboardWithToast(response)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CactoGreen
                        )
                    ) {
                        MonoText(text = "COPY", fontSize = 10.sp)
                    }
                }
            }
            
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                MonoText(
                    text = "ERROR: ${error.take(50)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

