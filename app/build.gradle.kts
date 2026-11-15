import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    applyDefaultHierarchyTemplate()

    android {
        namespace = "peekniko.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    js {
        browser()
    }
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Peekniko"
            isStatic = true
            export(project(":library"))
            export(project(":library-client-ktor"))
            export(project(":library-storage-sqldelight"))
            linkerOpts("-lsqlite3")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.sqldelight.coroutines.extensions)
            compileOnly(project(":library-client-ktor"))
        }
        val androidJvmMain = create("androidJvmMain") {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.mockwebserver3)
                implementation(libs.okhttp)
                // Set as compileOnly to allow appAndroid to choose whether to use noop or actual implementations
                compileOnly(project(":library-client-ktor"))
                compileOnly(project(":library-client-okhttp"))
                compileOnly(project(":library-storage-sqldelight"))
            }
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
            api(project(":library-client-ktor"))
        }
        androidMain {
            dependsOn(androidJvmMain)
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }
        jvmMain {
            dependsOn(androidJvmMain)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            api(project(":library"))
            api(project(":library-client-ktor"))
            api(project(":library-storage-sqldelight"))
        }
    }
}
