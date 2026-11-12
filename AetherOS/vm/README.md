# Aether OS VM Edition 4.0

Aether OS VM Edition is a real ARM64 Linux guest running inside the Android app through the QEMU/TCG virtual machine engine. It is not a web page and it is not an Android launcher pretending to be an OS.

## What runs

- An ARM64 Linux kernel and Alpine Linux guest, booted by QEMU inside the app.
- A separate persistent virtual disk for guest files and installed packages.
- A native, full-screen GTK 3 phone-style Aether desktop in Russian, displayed through the guest X11 desktop.
- A native Android launcher screen that starts the VM and opens the guest display.
- Guest apps: file chooser, terminal, text-mode browser, notes, calculator, package store, system information, clock and guest power controls.
- The package store installs selected Alpine Linux packages into the persistent guest disk.

The Android phone remains the host. This release is a Linux virtual machine, not an Android virtual machine, not iOS, and not replacement firmware for the Honor phone. The VM's Aether desktop is the guest operating system's interface.

## Permissions and storage

The build removes the app's notification, battery-exemption, broad-storage and AVF permission declarations. QEMU/TCG runs without root and without a user-granted runtime permission. The guest's files live in the app's private data directory. Network functionality uses the normal INTERNET permission. Android still controls app installation and may stop background work under its own power-management rules.

## Target

- Honor X8b / Android 14 (ARM64)
- Android min SDK 26
- QEMU ARM64 software virtualization (TCG)
- Portrait guest framebuffer: 720 × 1600

The guest VM is resource-intensive. First boot extracts the Linux guest image and can require several hundred MB of free storage. Actual speed depends on the phone's CPU and available RAM.

## Build pipeline

The workflow pins the upstream Podroid VM engine source, applies the Aether overlays in `patch-aether.sh`, builds the kernel/initramfs, Alpine root filesystem, ARM64 QEMU binary, then the APK. It also uploads the patched source archive so the GPL-covered source corresponding to the binary is available.

## Upstream and licenses

The VM engine, Android host-side VM code, kernel and Alpine integration are based on **Podroid** by ExTV contributors, licensed under **GNU GPL version 2**:
https://github.com/ExTV/Podroid

Other upstream components retain their own licenses, including QEMU, Linux, Alpine Linux, and the vendored Termux terminal libraries. See the upstream `CREDITS.md` and license files inside the source archive. Aether-specific UI overlays in this folder are contributed for this project; the combined derivative follows the applicable GPLv2 obligations.
