# Verity Firmware Center 1.0.0

Standalone Android app module for the Verity repository.

## Target
- HONOR X8b family (model codes to confirm in Settings: LLY-LX1 / LLY-LX2 / LLY-LX3)
- Android 8.0+; built with compile/target SDK 34
- No root, USB debugging, ADB, storage, camera, accessibility, overlay, or device-admin permissions
- INTERNET is the only declared permission, used to read the public JSON catalog

## What it does
- Displays read-only device build properties (no IMEI or serial number).
- Opens the phone's built-in system update screen when Android/HONOR exposes it; falls back to Settings.
- Downloads a small HTTPS JSON catalog from this repository.
- Opens official support/update information and a community compatibility index in the browser.
- Clearly labels unverified firmware and warns against cross-model/cross-region packages.

## What it deliberately does NOT do
- It does not unlock the bootloader, root the phone, write partitions, flash firmware, bypass signature checks, use EDL, or install arbitrary update ZIPs.
- Android app permissions cannot grant those capabilities. A normal app cannot replace the signed system updater or override the locked boot chain.
- No direct firmware binaries are bundled or linked. Use only firmware offered by the phone's official updater for its exact region/build.

## Build
From this directory:
`gradle wrapper --gradle-version 8.7`
`./gradlew assembleDebug`

The GitHub Actions workflow at `.github/workflows/build-firmware-center.yml` builds a debug APK and publishes it as an Actions artifact.

## Important
The model code must be checked manually in Settings > About phone. The catalog is informational and does not prove that any custom ROM exists for a particular variant.
