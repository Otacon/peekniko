import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

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
            baseName = if (buildType == NativeBuildType.DEBUG) "Peekniko" else "App"
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

val iosSwiftSourceDir = rootProject.file("library/src/iosMain/swift")

tasks.withType<KotlinNativeLink>().configureEach {
    val frameworkBinary = binary as? Framework ?: return@configureEach
    val konanTargetName = frameworkBinary.target.konanTarget.name
    val frameworkBaseName = frameworkBinary.baseName
    if (frameworkBaseName != "Peekniko") return@configureEach
    val swiftDir = iosSwiftSourceDir

    inputs.dir(swiftDir).withPropertyName("iosSwiftSources").optional()

    doLast {
        if (!System.getProperty("os.name").contains("Mac", ignoreCase = true)) return@doLast
        val (sdk, targetTriple, moduleTriple) = when (konanTargetName) {
            "ios_arm64" -> Triple("iphoneos", "arm64-apple-ios17.0", "arm64-apple-ios")
            "ios_simulator_arm64" -> Triple(
                "iphonesimulator", "arm64-apple-ios17.0-simulator", "arm64-apple-ios-simulator"
            )

            "ios_x64" -> Triple("iphonesimulator", "x86_64-apple-ios17.0-simulator", "x86_64-apple-ios-simulator")
            else -> return@doLast
        }

        val swiftFiles = swiftDir.walkTopDown()
            .filter { it.isFile && it.extension == "swift" }
            .map { it.absolutePath }
            .sorted()
            .toList()
        if (swiftFiles.isEmpty()) return@doLast

        val frameworkDir = outputFile.get()
        val binaryFile = File(frameworkDir, frameworkBaseName)
        val swiftModuleDir = File(frameworkDir, "Modules/$frameworkBaseName.swiftmodule").apply { mkdirs() }
        val tempObjFile = File(temporaryDir, "${frameworkBaseName}_swift.o")
        val swiftModuleFile = File(swiftModuleDir, "$moduleTriple.swiftmodule")
        val swiftInterfaceFile = File(swiftModuleDir, "$moduleTriple.swiftinterface")

        val swiftcCmd = buildList {
            add("xcrun")
            add("--sdk")
            add(sdk)
            add("swiftc")
            add("-target")
            add(targetTriple)
            add("-swift-version")
            add("5")
            add("-F")
            add(frameworkDir.parentFile.absolutePath)
            add("-module-name")
            add(frameworkBaseName)
            add("-import-underlying-module")
            add("-parse-as-library")
            add("-whole-module-optimization")
            add("-enable-library-evolution")
            add("-emit-object")
            add("-o")
            add(tempObjFile.absolutePath)
            add("-emit-module")
            add("-emit-module-path")
            add(swiftModuleFile.absolutePath)
            add("-emit-module-interface-path")
            add(swiftInterfaceFile.absolutePath)
            addAll(swiftFiles)
        }

        val swiftcProc = ProcessBuilder(swiftcCmd).redirectErrorStream(true).start()
        val swiftcOutput = swiftcProc.inputStream.bufferedReader().readText()
        if (swiftcProc.waitFor() != 0) {
            throw GradleException("swiftc failed for $konanTargetName:\n$swiftcOutput")
        }

        val libtoolCmd = listOf(
            "xcrun",
            "libtool",
            "-static",
            "-o",
            binaryFile.absolutePath,
            binaryFile.absolutePath,
            tempObjFile.absolutePath,
        )
        val libtoolProc = ProcessBuilder(libtoolCmd).redirectErrorStream(true).start()
        val libtoolOutput = libtoolProc.inputStream.bufferedReader().readText()
        if (libtoolProc.waitFor() != 0) {
            throw GradleException("libtool failed for $konanTargetName:\n$libtoolOutput")
        }
    }
}
