# iOS Game Hub for Android

This launcher is built around the open-source touchHLE emulator. It imports legally obtained .ipa files, stores them in the emulator's app directory, starts a selected game directly, and can create a separate Android home-screen shortcut for each imported game.

## Important compatibility limit

This is not a universal iPhone emulator. touchHLE currently targets early 32-bit iPhone/iPad apps from iPhone OS 2.x, iPhone OS 3.x and iOS 4.0.x. The app binary must be decrypted, and only a subset of games in that era work. Modern iOS apps and 64-bit iOS games are not supported. Check the touchHLE compatibility database before expecting a game to launch: https://appdb.touchhle.org/

The build is ARM64 (AArch64) and is intended for modern Android phones such as the Honor X8b. The launcher does not request broad storage access: it imports a selected file using Android's system document picker.

## Use

1. Install the APK.
2. Open iOS Game Hub and choose "Добавить IPA".
3. Select a legally obtained .ipa file.
4. Press "ИГРАТЬ" next to the imported game.
5. Use "На главный экран" to pin a shortcut for that game.

The imported files are stored in the app's private external-files directory under touchHLE_apps.

## Engine and license

The emulator engine is touchHLE v0.3.0, an open-source project licensed under MPL-2.0. This launcher patches its Android argument handling to launch a selected IPA directly. Original project: https://github.com/touchHLE/touchHLE

Only use game files that you are legally allowed to use. This project is not affiliated with or endorsed by Apple.
