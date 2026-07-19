# Robust Background Playback (Optional Feature)

The goal is to allow users to keep listening to audio when the app is backgrounded, while respecting their battery preferences by keeping the feature OFF by default.

## Proposed Changes

### Core Component (Permissions & Manifest)

#### [AndroidManifest.xml](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/AndroidManifest.xml)
- Add `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission.
- Register `PlaybackService` with `mediaPlayback` foreground type.

### Data Component (Preferences)

#### [PreferencesManager.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/data/local/PreferencesManager.kt)
- Add `isBackgroundPlayEnabled` (default `false`).

### Service Component (The Bridge)

#### [NEW] [PlaybackService.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/services/PlaybackService.kt)
- Implement `MediaSessionService`.
- Use the shared `@Singleton ExoPlayer` instance.
- This tells Android the app is a media player, preventing the system from killing the audio when backgrounded.

### UI Component (Settings & Integration)

#### [SettingsScreen.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/settings/SettingsScreen.kt)
- Add a toggle for "Background Play".

#### [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainActivity.kt)
- Update `onPause` logic:
    - If `isBackgroundPlayEnabled` is **OFF** -> Pause player (existing behavior).
    - If `isBackgroundPlayEnabled` is **ON** -> Do NOT pause; let `PlaybackService` maintain audio flow.
- This ensures **PiP mode** remains unaffected as it's checked first.

## Verification Plan

### Manual Verification
1.  **Setting OFF (Default)**: Play a video, minimize app. Audio should pause immediately (preserving battery).
2.  **Setting ON**: Play a video, minimize app. Audio should continue playing. A media notification should appear.
3.  **PiP Interaction**: With Background Play **ON**, enter PiP. Then close the PiP window. Audio should behave according to the PiP-close rules (pause).
4.  **Device Compatibility**: Verify on Honor/Android 14+ that audio isn't killed by the system after ~30 seconds of backgrounding.
