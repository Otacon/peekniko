plugins {
    alias(libs.plugins.androidApplication)
}

dependencies {
    implementation(project(":app"))

    debugImplementation(project(":library-client-ktor"))
    debugImplementation(project(":library-client-okhttp"))
    debugImplementation(project(":library-storage-sqldelight"))

    releaseImplementation(project(":library-client-noop"))

    implementation(libs.ktor.client.core)
    implementation(libs.okhttp)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
}

android {
    namespace = "peekniko"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.cyanotic.peekniko"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    buildTypes {
        release {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
