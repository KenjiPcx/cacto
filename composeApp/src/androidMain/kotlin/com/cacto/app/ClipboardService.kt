package com.cacto.app

/**
 * Clipboard Service
 * =================
 *
 * PURPOSE:
 * Provides Android clipboard operations for copying generated responses and other text.
 * Wraps Android ClipboardManager with convenient methods and optional toast notifications.
 *
 * WHERE USED:
 * - Imported by: ShareReceiverActivity
 * - Called from: ShareReceiverScreen (copying generated responses)
 * - Used in: Response generation workflow (copying to clipboard)
 *
 * RELATIONSHIPS:
 * - Uses: Android ClipboardManager system service
 * - Provides: Clipboard operations to UI components
 * - Shows: Toast notifications for user feedback
 *
 * USAGE IN CLIPBOARD OPERATIONS:
 * - Called when user taps "Copy to Clipboard" button
 * - Copies generated responses for easy pasting
 * - Provides visual feedback via toast messages
 * - Can retrieve clipboard contents if needed
 *
 * DESIGN PHILOSOPHY:
 * Simple wrapper around Android clipboard API. Provides convenience methods with
 * optional toast notifications. Lazy initialization of ClipboardManager. Single
 * responsibility: clipboard operations only.
 */

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

class ClipboardService(private val context: Context) {
    
    private val clipboardManager: ClipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    
    fun copyToClipboard(text: String, label: String = "Cacto Response") {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
    }
    
    fun copyToClipboardWithToast(text: String, label: String = "Cacto Response") {
        copyToClipboard(text, label)
        Toast.makeText(context, "Response copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
    
    fun getClipboardText(): String? {
        return if (clipboardManager.hasPrimaryClip()) {
            clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
        } else {
            null
        }
    }
}

