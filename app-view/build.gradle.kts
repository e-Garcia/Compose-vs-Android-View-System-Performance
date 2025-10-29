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
}