plugins {
    alias(libs.plugins.peeknikoMultiplatform)
    alias(libs.plugins.peeknikoAndroidLibrary)
    alias(libs.plugins.peeknikoPublishing)
    alias(libs.plugins.peeknikoRuntimeDependencies)
    alias(libs.plugins.mokkery)
}

kotlin {
    android {
        namespace = "peekniko.okhttp"
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            implementation(libs.kotlinx.coroutines.core)
        }
        val androidJvmMain = create("androidJvmMain").apply {
            dependsOn(commonMain.get())
            dependencies {
                compileOnly(libs.okhttp)
            }
        }
        androidMain {
            dependsOn(androidJvmMain)
        }
        jvmMain {
            dependsOn(androidJvmMain)
        }
        jvmTest.dependencies {
            implementation(project(":library-client-contract-tests"))
            implementation(libs.kotest.runner.junit5)
            implementation(libs.kotest.assertions.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.mockwebserver3)
            implementation(libs.mokkery.runtime)
        }
    }
}

mavenPublishing {
    coordinates(
        artifactId = "client-okhttp",
    )
}
