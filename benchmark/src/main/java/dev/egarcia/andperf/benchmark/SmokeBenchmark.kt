package dev.egarcia.andperf.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmokeBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun coldStartup_compose_smoke() {
        rule.measureRepeated(
            packageName = "dev.egarcia.andperf.compose",
            metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
            iterations = 3,
            startupMode = StartupMode.COLD,
            measureBlock = { startActivityAndWait() }
        )
    }
}

