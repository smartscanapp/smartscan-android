import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization") version "2.0.21"
    kotlin("plugin.parcelize")
    id("com.google.devtools.ksp")
}

extensions.configure<ApplicationExtension> {
    namespace = "com.fpf.smartscan"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.fpf.smartscan"
        minSdk = 30
        targetSdk = 34
        versionCode = 23
        versionName = "1.3.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
            )
        )
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        viewBinding = false
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Required for F-droid reproducible builds
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
    ndkVersion = "27.0.12077973"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.documentfile)
//    implementation(libs.smartscan.ml)
    implementation("com.github.smartscanapp.smartscan-sdk:smartscan-ml:2.2.1")

    implementation(platform(libs.koin.bom))

    // Koin DI
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)

    // SplashScreen
    implementation(libs.androidx.core.splashscreen)

    // media loading
    implementation(libs.coil.compose)
    implementation(libs.coil.video)

    // Requests
    implementation(libs.okhttp)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation (libs.kotlinx.serialization.json)
    implementation (libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.paging.compose)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    debugImplementation(libs.androidx.ui.tooling)

    // JVM unit tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))

    // Android instrumented tests
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.espresso.core)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}