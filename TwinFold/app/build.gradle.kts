plugins {
    id("com.android.application")
}

android {
    namespace = "com.twinfold.duolink"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.twinfold.duolink"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
