# ShoeCycle Android

A Kotlin-based native Android application for tracking running shoe usage and distances, built with Jetpack Compose.

## Features

- Track running distances across multiple pairs of shoes
- Set preferred distance units (Miles/Kilometers)
- Configure first day of the week for weekly tracking
- Manage favorite distance shortcuts
- Hall of Fame for retired shoes

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: VSI (View State Interactor) pattern
- **Navigation**: Jetpack Navigation Compose
- **Data Persistence**: DataStore Preferences
- **Build System**: Gradle with Kotlin DSL

## Requirements

- Android 7.0 (API level 24) or higher
- Java 8 or higher

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

## Architecture

The app follows the VSI (View State Interactor) pattern:
- **State**: Data classes holding UI state
- **Interactor**: Business logic handlers with coroutine scopes
- **Actions**: Sealed classes defining user interactions

### Key Principles
- Views only modify state via interactors
- Interactors handle async operations and state management
- UI layer dispatches actions and observes state changes

## Project Structure

```
app/src/main/java/com/shoecycle/
├── data/                    # Data layer (repositories, models)
├── ui/
│   ├── screens/            # Screen composables
│   ├── settings/           # VSI implementations
│   └── theme/              # Material 3 theming
└── MainActivity.kt         # Main entry point
```

## Development

### Building
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test
```

### Code Style
The project follows standard Kotlin coding conventions with ktlint integration.

## License

[Add your license information here]