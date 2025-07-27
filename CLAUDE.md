# ShoeCycle Android App Memory

## Project Overview
**ShoeCycle Android** is a Kotlin-based native Android application using Jetpack Compose, converted from an existing iOS app. The app helps users track running shoe usage and distances.

## Architecture & Design Patterns
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: VSI (View State Interactor) - custom pattern from iOS codebase
- **Navigation**: Jetpack Navigation Compose with bottom tab bar
- **Data Persistence**: DataStore Preferences (replaces iOS UserDefaults)
- **State Management**: Compose state with unidirectional data flow

## VSI Pattern Implementation
The app uses a custom VSI (View State Interactor) pattern with three components:
1. **State**: Data classes holding UI state (e.g., `SettingsUnitsState`)
2. **Interactor**: Business logic handlers with sealed Action classes
3. **Actions**: Sealed classes defining user interactions (`UnitChanged`, `ViewAppeared`)

Example structure:
```kotlin
data class SettingsUnitsState(val selectedUnit: DistanceUnit = DistanceUnit.MILES)
class SettingsUnitsInteractor(private val repository: UserSettingsRepository) {
    sealed class Action {
        data class UnitChanged(val unit: DistanceUnit) : Action()
        object ViewAppeared : Action()
    }
}
```

## Project Structure
```
app/src/main/java/com/shoecycle/
├── MainActivity.kt              # Main activity with edge-to-edge support
├── data/                        # Data layer
│   ├── DataStoreExtensions.kt   # DataStore utilities
│   ├── UserSettings.kt          # Data classes and enums
│   └── UserSettingsRepository.kt # Repository with DataStore
├── ui/                          # UI layer
│   ├── ShoeCycleApp.kt         # Main app composable with navigation
│   ├── screens/                 # Screen composables
│   │   ├── AddDistanceScreen.kt
│   │   ├── ActiveShoesScreen.kt
│   │   ├── HallOfFameScreen.kt
│   │   └── SettingsScreen.kt    # Fully implemented with VSI
│   ├── settings/                # VSI implementations
│   │   ├── SettingsUnitsInteractions.kt
│   │   ├── SettingsFirstDayInteractions.kt
│   │   └── SettingsFavoriteDistancesInteractions.kt
│   └── theme/                   # Material 3 theming
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
```

## Data Layer Details
- **UserSettings.kt**: Defines `DistanceUnit` and `FirstDayOfWeek` enums with `fromOrdinal()` companion functions
- **UserSettingsRepository.kt**: Handles DataStore operations with proper error handling
- **Persistence**: Uses DataStore Preferences with Flow-based reactive streams
- **Error Handling**: IOException logging for DataStore operations

## UI Implementation Status
### ✅ Completed Features:
- **Settings Screen**: Fully implemented with VSI pattern
  - Units selection (Miles/KM) with enhanced FilterChip styling
  - First Day of Week selection (Sunday/Monday)  
  - Favorite Distances input fields (4 favorites) with unified action handling
  - About dialog with version info
- **Navigation**: Bottom tab navigation with 4 screens
- **Theme**: Material 3 with custom colors and typography

### 🚧 Placeholder Screens:
- Add Distance Screen
- Active Shoes Screen  
- Hall of Fame Screen

## Key Dependencies
- Jetpack Compose BOM 2024.02.00
- Material 3
- Navigation Compose 2.7.6
- DataStore Preferences 1.0.0
- Activity Compose 1.8.2
- Lifecycle ViewModel 2.7.0

## Build Configuration
- **Namespace**: com.shoecycle
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Kotlin**: JVM target 1.8

## Visual Enhancements
- Custom FilterChip styling with enhanced selection indicators
- Primary color containers and borders for selected states
- Material 3 color scheme integration
- Edge-to-edge display support

## Development Notes
- Error logging for DataStore operations
- Uses `entries` instead of deprecated `values()` for enums
- Proper separation of concerns with repository pattern
- Coroutines for asynchronous data operations
- VSI pattern is the assumed standard throughout the app (no need to document in comments)

## Next Steps for Development
1. Implement remaining screen functionality (Add Distance, Active Shoes, Hall of Fame)
2. Add Room database for shoe tracking data
3. Implement business logic for distance tracking
4. Add data validation and input sanitization
5. Create unit and integration tests