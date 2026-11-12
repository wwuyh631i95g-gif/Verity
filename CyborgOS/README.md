# CYBORG VM 1.5

CYBORG VM is a full-screen dual-workspace Android app. It gives you an iPhone-inspired workspace on the left and an Android-inspired workspace on the right. Android on the phone remains the real host operating system.

## 1.5 visual and custom features
- iPhone-side app badges are now circular; Android-side app badges are square with tighter corners, so the two workspaces are easier to tell apart.
- The PC dock, keyboard button, modifier keys, trackpad and function-key row are retained unchanged.
- Import a `.capp` CYBORG package from either app centre. A `.capp` file is UTF-8 JSON with `name`, `icon`, `side` (`ios` or `android`) and an `https://` or `http://` `url`. Installing it adds a CYBORG shortcut that opens the web app/game in the host browser; it does not install a native iOS game.
- New original vector wallpapers for both halves: fluid layered blue/pink curves on the iPhone side and teal/blue Material-style colour fields on the Android side.
- Rounded icon tiles, separate docks, date/time widgets, home-screen app grids and drag-and-drop shortcut transfer between halves.
- The visual references are inspired by public iOS 18 and Android/Material wallpaper styles; the included wallpapers are drawn by CYBORG and are not extracted Apple or Google system files.
- Create CYBORG mini-app shortcuts with a custom name, icon and optional web address.
- CYBORG catalogue modules can be added independently to either workspace.

## Android side: APK and Google Play
- The Android app centre can open Google Play when it is installed on the host phone.
- Install APK from files opens Android's document picker for a single .apk file.
- Android 8 and later require the user to allow this app to request installs from unknown sources. Android then displays its own installation confirmation. CYBORG cannot silently install an app or bypass Android's warning.
- The installed app is a real Android app on the phone and can be pinned to either CYBORG workspace from the installed-app list.

## iPhone side: App Store-style catalogue and IPA archive
- The left-side App Store is a CYBORG mini-app catalogue with iPhone-style presentation. It is not Apple's actual App Store.
- .ipa files can be imported into the IPA archive list for keeping track of files.
- An IPA is an iOS application package. It cannot be installed or executed by this Android APK: the iOS runtime, Apple signing environment and iOS platform are not present.
- CYBORG mini-apps and web shortcuts can be created and opened in either workspace, but they are not native iOS applications.

## PC dock and touch
The scrollable PC dock is unchanged in this version. Touch, long-press and cross-half drag-and-drop remain enabled. Keyboard and pointer controls act inside CYBORG; they do not inject input into unrelated apps outside it.

## Virtualization boundary
This is an Android-hosted dual workspace, not a hypervisor or a bootable iOS/Android virtual machine. The right side can launch compatible apps installed on the Android host. The left side reproduces selected iPhone-style interface patterns but cannot boot iOS, access Apple's App Store, or run .ipa binaries.

## Build
- minSdk 26, targetSdk 34, Kotlin/JVM 17.
- APK sideloading uses Android's user-confirmed package installer flow and REQUEST_INSTALL_PACKAGES.
- GitHub Actions builds a debug APK artifact.