# CYBORG OS 1.0

**One device. Three worlds.**

CYBORG OS is a standalone Android app for Android 14+ phones. Its main screen places two distinct interface spaces side by side.

- **iPhone-inspired half:** Dynamic Island-style header, iOS-like app tiles and an App Store-style Cyborg catalogue.
- **Android half:** Material-inspired panel, Play Store-style catalogue and access to installed Android apps.
- **PC layer:** A persistent horizontal control strip with a pointer indicator, keyboard bridge, Ctrl/Alt/Tab/Esc shortcuts, trackpad and desktop workspace.

## Cross-system behaviour

Both halves use one shared app shelf. Long-press an app tile and drag it across the centre divider to move it to the other half. Apps pinned from the phone's installed-app list launch the real Android app on the host phone, regardless of which half contains its tile. Notes and custom shortcuts are shared between the halves.

The catalog adds Cyborg modules and shortcuts to this app shell. The custom app creator saves an app name, icon and optional web address.

## Technical boundary

This is an original Android-hosted hybrid shell, not a bootable iOS image and not an iOS virtual machine. Apple does not provide a redistributable iOS guest image for an Android phone, and iOS apps cannot be executed by this APK. The iPhone half recreates interface patterns; the Android half can open actual Android apps installed on the host. Android remains in control of hardware, security and protected settings.

## Build

The project uses Kotlin and the Android SDK without external runtime libraries. It targets API 34 and supports API 26+. It requests no runtime permissions. GitHub Actions builds a debug APK and runs an Android 14 launch smoke test.

Workflow: Build CYBORG OS APK in the Actions tab.
