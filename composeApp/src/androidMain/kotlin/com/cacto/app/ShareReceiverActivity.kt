package com.cacto.app

/**
 * Share Receiver Activity
 * =======================
 *
 * PURPOSE:
 * Handles screenshot sharing intents from other apps. Receives shared images,
 * copies them to app cache, and automatically processes them through CactoPipeline.
 * Displays processing status and results with option to copy generated responses.
 *
 * WHERE USED:
 * - Launched by: Android system when user shares image to Cacto
 * - Entry point: Share intent receiver declared in AndroidManifest.xml
 * - Contains: ShareReceiverScreen composable
 *
 * RELATIONSHIPS:
 * - Receives: Image URIs via Android share intent
 * - Uses: CactoPipeline for processing, ClipboardService for copying responses
 * - Displays: Processing status and results
 * - Navigates to: MainActivity when user opens app
 *
 * USAGE IN SCREENSHOT PROCESSING:
 * - User shares screenshot from any app to Cacto
 * - Activity receives image URI from intent
 * - Copies image to app cache directory
 * - Automatically triggers pipeline processing
 * - Shows real-time processing status
 * - Displays results and allows copying response to clipboard
 *
 * DESIGN PHILOSOPHY:
 * Dedicated activity for share intents (better UX than handling in MainActivity).
 * Automatically processes shared images without user interaction. Shows processing
 * status for transparency. Provides quick actions (copy, open app). Handles
 * image URI copying to app-controlled location.
 */

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.cacto.app.ai.CactoPipeline
import com.cacto.app.ai.PipelineStatus
import com.cacto.app.ui.theme.CactoTheme
import com.cactus.CactusContextInitializer
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream

class ShareReceiverActivity : ComponentActivity() {
    
    private val pipeline: CactoPipeline by inject()
    private val clipboardService: ClipboardService by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Cactus context
        CactusContextInitializer.initialize(this)
        
        val imageUri = extractImageUri()
        
        setContent {
            CactoTheme {
                ShareReceiverScreen(
                    imageUri = imageUri,
                    pipeline = pipeline,
                    clipboardService = clipboardService,
                    onClose = { finish() },
                    onOpenApp = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
        
        // Auto-process if we have an image
        imageUri?.let { uri ->
            lifecycleScope.launch {
                val imagePath = copyUriToFile(uri)
                if (imagePath != null) {
                    try {
                        pipeline.processScreenshot(imagePath)
                    } finally {
                        // Clean up the temporary file
                        try {
                            File(imagePath).delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
    
    private fun extractImageUri(): Uri? {
        return when (intent?.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
                } else null
            }
            else -> null
        }
    }
    
    private fun copyUriToFile(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val cacheDir = File(cacheDir, "shared_images")
            cacheDir.mkdirs()
            val file = File(cacheDir, "screenshot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun ShareReceiverScreen(
    imageUri: Uri?,
    pipeline: CactoPipeline,
    clipboardService: ClipboardService,
    onClose: () -> Unit,
    onOpenApp: () -> Unit
) {
    val state by pipeline.state.collectAsState()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (imageUri == null) {
                Text(
                    text = "No image received",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClose) {
                    Text("Close")
                }
            } else {
                // Status display
                Text(
                    text = when (state.status) {
                        PipelineStatus.IDLE -> "🌵 Ready to process"
                        PipelineStatus.INITIALIZING -> "⚡ Loading AI model..."
                        PipelineStatus.ANALYZING -> "🔍 Analyzing screenshot..."
                        PipelineStatus.EXTRACTING_MEMORIES -> "🧠 Extracting memories..."
                        PipelineStatus.GENERATING_EMBEDDINGS -> "📊 Creating embeddings..."
                        PipelineStatus.EXTRACTING_ENTITIES -> "🔗 Building knowledge graph..."
                        PipelineStatus.RESOLVING_ENTITIES -> "🔍 Resolving entities..."
                        PipelineStatus.CREATING_RELATIONS -> "🔗 Creating relations..."
                        PipelineStatus.SAVING_DATA -> "💾 Saving data..."
                        PipelineStatus.GENERATING_RESPONSE -> "✨ Generating response..."
                        PipelineStatus.COMPLETE -> "✅ Done!"
                        PipelineStatus.ERROR -> "❌ Error"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = state.currentStep,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress indicator
                if (state.status != PipelineStatus.COMPLETE && state.status != PipelineStatus.ERROR) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                
                // Results
                state.result?.let { result ->
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (result.memoriesSaved > 0) {
                                Text("📝 ${result.memoriesSaved} memories saved")
                                Text("🔗 ${result.entitiesCreated} entities, ${result.relationsCreated} relations")
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            result.generatedResponse?.let { response ->
                                Text(
                                    text = "Generated Response:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = response,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { clipboardService.copyToClipboardWithToast(response) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📋 Copy to Clipboard")
                                }
                            }
                        }
                    }
                }
                
                // Error display
                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = onOpenApp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Cacto")
                    }
                }
            }
        }
    }
}

