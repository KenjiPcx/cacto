package com.cacto.app

/**
 * Main Activity
 * =============
 *
 * PURPOSE:
 * Main entry point for the Android application. Handles permissions,
 * service management, and UI setup.
 *
 * WHERE USED:
 * - Launched by: Android system when app starts
 * - Entry point: Main activity declared in AndroidManifest.xml
 */

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.CactusService
import com.cacto.app.data.repository.EntityRepository
import com.cacto.app.data.repository.HistoryRepository
import com.cacto.app.data.repository.MemoryRepository
import com.cactus.CactusContextInitializer
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    
    private val cactusService: CactusService by inject()
    private val pipeline: CactoPipeline by inject()
    private val memoryRepository: MemoryRepository by inject()
    private val entityRepository: EntityRepository by inject()
    private val historyRepository: HistoryRepository by inject()
    
    private var serviceRunning by mutableStateOf(false)
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Check overlay permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
            } else {
                startCactoService()
            }
        } else {
            Toast.makeText(
                this,
                "Permissions needed to detect screenshots",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Overlay permission launcher
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("MainActivity", "Overlay permission result received")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasOverlay = Settings.canDrawOverlays(this)
            Log.d("MainActivity", "Overlay permission after settings: $hasOverlay")
            if (hasOverlay) {
                Toast.makeText(
                    this,
                    "Overlay permission granted! Starting Cacto...",
                    Toast.LENGTH_SHORT
                ).show()
                startCactoService()
            } else {
                Toast.makeText(
                    this,
                    "Please enable 'Display over other apps' for Cacto to show floating widget",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("MainActivity", "Requesting overlay permission")
            Toast.makeText(
                this,
                "Opening settings to enable overlay permission...",
                Toast.LENGTH_LONG
            ).show()
            
            try {
                // Try the overlay permission intent first
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                
                // Check if the intent can be resolved
                if (intent.resolveActivity(packageManager) != null) {
                    overlayPermissionLauncher.launch(intent)
                } else {
                    // Fallback to app settings
                    Log.w("MainActivity", "Overlay permission intent not available, using app settings")
                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    overlayPermissionLauncher.launch(fallbackIntent)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error launching overlay permission settings", e)
                // Fallback to app settings
                try {
                    val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    overlayPermissionLauncher.launch(fallbackIntent)
                } catch (e2: Exception) {
                    Log.e("MainActivity", "Error launching app settings", e2)
                    Toast.makeText(
                        this,
                        "Please enable 'Display over other apps' manually:\nSettings > Apps > Cacto > Display over other apps",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Cactus context (required for Cactus SDK)
        CactusContextInitializer.initialize(this)
        
        enableEdgeToEdge()
        
        // Check if service is already running
        serviceRunning = checkServiceRunning()
        
        setContent {
            App(
                cactusService = cactusService,
                pipeline = pipeline,
                memoryRepository = memoryRepository,
                entityRepository = entityRepository,
                historyRepository = historyRepository,
                isServiceRunning = serviceRunning,
                onStartService = { requestPermissionsAndStart() },
                onStopService = { stopCactoService() }
            )
        }
    }
    
    override fun onResume() {
        super.onResume()
        serviceRunning = checkServiceRunning()
    }
    
    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf<String>()
        
        // Media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        } else {
            // Media permissions already granted, check overlay permission
            Log.d("MainActivity", "Media permissions already granted, checking overlay permission")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val hasOverlay = Settings.canDrawOverlays(this)
                Log.d("MainActivity", "Overlay permission granted: $hasOverlay")
                if (!hasOverlay) {
                    Log.d("MainActivity", "Requesting overlay permission")
                    requestOverlayPermission()
                } else {
                    Log.d("MainActivity", "All permissions granted, starting service")
                    startCactoService()
                }
            } else {
                // Android < M doesn't need overlay permission
                startCactoService()
            }
        }
    }
    
    private fun startCactoService() {
        val intent = Intent(this, CactoService::class.java).apply {
            action = CactoService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        serviceRunning = true
        Toast.makeText(this, "🌵 Cacto is now listening!", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopCactoService() {
        val intent = Intent(this, CactoService::class.java).apply {
            action = CactoService.ACTION_STOP
        }
        startService(intent)
        
        serviceRunning = false
        Toast.makeText(this, "Cacto stopped listening", Toast.LENGTH_SHORT).show()
    }
    
    @Suppress("DEPRECATION")
    private fun checkServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (CactoService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
