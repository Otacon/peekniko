plugins {
    id("com.dropbox.dependency-guard")
}

dependencyGuard {
    configuration("androidRuntimeClasspath") { modules = true }
    configuration("jvmRuntimeClasspath") { modules = true }
}
