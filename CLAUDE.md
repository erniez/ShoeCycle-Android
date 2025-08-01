# ShoeCycle Android App Memory

## Project Overview
**ShoeCycle Android** is a Kotlin-based native Android application using Jetpack Compose, converted from an existing iOS app. The app helps users track running shoe usage and distances.

## Architecture & Design Patterns
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture Pattern**: VSI (View State Interactor) - custom pattern from iOS codebase
- **Navigation**: Jetpack Navigation Compose with bottom tab bar
- **Data Persistence**: DataStore Preferences (replaces iOS UserDefaults)
- **State Management**: Strict VSI pattern - all state modifications via interactors only

## VSI Pattern Implementation
The app uses a custom VSI (View State Interactor) pattern with three components:
1. **State**: Data classes holding UI state (e.g., `SettingsUnitsState`)
2. **Interactor**: Business logic handlers with internal CoroutineScope and sealed Action classes
3. **Actions**: Sealed classes defining user interactions (`UnitChanged`, `ViewAppeared`)

### Architecture Rules
- **Rule 1**: Views can ONLY modify state via interactors - no direct state management allowed
- **Rule 2**: Interactors handle their own coroutine scope and async operations
- **Rule 3**: UI layer only dispatches actions and observes state changes
- **Rule 4**: No direct repository calls from UI components

Example structure:
```kotlin
data class SettingsUnitsState(val selectedUnit: DistanceUnit = DistanceUnit.MILES)

class SettingsUnitsInteractor(
    private val repository: UserSettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    sealed class Action {
        data class UnitChanged(val unit: DistanceUnit) : Action()
        object ViewAppeared : Action()
    }
    
    fun handle(state: MutableState<SettingsUnitsState>, action: Action) {
        when (action) {
            is Action.UnitChanged -> {
                state.value = state.value.copy(selectedUnit = action.unit) // Immediate UI update
                scope.launch { repository.updateDistanceUnit(action.unit) } // Async persistence
            }
        }
    }
}

// UI Layer - Clean action dispatching
FilterChip(onClick = { interactor.handle(state, Action.UnitChanged(unit)) })
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

## Testing Guidelines & Lessons Learned

### Testing Framework Consistency
- **Always check existing test patterns first** - Review existing test files to understand project conventions
- **Use Mockito** - This project uses Mockito throughout; maintain consistency over personal preference
- **Example**: When creating new tests, look at existing files like `HistoryCalculationsTest.kt` for mockito-kotlin usage patterns

### Android Framework Dependencies
- **Use Robolectric for Android framework testing** - Required for testing classes that use `android.util.Log` and other Android APIs
- **Keep Android Log calls in business logic** - Log.d() and Log.e() provide valuable production debugging information
- **Don't remove logging to fix tests** - Instead, use Robolectric to handle Android framework mocking
- **Test annotation**: Always use `@RunWith(RobolectricTestRunner::class)` for classes with Android dependencies

### Testing Dependencies (build.gradle.kts)
```kotlin
testImplementation("org.mockito:mockito-core:5.8.0")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
testImplementation("org.robolectric:robolectric:4.11.1")
```

### Key Testing Principles
1. **Framework Consistency**: Always match existing project testing patterns, not personal preferences
2. **Android Mocking**: Use Robolectric for Android framework dependencies rather than removing functionality
3. **Logging Preservation**: Keep Log calls for production debugging; tests should accommodate them, not remove them
4. **Dependency Minimization**: Remove unused testing libraries (like MockK) to keep dependencies clean

## Next Steps for Development
See **[ProductPlan.MD](../ProductPlan.MD)** for comprehensive development roadmap and implementation phases.

### Quick Reference - Current Priorities:
1. **Phase 1**: Implement Room database with Shoe and History entities
2. **Phase 1**: Create repository pattern for data management  
3. **Phase 1**: Port core shoe tracking business logic from iOS
4. **Phase 2**: Implement remaining screen functionality (Add Distance, Active Shoes, Hall of Fame)
5. **Phase 3**: Add external integrations (Health Connect, Strava)
6. **Phase 4**: Comprehensive testing and performance optimization
