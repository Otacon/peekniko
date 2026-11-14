plugins {
    alias(libs.plugins.peeknikoMultiplatform)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            api(libs.kotest.framework.engine)
            api(libs.kotest.assertions.core)
        }
    }
}
