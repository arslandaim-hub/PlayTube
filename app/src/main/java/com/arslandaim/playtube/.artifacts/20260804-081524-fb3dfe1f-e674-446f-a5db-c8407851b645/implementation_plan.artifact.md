# Fix Autoplay & Silent Pre-loaded Video

This plan fixes two issues: the player advancing to the next video even when autoplay is disabled, and pre-loaded videos having no audio.

## Root Cause Analysis

1.  **Autoplay Logic Error**: `PlayerViewModel` currently uses `player.addMediaItem(1, mediaItem)` to preload the next video. However, ExoPlayer automatically advances to the next item in the queue when the current one ends, regardless of our internal `_isAutoplayEnabled` state. The manual `loadVideo` call in the listener is redundant and potentially creates a race condition.
2.  **Missing Audio in Preload**: `prepareNextMediaItem` uses `MediaItem.Builder().setUri(stream.url)...build()`. This only sets the video stream URL. In PlayTube's architecture, high-quality streams are often "adaptive" (separate video and audio). The current preloading logic forgets to fetch and merge the audio track for these adaptive streams.

## Proposed Changes

### ViewModel Layer

#### [PlayerViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/player/PlayerViewModel.kt)

- **Disable Native Advancement**: Set `player.repeatMode = Player.REPEAT_MODE_OFF` and ensure the queue only advanced when we explicitly allow it. Actually, a better way is to NOT add the next item to the queue if autoplay is off.
- **Refine Preloading**: Update `prepareNextMediaItem` to use the correct `MediaSource` construction logic (similar to `setMediaSource`) which merges audio for adaptive streams.
- **Fix Transition Logic**: Ensure the listener respects the `_isAutoplayEnabled` flag correctly and prevents automatic queue advancement if disabled.

```kotlin
// Revised prepareNextMediaItem
private fun prepareNextMediaItem(video: VideoItem, bundle: StreamBundle) {
    if (!_isAutoplayEnabled.value) return // Don't preload if user doesn't want autoplay

    // ... logic to build full MediaItem with audio merging ...
}
```

---

## Verification Plan

### Manual Verification
- **Autoplay OFF**: Start a video, disable autoplay. Wait for it to end. Verify that it stops on the end screen/last frame and does NOT start the next video.
- **Autoplay ON**: Enable autoplay. Wait for video to end. Verify that the next video starts automatically AND has sound.
- **Manual Skip**: Start a video, click "Next". Verify the next video has sound (verifies that pre-loaded items are correctly configured with audio).
