plugins {
    id("com.android.test")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.egarcia.andperf.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Start by targeting the Compose app; later we’ll test both
    targetProjectPath = ":app-compose"

    buildTypes {
        // Only define the dedicated benchmark type; no release/debug blocks needed
        create("benchmark") {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            // If you want Gradle to reuse release-like resources from the target app:
            matchingFallbacks += listOf("release")
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(libs.benchmark.macro)
    implementation(libs.uiautomator)
}
