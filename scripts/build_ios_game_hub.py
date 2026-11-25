#!/usr/bin/env python3
"""Apply the small Android launcher integration patch to upstream touchHLE."""
from pathlib import Path
import shutil
import sys


def replace_once(path: Path, old: str, new: str) -> None:
    source = path.read_text(encoding="utf-8")
    count = source.count(old)
    if count < 1:
        raise SystemExit(f"Patch point not found in {path}")
    path.write_text(source.replace(old, new), encoding="utf-8")


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("Usage: build_ios_game_hub.py /path/to/touchHLE")
    root = Path(sys.argv[1]).resolve()
    here = Path(__file__).resolve().parents[1] / "IOSGameHub"
    if not (root / "src/lib.rs").is_file():
        raise SystemExit(f"Not a touchHLE source tree: {root}")

    # Forward Java's selected IPA path through SDL's argv to touchHLE's Rust entry point.
    replace_once(
        root / "src/lib.rs",
        """    match main([String::new()].into_iter()) {
        Ok(_) => echo!("touchHLE finished"),
        Err(e) => echo!("touchHLE errored: {e:?}"),
    }
""",
        """    let mut decoded_args = Vec::<String>::new();
    if !_argv.is_null() && _argc > 0 {
        for index in 0..(_argc as isize) {
            let arg_ptr = unsafe { *_argv.offset(index) };
            if !arg_ptr.is_null() {
                let arg = unsafe { std::ffi::CStr::from_ptr(arg_ptr) };
                decoded_args.push(arg.to_string_lossy().into_owned());
            }
        }
    }
    if decoded_args.is_empty() {
        decoded_args.push(String::new());
    }
    match main(decoded_args.into_iter()) {
        Ok(_) => echo!("touchHLE finished"),
        Err(e) => echo!("touchHLE errored: {e:?}"),
    }
""",
    )

    # Keep the native file-manager URI aligned with this app's provider authority.
    replace_once(
        root / "src/paths.rs",
        "content://org.touchhle.android{}{}.provider/root/root",
        "content://app.verity.iosgamehub{}{}.provider/root/root",
    )

    gradle = root / "android/app/build.gradle.kts"
    replace_once(gradle, 'applicationId = "org.touchhle.android"', 'applicationId = "app.verity.iosgamehub"')
    replace_once(gradle, 'join("touchHLE", " ", branding)', 'join("iOS Game Hub", " ", branding)')

    manifest = root / "android/app/src/main/AndroidManifest.xml"
    old_activity = """        <activity
            android:name="org.touchhle.android.MainActivity"
            android:alwaysRetainTaskState="true"
            android:configChanges="layoutDirection|locale|orientation|uiMode|screenLayout|screenSize|smallestScreenSize|keyboard|keyboardHidden|navigation"
            android:exported="true"
            android:launchMode="singleInstance"
            android:preferMinimalPostProcessing="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- Let Android know that we can handle some USB devices and should receive this event -->
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
            <!-- Drop file event -->
            <!--
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="*/*" />
            </intent-filter>
            -->
        </activity>
"""
    new_activity = """        <activity
            android:name="org.touchhle.android.LauncherActivity"
            android:exported="true"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="content" />
                <data android:scheme="file" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="application/octet-stream" />
            </intent-filter>
        </activity>

        <activity
            android:name="org.touchhle.android.MainActivity"
            android:alwaysRetainTaskState="true"
            android:configChanges="layoutDirection|locale|orientation|uiMode|screenLayout|screenSize|smallestScreenSize|keyboard|keyboardHidden|navigation"
            android:exported="false"
            android:launchMode="singleInstance"
            android:preferMinimalPostProcessing="true"
            android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
            <!-- Let Android know that we can handle some USB devices and should receive this event -->
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
        </activity>
"""
    replace_once(manifest, old_activity, new_activity)

    java_dir = root / "android/app/src/main/java/org/touchhle/android"
    java_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(here / "LauncherActivity.java", java_dir / "LauncherActivity.java")
    shutil.copy2(here / "MainActivity.java", java_dir / "MainActivity.java")

    drawable_dir = root / "android/app/src/main/res/drawable"
    drawable_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy2(here / "ic_game_hub.xml", drawable_dir / "ic_game_hub.xml")

    print("Applied iOS Game Hub launcher patch to touchHLE.")
    print("Application ID: app.verity.iosgamehub")


if __name__ == "__main__":
    main()
