# Implementation Plan - Smooth and Stable Scroll-to-Hide Bars

Improve the smoothness and stability of the top and bottom bars' appearance/disappearance. We will move away from simple visibility toggling (which causes layout jumps) to animated height transitions and refined scroll detection.

## Proposed Changes

### [Animations & Layout]

#### [MainActivity.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/MainActivity.kt)

- **Animated Heights**: Replace `AnimatedVisibility` with `animateDpAsState` for the heights of the top and bottom bars.
- **Stable Slots**: Wrap `TopAppBar` and `PlayTubeBottomBar` in `Box` containers with animated heights and `clipToBounds`. This ensures that the `Scaffold`'s `innerPadding` changes smoothly, causing the content to slide rather than jump.
- **Transition Specs**: Use `spring` (low stiffness) or `tween` (cubic bezier) for "buttery smooth" motion.
- **MiniPlayer Integration**: Ensure the `MiniPlayer` stays stable and moves smoothly with the bottom bar.

---

### [Scroll Detection]

#### [ScrollUtils.kt](file:///C:/Users/AK/AndroidStudioProjects/PlayTube/app/src/main/java/com/example/playtube/utils/ScrollUtils.kt)

- **Scroll Accumulator**: Implement a scroll delta accumulator to prevent jitter from tiny finger movements.
- **Directional Hysteresis**: Use different thresholds for showing (easier) and hiding (requires more deliberate scroll) to prevent rapid toggling.
- **Source Filtering**: Only react to `UserInput` (touch) to avoid issues with programmatic scrolls or momentum.

---

## Verification Plan

### Automated Tests
- Run `:app:assembleDebug` to verify build integrity.

### Manual Verification
- **Slow Scroll**: Verify that slow, deliberate scrolls trigger the bars without jitter.
- **Direction Change**: Verify that changing scroll direction mid-way behaves predictably.
- **Stable Content**: Ensure there are no white flashes or "jumps" where the content suddenly shifts by a large amount.
- **MiniPlayer**: Verify the `MiniPlayer` moves in sync with the bottom bar visibility.
