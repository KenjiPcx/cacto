# 🌵 Cacto

**Your Personal AI Memory & Action Assistant**

Cacto is a Kotlin Multiplatform Android app that uses on-device AI to help you remember important information and generate contextual responses.

## Features

- **📸 Screenshot Analysis** - Share screenshots to extract important information
- **🧠 Memory Extraction** - Automatically saves personal facts, preferences, and life events
- **🔗 Knowledge Graph** - Visualize connections between people, places, and preferences
- **✨ Smart Responses** - Generate contextual replies using your personal memory

## Demo Use Case

1. You're on a dating app and don't know how to reply
2. Take a screenshot and share it to Cacto
3. Cacto analyzes the conversation and finds relevant memories about you
4. Generates a witty, personalized response
5. Copies it to your clipboard - just paste and send!

## Tech Stack

- **Kotlin Multiplatform** - Cross-platform foundation
- **Cactus SDK** - On-device AI (LLM, Vision, Embeddings)
- **SQLDelight** - Local SQLite database
- **Jetpack Compose** - Modern UI toolkit
- **Koin** - Dependency injection
- **D3.js** - Knowledge graph visualization

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 17+
- Android SDK 26+ (device/emulator)
- High-end Android device recommended (8GB+ RAM) for AI model performance

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/cacto.git
cd cacto

# If gradle wrapper doesn't work, generate it first:
# gradle wrapper --gradle-version 8.9

# Build the project
./gradlew build

# Install on connected device
./gradlew :composeApp:installDebug
```

**Note:** On first build, Gradle will download all dependencies including the Cactus SDK. On first launch, the app will download the AI model. This may take a few minutes and require significant storage (~2-3GB depending on model choice).

### First Use

1. Open Cacto on your Android device
2. The app will download the AI model on first launch
3. Take a screenshot of any conversation or content
4. Share it to Cacto from the share menu
5. Watch as Cacto extracts memories or generates responses!

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Share Intent                      │
│                  (User shares screenshot)            │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              Cactus Vision Model                     │
│           (Analyze screenshot content)               │
└──────────────────────┬──────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│              Action Classifier                       │
│    ┌─────────────┬─────────────┬─────────────┐      │
│    │ SAVE_MEMORY │ TAKE_ACTION │    BOTH     │      │
│    └─────────────┴─────────────┴─────────────┘      │
└──────────────────────┬──────────────────────────────┘
                       │
        ┌──────────────┴──────────────┐
        ▼                             ▼
┌───────────────┐            ┌───────────────────┐
│ Memory Pipeline│            │ Action Pipeline   │
│ - Extract facts│            │ - Vector search   │
│ - Get entities │            │ - Load context    │
│ - Gen embeddings│           │ - Generate reply  │
│ - Save to DB   │            │ - Copy clipboard  │
└───────────────┘            └───────────────────┘
```

## Project Structure

```
cacto/
├── composeApp/
│   └── src/
│       ├── commonMain/          # Shared code
│       │   └── kotlin/com/cacto/app/
│       │       ├── ai/          # AI pipeline
│       │       ├── data/        # Models & repos
│       │       ├── di/          # Dependency injection
│       │       └── ui/          # UI screens
│       └── androidMain/         # Android-specific
│           └── kotlin/com/cacto/app/
│               ├── MainActivity.kt
│               └── ShareReceiverActivity.kt
├── .doc/
│   └── context.md               # Project documentation
└── README.md
```

## License

MIT License - See [LICENSE](LICENSE) for details.

## Acknowledgments

- [Cactus SDK](https://cactuscompute.com/) - On-device AI inference
- [D3.js](https://d3js.org/) - Knowledge graph visualization
- Built for hackathon demo purposes 🚀
