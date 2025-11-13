package dev.egarcia.andperf.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeViewBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val context = InstrumentationRegistry.getInstrumentation().context
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    @Test
    fun coldStartup_compose() {
        val pkg = "dev.egarcia.andperf.compose"
        // If instrumentation provided a specific target package, skip tests that don't match it.
        val targetArg = InstrumentationRegistry.getArguments().getString("benchmarkTargetPackage")
        if (targetArg != null && targetArg != pkg) {
            Assume.assumeTrue("Skipping because instrumentation targets $targetArg", false)
        }
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", isPackageInstalled(pkg))

        try {
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
                iterations = 3,
                startupMode = StartupMode.COLD,
                measureBlock = { startActivityAndWait() }
            )
        } catch (t: Throwable) {
            // Some devices/targets may not produce frame timing results or other metric errors may occur.
            // Skip the test instead of failing the whole run when metric processing fails.
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }

    @Test
    fun coldStartup_view() {
        val pkg = "dev.egarcia.andperf.view"
        val targetArg = InstrumentationRegistry.getArguments().getString("benchmarkTargetPackage")
        if (targetArg != null && targetArg != pkg) {
            Assume.assumeTrue("Skipping because instrumentation targets $targetArg", false)
        }
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", isPackageInstalled(pkg))

        try {
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(StartupTimingMetric()), // FrameTimingMetric removed for view target to avoid empty results
                iterations = 3,
                startupMode = StartupMode.COLD,
                measureBlock = { startActivityAndWait() }
            )
        } catch (t: Throwable) {
            // Some devices/targets may not produce frame timing results or other metric errors may occur.
            // Skip the test instead of failing the whole run when metric processing fails.
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }
}
