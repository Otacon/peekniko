plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("com.dropbox.dependency-guard:dependency-guard:${libs.versions.dependency.guard.get()}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    implementation("com.android.tools.build:gradle:${libs.versions.agp.get()}")
    implementation("com.vanniktech:gradle-maven-publish-plugin:${libs.versions.maven.publish.get()}")
}
