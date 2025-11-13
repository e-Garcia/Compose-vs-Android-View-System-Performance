plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "dev.egarcia.andperf.view"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "dev.egarcia.andperf.view"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
        debug { isMinifyEnabled = false }

        create("benchmark") {
            initWith(getByName("release"))
            // The target app must NOT be debuggable for accurate macrobenchmarks
            isDebuggable = false
            isMinifyEnabled = false           // keep off for now to avoid R8 noise
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.material)

    // Include profileinstaller 1.4.1+ to support API 34+ devices for macrobenchmark profile installs
    implementation(libs.profileinstaller)

    // Include benchmark junit4 in instrumentation classpath for lint/runner expectations
    androidTestImplementation(libs.benchmark.junit4)
}