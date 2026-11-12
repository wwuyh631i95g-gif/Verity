from pathlib import Path
import re
import shutil

ROOT = Path("Verity-main")
JAVA = ROOT / "app/src/main/java/dev/veriti/app"
SOURCE = Path("scripts/verity55/VerityPhoneExperience.kt")

if not SOURCE.is_file():
    raise SystemExit("Phone OS 18 composable source is missing")
JAVA.mkdir(parents=True, exist_ok=True)
shutil.copyfile(SOURCE, JAVA / "VerityPhoneExperience.kt")

activity = JAVA / "PhoneOSStandaloneActivity.kt"
activity.write_text("""package dev.veriti.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

class PhoneOSStandaloneActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                VerityPhoneExperience()
            }
        }
    }
}
""", encoding="utf-8")

manifest = ROOT / "app/src/main/AndroidManifest.xml"
manifest.write_text("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="dev.veriti.app">

    <uses-permission android:name="android.permission.INTERNET" tools:node="remove" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" tools:node="remove" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" tools:node="remove" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" tools:node="remove" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" tools:node="remove" />
    <uses-permission android:name="android.permission.CAMERA" tools:node="remove" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" tools:node="remove" />
    <uses-permission android:name="android.permission.READ_CONTACTS" tools:node="remove" />
    <uses-permission android:name="android.permission.SEND_SMS" tools:node="remove" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:node="remove" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" tools:node="remove" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" tools:node="remove" />

    <application
        android:allowBackup="false"
        android:label="Phone OS 18"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".PhoneOSStandaloneActivity"
            android:exported="true"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""", encoding="utf-8")

gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text(encoding="utf-8")
if not re.search(r'applicationId\s*=\s*"[^"]+"', g):
    raise SystemExit("applicationId was not found")
g = re.sub(r'applicationId\s*=\s*"[^"]+"', 'applicationId = "dev.verity.phoneos18"', g, count=1)
if not re.search(r'versionCode\s*=\s*\d+\s+versionName\s*=\s*"[^"]+"', g):
    raise SystemExit("version metadata was not found")
g = re.sub(r'versionCode\s*=\s*\d+\s+versionName\s*=\s*"[^"]+"',
           'versionCode = 18000\n        versionName = "18.0"', g, count=1)
gradle.write_text(g, encoding="utf-8")

print("Standalone Phone OS 18 app configured: dev.verity.phoneos18, version 18.0")
