plugins { id("com.android.application") }
android {
    namespace = "app.verity.cartoonblocks"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.verity.cartoonblocks"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
