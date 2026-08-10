# Walkthrough - Fix PiP Running Without Permission

I have fixed the bug where Picture-in-Picture (PiP) mode was activating even when disabled in the app settings.

## Changes Made

### ViewModel & Preference Logic
- **`MainViewModel.kt`**:
    - Updated the sharing strategy for `isPipEnabled`, `isBackgroundPlayEnabled`, and `isIncognitoMode` from `WhileSubscribed(5000)` to `SharingStarted.Eagerly`.
    - **Why?**: The `WhileSubscribed` strategy stops the background flow when no UI components are observing it. When the user leaves the app (triggering `onUserLeaveHint`), the flow was often inactive, causing it to return its hardcoded initial value (`true`) instead of the actual user preference from disk. By using `Eagerly`, we ensure the latest preference is always loaded and ready for system callbacks.
    - Corrected the initial value of `isPipEnabled` to `false` to prevent a "race to true" during startup.

## Verification Results

### Automated Tests
- **Gradle Build**: Successfully completed `:app:assembleDebug`.

### Manual Verification Recommended
1. **Setting Toggle**: Disable PiP in Settings and verify that pressing Home during playback does NOT trigger the PiP window.
2. **Persistence**: Disable PiP, restart the app, and verify it remains disabled and respected.
3. **Background Play**: Ensure that disabling PiP doesn't break the "Background Play" (audio-only) feature if that remains enabled.
