# TwinFold OS 0.4.0 — two-phone split-session prototype

TwinFold OS is an Android app that pairs two phones into a shared launcher/workspace. The first target device is HONOR X8b / Android 14. Install the same APK on both phones and pair them over the same local Wi-Fi network.

## New in 0.4.0: choose normal launch or two-phone split session

When you tap an app icon in TwinFold, you can choose:
- **Normal launch** — open the app locally on the phone that owns that icon.
- **On two phones** — launch the app on the owning phone, start a consent-based screen capture, and automatically open a fullscreen companion view on the partner phone. The partner receives the selected half of the source display. If the owner enables TwinFold Control in Android Accessibility settings, taps and one-finger swipes on the partner view are sent back to the running app on the source phone.

The split side is selectable (left, right, top, or bottom) using the existing remote-side setting. The partner view hides system bars and stretches the selected crop to fill its display. The session stops when the user returns to TwinFold on the source phone, or when the partner exits the viewer.

## Important: what this split mode does and does not do

This is the closest practical split-session that an ordinary Android APK can provide without system privileges:
- the source phone still runs and displays the original app in the foreground;
- the second phone receives a live cropped copy of the selected half;
- partner touch gestures are mapped back into the corresponding half of the source phone's display through the user-enabled accessibility service;
- pairing, app-library page changes, ownership and notes remain synchronized through TwinFold.

It is **not** a true extended Android display or a single wider tablet canvas. Android continues to render the third-party app on the source phone's own display. The app is not being asked to render a native 2-screen-wide layout, and the source phone is not physically clipped to show only its half. This means some games may not feel seamless: aspect ratio, frame rate, latency, secure windows, multi-touch controls, DRM video, and apps that pause when they lose focus can limit the experience. The remote gesture bridge supports one-finger tap/swipe paths; it is not a full multi-touch or game-controller emulator.

A genuine native app spanning two phone displays would require OS/vendor-level multi-display support or privileged system integration on both phones. An ordinary APK cannot turn two independent Android displays into one system-wide display.

## TwinFold Control permission

For interactive split sessions, enable **TwinFold Control** in Android Settings → Accessibility on the source phone. It declares gesture-injection capability but does not request window-content access or read screen text. Android requires the user to enable this service manually. If it is not enabled, the split session still works as a view-only companion stream.

The normal launch option does not require this accessibility service.

## Existing features retained

- Local Wi-Fi pairing with IP and six-digit room code.
- Shared launcher catalogue and ownership map so each app icon appears on one side.
- Move app icons between sides, with recipient approval.
- Optional APK and split-APK transfer using Android PackageInstaller confirmation.
- Synchronized app-library page changes.
- Left/right and top/bottom physical layout selector.
- Legacy screen crop stream and fullscreen receiver.
- Shared text notes and explicit disconnect.

## Package transfer limits

Some protected system packages cannot be read by a normal app. Some installations require device-specific APK splits, matching signatures, architecture compatibility or manufacturer permissions. TwinFold never bypasses Android signature or installation checks.

## Build

- Java 17
- Android Gradle Plugin 8.6.1
- Compile SDK 34, target SDK 34, minimum SDK 26
- Gradle 8.7
- GitHub Actions validates the APK and performs an Android 14 emulator launch smoke test.

Application ID: `com.twinfold.duolink`
