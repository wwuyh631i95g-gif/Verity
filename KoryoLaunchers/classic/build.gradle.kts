plugins { id("com.android.application") }

android {
    namespace = "com.verity.koryoclassic"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.verity.koryoclassic"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").java.srcDir("../shared/src/main/java")
}
