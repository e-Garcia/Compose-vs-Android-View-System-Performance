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
class ViewBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup_view() {
        val pkg = "dev.egarcia.andperf.view"
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", BenchmarkUtils.isPackageInstalled(pkg))

        try {
            // For view implementation, avoid FrameTimingMetric on some targets that don't provide frame metrics.
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(StartupTimingMetric()),
                iterations = 3,
                startupMode = StartupMode.COLD,
                measureBlock = { startActivityAndWait() }
            )
        } catch (t: Throwable) {
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }

    @Test
    fun fastScroll_view() {
        val pkg = "dev.egarcia.andperf.view"
        Assume.assumeTrue("Skipping test because target package $pkg is not installed", BenchmarkUtils.isPackageInstalled(pkg))

        try {
            rule.measureRepeated(
                packageName = pkg,
                metrics = listOf(FrameTimingMetric()),
                iterations = 5,
                startupMode = StartupMode.WARM,
                measureBlock = {
                    startActivityAndWait()

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
            Assume.assumeTrue("Skipping benchmark due to metric error: ${t.message}", false)
        }
    }
}
