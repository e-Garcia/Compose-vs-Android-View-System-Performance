plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Convenience benchmark tasks (use Exec tasks to avoid deprecated inline `exec {}`)
val deviceSerial: String? = (project.findProperty("deviceSerial") as? String)

// Assemble+install for each app as Exec tasks
tasks.register<Exec>("assembleInstallCompose") {
    group = "benchmark"
    description = "Assemble & install :app-compose:benchmark"
    doFirst {
        commandLine("./gradlew", ":app-compose:assembleBenchmark", ":app-compose:installBenchmark")
    }
}

tasks.register<Exec>("assembleInstallView") {
    group = "benchmark"
    description = "Assemble & install :app-view:benchmark"
    doFirst {
        commandLine("./gradlew", ":app-view:assembleBenchmark", ":app-view:installBenchmark")
    }
}

// Install both apps
tasks.register<Exec>("benchInstallAll") {
    group = "benchmark"
    description = "Assemble & install benchmark APKs for both app modules (:app-compose and :app-view)"
    doFirst {
        commandLine("./gradlew", ":app-compose:assembleBenchmark", ":app-compose:installBenchmark",
            ":app-view:assembleBenchmark", ":app-view:installBenchmark")
    }
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

fun buildBenchmarkCmd(targetProject: String): List<String> {
    val cmd = mutableListOf("./gradlew", ":benchmark:assembleBenchmark", ":benchmark:connectedBenchmarkAndroidTest", "-PbenchmarkTarget=$targetProject")
    deviceSerial?.let { cmd.add("-Pandroid.testInstrumentationRunnerArguments.serial=$it") }
    // Pass the intended package down to the instrumentation APK so tests can skip non-targets
    cmd.add("-Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=${projectPathToPackage(targetProject)}")
    val testClass = testClassForTarget(targetProject)
    if (testClass.isNotBlank()) {
        cmd.add("-Pandroid.testInstrumentationRunnerArguments.class=$testClass")
    }
    return cmd
}

// Exec tasks to run the benchmark instrumentation for a given target
tasks.register<Exec>("benchmarkComposeRun") {
    group = "benchmark"
    description = "Run :benchmark:connectedBenchmarkAndroidTest for :app-compose"
    doFirst {
        commandLine(buildBenchmarkCmd(":app-compose"))
    }
}

tasks.register<Exec>("benchmarkViewRun") {
    group = "benchmark"
    description = "Run :benchmark:connectedBenchmarkAndroidTest for :app-view"
    doFirst {
        commandLine(buildBenchmarkCmd(":app-view"))
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
    // enforce ordering: run view after compose
    // moved mustRunAfter to top-level below to avoid NamedDomainObjectProvider.configure in task context
}

// enforce ordering so installs happen before running benchmarks
tasks.named("benchmarkComposeRun").configure { mustRunAfter("benchInstallAll") }
// enforce ordering: run view after compose
tasks.named("benchmarkViewRun").configure { mustRunAfter("benchmarkComposeRun") }
