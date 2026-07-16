<div align="center">

# PlayTube

<img src="fastlane/metadata/android/en-US/images/icon.png" alt="PlayTube Icon" width="120">

<p>
  <b>Fast, private, and feature-rich YouTube client for Android. No Ads, tracking or data collection</b>
</p>

<img src="fastlane/metadata/android/en-US/images/PlaytubefeatureGraphic.png" alt="PlayTube Feature Graphic" width="100%" style="max-width: 800px; border-radius: 12px; margin-top: 10px; margin-bottom: 20px;">

<br>

<a href="https://github.com/arslandaim-hub/PlayTube/releases/latest"><img src="https://img.shields.io/badge/GET%20IT%20ON-GitHub-000000?style=for-the-badge&logo=github&logoColor=white" height="50" alt="Get it on GitHub" style="margin-right: 15px; margin-bottom: 4px;"></a><a href="https://f-droid.org/packages/com.arslandaim.playtube/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="50" alt="Get it on F-Droid"></a>

</div>

---

## ✨ Key Features
* Android 7+
* Picture-in-Picture Support
* High quality video downloads up to 4K
* Built-in video volume/brightness and Seek forward/backward gestures
* Push up & down landscape/portrait modes
* Subscription Management (No Gmail Required)
* Search History & Privacy
* Dynamic UI, Smooth, full-screen browsing experience.

---

## 🛠️ Technology Stack

| Category | Technology | Description |
| :--- | :--- | :--- |
| **App Architecture** | **MVVM** | (Model-View-ViewModel) |
| | **Clean Architecture** | (Domain, Data & UI Layers) |
| | **Repository Pattern** | Centralized data access from local and remote sources |
| **Kotlin & Reactive**| **Kotlin Coroutines** | Handles asynchronous tasks and background operations |
| | **StateFlow** | Reactive UI state management |
| **UI Framework** | **Jetpack Compose** | 100% declarative UI toolkit |
| | **Material Design 3** | Modern Android UI components and dynamic theming |
| | **Compose Animations**| Smooth UI animations and transitions |
| **Data Storage** | **Room Database** | Stores user Metadata |
| | **Jetpack DataStore** | Stores user preferences such as PiP and History settings |
| **Media & Networking**| **AndroidX Media3** | (ExoPlayer) video, audio playback |
| | **Coil 3** | Fast image loading with caching |
| | **NewPipeExtractor** | Extracts YouTube streams and metadata |
| | **OkHttp** | HTTP networking client |
| **Background & DI** | **Hilt (Dagger)** | Dependency Injection |
| | **WorkManager** | Reliable background downloads |
| **Build & Tooling** | **KSP** | Kotlin Symbol Processing annotation processing for Room & Hilt |
| | **Version Catalogs** | Centralized dependency version management |

---

## 📱 Screenshots

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/home.png" width="180" style="margin: 5px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-1.png" width="180" style="margin: 5px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot-5.png" width="180" style="margin: 5px;">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/blacktheme.png" width="180" style="margin: 5px;">
</div>

<br>

<div align="center">
  <h2 style="color: #d73a49;">🛑 WARNING 🛑</h2>
  <p style="color: #d73a49; font-weight: bold; font-size: 16px;">
    Publishing this app on the Google Play Store violates their Terms of Service.
  </p>
</div>

<br>

---

## 📌 Important Notes

* **Subscriptions:** Subscriptions tab in home screen shows videos only when you subscribe to a channel. Videos in subscriptions tab appear only from subscribed channels.
* **Recommendations:** The home page suggests videos based on your search history. If search history is paused, the app will not suggest search related videos in home screen.
