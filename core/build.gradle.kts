plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.serialization") version "2.0.21"
    kotlin("plugin.parcelize")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.fpf.smartscan.core"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }

}


dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.documentfile)
//    implementation(libs.smartscan.ml)
    api("com.github.smartscanapp.smartscan-sdk:smartscan-ml:2.2.1")
    api("com.github.devdiaries41.llmconnect-android:llmconnect:1.0.0")

    // media loading
    api(libs.coil.compose)
    api(libs.coil.video)

    // Requests
    api(libs.okhttp)
    api (libs.kotlinx.serialization.json)

    // RoomDB
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Used for PagingSource
    api(libs.androidx.paging.compose)

    // ExoPlayer
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.ui)

    // JVM unit tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // Android instrumented tests
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.mockk.android)
}