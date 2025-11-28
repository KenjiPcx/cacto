# Cacto - Personal AI Memory & Action Assistant

## Project Overview

Cacto is a Kotlin Multiplatform Android app that serves as a personal AI memory assistant. Users can share screenshots to the app, which then uses on-device AI (Cactus SDK) to:

1. **Extract and save memories** - Personal facts, preferences, life events with sophisticated filtering
2. **Build a knowledge graph** - Entities and relationships with vector-based deduplication
3. **Generate contextual responses** - Help users reply to messages using their personal context

## Tech Stack

- **Framework**: Kotlin Multiplatform (Android-first)
- **AI**: Cactus SDK (on-device vision model + embeddings + LLM)
- **Database**: SQLDelight (SQLite)
- **UI**: Jetpack Compose / Compose Multiplatform
- **DI**: Koin
- **Graph Viz**: WebView + D3.js

## Architecture

```
composeApp/
├── src/
│   ├── commonMain/kotlin/com/cacto/app/
│   │   ├── ai/
│   │   │   ├── CactusService.kt           # Cactus SDK wrapper
│   │   │   ├── MemoryExtractor.kt         # Memory & entity extraction
│   │   │   ├── ActionGenerator.kt         # Response generation
│   │   │   ├── EntityResolutionService.kt # Entity deduplication
│   │   │   ├── CactoPipeline.kt           # Main processing pipeline
│   │   │   ├── VectorSearch.kt            # Cosine similarity search
│   │   │   └── Prompts.kt                 # AI prompts
│   │   ├── data/
│   │   │   ├── model/Models.kt            # Data models
│   │   │   ├── repository/                # Data access layer
│   │   │   └── DatabaseDriverFactory.kt
│   │   ├── di/AppModule.kt                # Dependency injection
│   │   ├── ui/
│   │   │   ├── components/                # UI components
│   │   │   ├── screens/                   # UI screens
│   │   │   ├── theme/Theme.kt             # App theming
│   │   │   └── navigation/                # Navigation
│   │   └── App.kt                         # Main composable
│   └── androidMain/kotlin/com/cacto/app/
│       ├── MainActivity.kt
│       ├── ShareReceiverActivity.kt       # Handles shared screenshots
│       ├── ClipboardService.kt
│       └── CactoApplication.kt            # Koin setup
```

## Pipeline Flow

```
Screenshot → Action Classification → 
    ├── SAVE_MEMORY:
    │   └── Memory Extraction → Embedding Generation → 
    │       Batch Entity Extraction → Entity Resolution (dedup) →
    │       Memory→Entity Relations → Entity→Entity Relations
    │
    ├── TAKE_ACTION:
    │   └── Screenshot Description → Vector Search (context) →
    │       Response Generation → Clipboard
    │
    └── BOTH: Both flows execute
```

## Memory Model

Memory types:
- **fact**: Personal, verifiable information (e.g., "User is pursuing UK visa")
- **preference**: Likes/dislikes/choices with structured_data (category, strength)
- **insight**: Behavioral patterns, habits, working style
- **event**: Personal events, commitments, life changes
- **decision**: Important choices, purchases, commitments

Importance levels: low, medium, high

## Entity Resolution (Deduplication)

Three-step process:
1. **Exact Match** (fast path) - Case-insensitive name + type
2. **Vector Similarity** (threshold ≥0.75) - Embedding-based search
3. **LLM Verification** (judge) - For high-similarity candidates

Prevents duplicates like "John Doe" vs "Johnathan Doe" or "Project X" vs "Project X - Marketing".

## Database Schema

- **memories**: id, content, memory_type, importance, context, embedding, structured_data, ...
- **entities**: id, name, entity_type, description, embedding, ...
- **relations**: id, source_type, source_entity_id, target_entity_id, relation_type, ...
- **memory_entity_links**: id, memory_id, entity_id (many-to-many)

## Build & Run

