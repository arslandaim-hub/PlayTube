# Implementation Plan - Structural Audit & Robustness Refactoring

This plan outlines a comprehensive refactoring of the PlayTube codebase to align with professional Android app design standards. The goal is to improve maintainability, testability, and robustness by enforcing strict separation of concerns and modern architecture patterns.

## User Review Required

> [!IMPORTANT]
> This refactoring involves significant changes to the core of the application, including splitting the `PlayerViewModel` and moving significant logic out of `MainActivity`. While the functionality will remain identical, the internal structure will change.

## Proposed Changes

### 1. Architecture & State Management (Core)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainActivity.kt)
- **Separation of Concerns**: Move Composable UI components (`PlayTubeTopAppBar`, `PlayTubeBottomBar`, `OfflineBottomBanner`, `RestorationBanner`) to a new package `ui.components.main`.
- **State Hoisting**: Move UI-related state (bars visibility, current screen tracking, PIP states) to `MainViewModel`.
- **Deep Link Handling**: Refactor `handleDeepLink` into a dedicated `DeepLinkHandler` or move logic to `MainViewModel`.

#### [MODIFY] [MainViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainViewModel.kt)
- **Expand Responsibilities**: Take over state management for global UI components from `MainActivity`.
- **Unified UI State**: Introduce a `MainUiState` data class to manage global visibility and system states.

### 2. Player Layer Refactoring (Decoupling)

#### [NEW] [PlaybackManager.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlaybackManager.kt)
- **Responsibility**: Handle low-level ExoPlayer interaction, media item conversion, and player state synchronization. This decouples the ViewModel from direct Media3 API calls where possible.

#### [MODIFY] [PlayerViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerViewModel.kt)
- **Signature Reduction**: Reduce constructor parameters by delegating specialized logic to Use Cases or dedicated Managers (e.g., `QueueManager`, `MiniPlayerManager`).
- **Logic Extraction**: Consider splitting into `VideoDetailsViewModel` (for the bottom sheet content) and `PlaybackViewModel` (for the player controls).

### 3. Data Layer Robustness

#### [MODIFY] [VideoRepositoryImpl.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/data/repository/VideoRepositoryImpl.kt)
- **Remove Application Dependency**: Replace `PlayTubeApp.waitInit()` with a more robust initialization check or a Hilt-managed initialization worker.
- **Error Handling**: Replace `e.printStackTrace()` with a `Result`-based error propagation system.

### 4. UI Consistency & Cleanliness

#### [MODIFY] [Theme.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/theme/Theme.kt)
- **Centralize Styles**: Ensure all custom "Glass" and "Incognito" styles are defined as part of the theme, not as local constants in Activities.

## Verification Plan

### Automated Tests
- **Build Verification**: Run `:app:assembleDebug` to ensure no compilation issues after refactoring.
- **Unit Tests**: Ensure existing repository and view model tests pass (or update them to reflect the new structure).

### Manual Verification
- **Navigation Flow**: Verify that all screens navigate correctly and the bottom/top bars behave as expected.
- **Player Continuity**: Verify that expanding/minimizing the player and PIP mode work without regression.
- **Offline Transitions**: Verify that banners appear/disappear correctly when toggling Airplane mode.
