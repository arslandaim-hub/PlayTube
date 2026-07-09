<table align="center">
  <tr>
    <td>
      <img src="fastlane/metadata/android/en-US/Images/icon.png" alt="PlayTube Icon" width="50">
    </td>
    <td>
      <h1 style="margin: 0;">PlayTube</h1>
    </td>
  </tr>
</table>

<p align="center">
  Fast, private, and feature-rich YouTube client for Android.
</p>

## Download

<p align="center">

<a href="https://github.com/arslandaim-hub/PlayTube/releases/latest">
  <img src="https://img.shields.io/badge/GET%20IT%20ON-GitHub-000000?style=for-the-badge&logo=github&logoColor=white" alt="Get it on GitHub">
</a>
<br><br>
<a href="https://f-droid.org/packages/com.arslandaim.playtube/">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="220" alt="Get it on F-Droid">
</a>
<br><br>
<a href="https://github.com/arslandaim-hub/PlayTube/releases/latest">
  <img src="https://img.shields.io/github/downloads/arslandaim-hub/PlayTube/total?style=for-the-badge&logo=github&label=GitHub%20Downloads" alt="GitHub Downloads">
</a>

</p>

## Key Features
- Supports android 7 and above
- Background Play
- Picture-in-Picture Support
- Highest available video quality downloads
- Built-in video volume/brightness and Seek forward/backward gestures
- Push up & down landscape/portrait modes
- Subscription Management
- Search History & Privacy
- Dynamic UI, Smooth, full-screen browsing experience.

## 🛠️ Technology Stack

### App Architecture

| | |
|---|---|
| **Architecture** | MVVM (Model-View-ViewModel) |
| **Design Pattern** | Clean Architecture (Domain, Data & UI Layers) |
| **Repository Pattern** | Centralized data access from local and remote sources |

### Kotlin & Reactive Programming

| | |
|---|---|
| **Kotlin Coroutines** | Handles asynchronous tasks and background operations |
| **StateFlow** | Reactive UI state management |

### UI Framework

| | |
|---|---|
| **Jetpack Compose** | 100% declarative UI toolkit |
| **Material Design 3** | Modern Android UI components and dynamic theming |
| **Compose Animations** | Smooth UI animations and transitions |

### Data Storage

| | |
|---|---|
| **Room Database** | Stores user Metadata |
| **Jetpack DataStore** | Stores user preferences such as PiP and History settings |

### Media & Networking

| | |
|---|---|
| **AndroidX Media3 (ExoPlayer)** | video, audio playback |
| **Coil 3** | Fast image loading with caching |
| **NewPipeExtractor** | Extracts YouTube streams and metadata |
| **OkHttp** | HTTP networking client |

### Dependency Injection & Background Tasks

| | |
|---|---|
| **Hilt (Dagger)** | Dependency Injection |
| **WorkManager** | Reliable background downloads |

### Build & Tooling

| | |
|---|---|
| **Kotlin Symbol Processing (KSP)** | Annotation processing for Room & Hilt |
| **Version Catalogs** | Centralized dependency version management |

## Subscriptions
- Subscriptions section in home screen shows videos only when you subscribe to a channel.
- Videos in subscriptions section appear only from subscribed channels.

## Screenshots

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/home.png" 
  width="130">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-1.png" 
  width="130">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-5.png" 
  width="130">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/blacktheme.png" 
  width="130">

Enjoy a premium experience with no ads, tracking or whatsoever. Expect some bugs.

There is also support for downloading whole playlists, but for that your network connection has to be stable for smooth downloading.

The home page suggests videos based on your search history. If search history is paused, the app will not suggest search related videos in home screen.
