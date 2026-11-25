# TouchDuo 1.1 — simultaneous A+B

TouchDuo is an Android 8.0+ simultaneous two-touch trigger, not an auto-clicker.

## What one press does
A single tap on the floating A+B button sends one Android Accessibility GestureDescription containing two stationary strokes:
- Point A and point B have the same start time (0 ms).
- Both contacts have the same configured hold duration.
- Both contacts are released by the end of that one gesture.
- There is no repeat timer, no sequence, and no automatic clicking.

The two positions are configured as X/Y percentages of the full display. The A+B overlay button can be dragged to a convenient location.

## Setup
1. Install and open TouchDuo.
2. Set the X/Y percentage for the centres of the two in-game buttons.
3. Set the simultaneous hold duration and tap Save.
4. Enable TouchDuo in Android Accessibility settings.
5. Return to the game and tap the floating A+B button once.

Use Check two points to verify that Android delivers two pointers to the test view.

## Compatibility notes
- Minimum Android version: 8.0 (API 26).
- Uses the Android Accessibility gesture API (API 24+) and requests gesture dispatch only.
- No network, root, screen-reading, or general overlay permission is used.
- dispatchGesture() cancels touch gestures already in progress. Thus the combination must be started from A+B itself; it cannot add a software pointer on top of a finger that is already physically held down.
- A game must accept Android synthetic multi-pointer input for the combination to affect its controls. Successful APK compilation alone does not prove compatibility with every game/device.
