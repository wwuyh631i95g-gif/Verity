# DroidSwitch 1.0.0

A standalone Android utility project for HONOR X8b owners.

## Purpose
DroidSwitch checks the visible device identity, checks whether the Android DSU system package is present, opens built-in Android/HONOR update settings, and loads a live catalogue of official documentation and possible test-system routes.

## Important technical limit
A PC emulator running inside an ordinary Android app does not gain bootloader or firmware privileges. When the phone reboots into bootloader/fastboot, the Android app is no longer running as the host. Android permissions such as file access or accessibility do not unlock the bootloader or authorize unsigned firmware.

## DSU is the closest safe no-PC path, if the manufacturer enabled it
Android Dynamic System Updates (DSU) can boot a guest Android system image while preserving the current system image for switching back. However:
- DSU is provided/controlled by the device manufacturer.
- Images must satisfy Android's trust/signature checks.
- The standard GSI requirements include a Treble-compliant device and unlocked bootloader.
- The HONOR X8b / exact LLY variant is NOT confirmed to expose DSU Loader or to accept any GSI.

The app only checks for the presence of the Android DSU package and opens Developer options so the owner can see whether "DSU Loader" is actually offered. Package presence alone is not proof of DSU compatibility.

## What this app does not do
- No bootloader unlocking or security bypass.
- No partition writing, fastboot, EDL, recovery flashing, or arbitrary ZIP installation.
- No claims that an image is compatible just because the CPU is Qualcomm.
- No fake progress or simulated "successful flash".

## Permissions
Only INTERNET is declared, for fetching the public JSON catalogue. No storage, camera, accessibility, overlay, root, USB, or device-admin permissions.

## Build
`gradle wrapper --gradle-version 8.7`
`./gradlew --no-daemon assembleDebug`

GitHub Actions builds a debug APK and performs an Android 14 emulator launch smoke test.
