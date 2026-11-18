import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    sourceSets.jvmMain.dependencies {
        implementation(project(":app"))
        implementation(project(":library"))
        implementation(project(":library-client-ktor"))
        implementation(project(":library-client-okhttp"))
        implementation(project(":library-storage-sqldelight"))
        implementation(compose.desktop.currentOs)
    }
}

compose.desktop {
    application {
        mainClass = "peekniko.DesktopMainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg)
            packageName = "Peekniko"
            packageVersion = "1.0.0"
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/macos/Peekniko.icns"))
            }
        }
    }
}
