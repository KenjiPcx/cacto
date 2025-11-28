# Cacto Engineering Standards

> **GLOBAL RULES**: These standards apply to the entire codebase. See directory-specific documentation for domain-specific patterns.

## Core Philosophy

- **No Legacy Code**: We are in active development. If code is unused or replaced, delete it immediately. Do not comment it out or keep it "just in case."

- **Documentation**:
  - **No separate summary files**: Do not create `README.md` files for logic unless explicitly asked.
  - **Inline Source of Truth**: Documentation belongs in the code (KDoc comments, inline explanations).
  - **Context Tracking**: Use `.doc/context.md` for high-level task tracking and architectural notes.

- **Strict Typing**: Use Kotlin's type system fully. Prefer explicit return types for public functions. Use nullable types (`?`) and sealed classes appropriately.

## Documentation Standards

Every major logic file (Models, Repositories, Services, Composables, Activities) must begin with a KDoc header block that includes:

1. **What the file does** - Clear description of the module's purpose
2. **Where it's used** - Import locations and call sites
3. **How it's used** - Usage patterns, workflows, and integration points

### Header Block Template

```kotlin
/**
 * MODULE NAME
 * ===========
 *
 * PURPOSE:
 * Description of what this module does and its role in the system.
 *
 * WHERE USED:
 * - Imported by: [list files/modules that import this]
 * - Called from: [list call sites, e.g., "CactoPipeline.processScreenshot()"]
 * - Used in workflows: [describe integration points, e.g., "Screenshot processing pipeline"]
 *
 * RELATIONSHIPS:
 * - Belongs to: Parent models/entities
 * - Has many: Child models/entities
 * - Links to: Related entities
 *
 * USAGE IN [DOMAIN]:
 * - How this module is used in the broader system
 * - Key use cases or workflows
 * - Example: "Called during screenshot processing to extract memories"
 *
 * DESIGN PHILOSOPHY:
 * - Core architectural decisions
 * - Why certain patterns were chosen
 * - Important constraints or considerations
 */
```

### Model Documentation

All data models should include:

- **PURPOSE**: What the model represents
- **WHERE USED**: Where this model is queried/used (repositories, services, composables)
- **RELATIONSHIPS**: Foreign keys and associations
- **USAGE IN [DOMAIN]**: How it's used in specific contexts (e.g., "USAGE IN MEMORY EXTRACTION")
- **DESIGN PHILOSOPHY**: Architectural decisions and constraints

Example:

```kotlin
/**
 * Memory Model
 *
 * PURPOSE:
 * Stores extracted insights, facts, and contextual information from user screenshots.
 *
 * WHERE USED:
 * - Queried by: MemoryRepository (all CRUD operations)
 * - Created by: CactoPipeline.processMemories()
 * - Used in: VectorSearch for similarity matching
 * - Displayed in: MemoriesScreen composable
 *
 * RELATIONSHIPS:
 * - Links to: Entity (via Relation table)
 * - Source: Screenshot images via imagePath
 *
 * USAGE IN MEMORY EXTRACTION:
 * - Created when processing screenshots
 * - Embeddings generated for vector search
 * - Used to find relevant context for action generation
 *
 * DESIGN PHILOSOPHY:
 * This is for high-value extracted information, not raw data storage.
 * Embeddings are stored as JSON array for SQLite compatibility.
 */
```

### Service/Repository Documentation

Service files and repositories should document:

- **PURPOSE**: What the service/repository provides
- **WHERE USED**: Import locations and call sites
- **HOW IT'S USED**: Usage patterns and examples
- **INTEGRATION POINTS**: How it fits into workflows

Example:

```kotlin
/**
 * Memory Repository
 *
 * PURPOSE:
 * Provides data access layer for Memory entities. Handles all database operations
 * for storing and retrieving memories with their embeddings.
 *
 * WHERE USED:
 * - Imported by: CactoPipeline (memory insertion)
 * - Called from: App composable (memory retrieval for UI)
 * - Used in: VectorSearch (loading memories for similarity search)
 *
 * HOW IT'S USED:
 * 1. Called during screenshot processing to save extracted memories
 * 2. Used by UI to display memory lists
 * 3. Provides data for vector search operations
 *
 * INTEGRATION POINTS:
 * - Part of data layer, accessed through repositories
 * - Works with SQLDelight for type-safe queries
 * - Converts between database models and domain models
 */
```

### Composable Documentation

UI composables should document:

- **PURPOSE**: What the screen/composable displays
- **WHERE USED**: Navigation paths and parent composables
- **STATE MANAGEMENT**: How state flows through the composable
- **USER INTERACTIONS**: What actions users can take

Example:

