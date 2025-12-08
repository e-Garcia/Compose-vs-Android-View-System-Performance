plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.egarcia.andperf.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // Start by targeting the Compose app; can be overridden with -PbenchmarkTarget=":app-view"
    val benchmarkTarget = (project.findProperty("benchmarkTarget") as? String) ?: ":app-compose"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"

        // Instrumentation arguments for benchmark tests
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,LOW-BATTERY"
        testInstrumentationRunnerArguments["listener"] = "androidx.benchmark.junit4.InstrumentationResultsRunListener"

        // Enable self-instrumenting so the benchmark APK isn't declared as targeting the app package
        experimentalProperties["android.experimental.self-instrumenting"] = true

        // Set the benchmarkTargetPackage instrumentation argument so the runner knows which app
        // package to measure. This maps the known module path to the correct applicationId.
        testInstrumentationRunnerArguments["benchmarkTargetPackage"] = when (benchmarkTarget) {
            ":app-compose" -> "dev.egarcia.andperf.compose"
            ":app-view" -> "dev.egarcia.andperf.view"
            else -> "dev.egarcia.andperf.compose"
        }
    }

    targetProjectPath = benchmarkTarget
    // Ensure we target the non-debuggable 'benchmark' variant of the app so the installed
    // target APK is the benchmark build (which should have isDebuggable=false in app modules).
    // This prevents Macrobenchmark from failing with the DEBUGGABLE error.
    targetVariant = "benchmark"

    buildTypes {
        // Debug build type for Android Studio test recognition
        debug {
            isDebuggable = true
            matchingFallbacks += listOf("benchmark", "release")
        }

        // Dedicated benchmark type for running performance tests
        create("benchmark") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    // Use explicit coordinates so the test/runtime classpath contains the benchmark and test
    // libraries that the benchmark sources import (MacrobenchmarkRule, AndroidJUnit4, etc.).
    implementation(libs.benchmark.macro)
    implementation(libs.uiautomator)

    // AndroidX JUnit runner / ext for AndroidJUnit4
    implementation(libs.androidx.junit)
    // Test runner (optional, included transitively in some setups)
    implementation(libs.androidx.runner)

    // Ensure the benchmark junit4 library (contains InstrumentationResultsRunListener) is on the
    // classpath of the instrumentation APK.
    implementation(libs.benchmark.junit4)
}
