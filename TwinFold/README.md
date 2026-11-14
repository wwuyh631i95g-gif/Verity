# TwinFold OS 0.5.0 — paired two-phone workspace and split-view prototype

TwinFold OS is an Android app installed on both phones. After pairing the two devices over the same local Wi-Fi network, it provides a shared launcher-style workspace plus an optional companion split-view session for an app running on one phone. The first target is HONOR X8b / Android 14.

## 0.5.0 — split-mode input improvements

When an app icon is tapped, choose Normal launch or On two phones. In the second mode, one phone launches the selected app and captures its display; the partner automatically opens a fullscreen view of the chosen left/right/top/bottom crop.

New in 0.5.0:
- The companion viewer records multiple simultaneous pointer contacts, not just a single finger.
- It groups the finger paths from one complete touch sequence and sends them as one multi-stroke gesture.
- The source phone maps each pointer path into the chosen half of its own display and dispatches the simultaneous strokes through Android's Accessibility gesture API.
- This enables grouped multi-finger taps, two-finger gestures, and button combinations to be represented in one dispatched gesture.
- The stream is tuned for a target of roughly 13 FPS, with crops scaled up to 960 pixels on the long edge and JPEG quality 54. Actual rate depends on Wi-Fi, phone load, thermal state and Android screen-capture behavior.
- The remote viewer stays fullscreen; a user can touch the partner display without having to switch back and forth to the TwinFold interface.
- TwinFold Control is opt-in in Android Accessibility settings. It does not request window-content access or read screen text.

### Important limits of touch forwarding

This is still an APK-level companion view, not a real extended Android display. Android's public AccessibilityService dispatchGesture() API dispatches finite gestures, and the Android documentation states that dispatching a new gesture cancels gestures already in progress, including user gestures. Therefore 0.5.0 sends a completed multi-finger touch sequence after the fingers are lifted. It is not a continuous low-latency stream of live touch-down/move/up events. Held virtual joysticks, simultaneous live local-plus-remote touches, rapid game controls, pressure/analog input, and some games may not behave like a native tablet. The source phone continues to render the original application on its own screen; the second phone receives a cropped image stream.

A true one-canvas tablet mode with live independent multi-touch from both screens needs privileged OS/vendor-level input and multi-display integration. An ordinary Android APK cannot safely or reliably inject arbitrary simultaneous live touchscreen streams into every third-party app.

The normal-launch mode remains available without enabling TwinFold Control. Screen capture requires the Android MediaProjection approval prompt on the source phone. The second phone needs the same TwinFold version and a live paired connection.

## Previous versions and retained features

### 0.2.x — paired workspace foundation
- Pair two phones over the same local Wi-Fi network using the first phone's IP address and six-digit room code.
- Maintain a TCP connection for messages and frame transfer.
- Give each phone a persistent local device ID.
- Show a package/app catalogue exposed by Android, with search and paged app grid.
- Choose physical device arrangement: left-to-right, right-to-left, top-to-bottom or bottom-to-top.
- Show local and partner device panels and allow drag-and-drop of an app tile to the partner.
- Ask the receiving user to approve a move.
- If the app already exists on the receiving phone, move only its TwinFold desktop ownership; do not uninstall the original copy.
- If the app is missing on the receiving phone, offer to send its base APK and split APK files over the local Wi-Fi connection.
- Request permission to install unknown apps when Android requires it, then use Android PackageInstaller and require its confirmation.
- Launch an app on the phone that owns its TwinFold icon.
- Share simple text notes between the two devices.
- Share a cropped screen using Android MediaProjection, a visible foreground-service notification, a selectable crop and a fullscreen receiving view.
- Explicitly disconnect the paired devices.

### 0.3.0 — shared app ownership and transfer
- Exchange installed-app catalogues when phones connect.
- Create a shared ownership map so each app icon is assigned to one phone and is not duplicated across the two TwinFold home spaces.
- Keep the ownership map on both phones for reconnection.
- Synchronize icon ownership after a recipient approves a move.
- Transfer app base/split APKs when the receiving device does not have the app, then move the shared icon after Android reports a successful install.
- Keep the app installed on its original phone when only its shared desktop icon is moved.
- Synchronize app-library page changes.
- Retain the earlier screen crop stream and shared notes.

### 0.4.0 — selectable two-phone split session
- Ask on each app launch whether to open it normally or use the two-phone split session.
- In split mode, ask Android for MediaProjection permission and launch the app on the source phone.
- Tell the partner to open a fullscreen companion viewer.
- Select which half is sent: left, right, top or bottom.
- Hide system bars in the companion viewer and display the incoming crop edge-to-edge.
- Provide an optional TwinFold Control Accessibility service to send gestures back to the source phone.
- End the companion view when the partner exits or the source returns to TwinFold.

### 0.5.0 — multi-touch sequence forwarding
- Track up to ten pointer contacts in one remote touch sequence.
- Preserve each finger's start delay, duration and sampled path.
- Dispatch multiple strokes together through AccessibilityService.
- Raise the capture crop scale and stream refresh target for a clearer, smoother companion view.

## APK and transfer limitations
- Android may hide or protect some installed packages. Not every listed system package has a launchable activity.
- Some apps cannot be transferred because their APKs are unreadable, split sets are incomplete, signatures differ, or the destination device is incompatible.
- APK installation always uses Android's own installer confirmation. TwinFold does not bypass signatures or system restrictions.
- Secure/DRM app surfaces can appear black or blank in MediaProjection captures.
- Network speed and phone performance affect the stream's quality and delay.

## Build configuration
- Application ID: com.twinfold.duolink
- Java 17; Android Gradle Plugin 8.6.1; Gradle 8.7
- Compile SDK 34; target SDK 34; minimum SDK 26
- GitHub Actions builds and checks the APK archive and package metadata, then attempts an Android 14 emulator launch smoke test.
