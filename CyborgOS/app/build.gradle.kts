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
        versionCode = 170
        versionName = "1.7.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
