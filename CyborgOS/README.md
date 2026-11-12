# CYBORG VM 1.2

CYBORG VM is a full-screen, self-contained virtual-device-style Android application. When launched, it opens its own two-panel mobile workspace with persistent app shortcuts, shared notes, a small module catalogue, and a PC-style touch/keyboard control dock.

## Two mobile workspaces

- Left workspace: iPhone-inspired home screen layout, island-style top element, app grid and dock.
- Right workspace: Android-inspired launcher, search bar, app grid and dock.
- Both halves use touch interaction. Tap app icons to open them; long-press and drag an icon across the divider to move it between workspaces.
- Real Android apps installed on the host phone can be pinned and launched from either workspace. The custom catalogue and app creator store local shortcuts and modules.

The visual design follows familiar mobile interface patterns, but the two workspaces are not exact copies of Apple's or Google's operating systems.

## PC controls

The bottom dock scrolls horizontally and includes a mouse pointer, keyboard, trackpad, Ctrl, Alt, Shift, Tab, Esc, Enter, Backspace, Delete, F1-F12, arrow keys, Insert, Home, End, Page Up and Page Down.

Function keys have local actions inside CYBORG VM:
- F1 Help
- F2 Rename selected app tile
- F3 Search
- F4 Desktop workspace
- F5 Refresh
- F6 Switch active workspace
- F7 Keyboard
- F8 Trackpad
- F9 Installed Android apps
- F10 Android Settings
- F11 Immersive full-screen toggle
- F12 About

Touch controls remain enabled. The pointer and keyboard controls operate inside CYBORG VM; they do not inject input globally into unrelated Android apps.

## Important virtualization boundary

CYBORG VM is delivered as an APK that runs as a separate full-screen environment inside the phone's Android system. It is a virtual-device-style app shell, not a hardware hypervisor or a bootable Android/iOS disk image. Android remains the host operating system and provides actual hardware access.

The left workspace is an iPhone-inspired interface, not genuine iOS. Apple does not provide a redistributable iOS guest image for Android, and this APK cannot run iOS apps or the real Apple App Store. The right workspace launches actual Android apps already installed on the host device.

## Compatibility and build

- Android API 26 or newer; build target Android 14/API 34.
- Kotlin Android app; no external runtime libraries.
- No runtime permissions requested by the application.
- GitHub Actions creates a debug APK artifact.

This APK can be installed on an Android phone or inside an Android-based VM that supports APK installation. It is not an ISO, QCOW2, or raw virtual-disk image.
