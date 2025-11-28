package com.cacto.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cacto.app.data.model.KnowledgeGraph

@Composable
expect fun KnowledgeGraphWebView(
    graph: KnowledgeGraph,
    modifier: Modifier = Modifier
)

