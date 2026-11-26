import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

// Packages the Swift-facing PeeknikoCore XCFramework, released through Swift Package Manager.
// It bundles the core inspector with SQLDelight storage; the Maven artifacts are unaffected.
plugins {
    alias(libs.plugins.peeknikoMultiplatform)
}

kotlin {
    val xcf = XCFramework("PeeknikoCore")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "PeeknikoCore"
            isStatic = true
            export(project(":library"))
            export(project(":library-storage-sqldelight"))
            binaryOption("smallBinary", "true")
            xcf.add(this)
        }
    }

    sourceSets {
        iosMain.dependencies {
            api(project(":library"))
            api(project(":library-storage-sqldelight"))
        }
    }
}
