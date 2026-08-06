# Implementation Plan - Online Status Banner

This plan adds a transient "Online" banner that appears just below the Top App Bar for 3 seconds when the internet connection is restored.

## Proposed Changes

### UI & Animation Layer

#### [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainActivity.kt)

- **State Observation**: Observe the `isOffline` state to detect the transition from offline to online.
- **Timer Logic**: Use a `LaunchedEffect` with `snapshotFlow` or similar to detect when `isOffline` becomes `false` after having been `true`.
- **Implement `OnlineTopBanner`**: Create a compact green banner component.
    - Style: Material 3 `successContainer` or a custom green variant.
    - Message: "Back Online".
- **Placement**: Anchor the banner inside the `TopAppBar` column, ensuring it appears below the bar but above the main content.

```kotlin
// Detection Logic
var showOnlineBanner by remember { mutableStateOf(false) }
var wasPreviouslyOffline by remember { mutableStateOf(false) }

LaunchedEffect(isOffline) {
    if (!isOffline && wasPreviouslyOffline) {
        showOnlineBanner = true
        delay(3000L) // 3 second timer
        showOnlineBanner = false
    }
    wasPreviouslyOffline = isOffline
}

// UI Integration in the Scaffold topBar Column
AnimatedVisibility(
    visible = showOnlineBanner,
    enter = expandVertically() + fadeIn(),
    exit = shrinkVertically() + fadeOut()
) {
    OnlineTopBanner()
}
```

---

## Verification Plan

### Manual Verification
- **Reconnection Check**:
    1. Start the app offline (Bottom banner should appear/timer out).
    2. Enable Wi-Fi.
    3. Verify a green "Back Online" banner appears under the Top App Bar.
    4. Verify it disappears automatically after 3 seconds.
- **Initial Launch**: Verify the banner does NOT appear when the app is launched with a working connection (it should only trigger on restoration).
- **Z-Index/Layout**: Ensure it doesn't push down the Top App Bar or overlap icons in a jarring way.
