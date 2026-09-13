plugins {
    kotlin("multiplatform") version "2.4.0-RC"
    id("com.android.kotlin.multiplatform.library") version "9.2.1"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.0-RC"
    id("com.android.application") version "9.2.1" apply false
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

group = providers.gradleProperty("GROUP").get()
version = providers.gradleProperty("VERSION_NAME").get()

kotlin {
    android {
        namespace = "dev.ggoggam.inklet"
        compileSdk = 36
        minSdk = 23
    }
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.compose.foundation:foundation:1.11.0")
            api("org.jetbrains.compose.material3:material3:1.9.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
