# TwinFold OS 0.3.0 — shared two-phone desktop prototype

TwinFold OS is an Android app that provides a paired launcher-style workspace for two phones. It is designed for use by the owners of both phones with explicit consent. The first target is HONOR X8b / Android 14; other Android devices may work subject to their firmware and manufacturer restrictions.

## New in 0.3.0: one shared app desktop

- Each phone gets a persistent device ID.
- After pairing on the same Wi-Fi network, both phones exchange their installed-package catalogues.
- When the same package is installed on both phones, the host randomly assigns its single TwinFold desktop icon to one of the two sides. Apps installed on only one phone stay assigned to that phone.
- The owner map is saved on both devices and reused when reconnecting to the same partner.
- Each side displays only the app icons assigned to that side while paired. The app remains installed on the original phone; moving its TwinFold icon does not uninstall it.
- Drag an app tile across to the partner panel. The destination owner approves the move. If the package is already installed there, only the shared desktop ownership changes and the icon disappears from one side and appears on the other.
- If it is not installed on the receiving phone, TwinFold can ask the source to transfer its base APK and split APK files. The receiver accepts the transfer, enables installation from TwinFold if Android requests it, and confirms the installation in Android's own installer. Only after successful installation is the icon moved.
- Tapping an icon launches the app on the phone that owns that desktop icon. The launch is started from TwinFold's activity task so Back is intended to return to the TwinFold desktop, rather than require the user to find the app again.
- Google Play and other system apps launch locally on the phone where their icon is assigned, where the package has a launchable activity.

## Retained from 0.2

- Pairing over a local Wi-Fi network with IP address and a six-digit code.
- Search and paged library of packages returned by Android's installed-application query, including system packages where Android exposes them.
- Layout selector for left/right and top/bottom physical phone arrangement.
- Screen capture with the Android MediaProjection consent screen, visible foreground-service notification, and selectable left/right/top/bottom crop.
- Fullscreen viewer for the half-screen stream received from the paired phone.
- Shared text notes and explicit disconnect.

## Important platform boundary: this is not a true extended Android display

A normal third-party Android app cannot merge two independent phones into one system-wide display or make an arbitrary third-party app such as YouTube or a game render one continuous native tablet canvas across both physical screens. TwinFold 0.3 does not claim to do that.

The screen-share mode remains a consent-based cropped live copy: the source phone keeps showing its app, while the partner receives the selected half of the captured screen. The receiving phone can show that crop fullscreen. This is useful as a companion pane, but it is not the same as the app drawing its left half on one phone and its right half on the other, and touch input is not forwarded into arbitrary apps.

A genuinely extended canvas for arbitrary apps would require operating-system/vendor support or a privileged system-level implementation on both devices. TwinFold cannot obtain that capability through an ordinary APK or accessibility permission.

## Package transfer limits

APK transfer is real file transfer through the local Wi-Fi connection and Android PackageInstaller, not a copied shortcut. Some protected system packages cannot be read by a normal app. Some installations require device-specific APK splits, matching signatures, architecture compatibility or manufacturer permissions. TwinFold never bypasses Android's signature or installation checks. Apps which have no launchable activity can be listed but will not open from a normal home-screen icon.

## Build

- Java 17
- Android Gradle Plugin 8.6.1
- Compile SDK 34, target SDK 34, minimum SDK 26
- Gradle 8.7 wrapper
- GitHub Actions verifies APK metadata and archive structure, then performs an Android 14 emulator launch smoke test.

Application ID: com.twinfold.duolink
