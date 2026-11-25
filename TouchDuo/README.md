# TouchDuo 1.2.1 — Physical Multitouch Check

This version removes the old accessibility automation code completely. It does not generate, repeat, or inject taps and requests no special permissions.

## Purpose
Touch the test screen with two physical fingers at the same time. The app shows the number of pointers Android actually reports to the activity and the maximum count seen during the test.

## How to test
1. Open the app and select the physical multitouch test.
2. Keep one finger on the screen.
3. While holding the first finger, place a second finger elsewhere.
4. Read the current count and the maximum count.

If the maximum reaches 2, the watch's touchscreen and Android input stack delivered two simultaneous physical pointers to the test app. You can then try two physical touches directly in a game. The game itself must support multi-pointer input.

If the maximum stays at 1 after repeating the test carefully, a normal third-party app cannot enable hardware multitouch on a single-touch digitizer. The app has no root, hidden system, or privileged input access.

## Compatibility
- Android 8.0+ (minSdk 26), including Android 8.1.
- No permissions are requested.
- No accessibility service, overlay, network access, or automatic input injection.
