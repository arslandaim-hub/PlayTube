# Task: Fix Autoplay and Silent Next Video

- [x] Research and Planning
    - [x] Analyze `PlayerViewModel` queue logic
    - [x] Identify cause of silent pre-loaded videos (missing audio merge)
    - [x] Identify cause of forced autoplay (ExoPlayer queue behavior)
    - [x] Create implementation plan
- [x] Implementation
    - [x] Update `prepareNextMediaItem` to merge audio for adaptive streams
    - [x] Refactor autoplay logic to strictly respect the `_isAutoplayEnabled` flag
- [ ] Verification
    - [ ] Verify autoplay OFF behavior
    - [ ] Verify autoplay ON transitions have audio
    - [ ] Verify manual skip has audio
