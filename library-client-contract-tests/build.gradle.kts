plugins {
    alias(libs.plugins.peeknikoMultiplatform)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            api(libs.kotest.framework.engine)
            api(libs.kotest.assertions.core)
        }
    }
}
