plugins { id("com.android.application") }

android {
    namespace = "app.verity.blocktetris"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.verity.blocktetris"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
