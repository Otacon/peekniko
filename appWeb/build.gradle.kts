import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets.jsMain.dependencies {
        implementation(project(":app"))
        implementation(project(":library"))
        implementation(libs.androidx.navigation.compose)
        implementation(libs.compose.foundation)
        implementation(libs.compose.material3)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
        implementation(libs.ktor.client.core)
    }
}

tasks.named<KotlinWebpack>("jsBrowserProductionWebpack") {
    sourceMaps = false
}

val copyWebDemoToDocs = tasks.register<Sync>("copyWebDemoToDocs") {
    description = "Copies the production web bundle into docs/demo for MkDocs."
    from(tasks.named("jsBrowserDistribution"))
    into(rootProject.layout.projectDirectory.dir("docs/demo"))
}

tasks.named("jsBrowserDistribution") {
    finalizedBy(copyWebDemoToDocs)
}
