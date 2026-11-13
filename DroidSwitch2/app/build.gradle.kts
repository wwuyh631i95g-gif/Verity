plugins {
    id("com.android.application")
}

android {
    namespace = "com.droidswitch2.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.droidswitch2.mobile"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "2.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
