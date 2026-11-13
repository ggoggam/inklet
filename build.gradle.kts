import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

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

// Every release, stable or prerelease, goes to this repository's GitHub Packages registry with
// `mise run publish`. Versions up to 0.3.0 stay on Maven Central. The credentials are the gradle
// properties GitHubPackagesUsername and GitHubPackagesPassword, which the release workflow passes
// as ORG_GRADLE_PROJECT_*; Gradle only asks for them when a task publishing here is in the graph.
// Uploads are not atomic, and GitHub Packages rejects a file that already exists (409), so a
// version cannot be overwritten or re-published after a partial upload: delete that version from
// each package first, or bump VERSION_NAME.
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ggoggam/inklet")
            credentials(PasswordCredentials::class)
        }
    }
}
