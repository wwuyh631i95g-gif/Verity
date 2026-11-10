from pathlib import Path
import shutil

ROOT = Path("IOS18-Simulator")
APP = ROOT / "app"
JAVA = APP / "src/main/java/dev/ios18/simulator"
RES = APP / "src/main/res/drawable"
SOURCE = Path("scripts/verity55/VerityPhoneExperience.kt")

if not SOURCE.is_file():
    raise SystemExit("The iOS 18 interface source is missing")

JAVA.mkdir(parents=True, exist_ok=True)
RES.mkdir(parents=True, exist_ok=True)

ui = SOURCE.read_text(encoding="utf-8")
ui = ui.replace("package dev.veriti.app", "package dev.ios18.simulator", 1)
ui = ui.replace("fun VerityPhoneExperience()", "fun IOS18Experience()", 1)
replacements = {
    "локальная виртуальная система Verity": "локальная виртуальная система iOS 18",
    "Phone OS": "iOS 18",
    "https://verity.local": "https://ios18.local",
    "Стартовая страница Verity": "Стартовая страница iOS 18",
    "Verity Radio": "iOS Music",
    "Verity Phone 18": "iOS 18",
    "Виртуальное дополнение Verity": "Виртуальное дополнение iOS 18",
    "Добро пожаловать в Verity": "Добро пожаловать в iOS 18",
    "Verity Support": "iOS 18 Support",
}
for old, new in replacements.items():
    ui = ui.replace(old, new)
(JAVA / "IOS18Experience.kt").write_text(ui, encoding="utf-8")

(ROOT / "settings.gradle.kts").write_text("""pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "iOS18Simulator"
include(":app")
""", encoding="utf-8")

(ROOT / "build.gradle.kts").write_text("""plugins {
    id("com.android.application") version "8.6.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
""", encoding="utf-8")

(ROOT / "gradle.properties").write_text("""org.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8
org.gradle.caching=true
org.gradle.parallel=true
android.useAndroidX=true
kotlin.code.style=official
""", encoding="utf-8")

(APP / "build.gradle.kts").write_text("""plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.ios18.simulator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.virtual.ios18.simulator"
        minSdk = 26
        targetSdk = 34
        versionCode = 18001
        versionName = "18.0.1"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
""", encoding="utf-8")

(RES / "ios18_icon.xml").write_text("""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="48dp"
    android:height="48dp"
    android:viewportWidth="48"
    android:viewportHeight="48">
    <path android:fillColor="#253A73" android:pathData="M0,0h48v48h-48z" />
    <path android:fillColor="#FFFFFF" android:pathData="M16,4h16c2.2,0 4,1.8 4,4v32c0,2.2 -1.8,4 -4,4h-16c-2.2,0 -4,-1.8 -4,-4v-32c0,-2.2 1.8,-4 4,-4z" />
    <path android:fillColor="#8DAEFF" android:pathData="M16,9h16v27h-16z" />
    <path android:fillColor="#253A73" android:pathData="M20,6h8v1.5h-8z" />
    <path android:fillColor="#253A73" android:pathData="M22,39h4v1.4h-4z" />
</vector>
""", encoding="utf-8")

(JAVA / "IOS18Activity.kt").write_text("""package dev.ios18.simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

class IOS18Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                IOS18Experience()
            }
        }
    }
}
""", encoding="utf-8")

(APP / "src/main/AndroidManifest.xml").write_text("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="false"
        android:hardwareAccelerated="true"
        android:label="iOS 18 Simulator"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name="dev.ios18.simulator.IOS18Activity"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:exported="true"
            android:screenOrientation="portrait"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""", encoding="utf-8")

print("Generated clean standalone iOS 18 simulator project for Android 14 (API 34).")
