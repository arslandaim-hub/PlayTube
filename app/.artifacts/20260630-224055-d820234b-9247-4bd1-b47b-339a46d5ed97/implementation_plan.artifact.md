# High-Quality Thumbnail Implementation Plan

Significantly upgrade thumbnail sharpness and visual quality across the app while maintaining smooth performance and fast loading.

## Proposed Changes

### [Utils] Premium Thumbnail Selection

#### [VideoUtils.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/utils/VideoUtils.kt)
- Add `getMaxResThumbnail` (1280x720) as the primary target for high-end devices.
- Implement a sophisticated `getBestThumbnailUrl` that falls back from MaxRes -> HighRes -> MediumRes to ensure we always get the sharpest available image.
- Add `getThumbnailFromList` to handle NewPipe's dynamic thumbnail objects with a preference for widths >= 720px.

```kotlin
fun getMaxResThumbnail(videoId: String): String {
    return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
}

// Logic to try maxres first, fallback to hq/mq if maxres doesn't exist for older videos
fun getBestThumbnailUrl(videoId: String): String = getMaxResThumbnail(videoId)
```

---

### [Repositories] Maximize Image Quality

#### [SearchRepositoryImpl.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/data/repository/SearchRepositoryImpl.kt)
- Switch from `mqdefault` (Medium) to `maxresdefault` (Max Quality) for all video items.
- *Performance Note:* Coil will automatically downsample these to fit the screen size, keeping memory usage low.

#### [VideoRepositoryImpl.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/data/repository/VideoRepositoryImpl.kt)
- Use `maxresdefault` for related videos and channel video lists.
- For profile avatars and banners, explicitly pick the largest resolution provided by the extractor (`maxByOrNull { it.width }`).

---

### [DI] Advanced Image Optimization

#### [CoilModule.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/di/CoilModule.kt)
- Enable **High Quality Scaling**: Set `filterQuality(FilterQuality.High)` globally in the `ImageLoader`.
- **Pre-multiplied Alpha**: Ensure alpha is handled correctly for crisp edges.
- **Aggressive Caching**: Maintain the 10% disk cache to keep high-res images local.

---

### [UI] Crisp Rendering

#### Multiple UI Files
- Inject the optimized `ImageLoader` explicitly or ensure it's the `Singleton` used by `AsyncImage`.
- Apply `FilterQuality.High` to all `AsyncImage` instances in:
    - `SearchScreen.kt` (Video rows)
    - `PlayerScreen.kt` (Player placeholder & related)
    - `ChannelScreen.kt` (Banner & avatar)
    - `LibraryScreen.kt` (History & favorites)

## Verification Plan

### Manual Verification
- **Pixel-Peeper Test**: Zoom in on thumbnails on a high-density (1440p) device to confirm "retina" quality.
- **Scroll Smoothness**: Profile GPU rendering while scrolling the search results to ensure no frames are dropped due to downsampling overhead.
- **Cache Hits**: Verify that revisiting a screen loads high-res images instantly from the disk cache.
