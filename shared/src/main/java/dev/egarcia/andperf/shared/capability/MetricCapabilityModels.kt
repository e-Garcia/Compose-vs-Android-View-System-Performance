/*
 * Copyright (c) 2025 Compose-vs-Android-View-System-Performance
 *
 * Licensed under the MIT License. See LICENSE for details.
 */

package dev.egarcia.andperf.shared.capability

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Canonical metric families understood by the benchmark suite.
 */
@Serializable
enum class MetricType {
    @SerialName("startup")
    STARTUP,

    @SerialName("frame_timing")
    FRAME_TIMING,

    @SerialName("power")
    POWER,

    @SerialName("memory")
    MEMORY,

    @SerialName("thermal")
    THERMAL,

    @SerialName("network")
    NETWORK
}

/**
 * Reasons describing why a metric is or is not supported on a given device/OS.
 */
@Serializable
enum class AvailabilityReason(val friendlyMessage: String) {
    @SerialName("supported")
    SUPPORTED("Metric can be collected on this configuration."),

    @SerialName("os_too_old")
    OS_TOO_OLD("Android version below required API level."),

    @SerialName("hw_unsupported")
    HW_UNSUPPORTED("Device hardware counters are missing."),

    @SerialName("permission_denied")
    PERMISSION_DENIED("Missing shell or adb permissions for this probe."),

    @SerialName("no_data_collected")
    NO_DATA_COLLECTED("Benchmark produced no samples for the metric."),

    @SerialName("runtime_error")
    RUNTIME_ERROR("Unexpected runtime failure while measuring."),

    @SerialName("not_configured")
    NOT_CONFIGURED("Benchmark task skipped because it was not configured."),

    @SerialName("unknown")
    UNKNOWN("Metric capability is unknown.");

    /**
     * Resolve a user-facing message, preferring a caller supplied detail string.
     */
    fun resolveMessage(details: String?): String = details?.takeIf { it.isNotBlank() } ?: friendlyMessage
}

/**
 * Immutable description of the capability status for a single metric.
 */
@Serializable
data class MetricCapability(
    val metric: MetricType,
    val supported: Boolean,
    val reason: AvailabilityReason,
    val details: String? = null
) {
    val message: String
        get() = reason.resolveMessage(details)
}

/**
 * Snapshot of the device/OS data captured when a probe is executed.
 */
@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val device: String,
    val sdkInt: Int,
    val abi: String,
    val buildId: String,
    val fingerprint: String
)

/**
 * Minimal environment supplied to metric probes. Keeping this separate from Android framework
 * classes lets unit tests exercise classification without a device.
 */
data class ProbeEnvironment(
    val deviceInfo: DeviceInfo,
    val hasHardwareCounters: Boolean = true,
    val hasShellMetricPermissions: Boolean = true
)

/**
 * Declarative requirement for a metric family requested by a benchmark.
 */
data class MetricRequirement(
    val metric: MetricType,
    val minSdk: Int,
    val requiresHardwareCounters: Boolean = false,
    val requiresShellMetricPermissions: Boolean = false
)

/**
 * Serializable metadata emitted for each benchmark run before unsupported metrics are removed.
 */
@Serializable
data class MetricCapabilityReport(
    val targetPackage: String,
    val device: DeviceInfo,
    val requested: List<MetricType>,
    val capabilities: List<MetricCapability>
) {
    val available: List<MetricType>
        get() = capabilities.filter { it.supported }.map { it.metric }

    val skipped: List<MetricCapability>
        get() = capabilities.filterNot { it.supported }

    fun humanReadableSummary(): String {
        val skippedSummary = skipped.joinToString(separator = "; ") { capability ->
            "${capability.metric}: ${capability.reason} (${capability.message})"
        }.ifBlank { "none" }

        return "target=$targetPackage, sdk=${device.sdkInt}, requested=$requested, " +
            "available=$available, skipped=$skippedSummary"
    }
}

/**
 * Builds a metric capability report from declarative requirements and optional device probes.
 */
class MetricCapabilityProbe(
    private val probeOverrides: Map<MetricType, (ProbeEnvironment) -> MetricCapability?> = emptyMap()
) {
    fun report(
        targetPackage: String,
        environment: ProbeEnvironment,
        requirements: List<MetricRequirement>
    ): MetricCapabilityReport {
        val capabilities = requirements.map { requirement -> classify(environment, requirement) }

        return MetricCapabilityReport(
            targetPackage = targetPackage,
            device = environment.deviceInfo,
            requested = requirements.map { it.metric },
            capabilities = capabilities
        )
    }

    private fun classify(
        environment: ProbeEnvironment,
        requirement: MetricRequirement
    ): MetricCapability {
        val override = probeOverrides[requirement.metric]
        if (override != null) {
            return try {
                override(environment) ?: defaultClassify(environment, requirement)
            } catch (t: Throwable) {
                MetricCapability(
                    metric = requirement.metric,
                    supported = false,
                    reason = AvailabilityReason.RUNTIME_ERROR,
                    details = t.message ?: "Probe threw ${t::class.simpleName}."
                )
            }
        }

        return defaultClassify(environment, requirement)
    }

    private fun defaultClassify(
        environment: ProbeEnvironment,
        requirement: MetricRequirement
    ): MetricCapability = when {
        environment.deviceInfo.sdkInt < requirement.minSdk -> MetricCapability(
            metric = requirement.metric,
            supported = false,
            reason = AvailabilityReason.OS_TOO_OLD,
            details = "Requires API ${requirement.minSdk}+; device is API ${environment.deviceInfo.sdkInt}."
        )

        requirement.requiresHardwareCounters && !environment.hasHardwareCounters -> MetricCapability(
            metric = requirement.metric,
            supported = false,
            reason = AvailabilityReason.HW_UNSUPPORTED
        )

        requirement.requiresShellMetricPermissions && !environment.hasShellMetricPermissions -> MetricCapability(
            metric = requirement.metric,
            supported = false,
            reason = AvailabilityReason.PERMISSION_DENIED
        )

        else -> MetricCapability(
            metric = requirement.metric,
            supported = true,
            reason = AvailabilityReason.SUPPORTED
        )
    }
}

object MetricRequirements {
    val Startup = MetricRequirement(metric = MetricType.STARTUP, minSdk = 23)
    val FrameTiming = MetricRequirement(metric = MetricType.FRAME_TIMING, minSdk = 23)
}
