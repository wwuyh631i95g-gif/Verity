# Aether OS Fusion 3.0.0 — capability matrix

Aether OS 3.0 is a much larger Android-hosted mobile environment. The APK is designed for Android 14 (API 34) and also supports Android 8.0+ (API 26+).

## Aether features implemented in the app

### Home and launcher
- Aether home dashboard with date, time, profile and battery summary.
- Launcher entry point registered as both a normal app and an Android HOME app. Android may offer Aether in its default Home-app picker; the user chooses whether to use it.
- Searchable app drawer for built-in Aether modules and installed Android apps.
- Local favorites that appear on the Aether home screen and are included in backups.
- One-tap launch of apps already installed on the phone.
- Aether module hub and search command hub.
- Recent activity timeline for actions carried out inside Aether.
- In-app workspace cards/bubbles and a two-pane browser workspace.
- Saved Aether mini-app shortcuts and web-app entries.

### Built-in apps and personal data
- Web browser with address/search field, JavaScript and DOM storage enabled, Safe Browsing where Android provides it.
- Aether Store catalogue of web services and user-created mini-apps. It is not a native APK store.
- Aether Studio: create a website shortcut or edit/save a small HTML mini-app.
- Offline launch of saved HTML mini-apps.
- Local notes and private app-sandbox files.
- Encrypted Vault entries using AES-GCM and a device-local Android Keystore key.
- Planner with locally saved tasks/events and completion toggles.
- Manual routines for common Aether actions.
- Clock, stopwatch and countdown timer.
- Built-in offline calculator supporting parentheses, decimals, +, −, ×, ÷ and powers.
- Calendar event creation hand-off, launch of calendar apps and alarm hand-off.
- Contacts, dialer, SMS, email and share-sheet hand-offs to installed phone apps.
- Camera launch and Android document/photo picker integration.

### Personalization and accessibility
- Dark, light and graphite appearances.
- Aurora, ocean, sunrise and graphite wallpapers.
- App-wide text scale controls (90%, 100%, 110%, 120%).
- Reduced-motion preference for Aether interface.
- Profile display name.
- Aether lock-screen-style welcome screen.
- Theme Studio and dashboard shortcut cards.

### Device center and control shortcuts
- Live device manufacturer/model, Android release/API and build display.
- Battery percentage, available/total RAM, private-storage volume and screen resolution/density.
- Quick panel shortcuts for Wi-Fi, Bluetooth, network, wireless settings, NFC, DND settings, sound, display, battery, privacy and system settings.
- System Settings hub for notifications, default apps, language, keyboard, text-to-speech, accessibility, security, date/time, storage, developer options and printing.
- App manager to launch apps, open their Android app-info page, and request uninstall through Android.
- Security and privacy center with permission and Android privacy-dashboard links.
- Local focus sessions and Aether-only digital-wellbeing/activity summaries.
- Export/import of Aether JSON backups, including launcher favorites, theme, text scale, routines, notes and mini-app definitions.

## Android system functions intentionally handed to Android

Aether uses Android's supported intents to open the phone's own settings, calendar, alarm, contacts, dialer, messaging, email, photo picker, camera and app-management screens. Some menu items are device/OEM dependent; Android may show a chooser, open a settings page, or report that a target is unavailable.

Wi-Fi, Bluetooth, cellular, NFC, DND, airplane mode, brightness, volume, notification permissions, accessibility, default apps and uninstall confirmation remain controlled by Android. An ordinary third-party launcher cannot silently toggle privileged system features.

## Important platform boundary

Aether OS is an Android application and optional Home launcher, not a replacement kernel or independent bootable operating system. It does not replace Android on the Honor phone, boot a separate Android guest kernel, run iOS apps, contain Apple's iOS or proprietary Google Play Services, or install arbitrary APKs into a virtual guest system. Installed phone apps are launched by Android as normal host apps. The Aether Store is a curated web/mini-app catalogue, not Google Play.

This release makes Aether behave more like a complete personal mobile shell while retaining Android 14 compatibility and avoiding root, custom recovery, overlay permission, accessibility-service privileges, broad file access, contacts permissions, call/SMS permissions, microphone, camera or location permissions.

## Permissions

The manifest requests only INTERNET. Files and photos are accessed through system document pickers, and camera/communication/calendar features are handed off to the corresponding Android apps. The app does not request package-wide visibility; it queries launcher activities through the declared launcher intent query.

## Build and target device

- Android compile/target SDK: 34.
- Minimum SDK: 26.
- Java/Kotlin target: 17.
- Package: `com.aetheros.shell`.
- A successful GitHub Actions build confirms compilation and artifact generation. It is not a physical-device test on the Honor X8b.
