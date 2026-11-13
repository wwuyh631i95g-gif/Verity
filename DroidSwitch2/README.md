# DroidSwitch 2.0.0

A phone-only Android companion for checking whether the device exposes the official Dynamic System Updates (DSU) loader and opening that system UI.

## What it does
- Reads non-sensitive Android build properties (model, device, Android/API level, ABI, build display and security patch).
- Checks whether the system package `com.android.dynsystem` is installed.
- Attempts to open the Android Settings DSU Loader action: `android.settings.development.START_DSU_LOADER`.
- Falls back to Android Developer Options if the action is unavailable.
- Links to Google's official DSU and GSI documentation.

## What it does not do
- It does not claim to permanently replace MagicOS.
- It does not call the hidden `DynamicSystemClient` API or request privileged `INSTALL_DYNAMIC_SYSTEM`.
- It does not unlock the bootloader, write partitions, use root/ADB/USB, or bypass verified boot.
- It does not download arbitrary firmware images.

DSU is an Android-supported guest-system mechanism. It is manufacturer-dependent and uses trusted system images. A package-presence check is only a clue, not proof that DSU installation is enabled. DSU normally allows a temporary guest system and a return to the original system; it is not a permanent firmware replacement.

## HONOR X8b status
DSU and compatible GSI support have not been confirmed for HONOR X8b (including LLY-LX1). The app must not be represented as a guaranteed phone flasher.

## Build
Open this directory as an Android Gradle project, or run the repository GitHub Actions workflow:
`.github/workflows/build-droidswitch2.yml`.
