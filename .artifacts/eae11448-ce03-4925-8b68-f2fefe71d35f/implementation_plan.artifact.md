# Implementation Plan - Clean Up Timestamps and Move Fullscreen Button to Top

Refining the video player UI to remove duplicate timestamps and replace the top repeat button with a fullscreen button.

## User Review Required

> [!IMPORTANT]
> - Remove the timestamps located above the progress bar in `PersistentProgressBar` to avoid duplication.
> - Remove the repeat button from the top settings pill in `PlayerControlsOverlay`.
> - Replace the repeat button slot in the top settings pill with the dedicated fullscreen button.

## Proposed Changes

### UI Components & Player Screen

#### [MODIFY] [PlaybackProgressComponents.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/components/player/PlaybackProgressComponents.kt)
- Remove the timestamp `Row` (`if (isInteractive && durationMs > 0L) { ... }`) from `PersistentProgressBar`.

#### [MODIFY] [PlayerScreen.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerScreen.kt)
- In `PlayerControlsOverlay`:
  - Remove the repeat button `IconButton` from the top settings pill.
  - Replace it with a fullscreen button (`IconButton` with `Fullscreen` / `FullscreenExit` icon using `onToggleFullscreen`).
  - Keep the bottom bar clean (timestamps only or bottom layout).

## Verification Plan

### Automated Tests
- Build project using Gradle (`app:assembleDebug`).

### Manual Verification
- Deploy and test video player:
  - Verify upper timestamps above progress bar are gone.
  - Verify repeat button is removed from top bar, and replaced by fullscreen button.
  - Verify clicking the top fullscreen button toggles screen orientation correctly.
