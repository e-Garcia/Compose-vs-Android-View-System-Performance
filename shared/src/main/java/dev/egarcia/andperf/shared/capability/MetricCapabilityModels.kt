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
