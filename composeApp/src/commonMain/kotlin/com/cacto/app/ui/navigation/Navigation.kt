package com.cacto.app.ui.navigation

/**
 * Navigation
 * ==========
 *
 * PURPOSE:
 * Defines the navigation structure for the app. Enum-based screen identifiers
 * used for routing between different screens in the application.
 *
 * WHERE USED:
 * - Imported by: App composable
 * - Referenced from: App composable (screen routing logic)
 * - Used in: State management for current screen
 *
 * RELATIONSHIPS:
 * - Used by: App composable for navigation state
 * - Defines: Available screens (HOME, MEMORIES, KNOWLEDGE_GRAPH)
 * - Controls: Screen transitions and routing
 *
 * USAGE IN NAVIGATION:
 * - App composable uses Screen enum to determine which screen to render
 * - Navigation state stored in App composable
 * - Screen transitions triggered by user interactions (card taps, back buttons)
 *
 * DESIGN PHILOSOPHY:
 * Simple enum-based navigation without external navigation library. Keeps navigation
 * logic lightweight. Easy to extend with new screens. Centralized screen definitions.
 */

enum class Screen {
    HOME,
    MEMORIES,
    KNOWLEDGE_GRAPH,
    DEBUG
}

