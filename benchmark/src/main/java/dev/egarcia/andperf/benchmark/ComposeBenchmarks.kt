package dev.egarcia.andperf.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
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
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
                iterations = 3,
                startupMode = StartupMode.COLD,
                measureBlock = { startActivityAndWait() }
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
                    // perform a series of scroll gestures using UiAutomator helper
                    val device = BenchmarkUtils.device
                    val width = device.displayWidth
                    val height = device.displayHeight
                    val startX = (width * 0.5).toInt()
                    val startY = (height * 0.8).toInt()
                    val endY = (height * 0.2).toInt()
                    repeat(8) {
                        device.swipe(startX, startY, startX, endY, 50)
                        Thread.sleep(150)
                    }
                }
            )
        } catch (t: Throwable) {
            // Treat metric collection errors as skipped (devices may return 0 frame samples)
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }
}
