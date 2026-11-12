#!/usr/bin/env bash
set -euo pipefail

PODROID_DIR="${1:?Usage: patch-aether.sh /path/to/Podroid}"
AETHER_DIR="$(cd "$(dirname "$0")" && pwd)"
test -d "$PODROID_DIR/app/src/main/java/com/excp/podroid"
test -d "$PODROID_DIR/build-rootfs/files"

copy_file() {
  local src="$1" dst="$2"
  mkdir -p "$(dirname "$dst")"
  cp "$src" "$dst"
}

copy_file "$AETHER_DIR/aether-shell.py" "$PODROID_DIR/build-rootfs/files/usr/local/share/aether/aether-shell.py"
copy_file "$AETHER_DIR/aether-shell-launcher" "$PODROID_DIR/build-rootfs/files/usr/local/bin/aether-shell-launcher"
copy_file "$AETHER_DIR/aether-shell-init" "$PODROID_DIR/build-rootfs/files/etc/init.d/aether-shell"
chmod 0755 \
  "$PODROID_DIR/build-rootfs/files/usr/local/share/aether/aether-shell.py" \
  "$PODROID_DIR/build-rootfs/files/usr/local/bin/aether-shell-launcher" \
  "$PODROID_DIR/build-rootfs/files/etc/init.d/aether-shell"

cp "$AETHER_DIR/HomeScreen.kt" "$PODROID_DIR/app/src/main/java/com/excp/podroid/ui/screens/home/HomeScreen.kt"
cp "$AETHER_DIR/SetupScreen.kt" "$PODROID_DIR/app/src/main/java/com/excp/podroid/ui/screens/setup/SetupScreen.kt"
cp "$AETHER_DIR/LanguageManager.kt" "$PODROID_DIR/app/src/main/java/com/excp/podroid/data/repository/LanguageManager.kt"
mkdir -p "$PODROID_DIR/app/src/main/res/values-ru"
cp "$AETHER_DIR/strings-ru.xml" "$PODROID_DIR/app/src/main/res/values-ru/strings.xml"

python3 - "$PODROID_DIR" <<'PY'
from pathlib import Path
import re
import sys

root = Path(sys.argv[1])

