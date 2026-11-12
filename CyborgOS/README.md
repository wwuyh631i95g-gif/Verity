# CYBORG VM 1.3

CYBORG VM is a full-screen, dual-workspace Android app. It presents two side-by-side mobile home screens and a PC-style control dock. Android on the host phone remains the real operating system.

## Visual improvements in 1.3
- Two distinct wallpaper palettes and rounded screen surfaces.
- Left workspace uses an iPhone-inspired date/time widget, squircle-style icon tiles and bottom dock.
- Right workspace uses an Android-inspired glance/search layout and home dock.
- The app grid now uses three columns in each half.
- Where a matching app is installed on the host Android device, CYBORG displays that app's actual launcher icon and can launch the real app.
- Long-press an icon, drag it across the centre divider, and release to move that shortcut between the two halves. This works with touch; keyboard navigation remains available too.
- App Store tile opens the CYBORG local catalogue. It is not Apple's App Store and cannot install iOS apps.
- Play Store tile opens the CYBORG catalogue; the Android store page also has an action to open Google Play if it is installed.

## PC dock
The bottom dock scrolls horizontally and includes mouse pointer, keyboard, trackpad, Ctrl, Alt, Shift, Tab, Esc, Enter, Backspace, Delete, F1-F12, arrow keys, Insert, Home, End, Page Up and Page Down.

Function keys:
- F1 Help; F2 rename selected shortcut; F3 Search; F4 Desktop
- F5 Refresh; F6 switch active half; F7 Keyboard; F8 Trackpad
- F9 installed apps; F10 Android Settings; F11 immersive mode; F12 About

Touch interaction is still enabled. These controls act inside CYBORG VM; they do not inject input into unrelated apps outside it.

## Virtualization boundary
This APK runs as its own full-screen workspace inside Android. It is not a hypervisor or a bootable ISO/QCOW2 image. Android remains the host OS. The left side is an iPhone-inspired interface, not genuine iOS; Apple's App Store and iOS apps cannot run inside this APK. The right side can launch compatible Android apps already installed on the phone.

## Build
- minSdk 26, targetSdk 34, Kotlin.
- No runtime permissions requested.
- GitHub Actions builds a debug APK artifact.
