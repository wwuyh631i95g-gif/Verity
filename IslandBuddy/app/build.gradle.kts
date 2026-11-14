plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.verity.islandbuddy"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.verity.islandbuddy"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}