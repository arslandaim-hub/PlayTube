# Task: Implementation of Transient Banners & Resume Fix

- [x] Research and Planning
    - [x] Analyze Top Bar structure for "Online" banner
    - [x] Design restoration detection logic
    - [x] Investigate "Black Screen" on resume for local videos
    - [x] Create implementation plan
- [x] Implementation
    - [x] Add 3s timer for "Online" banner
    - [x] Add 5s timer for "Offline" banner
    - [x] Shrink both banners for a more compact look
    - [x] Fix overlap by increasing Z-index to 200f
    - [x] Robustify `PlayerView` update block to force surface re-attach
- [ ] Verification
    - [ ] Verify banner timers work as expected
    - [ ] Verify banners appear above mini-player
    - [ ] Verify local videos resume correctly after long pauses
