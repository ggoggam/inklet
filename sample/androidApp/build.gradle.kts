plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.ggoggam.inklet.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.ggoggam.inklet.sample"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":sample"))
    implementation("androidx.activity:activity-compose:1.13.0")
}
