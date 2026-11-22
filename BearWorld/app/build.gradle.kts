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
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
