# Aether OS 2.0.0 — feature matrix

## Implemented in the APK
- Aether home dashboard, adaptive time/date and battery card.
- Liquid-glass-inspired translucent cards, dark/light/graphite appearances, aurora/ocean/sunrise/graphite wallpapers.
- In-app command assistant that can open modules, search the web, switch appearance and show local battery information. This is a local command engine, not a generative AI model.
- Integrated WebView browser with Google Search entry point and a curated Store of web services.
- Aether Studio for creating and saving small HTML mini-apps (they can run offline) or website shortcuts.
- App Library that lists host Android apps and launches them without copying them into another OS.
- In-app workspace bubbles and a two-pane browser view. These stay inside Aether; they are not system-wide overlays.
- Private notes and files stored in app-private storage.
- Encrypted Vault entries using AES-GCM and an Android Keystore key.
- Local planner, completion toggles, manual one-tap routines, stopwatch and countdown.
- Local activity timeline, internal dashboard cards, profile display name.
- Android system-settings hand-offs for network, Bluetooth, display, battery and privacy.
- Android document-picker backup export/import, photo picker and camera hand-off.
- Privacy screen; only INTERNET is requested in the manifest.

## Not a full second OS
Aether OS is an application-level shell hosted by Android. It does not boot an Android guest kernel, replace the Honor launcher, or provide an independent Android package manager. Android apps opened from App Library still run in the host Android. Aether Store currently contains web-app links and Aether mini-apps, not arbitrary native APKs.

It does not contain Apple's iOS, Google Play Store, or Google Play Services. Those are proprietary platform components and are not distributed inside this project. Some Google websites can be opened in the browser, but that is not the same as embedding Google services.

## Bundling / offline behavior
All Aether application code, interface resources and built-in catalogue data are packaged into the APK. First launch does not download a guest operating system. Notes, Vault, local files, planner, routines, settings and saved HTML mini-apps work offline. The browser and online web apps need internet access. The APK is intentionally small rather than carrying a multi-gigabyte Android system image that this app cannot boot as a second OS on the Honor device.

## Current platform research incorporated as design references
- Android 17: adaptive-first UI, app bubbles, interactive multi-window concepts, stronger privacy and app-function/assistant direction.
- iOS 27.2/27.2.1: refined glass styling, more contextual assistant patterns, suggestions and privacy controls.
These are inspiration and selected app-level analogues, not claims of implementing the native operating-system features.

## Device target
Built against Android API 34 for Android 14 compatibility; minimum SDK 26. The GitHub build workflow compiles the APK and attempts an Android 14 emulator launch smoke test. Emulator availability can vary by runner configuration.
