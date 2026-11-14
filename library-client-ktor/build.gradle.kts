plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotest)
    alias(libs.plugins.peeknikoMultiplatform)
    alias(libs.plugins.peeknikoAndroidLibrary)
    alias(libs.plugins.peeknikoPublishing)
    alias(libs.plugins.peeknikoRuntimeDependencies)
    alias(libs.plugins.mokkery)
}

kotlin {
    android {
        namespace = "peekniko.ktor"
    }

    jvm()
    js {
        browser()
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            compileOnly(libs.ktor.client.core)
        }
        nativeMain.dependencies {
            api(libs.ktor.client.core)
        }
        jsMain.dependencies {
            api(libs.ktor.client.core)
        }
        commonTest.dependencies {
            implementation(libs.kotest.framework.engine)
            implementation(libs.kotest.assertions.core)
            implementation(libs.ktor.client.mock)
            implementation(libs.mokkery.runtime)
        }
        jvmTest.dependencies {
            implementation(project(":library-client-contract-tests"))
            implementation(libs.kotest.runner.junit5)
        }
    }
}

mavenPublishing {
    coordinates(
        artifactId = "client-ktor",
    )
}
