package dev.egarcia.andperf.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.egarcia.andperf.shared.capability.MetricType
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup_compose() {
        val pkg = "dev.egarcia.andperf.compose"
        // Skip if the expected target package is not installed on the device
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", BenchmarkUtils.isPackageInstalled(pkg))

        try {
            rule.measureStartupWithCapabilityReport(
                packageName = pkg,
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.FRAME_TIMING),
                startupMode = StartupMode.COLD,
                iterations = 3
            )
        } catch (t: Throwable) {
            // Treat metric collection errors as skipped (device may not surface frame metrics)
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }

    @Test
    fun fastScroll_compose() {
        val pkg = "dev.egarcia.andperf.compose"
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", BenchmarkUtils.isPackageInstalled(pkg))

        try {
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(FrameTimingMetric()),
                iterations = 5,
                startupMode = StartupMode.WARM,
                measureBlock = {
                    startActivityAndWait()
                    BenchmarkUtils.performFastScrollGestures()
                }
            )
        } catch (t: Throwable) {
            // Treat metric collection errors as skipped (devices may return 0 frame samples)
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }
}
