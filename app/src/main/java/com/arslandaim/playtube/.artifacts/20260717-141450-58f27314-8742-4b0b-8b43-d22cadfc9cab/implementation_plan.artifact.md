# Fix Subtitle Visibility Bug

Audit of the codebase revealed that subtitles are not appearing due to a conflict between the `PlayerViewModel` and `PlayerScreen`. Specifically, the `onTracksChanged` listener in the ViewModel was overwriting the user's preferred Closed Caption (CC) state with the player's initial "no tracks selected" state, and the `PlayerScreen` was redundantly overwriting track selection parameters, losing any manual overrides.

## Proposed Changes

### Player Component

Consolidate track selection logic and fix state management for subtitles.

#### [PlayerViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerViewModel.kt)

- **Fix `onTracksChanged`**: Stop blindly overwriting `_isCcEnabled` from the player's internal state. Instead, use it to ensure the player matches the user's desired CC state once tracks are loaded.
- **Improve `updateCcState`**: Ensure it handles cases where tracks are not yet available and use `TrackSelectionOverride` more robustly to force selection when requested.

```kotlin
// Updated onTracksChanged logic
override fun onTracksChanged(tracks: Tracks) {
    val isCcActive = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected }

    // If the user wants CC enabled but it's not active (common on first load), try to enable it
    if (_isCcEnabled.value && !isCcActive) {
        val hasTextTracks = tracks.groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
        if (hasTextTracks) {
            updateCcState(true)
        }
    }
}
```

#### [PlayerScreen.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerScreen.kt)

- **Remove redundant `update` block**: Remove the `player.trackSelectionParameters` update from the `AndroidView`'s `update` block. This logic is already handled by the ViewModel, and having it in the Screen was overwriting overrides set by the ViewModel.

```diff
-        update = {
-            // State Updates - Does NOT recreate the view
-            player.trackSelectionParameters = player.trackSelectionParameters
-                .buildUpon()
-                .setTrackTypeDisabled(androidx.media3.common.C.TRACK_TYPE_TEXT, !isCcEnabled)
-                .build()
-        },
+        update = { /* Managed by ViewModel */ },
```

## Verification Plan

### Manual Verification
- **Test CC Toggle**: Play a video with subtitles, toggle CC on and off using the UI button. Verify subtitles appear/disappear.
- **Test Persistence**: Enable CC, close the player, and open another video. Verify CC remains enabled and subtitles appear automatically.
- **Test Quality Change**: Change video quality. Verify subtitles persist through the media source reload.
- **Test Adaptive Streams**: Play a high-resolution video (1080p+) which uses `MergingMediaSource`. Verify subtitles still show.

### Logcat Monitoring
- Monitor Logcat for any `ExoPlayer` or `Media3` warnings related to track selection or subtitle loading.
