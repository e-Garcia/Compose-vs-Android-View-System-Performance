/*
 * Copyright (c) 2025 Compose-vs-Android-View-System-Performance
 *
 * Licensed under the MIT License. See LICENSE for details.
 */

package dev.egarcia.andperf.shared.capability

import kotlinx.serialization.Serializable

/**
 * Pure metric-selection request used before instrumentation-only metric objects
 * are created.
 */
@Serializable
data class MetricSelectionRequest(
    val requestedMetrics: List<MetricType>,
    val requiredMetrics: Set<MetricType> = emptySet()
) {
    init {
        require(requestedMetrics.isNotEmpty()) { "At least one metric must be requested." }
        require(requiredMetrics.all { it in requestedMetrics }) {
            "Required metrics must also be present in requestedMetrics."
        }
    }
}

/**
 * Deterministic result of sanitizing a requested metric set against known
 * capabilities.
 */
@Serializable
data class MetricSelectionResult(
    val requestedMetrics: List<MetricType>,
    val selectedMetrics: List<MetricType>,
    val skippedMetrics: List<MetricCapability>,
    val requiredMetricsSatisfied: Boolean
) {
    val canRun: Boolean
        get() = selectedMetrics.isNotEmpty() && requiredMetricsSatisfied
}

/**
 * Device-independent metric selection logic. Instrumented code can probe the
 * device/OS and then pass the resulting [MetricCapability] values here before
 * building AndroidX Macrobenchmark Metric instances.
 */
object BenchmarkMetricSelector {

    fun select(
        request: MetricSelectionRequest,
        capabilities: Collection<MetricCapability>
    ): MetricSelectionResult {
        val capabilityByMetric = capabilities.associateBy { it.metric }
        val selected = mutableListOf<MetricType>()
        val skipped = mutableListOf<MetricCapability>()

        val uniqueRequested = request.requestedMetrics.distinct()
        uniqueRequested.forEach { metric ->
            val capability = capabilityByMetric[metric] ?: MetricCapability(
                metric = metric,
                supported = false,
                reason = AvailabilityReason.UNKNOWN,
                details = "No capability probe result was provided for $metric."
            )

            if (capability.supported) {
                selected += metric
            } else {
                skipped += capability
            }
        }

        val requiredSatisfied = request.requiredMetrics.all { it in selected }
        return MetricSelectionResult(
            requestedMetrics = uniqueRequested,
            selectedMetrics = selected,
            skippedMetrics = skipped,
            requiredMetricsSatisfied = requiredSatisfied
        )
    }
}
