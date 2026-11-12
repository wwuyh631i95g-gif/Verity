# CYBORG VM 1.6

CYBORG VM is an Android-hosted dual virtual workspace. The left workspace has iPhone-inspired interaction patterns; the right workspace has Android/Material-inspired panels. Android remains the host operating system. This is not a bootable iOS or Android guest OS.

## 1.6 features
- iPhone Control Center: virtual Focus and silent modes, Live Island activity demo, lock-screen preview and a saved virtual brightness slider.
- Android Quick Settings: separate simulated Wi-Fi, Bluetooth, Airplane, Battery Saver and Do Not Disturb tiles; Material You accent palettes; notification preview and app-info shortcut.
- The two workspaces have distinct visual systems: round iPhone-style app badges, translucent dock and Control Center; squarer Android app badges, Material-style quick tiles and theme colours.
- The PC dock is retained: on-screen keyboard dialog, mouse pointer, trackpad, CTRL/ALT/SHIFT, function keys, arrows and desktop shortcut remain.
- CYBORG packages (.capp) install as internal web-app shortcuts. Their games open in a CYBORG full-screen WebView, not an external browser. The WebView allows HTTP/HTTPS only, disables local-file access and blocks mixed content.
- The file-backed app shelf and workspace selections are stored in app preferences.

## iOS apps and games
- A native iOS .ipa is not an Android APK and cannot generally be installed or executed by this app.
- CYBORG can keep an IPA in its archive and can run compatible web games through .capp packages.
- There are experimental Android compatibility projects for narrow subsets of iOS games; one current research project targets ARM64 iOS applications built with Unreal Engine 3. That is not a general iOS runtime, and it is not bundled into CYBORG. Compatibility depends on the exact game and device.
- CYBORG does not include Apple's App Store or distribute copyrighted games.

## Security Lab / Quarantine
- The Virus Lab copies a selected file into CYBORG's private internal app storage, with a 128 MB intake limit.
- It calculates a SHA-256 fingerprint and records the filename, extension and size. It never installs, opens or executes the quarantined file.
- You can inspect the fingerprint, delete one copy or empty the quarantine. Deletion affects only CYBORG's private copy, not the original file.
- A harmless visual threat simulation demonstrates blocked actions. It does not run real malware.
- This is not an antivirus engine, not a full Android virtual machine and not an airtight malware-analysis sandbox. A hash is not a clean-file verdict. Do not install suspicious APKs on the host phone.
- The separate APK Installer sends a package to Android's real system installer and is not isolated by the Virus Lab.

## Android APK installation
- The Android app centre can open Google Play when present on the host.
- APK installation uses Android's own package installer and user confirmation. Android may require the user to allow installs from this app. Installed APKs become apps on the real phone, not apps inside a separate guest Android.

## PC dock and touch
- The PC dock and keyboard controls remain present at the bottom of the screen. These controls are part of CYBORG and do not inject input into unrelated apps.

## Build
- minSdk 26, targetSdk 34, Kotlin/JVM 17.
- INTERNET is used for CYBORG WebView mini-apps. No new runtime permission prompts were added for the security lab.
- GitHub Actions builds a debug APK artifact named CYBORG-VM-1.6.0-Android-APK.
