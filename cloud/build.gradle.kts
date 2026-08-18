plugins {
    alias(libs.plugins.android.library)
    kotlin("plugin.serialization") version "2.0.21"
}

android {
    namespace = "com.fpf.smartscan.cloud"
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

}

dependencies {
    implementation(project(":core"))

    // LLMs
    implementation(libs.llmconnect.android)

}