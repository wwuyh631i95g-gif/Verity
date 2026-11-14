# TwinFold 0.2.0 — Android 14 dual-device prototype

TwinFold is an app-level companion workspace for two Android phones. Pair devices only with the knowledge and consent of both owners. The first target is HONOR X8b / Android 14; other Android 8.0+ devices may work, subject to OEM limits.

## Included in 0.2.0

- Local Wi-Fi pairing with a six-digit room code.
- App library built from installed application packages, with search, icons and multiple pages. The manifest requests QUERY_ALL_PACKAGES so sideloaded builds can enumerate packages; some applications still have no launcher activity.
- Local app launch: tapping a tile launches the app on the phone whose TwinFold screen was tapped. Android system apps such as Google Play open on that same phone.
- Synchronized app-library page changes between paired devices.
- Drag an app tile to the other-device panel to request transfer of its APK package.
- APK transfer includes base APK and split APK files. The receiving owner must accept the transfer; TwinFold then uses Android PackageInstaller. Android shows the install confirmation and the receiving owner must approve it. The receiving phone may ask the user to allow TwinFold as an install source.
- Live screen capture through Android MediaProjection. The user starts the capture and approves Android's screen-sharing prompt. TwinFold runs a visible foreground service and sends one selected crop (left, right, top or bottom half) to the paired phone. Tap the preview to open a full-screen viewer.
- Layout selector for left/right or top/bottom device placement.
- Shared text notes and an explicit disconnect control.

## Important platform boundaries

This is not a merged Android operating system and it does not create a true extended Android display. In this version, the source phone continues showing its app normally, while the partner phone receives a live cropped view of the selected half. Android does not offer ordinary apps a way to force arbitrary third-party apps and games to render across two independent physical displays as one native tablet canvas. This build does not inject touches from one phone into arbitrary apps on the other.

APK transfer is a real file transfer and install flow, not a shortcut icon. Some packages cannot be exported by an ordinary app, some OEM/system applications are protected, and some APKs depend on device-specific splits, architecture or signatures. An APK may be rejected by Android if it is incompatible or its signing identity does not match an installed update. TwinFold never bypasses Android's package installer or signature checks.

Screen capture is visible and consent-based. Protected/secure apps may appear black or refuse capture. The stream is transmitted over the local Wi-Fi connection without an internet relay; use a trusted private network. Both devices must stay paired while transferring APKs or frames.

## Build

- Java 17
- Android Gradle Plugin 8.6.1
- Compile SDK 34, target SDK 34, minimum SDK 26
- Create Gradle wrapper with 8.7, then run clean assembleDebug.
- GitHub Actions builds the APK and verifies its package metadata and ZIP structure.

Application ID: com.twinfold.duolink
