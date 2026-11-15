plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    alias(libs.plugins.peeknikoMultiplatform)
    alias(libs.plugins.peeknikoAndroidLibrary)
    alias(libs.plugins.peeknikoPublishing)
    alias(libs.plugins.peeknikoRuntimeDependencies)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "peekniko.storage.sqldelight"
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            compileOnly(libs.sqldelight.runtime)
            compileOnly(libs.sqldelight.coroutines.extensions)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            compileOnly(libs.sqldelight.android.driver)
        }
        jvmMain.dependencies {
            compileOnly(libs.sqldelight.sqlite.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        jvmTest.dependencies {
            implementation(libs.kotest.runner.junit5)
            runtimeOnly(libs.sqldelight.runtime)
            runtimeOnly(libs.sqldelight.coroutines.extensions)
            runtimeOnly(libs.sqldelight.sqlite.driver)
        }
        androidHostTest.dependencies {
            runtimeOnly(libs.sqldelight.runtime)
            runtimeOnly(libs.sqldelight.coroutines.extensions)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotest.runner.junit5)
            runtimeOnly(libs.junit.platform.launcher)
        }
        iosMain.dependencies {
            api(libs.sqldelight.native.driver)
            api(libs.sqldelight.coroutines.extensions)
            api(libs.sqldelight.runtime)
        }
    }
}

// SQLDelight 2.2.1 unconditionally injects runtime into commonMainApi in
// afterEvaluate. Keep our compileOnly declaration for Android/JVM and the
// explicit iOS API dependency instead of exporting a host-library version.
afterEvaluate {
    configurations.getByName("commonMainApi").dependencies.removeIf {
        it.group == "app.cash.sqldelight" && it.name == "runtime"
    }
}

sqldelight {
    databases {
        create("PeeknikoDatabase") {
            packageName.set("peekniko.storage.sqldelight.db")
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}

mavenPublishing {
    coordinates(artifactId = "storage-sqldelight")
}
