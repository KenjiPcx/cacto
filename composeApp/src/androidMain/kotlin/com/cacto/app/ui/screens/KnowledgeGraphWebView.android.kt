package com.cacto.app.ui.screens

/**
 * Knowledge Graph WebView (Android)
 * =================================
 *
 * PURPOSE:
 * Android-specific implementation of KnowledgeGraphWebView. Renders D3.js force-directed
 * graph visualization in an Android WebView. Loads generated HTML with embedded graph data.
 *
 * WHERE USED:
 * - Actual implementation of: KnowledgeGraphWebView (expect)
 * - Called from: KnowledgeGraphScreen composable
 * - Used in: Knowledge graph visualization screen
 *
 * RELATIONSHIPS:
 * - Implements: KnowledgeGraphWebView expect function
 * - Uses: Android WebView for rendering
 * - Loads: Generated HTML from generateD3GraphHtml()
 * - Displays: D3.js force-directed graph visualization
 *
 * USAGE IN GRAPH VISUALIZATION:
 * - Renders interactive graph in WebView
 * - Loads D3.js library from CDN
 * - Displays nodes (entities) and edges (relations)
 * - Supports drag-and-drop interaction
 *
 * DESIGN PHILOSOPHY:
 * Platform-specific WebView implementation for Android. Enables JavaScript for D3.js.
 * Uses transparent background to match app theme. Loads HTML with base URL for
 * proper D3.js CDN access. Single responsibility: WebView rendering only.
 */

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.cacto.app.data.model.KnowledgeGraph

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun KnowledgeGraphWebView(
    graph: KnowledgeGraph,
    modifier: Modifier
) {
    val html = remember(graph) { generateD3GraphHtml(graph) }
    
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "https://d3js.org/",
                html,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

