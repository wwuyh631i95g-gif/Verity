# CYBORG OS 1.1

**Two workspaces and a PC-style control strip in one Android app.**

CYBORG OS is an installable Android APK. It provides two side-by-side workspaces: an iPhone-inspired interface on the left and the phone's real Android app environment on the right.

## Controls

The bottom dock scrolls horizontally and now has two rows.

- **F1:** Help and keyboard map.
- **F2:** Rename the last selected app tile.
- **F3:** Open MyKit Search.
- **F4:** Open the PC desktop workspace.
- **F5:** Refresh the CYBORG screen.
- **F6:** Switch the active workspace.
- **F7:** Open the on-screen keyboard.
- **F8:** Show or hide the trackpad.
- **F9:** List apps installed on the Android host.
- **F10:** Open Android Settings.
- **F11:** Toggle immersive full screen.
- **F12:** Open About.

The second row also includes arrow keys, Insert, Home, End, Page Up and Page Down. Arrow keys move focus through controls; Tab switches workspaces. CTRL, ALT and SHIFT are one-shot modifiers for the next key sent to a focused control. Physical keyboard function keys are also handled while CYBORG OS is open.

The mouse pointer and trackpad are an in-app control aid. Touchscreen input remains available. These controls do not inject input into unrelated apps or take over Android globally.

## App and workspace behaviour

Both halves share one app shelf. Long-press an app tile and drag it across the divider to move the tile to the other workspace. Apps pinned from the installed-app list launch the real Android apps on the host device. Notes and custom shortcuts are shared. The catalogue adds local CYBORG modules and shortcuts; it is not Apple's App Store or Google Play.

## Important system boundary

This APK is an Android application, **not a bootable operating-system image** and not a true iOS or Android guest OS. The right side opens actual Android apps installed on the Android host. The left side is an iPhone-inspired workspace; it cannot boot Apple's proprietary iOS or run iOS apps.

You can install this APK on an Android phone or inside an Android-compatible virtual machine that supports APK installation and Android API 26 or newer. It is not an ISO/QCOW2 disk image for a PC-style virtual machine. A normal APK cannot contain a redistributable iOS system image.

## Build and compatibility

Kotlin app, target SDK 34, minimum SDK 26. No runtime permissions are requested. GitHub Actions builds the debug APK and launches it in an Android 14 emulator as a smoke test.

Build workflow: **Build CYBORG OS APK** in the repository's Actions tab.
