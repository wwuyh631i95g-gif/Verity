plugins {
    id("com.android.application")
}

android {
    namespace = "com.verity.firmwarecenter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.verity.firmwarecenter"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
