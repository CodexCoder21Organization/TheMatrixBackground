import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    `maven-publish`
}

group = "com.matrix"
version = "1.0.0"

kotlin {
    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "matrixBackground"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "matrixBackground.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.components.resources)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
            }
        }

        val wasmJsMain by getting {
            dependencies {
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.matrix.background.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TheMatrixBackground"
            packageVersion = "1.0.0"
        }
    }
}

publishing {
    publications.withType<MavenPublication> {
        val baseName = "matrix-3d-background"
        artifactId = when (name) {
            "kotlinMultiplatform" -> baseName
            "desktop" -> "$baseName-desktop"
            "wasmJs" -> "$baseName-wasm-js"
            else -> "$baseName-$name"
        }
    }
    repositories {
        maven {
            name = "kotlinDirectory"
            url = uri("https://kotlin.directory")
        }
    }
}
