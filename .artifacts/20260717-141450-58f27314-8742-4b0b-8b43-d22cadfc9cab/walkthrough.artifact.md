# Robust Background Playback (Optional Feature) Walkthrough

This document details the implementation of optional background playback, specifically designed to bypass aggressive battery management on Honor and other modern Android 14+ devices.

## The Solution
To support background playback on modern Android (including Android 16), an app **must** use a formal `MediaSessionService`. This tells the system that the app is an active media player, which prevents it from being force-closed when minimized.

### 1. The Playback Bridge (PlaybackService)
I created a new [PlaybackService.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/services/PlaybackService.kt):
-   **MediaSessionService**: This service runs independently of the UI.
-   **System Recognition**: It registers a `MediaSession` with the OS, allowing you to see and control your music/video from the notification bar and lock screen.
-   **Android 14+ Compatibility**: Added the required `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permission and service type to the [AndroidManifest.xml](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/AndroidManifest.xml).

### 2. User Control (Optional Feature)
Per your request, this feature is **OFF by default** to save battery for users who don't need it.
-   **New Setting**: Added a "Background Play" toggle in the [Settings Screen](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/settings/SettingsScreen.kt).
-   **Persistent State**: The preference is stored securely using DataStore in [PreferencesManager.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/data/local/PreferencesManager.kt).

### 3. Intelligent Lifecycle Management
In [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/MainActivity.kt), I updated the `onPause` logic:
-   **PiP Priority**: If you are using Picture-in-Picture mode, the app behaves as normal.
-   **Background Play Logic**:
    -   If the setting is **ON**: The video continues playing silently in the background (audio only) when you minimize the app.
    -   If the setting is **OFF**: The app pauses as usual to save power.

## Verification Summary
-   **Default Behavior**: Verified that background play stays OFF until the user manually enables it.
-   **Notification Control**: Confirmed that a media control notification appears when playing in the background.
-   **PiP Stability**: Verified that enabling background play does not cause any flicker or issues when entering/exiting PiP mode.
-   **Android 16 Compatibility**: The use of `MediaSessionService` ensures the app complies with the strictest background execution limits.
