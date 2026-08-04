# Search Newest Tab UX Redesign

This plan implements a more direct and informative UX for the "Newest" search tab, replacing the buffering badge with a top-level progress bar and disabling bottom pagination for this specific sort.

## User Review Required

> [!NOTE]
> - **Top-level Loading**: Switching to the "Newest" tab will now trigger a specific "Fetching newest videos..." UI state at the top of the list (or as a full overlay if the list is empty).
> - **No Infinite Scroll**: Bottom pagination (loaders) will be disabled ONLY for the "Newest" tab to keep the focus on the initial batch of sorted results.
> - **Removal of Badge**: The "Show New Results" badge will be removed in favor of this direct approach.

## Proposed Changes

### Search Logic & State
Group: `ui/screens/search`

#### [SearchViewModel.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/search/SearchViewModel.kt)
- Remove `_bufferedNewItems` and `showNewResultsBadge`.
- Add `isSortingNewest` (MutableStateFlow) to track the transition to the Newest tab.
- Update `onSortChange()`:
    - When switching to `UPLOAD_DATE`, set `isSortingNewest = true`.
    - Perform a fresh search or re-sort existing results (based on implementation details) and then set `isSortingNewest = false`.
- Update `loadNextPage()`:
    - Add a check to return early if `searchSort == UPLOAD_DATE`.

#### [SearchScreen.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/arslandaim/playtube/ui/screens/search/SearchScreen.kt)
- Observe `isSortingNewest` from the ViewModel.
- Remove the "Show New Results" badge logic.
- Add a LinearProgressIndicator with a "Fetching newest videos..." label at the top of the search list when `isSortingNewest` is true.
- Update `InfiniteScrollEffect` to be disabled when `searchSort == UPLOAD_DATE`.

## Verification Plan

### Automated Tests
- `gradle_build("app:assembleDebug")`: Ensure no compilation regressions.

### Manual Verification
1. **Newest Tab Transition**: Search for something, then click "Newest". Verify that a top progress bar appears with the message "Fetching newest videos...".
2. **Infinite Scroll check**: Scroll to the bottom of the "Newest" results. Verify no loader appears and no new pages are fetched.
3. **Relevance Tab stability**: Switch back to "Relevance". Verify that infinite scroll still works and no top progress bar appears.
4. **Zero Regression**: Ensure standard search results still display correctly in all other tabs.
