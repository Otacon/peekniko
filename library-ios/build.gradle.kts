import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFrameworkTask

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
            isStatic = false
            linkerOpts("-lsqlite3")
            linkerOpts("-x")
            freeCompilerArgs += "-Xadd-light-debug=disable"
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

tasks.withType<XCFrameworkTask>().configureEach {
    doLast {
        (this as XCFrameworkTask).outputDir.walkTopDown().maxDepth(2)
            .filter { it.isDirectory && it.name.endsWith(".xcframework") }
            .forEach { xcframework ->
                xcframework.listFiles().orEmpty().forEach { it.resolve("dSYMs").deleteRecursively() }
                val plist = xcframework.resolve("Info.plist")
                plist.writeText(
                    plist.readText().replace(Regex("""\s*<key>DebugSymbolsPath</key>\s*<string>[^<]*</string>"""), "")
                )
            }
    }
}