```bash
# Build the project
./gradlew build

# Run on Android (requires connected device/emulator)
./gradlew :composeApp:installDebug
```

## Demo Flow

1. User is on a dating app, needs help replying
2. Takes screenshot, shares to Cacto
3. Cacto analyzes: identifies conversation context
4. Extracts memories about the conversation partner
5. Loads relevant memories about user's personality
6. Generates witty response
7. Copies to clipboard
8. User pastes and sends

Meanwhile, facts about the conversation partner are saved to the knowledge graph with proper deduplication.

## Status

✅ Project setup complete
✅ Database schema with sophisticated memory model
✅ Cactus SDK integration
✅ Sophisticated memory extraction with filtering
✅ Batch entity extraction
✅ Entity resolution with vector similarity + LLM verification
✅ Vector search for context retrieval
✅ Share receiver
✅ Action generator
✅ Main UI screens
✅ Memories list with filtering
✅ Knowledge graph visualization
✅ **Code documentation complete** - All files now have inline KDoc header comments
✅ **AGENTS.md created** - Engineering standards and documentation guidelines established

## Identified Risks & Issues (Audit)
- 🔴 **Storage Leak**: `ShareReceiverActivity` creates new files for every shared screenshot but never deletes them.
- 🔴 **Privacy**: `RECORD_AUDIO` permission is requested but unused.
- 🟠 **Model Size**: Code defaults to `gemma3-4b` (vision) which is likely >2GB, contradicting the 500MB claim in README.
- 🟠 **Performance**: Heavy model inference on main pipeline might cause OOMs on lower-end devices.

## Recent Changes

### Stability & UI Improvements
- **Crash Fix**: Moved `downloadModels()` to `Dispatchers.IO` to prevent main thread blocking (ANR).
- **UI Improvement**: Updated `ModelDownloadScreen` to use `LinearProgressIndicator` and clearer status text.
- **Previous Work**: Complete UI redesign with Glassmorphism / Nothing Phone aesthetic.

### UI Redesign (Completed)
- **Goal**: Minimalist glassmorphism design (Nothing Phone style) with Palantir enterprise aesthetics and animated gradient orb background.
- **Completed Tasks**:
    - [x] Created `OrbBackground` component (Lava lamp effect)
    - [x] Created `GlassCard`, `NeonButton`, `MonoText` components
    - [x] Updated `Theme.kt` with new color palette (Dark/Neon)
    - [x] Refactored `HomeScreen` to use new components
    - [x] Refactored `MemoriesScreen` to use new components
    - [x] Refactored `KnowledgeGraphScreen` to use new components
    - [x] Added `OnboardingOverlay` with fun, engaging steps
    - [x] Integrated Onboarding into `App.kt`

### Documentation Standards (Latest)
- Created `AGENTS.md` with Kotlin-adapted engineering standards
- Added comprehensive KDoc header comments to all files:
  - AI services (CactusService, MemoryExtractor, ActionGenerator, CactoPipeline, VectorSearch, Prompts)
  - Data layer (Models, MemoryRepository, EntityRepository, DatabaseDriverFactory)
  - UI layer (App, HomeScreen, MemoriesScreen, KnowledgeGraphScreen, Navigation, Theme)
  - Android-specific (MainActivity, ShareReceiverActivity, ClipboardService, CactoApplication, DI modules)
- All headers follow consistent template with PURPOSE, WHERE USED, RELATIONSHIPS, USAGE, and DESIGN PHILOSOPHY sections
- Documentation serves as inline source of truth per engineering standards

## Next Steps (Post-Hackathon)

- [ ] Add overlay UI for quick actions
- [ ] Implement accessibility service for auto-capture
- [ ] Add keyboard integration
- [ ] Improve prompt engineering based on testing
- [ ] Add memory editing/deletion UI
- [ ] Export/backup knowledge graph
- [ ] Add user profile synthesis from memories
