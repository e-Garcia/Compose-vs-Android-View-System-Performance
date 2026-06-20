package dev.egarcia.andperf.shared.capability

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BenchmarkMetricSelectionTest {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun `select keeps supported metrics in requested order`() {
        val result = BenchmarkMetricSelector.select(
            request = MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.FRAME_TIMING)
            ),
            capabilities = listOf(
                MetricCapability(MetricType.FRAME_TIMING, supported = true, reason = AvailabilityReason.SUPPORTED),
                MetricCapability(MetricType.STARTUP, supported = true, reason = AvailabilityReason.SUPPORTED)
            )
        )

        assertEquals(listOf(MetricType.STARTUP, MetricType.FRAME_TIMING), result.selectedMetrics)
        assertEquals(emptyList(), result.skippedMetrics)
        assertTrue(result.requiredMetricsSatisfied)
        assertTrue(result.canRun)
    }

    @Test
    fun `select records unsupported metrics with probe reason`() {
        val result = BenchmarkMetricSelector.select(
            request = MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.FRAME_TIMING)
            ),
            capabilities = listOf(
                MetricCapability(MetricType.STARTUP, supported = true, reason = AvailabilityReason.SUPPORTED),
                MetricCapability(
                    metric = MetricType.FRAME_TIMING,
                    supported = false,
                    reason = AvailabilityReason.NO_DATA_COLLECTED,
                    details = "No frameDurationCpuMs samples were emitted."
                )
            )
        )

        assertEquals(listOf(MetricType.STARTUP), result.selectedMetrics)
        assertEquals(
            listOf(
                MetricCapability(
                    metric = MetricType.FRAME_TIMING,
                    supported = false,
                    reason = AvailabilityReason.NO_DATA_COLLECTED,
                    details = "No frameDurationCpuMs samples were emitted."
                )
            ),
            result.skippedMetrics
        )
        assertTrue(result.canRun)
    }

    @Test
    fun `select treats missing capability as unknown skipped metric`() {
        val result = BenchmarkMetricSelector.select(
            request = MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.POWER)
            ),
            capabilities = listOf(
                MetricCapability(MetricType.STARTUP, supported = true, reason = AvailabilityReason.SUPPORTED)
            )
        )

        assertEquals(listOf(MetricType.STARTUP), result.selectedMetrics)
        assertEquals(1, result.skippedMetrics.size)
        assertEquals(MetricType.POWER, result.skippedMetrics.single().metric)
        assertEquals(AvailabilityReason.UNKNOWN, result.skippedMetrics.single().reason)
        assertTrue(result.skippedMetrics.single().message.contains("No capability probe result"))
    }

    @Test
    fun `required unsupported metric prevents run even when optional metric is selected`() {
        val result = BenchmarkMetricSelector.select(
            request = MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.FRAME_TIMING),
                requiredMetrics = setOf(MetricType.FRAME_TIMING)
            ),
            capabilities = listOf(
                MetricCapability(MetricType.STARTUP, supported = true, reason = AvailabilityReason.SUPPORTED),
                MetricCapability(
                    metric = MetricType.FRAME_TIMING,
                    supported = false,
                    reason = AvailabilityReason.RUNTIME_ERROR,
                    details = "Frame probe threw before measurement."
                )
            )
        )

        assertEquals(listOf(MetricType.STARTUP), result.selectedMetrics)
        assertFalse(result.requiredMetricsSatisfied)
        assertFalse(result.canRun)
    }

    @Test
    fun `duplicates are collapsed for deterministic reporting`() {
        val result = BenchmarkMetricSelector.select(
            request = MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP, MetricType.STARTUP, MetricType.MEMORY)
            ),
            capabilities = listOf(
                MetricCapability(MetricType.STARTUP, supported = true, reason = AvailabilityReason.SUPPORTED),
                MetricCapability(MetricType.MEMORY, supported = false, reason = AvailabilityReason.HW_UNSUPPORTED)
            )
        )

        assertEquals(listOf(MetricType.STARTUP, MetricType.MEMORY), result.requestedMetrics)
        assertEquals(listOf(MetricType.STARTUP), result.selectedMetrics)
        assertEquals(listOf(MetricType.MEMORY), result.skippedMetrics.map { it.metric })
    }

    @Test
    fun `selection result serializes requested selected and skipped metrics`() {
        val result = MetricSelectionResult(
            requestedMetrics = listOf(MetricType.STARTUP, MetricType.NETWORK),
            selectedMetrics = listOf(MetricType.STARTUP),
            skippedMetrics = listOf(
                MetricCapability(
                    metric = MetricType.NETWORK,
                    supported = false,
                    reason = AvailabilityReason.NOT_CONFIGURED,
                    details = "Network metrics are not part of startup benchmarks."
                )
            ),
            requiredMetricsSatisfied = true
        )

        val payload = json.encodeToString(result)

        assertTrue(payload.contains("\"requestedMetrics\":[\"startup\",\"network\"]"))
        assertTrue(payload.contains("\"selectedMetrics\":[\"startup\"]"))
        assertTrue(payload.contains("\"reason\":\"not_configured\""))

        assertEquals(result, Json.decodeFromString<MetricSelectionResult>(payload))
    }

    @Test
    fun `request rejects empty requested metrics`() {
        assertFailsWith<IllegalArgumentException> {
            MetricSelectionRequest(requestedMetrics = emptyList())
        }
    }

    @Test
    fun `request rejects required metrics outside requested set`() {
        assertFailsWith<IllegalArgumentException> {
            MetricSelectionRequest(
                requestedMetrics = listOf(MetricType.STARTUP),
                requiredMetrics = setOf(MetricType.FRAME_TIMING)
            )
        }
    }
}
