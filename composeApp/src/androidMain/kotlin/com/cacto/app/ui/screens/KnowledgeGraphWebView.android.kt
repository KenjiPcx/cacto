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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

private fun generateD3GraphHtml(graph: KnowledgeGraph): String {
    val json = Json { ignoreUnknownKeys = true }
    val nodesJson = json.encodeToString(graph.nodes)
    val edgesJson = json.encodeToString(graph.edges)

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://d3js.org/d3.v7.min.js"></script>
            <style>
                body { 
                    margin: 0; 
                    background-color: transparent; 
                    overflow: hidden;
                    font-family: 'Courier New', monospace;
                }
                svg { 
                    width: 100vw; 
                    height: 100vh; 
                }
                .node circle {
                    stroke: #fff;
                    stroke-width: 1.5px;
                }
                .link {
                    stroke: #999;
                    stroke-opacity: 0.6;
                }
                text {
                    font-family: 'Courier New', monospace;
                    font-size: 10px;
                    fill: #fff;
                    pointer-events: none;
                }
            </style>
        </head>
        <body>
            <div id="graph"></div>
            <script>
                const nodes = $nodesJson;
                const links = $edgesJson;

                const width = window.innerWidth;
                const height = window.innerHeight;

                const svg = d3.select("#graph")
                    .append("svg")
                    .attr("width", width)
                    .attr("height", height);

                const simulation = d3.forceSimulation(nodes)
                    .force("link", d3.forceLink(links).id(d => d.id).distance(100))
                    .force("charge", d3.forceManyBody().strength(-300))
                    .force("center", d3.forceCenter(width / 2, height / 2));

                const link = svg.append("g")
                    .attr("class", "links")
                    .selectAll("line")
                    .data(links)
                    .join("line")
                    .attr("class", "link")
                    .attr("stroke-width", 1);

                const node = svg.append("g")
                    .attr("class", "nodes")
                    .selectAll("g")
                    .data(nodes)
                    .join("g")
                    .call(d3.drag()
                        .on("start", dragstarted)
                        .on("drag", dragged)
                        .on("end", dragended));

                node.append("circle")
                    .attr("r", 8)
                    .attr("fill", d => {
                        switch(d.type) {
                            case "PERSON": return "#4ECDC4";
                            case "PLACE": return "#E94560";
                            case "ORGANIZATION": return "#7B2CBF";
                            default: return "#F7D060";
                        }
                    });

                node.append("text")
                    .attr("dx", 12)
                    .attr("dy", ".35em")
                    .text(d => d.name);

                simulation.on("tick", () => {
                    link
                        .attr("x1", d => d.source.x)
                        .attr("y1", d => d.source.y)
                        .attr("x2", d => d.target.x)
                        .attr("y2", d => d.target.y);

                    node
                        .attr("transform", d => `translate(${'$'}{d.x},${'$'}{d.y})`);
                });

                function dragstarted(event) {
                    if (!event.active) simulation.alphaTarget(0.3).restart();
                    event.subject.fx = event.subject.x;
                    event.subject.fy = event.subject.y;
                }

                function dragged(event) {
                    event.subject.fx = event.x;
                    event.subject.fy = event.y;
                }

                function dragended(event) {
                    if (!event.active) simulation.alphaTarget(0);
                    event.subject.fx = null;
                    event.subject.fy = null;
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}
