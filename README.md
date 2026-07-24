<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" alt="PlayTube Icon" width="120">

# PlayTube

**Fast, private, and feature-rich YouTube client for Android. No Ads, tracking, or data collection.**

<img src="fastlane/metadata/android/en-US/images/PlaytubefeatureGraphic.png" alt="PlayTube Feature Graphic" width="100%">

<br>

<a href="https://github.com/arslandaim-hub/PlayTube/releases/latest">
  <img src="https://img.shields.io/badge/GET%20IT%20ON-GitHub-000000?style=for-the-badge&logo=github&logoColor=white" height="50" alt="Get it on GitHub">
</a>
&nbsp;&nbsp;
<a href="https://f-droid.org/packages/com.arslandaim.playtube/">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="50" alt="Get it on F-Droid">
</a>

</div>

---

## ✨ Key Features

*   **Fluid Glass UI:** Dynamic, smooth, and full-screen browsing experience.
*   **Background Play with media controls.** 
*   **Picture-in-Picture (PiP).** 
*   **Subtitles Support.**
*   **High-Quality Downloads.**
*   **Gesture Controls.**
*   **Orientation Flexibility:** Push up/down landscape and portrait modes.
*   **Subscription Management:** (No Google account required).
*   **Privacy First.**

---

## 🛠️ Technology Stack

| Category | Technology | Description |
| :--- | :--- | :--- |
| **App Architecture** | MVVM | Model-View-ViewModel architecture |
| | Clean Architecture | Strict Domain, Data, & UI layer separation |
| | Repository Pattern | Centralized data access from local and remote sources |
| **Kotlin & Reactive** | Kotlin Coroutines | Asynchronous tasks and background operations |
| | StateFlow | Reactive UI state management |
| **UI Framework** | Jetpack Compose | 100% declarative UI toolkit |
| | Material Design 3 | Modern Android UI components and dynamic theming |
| | Compose Animations | Smooth UI animations and transitions |
| **Data Storage** | Room Database | Stores local user metadata |
| | Jetpack DataStore | Stores user preferences (e.g., PiP and History settings) |
| **Media & Networking**| AndroidX Media3 | ExoPlayer integration for video/audio playback |
| | Coil 3 | Fast, modern image loading with caching |
| | NewPipeExtractor | Extracts YouTube streams and metadata |
| | OkHttp | Reliable HTTP networking client |
| **Background & DI** | Hilt (Dagger) | Dependency Injection framework |
| | WorkManager | Guaranteed background task execution for downloads |
| **Build & Tooling** | KSP | Kotlin Symbol Processing for Room & Hilt |
| | Version Catalogs | Centralized dependency and version management |

---

## 📱 Screenshots

<div align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/home.png" width="18%" alt="Home Screen">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/library.png" width="18%" alt="Library Screen">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/subscriptions.png" width="18%" alt="Subscriptions Screen">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/settings.png" width="18%" alt="Settings Screen">
  &nbsp;
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/search.png" width="18%" alt="Search Screen">
</div>

<br>

> [!WARNING]
> **Publishing this app on the Google Play Store violates their Terms of Service.**

---

## Support PlayTube

If you enjoy using PlayTube and would like to support its continued development, consider becoming a patron. Your support helps fix bugs and ensures long-term maintenance of the project.

<div align="center">
  <a href="https://patreon.com/ArslanDaim77">
    <img src="https://img.shields.io/badge/Become%20a%20Patron-Patreon-FF424D?style=for-the-badge&logo=patreon&logoColor=white" alt="Become a Patron">
  </a>
</div>

---

## 📌 Important Notes

*   **Subscriptions:** The Subscriptions tab on the home screen only populates when you subscribe to a channel. Videos in this tab are strictly from your subscribed channels.
*   **Recommendations:** The home page suggests videos based on your search history. If you pause your search history in the settings, the app will stop suggesting search-related videos on the home screen.
