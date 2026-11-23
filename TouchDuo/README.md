# TouchDuo

Android 8.0+ helper for single-touch devices. The app creates a synthetic gesture containing 2–4 concurrent touch strokes and sends it when the small floating **DUO** button is tapped.

## Setup
1. Install the APK and open TouchDuo.
2. Open Android Accessibility settings and enable TouchDuo.
3. Configure the X/Y percentages for the game controls, then save.
4. Return to the game and tap the floating button. Drag the button to move it.
5. Use the built-in test screen to check whether the system delivers multiple pointers.

No Internet, root, or screen-reading permission is requested. The accessibility service does not inspect app content or accessibility nodes.

## Important limitation
This does not change the touchscreen digitizer or turn a single-touch controller into a real multi-touch panel. Android's accessibility gesture API can send multiple strokes as one synthetic gesture, but dispatching it cancels gestures already in progress. Consequently, TouchDuo is for preset simultaneous tap/hold combinations; it cannot add a virtual second finger while a physical finger is already held down.
