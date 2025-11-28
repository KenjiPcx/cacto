package com.cacto.app.ui.screens

/**
 * Knowledge Graph Screen
 * =====================
 *
 * PURPOSE:
 * Displays an interactive force-directed graph visualization of entities and
 * their relationships. Shows the knowledge graph built from extracted memories
 * using D3.js in a WebView. Provides statistics and legend for entity types.
 *
 * WHERE USED:
 * - Rendered by: App composable (when Screen.KNOWLEDGE_GRAPH is selected)
 * - Navigated to: From HomeScreen via "Entities" stat card
 * - Entry point: User taps entities card on home screen
 *
 * RELATIONSHIPS:
 * - Displays: KnowledgeGraph structure from EntityRepository
 * - Uses: KnowledgeGraphWebView (platform-specific WebView implementation)
 * - Shows: Graph nodes (entities) and edges (relations)
 * - Provides: Statistics and legend for graph understanding
 *
 * USAGE IN KNOWLEDGE GRAPH VISUALIZATION:
 * - Users explore their knowledge graph interactively
 * - Force-directed layout shows entity relationships
 * - Color-coded nodes by entity type (Person, Place, Preference, etc.)
 * - Draggable nodes for interactive exploration
 * - Edge labels show relation types
 *
 * DESIGN PHILOSOPHY:
 * Uses WebView for D3.js visualization (cross-platform compatibility). Provides
 * statistics bar for quick graph overview. Legend helps users understand entity
 * types. Empty state guides users to build their graph. Interactive drag-and-drop
 * for engaging user experience.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cacto.app.data.model.KnowledgeGraph
import com.cacto.app.ui.theme.CactoGreen
import com.cacto.app.ui.theme.CactoPink
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeGraphScreen(
    graph: KnowledgeGraph,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔗 Knowledge Graph") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            if (graph.nodes.isEmpty()) {
                EmptyGraphState(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Stats bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBadge(
                            label = "Nodes",
                            value = graph.nodes.size.toString(),
                            color = CactoGreen
                        )
                        StatBadge(
                            label = "Edges",
                            value = graph.edges.size.toString(),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    // Graph visualization
                    KnowledgeGraphWebView(
                        graph = graph,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(8.dp)
                    )
                    
                    // Legend
                    GraphLegend(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
expect fun KnowledgeGraphWebView(
    graph: KnowledgeGraph,
    modifier: Modifier = Modifier
)

@Composable
fun StatBadge(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GraphLegend(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Entity Types",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = CactoGreen, label = "Person")
                LegendItem(color = androidx.compose.ui.graphics.Color(0xFFE94560), label = "Place")
                LegendItem(color = androidx.compose.ui.graphics.Color(0xFF7B2CBF), label = "Preference")
                LegendItem(color = androidx.compose.ui.graphics.Color(0xFFF7D060), label = "Event")
            }
        }
    }
}

@Composable
fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = color,
                    shape = RoundedCornerShape(6.dp)
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyGraphState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🔗",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No connections yet",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Share screenshots with people,\nplaces, and preferences to build\nyour knowledge graph",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Helper to generate the D3.js HTML
fun generateD3GraphHtml(graph: KnowledgeGraph): String {
    val json = Json { prettyPrint = false }
    
    // Create nodes JSON
    val nodesJson = graph.nodes.map { node ->
        """{"id": ${node.id}, "name": "${node.name.replace("\"", "\\\"")}", "type": "${node.type}"}"""
    }.joinToString(",")
    
    // Create edges JSON
    val edgesJson = graph.edges.map { edge ->
        """{"source": ${edge.source}, "target": ${edge.target}, "label": "${edge.label.replace("\"", "\\\"")}"}"""
    }.joinToString(",")
    
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            background: transparent; 
            overflow: hidden;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        }
        svg { width: 100%; height: 100vh; }
        .node circle {
            stroke: #fff;
            stroke-width: 2px;
            cursor: pointer;
            transition: r 0.2s;
        }
        .node circle:hover { r: 12; }
        .node text {
            font-size: 10px;
            fill: #fff;
            pointer-events: none;
            text-anchor: middle;
            dominant-baseline: middle;
        }
        .link {
            stroke: rgba(255, 255, 255, 0.3);
            stroke-width: 1.5px;
        }
        .link-label {
            font-size: 8px;
            fill: rgba(255, 255, 255, 0.6);
        }
    </style>
</head>
<body>
    <svg></svg>
    <script>
        const nodes = [$nodesJson];
        const links = [$edgesJson];
        
        const typeColors = {
            'PERSON': '#4ECDC4',
            'PLACE': '#E94560',
            'PREFERENCE': '#7B2CBF',
            'EVENT': '#F7D060',
            'TOPIC': '#FF6B35',
            'ORGANIZATION': '#45B7D1',
            'OTHER': '#888888'
        };
        
        const width = window.innerWidth;
        const height = window.innerHeight;
        
        const svg = d3.select('svg')
            .attr('viewBox', [0, 0, width, height]);
        
        // Create a map for quick node lookup
        const nodeById = new Map(nodes.map(n => [n.id, n]));
        
        // Process links to use node references
        const processedLinks = links.map(l => ({
            source: nodeById.get(l.source),
            target: nodeById.get(l.target),
            label: l.label
        })).filter(l => l.source && l.target);
        
        const simulation = d3.forceSimulation(nodes)
            .force('link', d3.forceLink(processedLinks).id(d => d.id).distance(80))
            .force('charge', d3.forceManyBody().strength(-200))
            .force('center', d3.forceCenter(width / 2, height / 2))
            .force('collision', d3.forceCollide().radius(30));
        
        // Links
        const link = svg.append('g')
            .selectAll('line')
            .data(processedLinks)
            .join('line')
            .attr('class', 'link');
        
        // Link labels
        const linkLabel = svg.append('g')
            .selectAll('text')
            .data(processedLinks)
            .join('text')
            .attr('class', 'link-label')
            .text(d => d.label);
        
        // Nodes
        const node = svg.append('g')
            .selectAll('g')
            .data(nodes)
            .join('g')
            .attr('class', 'node')
            .call(d3.drag()
                .on('start', dragstarted)
                .on('drag', dragged)
                .on('end', dragended));
        
        node.append('circle')
            .attr('r', 8)
            .attr('fill', d => typeColors[d.type] || typeColors['OTHER']);
        
        node.append('text')
            .attr('dy', 20)
            .text(d => d.name.length > 12 ? d.name.substring(0, 10) + '...' : d.name);
        
        simulation.on('tick', () => {
            link
                .attr('x1', d => d.source.x)
                .attr('y1', d => d.source.y)
                .attr('x2', d => d.target.x)
                .attr('y2', d => d.target.y);
            
            linkLabel
                .attr('x', d => (d.source.x + d.target.x) / 2)
                .attr('y', d => (d.source.y + d.target.y) / 2);
            
            node.attr('transform', d => `translate(${'$'}{d.x},${'$'}{d.y})`);
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

