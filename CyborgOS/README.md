# CYBORG OS 1.7

CYBORG OS is an Android-hosted virtual dual-workspace app. The left workspace has iPhone-inspired controls and the right workspace has Android/Material-inspired controls. The Android 14 phone remains the host system; this is not a bootable iOS or Android guest OS.

## What's new in 1.7
- Russian-language home screens, store, settings, calculator, game hub, security lab and PC dock.
- CYBORG boot-style startup screen and a restart option inside the virtual environment. It does not replace the actual Android bootloader.
- A built-in CYBORG Market / App Store catalogue. Its built-in modules install as app tiles inside CYBORG without opening the phone's Play Store app.
- Google Search opens in an in-app WebView. YouTube opens the mobile YouTube website in the same in-app browser window. They do not launch Google's or YouTube's host Android apps. Website sign-in, video playback, or some features can be limited by the services' WebView support.
- A working internal calculator with +, −, ×, ÷, %, parentheses, sign change and backspace.
- iPhone workspace: Dynamic Island demo, Focus and silent-mode toggles, Live Photos and StandBy demos, Spotlight, app switcher, lock-screen preview, Face ID and AirDrop demonstrations, and a virtual cloud-status preview.
- Android workspace: Material You colour themes, virtual Quick Settings tiles, notification preview and a restartable CYBORG environment.
- App icons change shape and colour treatment when dragged from one half to the other. User-created .capp app shortcuts open inside CYBORG's WebView.
- The bottom PC dock remains available: keyboard dialog, mouse pointer, trackpad, CTRL/ALT/SHIFT, function keys and navigation keys.

## App store and iOS game limits
- CYBORG Market is an internal catalogue of CYBORG modules and web-app shortcuts. It is not Google's licensed Play Store and cannot download arbitrary Play Store APKs into an isolated Android guest system.
- Google and YouTube are rendered as websites inside CYBORG WebView, not native sandboxed Android guest apps.
- A native iOS .ipa is not an Android APK. CYBORG can archive IPA files but cannot generally install or execute native iPhone games.
- Compatible web games can be installed as CYBORG .capp shortcuts. A .capp file is JSON containing name, icon, side (ios or android) and an http:// or https:// url.

## Security Lab / Quarantine
- A selected file is copied into CYBORG's private app storage (maximum 128 MB) and assigned a SHA-256 fingerprint.
- The file is not opened, installed or executed. Users can delete one stored copy or clear the quarantine. Deleting the CYBORG copy does not change the original file.
- A harmless visual demo shows simulated blocked actions. This is not an antivirus engine and not a complete malware-analysis VM. A hash is not proof a file is safe.
- The separate APK installer calls the real Android package installer. APKs installed this way are installed on the actual phone, outside the quarantine.

## Technical
- Android minSdk 26, targetSdk 34, Kotlin/JVM 17.
- INTERNET is used only for the CYBORG in-app WebView.
- INTERNET does not create an extra runtime permission prompt. APK sideloading still requires Android's own consent / install-from-this-source flow.
- GitHub Actions artifact: CYBORG-OS-1.7.0-Android-APK.
