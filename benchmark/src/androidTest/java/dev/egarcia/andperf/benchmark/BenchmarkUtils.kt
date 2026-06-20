package dev.egarcia.andperf.benchmark

import android.os.Build
import android.util.Log
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import dev.egarcia.andperf.shared.capability.DeviceInfo
import dev.egarcia.andperf.shared.capability.MetricCapabilityProbe
import dev.egarcia.andperf.shared.capability.MetricRequirements
import dev.egarcia.andperf.shared.capability.MetricType
import dev.egarcia.andperf.shared.capability.ProbeEnvironment
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "BenchmarkCapability"

private val capabilityJson = Json {
    encodeDefaults = true
    prettyPrint = false
}

/**
 * Device/OS snapshot used by capability reports emitted from instrumented benchmark runs.
 */
fun currentDeviceInfo(): DeviceInfo = DeviceInfo(
    manufacturer = Build.MANUFACTURER ?: "unknown",
    model = Build.MODEL ?: "unknown",
    device = Build.DEVICE ?: "unknown",
    sdkInt = Build.VERSION.SDK_INT,
    abi = Build.SUPPORTED_ABIS.firstOrNull() ?: Build.CPU_ABI ?: "unknown",
    buildId = Build.ID ?: "unknown",
    fingerprint = Build.FINGERPRINT ?: "unknown"
)

/**
 * Converts requested metric families to concrete Macrobenchmark metrics after emitting a
 * structured capability report. Unsupported metric families are excluded before measurement.
 */
fun sanitizedMetricsForBenchmark(
    targetPackage: String,
    requestedMetrics: List<MetricType>,
    probe: MetricCapabilityProbe = MetricCapabilityProbe(),
    environment: ProbeEnvironment = ProbeEnvironment(deviceInfo = currentDeviceInfo())
): List<Metric> {
    val requirements = requestedMetrics.map { metric ->
        when (metric) {
            MetricType.STARTUP -> MetricRequirements.Startup
            MetricType.FRAME_TIMING -> MetricRequirements.FrameTiming
            else -> error("No Macrobenchmark metric mapping exists for $metric")
        }
    }
    val report = probe.report(targetPackage, environment, requirements)

    Log.i(TAG, "Metric capability report: ${report.humanReadableSummary()}")
    Log.i(TAG, "Metric capability JSON: ${capabilityJson.encodeToString(report)}")

    return report.available.map { metric ->
        when (metric) {
            MetricType.STARTUP -> StartupTimingMetric()
            MetricType.FRAME_TIMING -> FrameTimingMetric()
            else -> error("No Macrobenchmark metric mapping exists for $metric")
        }
    }
}

fun MacrobenchmarkRule.measureStartupWithCapabilityReport(
    packageName: String,
    requestedMetrics: List<MetricType>,
    startupMode: StartupMode = StartupMode.COLD,
    iterations: Int = 3
) {
    val metrics = sanitizedMetricsForBenchmark(
        targetPackage = packageName,
        requestedMetrics = requestedMetrics
    )

    check(metrics.isNotEmpty()) { "No supported benchmark metrics remain for $packageName" }

    measureRepeated(
        packageName = packageName,
        metrics = metrics,
        iterations = iterations,
        startupMode = startupMode,
        measureBlock = { startActivityAndWait() }
    )
}
