import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.peeknikoPublishing)
    alias(libs.plugins.peeknikoRuntimeDependencies)
}

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-api=strict")
    }

    android {
        namespace = "peekniko.noop"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm()

    // Swift module PeeknikoCoreNoop, the production stand-in for PeeknikoCore. It needs
    // its own name: two frameworks both called PeeknikoCore.framework collide in Xcode's
    // shared build folder when one app has a target for each.
    val xcf = XCFramework("PeeknikoCoreNoop")
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "PeeknikoCoreNoop"
            isStatic = true
            binaryOption("smallBinary", "true")
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            compileOnly(libs.ktor.client.core)
        }
        val androidJvmMain = create("androidJvmMain").apply {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.okhttp)
            }
        }
        androidMain { dependsOn(androidJvmMain) }
        jvmMain { dependsOn(androidJvmMain) }
        nativeMain.dependencies {
            api(libs.ktor.client.core)
        }
    }
}

mavenPublishing {
    coordinates(
        artifactId = "client-noop",
    )
}
