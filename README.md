<div align="center">

<img src="fastlane/metadata/android/en-US/images/icon.png" alt="PlayTube Icon" width="120">

# PlayTube

### A fast, private, and feature-rich YouTube client for Android

**No ads · No tracking · No data collection**

<img src="fastlane/metadata/android/en-US/images/PlaytubefeatureGraphic.png" alt="PlayTube Feature Graphic" width="100%">

<br>

<p>
  <a href="https://github.com/arslandaim-hub/PlayTube/releases/latest">
    <img src="https://img.shields.io/github/v/release/arslandaim-hub/PlayTube?style=flat-square&color=10b981" alt="Latest Release">
  </a>
  <a href="https://github.com/arslandaim-hub/PlayTube/stargazers">
    <img src="https://img.shields.io/github/stars/arslandaim-hub/PlayTube?style=flat-square&color=fbbf24" alt="GitHub Stars">
  </a>
  <a href="https://github.com/arslandaim-hub/PlayTube/network/members">
    <img src="https://img.shields.io/github/forks/arslandaim-hub/PlayTube?style=flat-square&color=fbbf24" alt="GitHub Forks">
  </a>
  <a href="https://github.com/arslandaim-hub/PlayTube/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/arslandaim-hub/PlayTube?style=flat-square&color=3b82f6" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Android-API%2024%2B-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android API 24+">
  <img src="https://img.shields.io/badge/Kotlin-100%25-B125EA?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin">
</p>

<p>
  <img src="https://img.shields.io/github/downloads/arslandaim-hub/PlayTube/total?style=flat-square&color=3b82f6&logo=github" alt="Total Downloads">
</p>

<br>

<a href="https://github.com/arslandaim-hub/PlayTube/releases/latest">
  <img src="https://img.shields.io/badge/GET%20IT%20ON-GitHub-000000?style=for-the-badge&logo=github&logoColor=white" height="50" alt="Get it on GitHub">
</a>

<br><br>

<a href="https://f-droid.org/packages/com.arslandaim.playtube/">
  <img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="50" alt="Get it on F-Droid">
</a>

<br><br>

<a href="https://github.com/ImranR98/Obtainium">
  <img src="https://img.shields.io/badge/Get%20Updates%20via-Obtainium-4CAF50?style=for-the-badge" height="50" alt="Get Updates via Obtainium">
</a>

<br>

<sub>Use <b>Obtainium</b> to receive updates directly from PlayTube's GitHub releases.</sub>

</div>

---

## Features

* **Fluid Glass UI:** A modern, dynamic, and smooth full-screen browsing experience.
* **Background Playback:** Continue listening with full media controls.
* **Picture-in-Picture:** Watch videos while using other apps.
* **Comments and Replies:** Browse comments and view replies.
* **Multi-language Subtitles:** Watch content with subtitle support.
* **Incognito Mode:** Browse without affecting personalized recommendations.
* **High-Quality Downloads:** Download supported content in high quality.
* **Gesture Controls:** Control brightness, volume, and playback with gestures.
* **Orientation Controls:** Easily switch between portrait and landscape modes.
* **Subscription Management:** Subscribe to and manage channels without a Google account.
* **Privacy First:** No ads, tracking, or unnecessary data collection.

### Personalized Recommendations

PlayTube includes a lightweight recommendation system that learns from user activity to provide more relevant video suggestions.

Recommendation learning can be paused, and learned data can be cleared at any time from the app settings.

---

## Screenshots

<div align="center">

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/homescreen.png" width="18%" alt="Home Screen">
&nbsp;
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/playerscreen.png" width="18%" alt="Player Screen">
&nbsp;
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/library.png" width="18%" alt="Library Screen">
&nbsp;
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/subscriptions.png" width="18%" alt="Subscriptions Screen">
&nbsp;
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/settings.png" width="18%" alt="Settings Screen">
&nbsp;
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/searchscreen.png" width="18%" alt="Search Screen">

</div>

---

## Technology Stack

| Category                 | Technology         | Description                                    |
| ------------------------ | ------------------ | ---------------------------------------------- |
| **Architecture**         | MVVM               | Model-View-ViewModel architecture              |
|                          | Clean Architecture | Separation between Domain, Data, and UI layers |
|                          | Repository Pattern | Centralized data access and management         |
| **Kotlin and Reactive**  | Kotlin Coroutines  | Asynchronous and background operations         |
|                          | StateFlow          | Reactive UI state management                   |
| **UI**                   | Jetpack Compose    | Modern declarative Android UI                  |
|                          | Material Design 3  | Modern components and dynamic theming          |
|                          | Compose Animations | Smooth transitions and UI animations           |
| **Storage**              | Room Database      | Local storage for user metadata                |
|                          | Jetpack DataStore  | User preferences and application settings      |
| **Media and Networking** | AndroidX Media3    | Video and audio playback                       |
|                          | Coil 3             | Image loading and caching                      |
|                          | NewPipeExtractor   | Stream and metadata extraction                 |
|                          | OkHttp             | HTTP networking                                |
| **Background and DI**    | Hilt (Dagger)      | Dependency injection                           |
|                          | WorkManager        | Reliable background tasks and downloads        |
| **Build Tools**          | KSP                | Kotlin Symbol Processing                       |
|                          | Version Catalogs   | Centralized dependency and version management  |

---

## Acknowledgements

PlayTube would not have been possible without the work of the open-source community.
Special thanks to:
* NewPipe
* NewPipe Extractor
* LibreTube
* PipePipe
* Flow
---

## Important Notice

> [!WARNING]
> Publishing this application on the Google Play Store may violate Google's policies and/or the platform's terms of service. Always review the applicable policies and terms before distributing the application.

---

## License and Code Usage

PlayTube is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

You are free to:

* Use the source code.
* Study how the application works.
* Modify the source code.
* Fork the project.
* Redistribute copies of the project.

### If You Modify or Redistribute PlayTube

When distributing a modified or derivative version of PlayTube, you must comply with the GPL-3.0 license.

This includes:
* Keeping GPL-covered code under the GPL-3.0 license.
* Providing the corresponding source code when required by the license.
* Preserving applicable copyright and license notices.
* Making GPL-covered source code available to recipients under GPL-3.0.
* Clearly documenting significant changes made to the original code.

For the complete license terms, see the [LICENSE](LICENSE) file.

---

## Support PlayTube

If you enjoy using PlayTube and would like to support its continued development, consider becoming a patron.

Your support helps with continued development, maintenance, bug fixes, and future improvements.

<div align="center">

<a href="https://patreon.com/ArslanDaim77">
  <img src="https://img.shields.io/badge/Become%20a%20Patron-Patreon-FF424D?style=for-the-badge&logo=patreon&logoColor=white" alt="Become a Patron">
</a>

</div>

---

<div align="center">

### If you enjoy PlayTube, consider giving the project a star

It helps more people discover the project and supports its continued development.

</div>
