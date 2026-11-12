plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.cyborgos.mobile"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.cyborgos.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 160
        versionName = "1.6.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