def replace_once(path, old, new, label):
    p = root / path
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Patch anchor mismatch in {path}: {label} expected 1, got {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")

# Install the guest phone shell dependencies into the real Alpine root image.
replace_once(
    "build-rootfs/build-rootfs.sh",
    "    ttf-dejavu\n",
    "    ttf-dejavu \\\n"
    "    python3 \\\n"
    "    py3-gobject3 \\\n"
    "    gtk+3.0 \\\n"
    "    xterm \\\n"
    "    links\n",
    "GTK guest packages",
)

# Bake the Aether launcher and its OpenRC service into the guest root filesystem.
replace_once(
    "build-rootfs/build-rootfs.sh",
    'cp /work/files/etc/init.d/podroid-ready     "$ROOTFS/etc/init.d/"\n',
    'cp /work/files/etc/init.d/podroid-ready     "$ROOTFS/etc/init.d/"\n'
    'mkdir -p "$ROOTFS/usr/local/share/aether"\n'
    'cp /work/files/usr/local/share/aether/aether-shell.py "$ROOTFS/usr/local/share/aether/"\n'
    'cp /work/files/usr/local/bin/aether-shell-launcher "$ROOTFS/usr/local/bin/"\n'
    'cp /work/files/etc/init.d/aether-shell "$ROOTFS/etc/init.d/"\n'
    'chmod 0755 "$ROOTFS/usr/local/share/aether/aether-shell.py" "$ROOTFS/usr/local/bin/aether-shell-launcher" "$ROOTFS/etc/init.d/aether-shell"\n',
    "Aether guest service assets",
)
replace_once(
    "build-rootfs/build-rootfs.sh",
    "podroid-x11 podroid-vsock",
    "podroid-x11 aether-shell podroid-vsock",
    "Aether service runlevel",
)
replace_once(
    "build-rootfs/files/etc/init.d/podroid-x11",
    "XVNC_GEOMETRY=1280x720",
    "XVNC_GEOMETRY=720x1600",
    "portrait guest display",
)

# Make the native Android front-end enter the actual guest desktop after boot.
replace_once(
    "app/src/main/java/com/excp/podroid/ui/navigation/NavGraph.kt",
    '                onNavigateToContainerBackup = {\n'
    '                    navController.navigate(Routes.CONTAINER_BACKUP) { launchSingleTop = true }\n'
    '                },\n',
    '                onNavigateToContainerBackup = {\n'
    '                    navController.navigate(Routes.CONTAINER_BACKUP) { launchSingleTop = true }\n'
    '                },\n'
    '                onNavigateToX11 = {\n'
    '                    navController.navigate(Routes.TERMINAL_X11) { launchSingleTop = true }\n'
    '                },\n',
    "Home to guest desktop route",
)

# Always choose the unprivileged QEMU/TCG backend. AVF needs device-specific
# privileged grants and is not required for this release.
replace_once(
    "app/src/main/java/com/excp/podroid/data/repository/SettingsRepository.kt",
    'runCatching { EngineSelection.valueOf(prefs[KEY_ENGINE_SELECTION] ?: "AUTO") }',
    'runCatching { EngineSelection.valueOf(prefs[KEY_ENGINE_SELECTION] ?: "QEMU") }',
    "default VM backend",
)

# Brand the app as Aether OS and bump the release identity.
build_file = root / "app/build.gradle.kts"
build = build_file.read_text(encoding="utf-8")
if 'versionCode = 34' not in build or 'versionName = "1.3.0"' not in build:
    raise SystemExit("Podroid Gradle version anchors changed; refusing a partial patch")
build = build.replace("versionCode = 34", "versionCode = 40", 1)
build = build.replace('versionName = "1.3.0"', 'versionName = "4.0.0"', 1)
build_file.write_text(build, encoding="utf-8")

strings_file = root / "app/src/main/res/values/strings.xml"
strings = strings_file.read_text(encoding="utf-8")
if '<string name="app_name">Podroid</string>' not in strings:
    raise SystemExit("App-name resource anchor changed")
strings_file.write_text(strings.replace(
    '<string name="app_name">Podroid</string>',
    '<string name="app_name">Aether OS</string>', 1
), encoding="utf-8")

# Android permission minimisation: no runtime permission prompts, broad storage,
# AVF grants, or battery-optimisation special access. The VM uses app-private
# storage and the QEMU TCG backend; INTERNET is the only network access needed.
manifest_file = root / "app/src/main/AndroidManifest.xml"
manifest = manifest_file.read_text(encoding="utf-8")
for perm in [
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.WRITE_EXTERNAL_STORAGE",
    "android.permission.MANAGE_VIRTUAL_MACHINE",
    "android.permission.USE_CUSTOM_VIRTUAL_MACHINE",
]:
    manifest, n = re.subn(
        r'\s*<uses-permission android:name="' + re.escape(perm) + r'"[^>]*/>\s*',
        "\n", manifest
    )
    if n == 0:
        raise SystemExit(f"Expected permission declaration not found: {perm}")

manifest, n = re.subn(
    r'\s*<uses-feature\s+android:name="android.software.virtualization_framework"\s+android:required="false"\s*/>\s*',
    "\n", manifest
)
if n != 1:
    raise SystemExit(f"AVF feature anchor expected once, got {n}")

# Keep manual automation start/stop broadcasts, but do not register for phone boot.
manifest = manifest.replace(
    '                <action android:name="android.intent.action.BOOT_COMPLETED" />\n', ''
)
manifest = manifest.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@drawable/aether_icon"')
manifest = manifest.replace('android:roundIcon="@mipmap/ic_launcher_round"', 'android:roundIcon="@drawable/aether_icon"')
manifest = manifest.replace('android:value="Podman emulation via QEMU"', 'android:value="Aether OS guest virtual machine"')
manifest_file.write_text(manifest, encoding="utf-8")

# The VM's data already lives in app-private storage; avoid exposing it to cloud backup.
if 'android:allowBackup="true"' in manifest:
    manifest_file.write_text(manifest.replace('android:allowBackup="true"', 'android:allowBackup="false"', 1), encoding="utf-8")

# Aether icon reference is resolved from this native vector drawable.
icon = root / "app/src/main/res/drawable/aether_icon.xml"
icon.parent.mkdir(parents=True, exist_ok=True)
icon.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:fillColor="#101831" android:pathData="M0,0h108v108h-108z" />
    <path android:fillColor="#6FE1CE" android:pathData="M54,12L96,54 54,96 12,54z" />
    <path android:fillColor="#172448" android:pathData="M54,29L79,54 54,79 29,54z" />
    <path android:fillColor="#FFFFFF" android:pathData="M54,40L68,54 54,68 40,54z" />
</vector>
''', encoding="utf-8")

print("Aether OS VM patches applied.")
PY
