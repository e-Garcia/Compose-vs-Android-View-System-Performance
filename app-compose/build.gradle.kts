plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.egarcia.andperf.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.egarcia.andperf.compose"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Use the Android benchmark runner so benchmark extras/listeners are recognized
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
        // Benchmark-friendly build: release-optimized but controllable
        create("benchmark") {
            initWith(getByName("release"))
            // The target app must NOT be debuggable for accurate macrobenchmarks
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures { compose = true }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material)
    debugImplementation(libs.androidx.ui.tooling)

    // Include benchmark junit4 in the instrumentation APK so runner listener classes are present
    androidTestImplementation(libs.benchmark.junit4)
}