```kotlin
/**
 * Home Screen
 *
 * PURPOSE:
 * Main entry point of the app. Displays processing status, statistics, and
 * navigation to other screens.
 *
 * WHERE USED:
 * - Rendered by: App composable (default screen)
 * - Navigated to: From MemoriesScreen and KnowledgeGraphScreen
 *
 * STATE MANAGEMENT:
 * - Observes: CactoPipeline.state for processing status
 * - Displays: Memory and entity counts from repositories
 * - Updates: When pipeline completes processing
 *
 * USER INTERACTIONS:
 * - Tap "Memories" card to navigate to memories list
 * - Tap "Entities" card to navigate to knowledge graph
 * - View real-time processing status during screenshot analysis
 */
```

## Code Formatting & Style

- **Formatter**: Use Android Studio's default Kotlin formatter
- **Settings**: 4 spaces for indentation, single quotes for strings where possible
- **Linting**: Code should pass Android Studio's inspections without errors
- **Naming**: Use camelCase for variables/functions, PascalCase for classes

## Database & Migrations

### Models

- Models in `app/data/model/Models.kt` using Kotlin data classes
- SQLDelight schema in `app/db/` directory
- Always use SQLDelight for type-safe database queries

### Migrations

- **NEVER manually create migration files**
- **ALWAYS use**: SQLDelight's migration system
- Review generated migrations before committing

### Database Session

- Use SQLDelight's generated queries
- All database operations should be wrapped in coroutines with `Dispatchers.IO`
- Use `withContext(Dispatchers.IO)` for blocking operations

## Project Structure

```
composeApp/src/
├── commonMain/kotlin/com/cacto/app/
│   ├── ai/              # AI processing services
│   │   ├── CactusService.kt      # Cactus SDK wrapper
│   │   ├── MemoryExtractor.kt    # Memory extraction pipeline
│   │   ├── ActionGenerator.kt   # Response generation
│   │   ├── CactoPipeline.kt     # Main processing pipeline
│   │   ├── VectorSearch.kt      # Cosine similarity search
│   │   └── Prompts.kt           # AI prompts
│   ├── data/
│   │   ├── model/Models.kt       # Data models
│   │   └── repository/           # Data access layer
│   ├── di/AppModule.kt          # Dependency injection
│   ├── ui/
│   │   ├── screens/              # UI screens
│   │   ├── theme/Theme.kt        # App theming
│   │   └── navigation/           # Navigation
│   └── App.kt                    # Main composable
└── androidMain/kotlin/com/cacto/app/
    ├── MainActivity.kt
    ├── ShareReceiverActivity.kt  # Handles shared screenshots
    ├── ClipboardService.kt
    └── CactoApplication.kt       # Koin setup
```

### Domain Organization

- Group related functionality by domain (e.g., `ai/`, `data/`, `ui/`)
- Don't place files in random utils unless they are truly generic
- Services contain business logic, repositories handle data access, composables handle UI

## Agent Behavior

- **Be Proactive**: If you see a potential issue or a better way to structure something relevant to your task, suggest it.
- **Update Standards**: If you make a significant architectural decision, update this `AGENTS.md` file to reflect it.
- **Follow Structure**: Respect the domain-driven design. Don't mix concerns.

## Key Patterns

### Screenshot Processing Pipeline

- Screenshots are processed through CactoPipeline
- Pipeline classifies action type (SAVE_MEMORY, TAKE_ACTION, BOTH)
- Memories are extracted, embedded, and stored
- Entities and relations are extracted and linked

### Vector Search

- Embeddings generated using Cactus SDK
- Cosine similarity used for finding relevant memories
- VectorSearch object provides utility functions

### State Management

- Use StateFlow for observable state (e.g., PipelineState)
- Compose observes state flows via collectAsState()
- State flows through composables via parameters

### Dependency Injection

- Use Koin for dependency injection
- Platform-specific modules via expect/actual pattern
- Shared module contains common dependencies

## Package Management

- **Use Gradle**: All package operations use Gradle
- **Kotlin Multiplatform**: Project uses KMP for Android-first development
- **Build**: Use `./gradlew build` to build the project

## Running the Application

- **Build**: `./gradlew build`
- **Install**: `./gradlew :composeApp:installDebug` (requires connected device/emulator)
- **Run**: Launch from Android Studio or via ADB

## Testing & Quality

- Run Android Studio's inspections before committing
- Test screenshot processing pipeline after changes
- Verify database migrations work correctly
- Test UI composables in different screen sizes

## Context Tracking

- Use `.doc/context.md` for:
  - High-level task tracking
  - Architectural decisions
  - Recent changes and their rationale
  - Next steps and TODO items

## Reference

- **Models**: See `composeApp/src/commonMain/kotlin/com/cacto/app/data/model/Models.kt` for data models
- **AI Services**: See `composeApp/src/commonMain/kotlin/com/cacto/app/ai/` for AI processing
- **Repositories**: See `composeApp/src/commonMain/kotlin/com/cacto/app/data/repository/` for data access
- **UI**: See `composeApp/src/commonMain/kotlin/com/cacto/app/ui/` for composables

