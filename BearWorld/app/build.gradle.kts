plugins {
    id("com.android.application")
}
android {
    namespace = "com.verity.bearworld"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.verity.bearworld"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
