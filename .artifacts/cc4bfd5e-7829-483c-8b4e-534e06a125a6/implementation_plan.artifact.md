# Task: PlayTube Core Download Engine Overhaul & Stability Bug Fixes

This plan aims to overhaul the download engine for better performance and stability, fix notification collisions, ensure correct media muxing, and resolve duplicate key issues in Compose LazyLists.

## User Review Required

> [!IMPORTANT]
> The specialized `OkHttpClient` for downloads will increase the connection pool size. Ensure this aligns with server-side rate limits if applicable.

## Proposed Changes

---

### Core Download Engine & Muxing

#### [MODIFY] [DownloadWorker.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/workers/DownloadWorker.kt)
- Configure `workerOkHttpClient` with expanded connection pool and dispatcher limits.
- Update `getRemoteFileSize` with a resilient probe mechanism (HEAD -> GET Range fallback).
- Refactor `downloadParallel` to use a 1Hz ticker coroutine for progress updates, removing mutex contention.
- Update `createForegroundInfo` to use deterministic notification IDs based on `videoId.hashCode()`.
- Overhaul `muxVideoAudio` with 8MB buffer, proper container alignment, and atomic file finalization.

---

### ViewModel Logic & Stability

#### [MODIFY] [PlaylistViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/playlist/PlaylistViewModel.kt)
- Implement symmetric audio/video format matching in `download` function.
- Ensure compatibility between `mp4/avc` and `m4a/aac`, and `webm/vp9` and `webm/opus`.

#### [MODIFY] [SearchViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/search/SearchViewModel.kt)
- Implement symmetric audio/video format matching in `download` function.
- Sanitize search results with `distinctBy { it.id }` (or `uniqueKey`) before updating UI state to prevent `LazyList` duplicate key exceptions.

#### [MODIFY] [SubscriptionsFeedViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/subscriptions/SubscriptionsFeedViewModel.kt)
- Implement symmetric audio/video format matching in `download` function.
- Sanitize feed items with `distinctBy { it.id }` before updating UI state.

---

### Data Import Robustness

#### [MODIFY] [ImportWorker.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/workers/ImportWorker.kt)
- Enhance `importHistory` with robust error handling for individual items to prevent full import failure on corrupted records.

## Verification Plan

### Automated Tests
- Run existing unit tests for `DownloadWorker` if available.
- Build the project to ensure no compilation errors.

### Manual Verification
- Test multiple concurrent downloads to verify no notification collisions.
- Verify download speeds are improved.
- Check that downloaded videos are playable and correctly muxed (especially WebM).
- Perform searches and scroll quickly to ensure no `LazyList` crashes.
- Import a `watch-history.json` with intentionally malformed entries.
