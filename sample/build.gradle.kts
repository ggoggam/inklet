import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    android {
        namespace = "dev.ggoggam.inklet.sample.shared"
        compileSdk = 36
        minSdk = 23
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "InkletSample"
            isStatic = true
        }
    }
    jvm("desktop")
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "inklet.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":"))
            implementation("org.jetbrains.compose.components:components-resources:1.11.0")
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.navigationevent:navigationevent:1.0.2")
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.ggoggam.inklet.sample.MainKt"
    }
}
