@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.antlr)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.peeknikoRuntimeDependencies)
    alias(libs.plugins.kotest)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.ksp)
    alias(libs.plugins.peeknikoPublishing)
}

composeCompiler {
    targetKotlinPlatforms.set(
        setOf(
            KotlinPlatformType.js,
            KotlinPlatformType.wasm,
            KotlinPlatformType.native,
            KotlinPlatformType.common,
        )
    )
}

val generateKotlinGrammarSource =
    tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
        dependsOn("cleanGenerateKotlinGrammarSource")
        source = fileTree(layout.projectDirectory.dir("antlr")) { include("**/*.g4") }
        packageName = "peekniko.internal.json.generated"
        arguments = listOf("-visitor", "-no-listener")
        outputDirectory = layout.buildDirectory
            .dir("generatedAntlr/peekniko/internal/json/generated").get().asFile
    }

kotlin {
    applyDefaultHierarchyTemplate()

    val xcf = XCFramework("PeeknikoCore")

    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-api=strict")
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlin.time.ExperimentalTime")
    }

    android {
        namespace = "peekniko.library"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
        androidResources {
            enable = true
        }
        withHostTest { }
    }

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { target ->
        target.binaries.framework {
            baseName = "PeeknikoCore"
            isStatic = true
            xcf.add(this)
        }
    }
    js {
        browser()
    }
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateKotlinGrammarSource)
            dependencies {
                implementation(libs.antlr.kotlin.runtime)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.recyclerview)
            implementation(libs.androidx.startup)
            implementation(libs.kotlinx.coroutines.android)
        }
        jvmMain.dependencies {
            implementation(libs.flatlaf)
            implementation(libs.kotlinx.coroutines.swing)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mokkery.runtime)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            runtimeOnly(libs.kotlinx.coroutines.swing)
        }
        iosTest.dependencies {
            implementation(project(":library-client-contract-tests"))
        }
        // Compose UI shared by the platforms without a native UI toolkit implementation (web and iOS).
        val composeMain = create("composeMain") {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.compose.foundation)
                api(libs.compose.material3)
                api(libs.compose.runtime)
                api(libs.compose.ui)
            }
        }
        jsMain { dependsOn(composeMain) }
        wasmJsMain { dependsOn(composeMain) }
        iosMain { dependsOn(composeMain) }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

mavenPublishing {
    coordinates(
        artifactId = "client-core",
    )
}

tasks.register("preparePeeknikoKitXCFramework") {
    dependsOn("assemblePeeknikoCoreDebugXCFramework")
}
