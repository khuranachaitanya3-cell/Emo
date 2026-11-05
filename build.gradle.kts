plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.chaitanya.emobuddy"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chaitanya.emobuddy"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
