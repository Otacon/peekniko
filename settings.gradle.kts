rootProject.name = "peekniko"

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":app")
include(":appAndroid")
include(":appDesktop")
include(":appWeb")
include(":library")
include(":library-client-ktor")
include(":library-client-okhttp")
include(":library-client-contract-tests")
include(":library-client-noop")
include(":library-storage-sqldelight")
