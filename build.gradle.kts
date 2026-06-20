plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Convenience benchmark tasks (use Exec tasks to avoid deprecated inline `exec {}`)
val deviceSerial: String? = (project.findProperty("deviceSerial") as? String)
val benchmarkClassOverride: String? = (project.findProperty("benchmarkClass") as? String)

// Assemble+install for each app as Gradle task dependencies (avoid spawning nested Gradle)
tasks.register("assembleInstallCompose") {
    group = "benchmark"
    description = "Assemble & install :app-compose:benchmark"
    // Depend on the concrete tasks in the subproject so this runs in the same Gradle invocation
    dependsOn(":app-compose:assembleBenchmark", ":app-compose:installBenchmark")
}

tasks.register("assembleInstallView") {
    group = "benchmark"
    description = "Assemble & install :app-view:benchmark"
    dependsOn(":app-view:assembleBenchmark", ":app-view:installBenchmark")
}

// Install both apps
tasks.register("benchInstallAll") {
    group = "benchmark"
    description = "Assemble & install benchmark APKs for both app modules (:app-compose and :app-view)"
    dependsOn(":app-compose:assembleBenchmark", ":app-compose:installBenchmark",
        ":app-view:assembleBenchmark", ":app-view:installBenchmark")
}

// NOTE: the small helper `projectPathToPackage` was removed to avoid an unused-symbol warning.
// If you need a mapping from project path to package in the future, re-add a minimal helper.

// Convenience tasks to run a single benchmark test class for Compose or View.
// They set the instrumentation runner arguments in the current Gradle invocation
// via the extra properties so the :benchmark:connectedBenchmarkAndroidTest task picks them up.

tasks.register("runBenchmarkComposeClass") {
    group = "benchmark"
    description = "Assemble/install :app-compose and run Compose benchmark class (sets instrumentation class & package)"
    doFirst {
        // Set instrumentation runner args as project properties for this invocation
        project.extensions.extraProperties.set("android.testInstrumentationRunnerArguments.class", "dev.egarcia.andperf.benchmark.ComposeBenchmarks")
        project.extensions.extraProperties.set("android.testInstrumentationRunnerArguments.benchmarkTargetPackage", "dev.egarcia.andperf.compose")
        if (deviceSerial != null) {
            project.extensions.extraProperties.set("android.testInstrumentationRunnerArguments.serial", deviceSerial)
            logger.lifecycle("Passing serial to instrumentation runner: $deviceSerial")
        }
    }
    dependsOn("assembleInstallCompose", ":benchmark:connectedBenchmarkAndroidTest")
}

fun projectPathToPackage(targetProject: String): String = when (targetProject) {
    ":app-compose" -> "dev.egarcia.andperf.compose"
    ":app-view" -> "dev.egarcia.andperf.view"
    else -> targetProject
}

fun testClassForTarget(targetProject: String): String = when (targetProject) {
    ":app-compose" -> "dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_compose"
    ":app-view" -> "dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_view"
    else -> ""
}

fun buildBenchmarkCmd(targetProject: String, singleClassOnly: Boolean = false): List<String> {
    val cmd = mutableListOf("bash", "./gradlew", ":benchmark:assembleBenchmark", ":benchmark:connectedBenchmarkAndroidTest", "-PbenchmarkTarget=$targetProject")
    deviceSerial?.let { cmd.add("-Pandroid.testInstrumentationRunnerArguments.serial=$it") }
    // Pass the intended package down to the instrumentation APK so tests can skip non-targets
    cmd.add("-Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=${projectPathToPackage(targetProject)}")
    val testClass = if (singleClassOnly) {
        benchmarkClassOverride ?: testClassForTarget(targetProject)
    } else {
        benchmarkClassOverride.orEmpty()
    }
    if (testClass.isNotBlank()) {
        cmd.add("-Pandroid.testInstrumentationRunnerArguments.class=$testClass")
    }
}

tasks.register<Exec>("runBenchmarkComposeClass") {
    group = "benchmark"
    description = "Run the Compose benchmark class against :app-compose; override with -PbenchmarkClass=<class#method>"
    doFirst {
        commandLine(buildBenchmarkCmd(":app-compose", singleClassOnly = true))
    }
}

tasks.register<Exec>("benchmarkViewRun") {
    group = "benchmark"
    description = "Run :benchmark:connectedBenchmarkAndroidTest for :app-view"
    dependsOn(":benchmark:assembleBenchmark", ":benchmark:connectedBenchmarkAndroidTest")
    doFirst {
        if (deviceSerial != null) {
            logger.lifecycle("deviceSerial project property is set but to pass it to the instrumentation runner please use -Pandroid.testInstrumentationRunnerArguments.serial=$deviceSerial or ANDROID_SERIAL env var")
        }
    }
}

tasks.register<Exec>("runBenchmarkViewClass") {
    group = "benchmark"
    description = "Run the View benchmark class against :app-view; override with -PbenchmarkClass=<class#method>"
    doFirst {
        commandLine(buildBenchmarkCmd(":app-view", singleClassOnly = true))
    }
}

// High-level user-facing tasks that compose the above Exec tasks
tasks.register("runBenchmarkCompose") {
    group = "benchmark"
    description = "Assemble/install :app-compose then run compose benchmarks"
    dependsOn("assembleInstallCompose", "benchmarkComposeRun")
}

tasks.register("runBenchmarkView") {
    group = "benchmark"
    description = "Assemble/install :app-view then run view benchmarks"
    dependsOn("assembleInstallView", "benchmarkViewRun")
}

tasks.register("runAllBenchmarks") {
    group = "benchmark"
    description = "Install both apps then run benchmarks for :app-compose then :app-view sequentially"
    dependsOn("benchInstallAll", "benchmarkComposeRun", "benchmarkViewRun")
}

// enforce ordering so installs happen before running benchmarks
tasks.named("benchmarkComposeRun").configure { mustRunAfter("benchInstallAll") }
// enforce ordering: run view after compose
tasks.named("benchmarkViewRun").configure { mustRunAfter("benchmarkComposeRun") }
