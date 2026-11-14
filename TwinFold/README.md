# TwinFold 0.1.0 — Android 14 prototype

TwinFold is an app-level companion workspace for two Android phones. It is designed for a pair of devices owned by, or used with the explicit consent of, both people.

## Included in this first build
- Dark split-device dashboard with a local phone panel and a partner phone panel.
- Pairing over the same local Wi-Fi network using an IP address, TCP port 38471 and a six-digit room code.
- A shelf of launchable apps detected on the current phone.
- Tap an app to open it locally.
- Long-press an app tile and drag it onto the partner panel to send a launch request to the other phone.
- The receiving phone must enable remote requests and then approve every launch in a visible confirmation dialog.
- Shared text note sending between paired devices.
- Disconnect control and connection state indicator.

## Important limits — this is not yet a merged Android operating system
Android does not let an ordinary app combine two separate phone displays into one system-wide display, relocate an installed app into another phone's Android installation, or silently install apps on the other device. TwinFold therefore creates a shared workspace inside TwinFold, not a single OS desktop spanning both phones.

This prototype sends an app package name as a launch request. It does not copy the app APK, install it on the partner phone, or mirror the running app's screen. The destination app must already be installed there. Both phones must be on the same trusted Wi-Fi network; internet relay and cross-network pairing are not part of this build.

No hidden screen capture or stealth remote control is implemented. Any future screen-sharing mode must be started on the source device with Android's own capture confirmation and a persistent, visible indicator. App installation must likewise require the receiving device owner's approval.

## Build
- Java 17
- Android Gradle Plugin 8.6.1
- Compile / target SDK 34
- Minimum SDK 26
- Create wrapper with Gradle 8.7, then run clean assembleDebug.

Package: com.twinfold.duolink
