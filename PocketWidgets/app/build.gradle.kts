plugins { id("com.android.application") }
android {
    namespace = "app.verity.pocketwidgets"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.verity.pocketwidgets"
        minSdk = 26
        targetSdk = 28
        versionCode = 3
        versionName = "3.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}