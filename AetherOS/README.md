# Aether OS — first Android build

Aether OS is a standalone Android application that presents a personal, customizable system shell inside an ordinary APK. It does not replace Android or virtualize a second Android kernel.

Target: Honor X8b / Android 14 (API 34)
Minimum Android: 8.0 (API 26)
App ID: com.aetheros.shell
Required permission: INTERNET (for the built-in browser)
No root, ADB, Shizuku, overlay, contacts, camera, microphone, or broad storage permission.

Working in this prototype: Home dashboard, in-app browser, Google Search entry point, local notes, private app files, web-app shortcuts, simple app studio, settings, profile name, quick panel shortcuts to Android Settings, app history, photo picker, external camera hand-off, calculator, and JSON backup export through Android's document picker.

Boundaries: This is an app-level environment, not a second Android OS. Android apps installed on the host do not automatically become apps inside Aether OS. The Store in this first version saves and opens web-app shortcuts; it does not install arbitrary APKs into a separate Android runtime. Google Play Services and Google Play Store are not bundled or emulated. The browser opens Google websites, subject to Google's WebView/login policies.

No permission is requested for photos or camera by this app: the user hands off to Android's system picker/camera app.
