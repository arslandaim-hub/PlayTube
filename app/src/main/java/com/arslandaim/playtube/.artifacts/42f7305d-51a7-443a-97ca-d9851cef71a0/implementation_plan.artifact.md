# Implementation Plan - Highly Robust Miniplayer Back Navigation Fix

This plan addresses a critical, recurring bug in PlayTube where the system back button fails to minimize the expanded player, instead causing the underlying navigation (Home/Search screens) to change. The fix utilizes a "Freshness Priority" strategy to ensure the player's back handler always takes precedence over internal `NavHost` handlers.

## User Review Required

> [!IMPORTANT]
> This change introduces a dependency on the `navController` within the `PlayerOverlay` component to track navigation state. This is necessary to ensure the back handler is re-registered whenever a new screen is navigated to, preventing "bleed-through" to the background screens.

## Proposed Changes

### UI & Navigation Layer

#### [MODIFY] [PlayerOverlay.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerOverlay.kt)

- Update the `PlayerOverlay` function signature to accept an optional `navController: NavHostController`.
- Wrap the `BackHandler` call in a `androidx.compose.runtime.key` block.
- Use `isExpanded`, `isMinimizing`, and `currentRoute` as keys to force re-registration of the back handler whenever the player state or navigation state changes.
- This ensures the player's back callback is always the "most recent" in the `OnBackPressedDispatcher`, giving it highest priority regardless of tree depth.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainActivity.kt)

- Pass the existing `navController` instance to the `PlayerOverlay` call.

## Verification Plan

### Manual Verification
1. **Basic Interception**: Open a video, expand it, and press the back button. Verify it minimizes to the miniplayer.
2. **Navigation Depth Test**:
    - Go to Home.
    - Navigate to Search.
    - Perform a search and navigate to a video.
    - Expand the video player.
    - Press the back button.
    - **Expected Result**: Player minimizes. Search screen remains active. (The bug currently causes the search screen to close or go back).
3. **Double-Back Test**: Quickly press the back button twice while the player is expanded.
    - **Expected Result**: First press minimizes. Second press (after animation) navigates back in the `NavGraph`.
4. **Miniplayer Back Test**: While the player is in mini-mode (minimized), press the back button.
    - **Expected Result**: Underlying navigation should work normally (pop backstack). The miniplayer should NOT intercept.

### Automated Verification
- I will run the project to ensure no compilation errors due to the signature change.
