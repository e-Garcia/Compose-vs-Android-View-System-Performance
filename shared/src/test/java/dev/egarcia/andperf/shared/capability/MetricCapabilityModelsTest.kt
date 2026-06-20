package dev.egarcia.andperf.shared.capability

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MetricCapabilityModelsTest {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    private val device = DeviceInfo(
        manufacturer = "Google",
        model = "Pixel 8",
        device = "shiba",
        sdkInt = 34,
        abi = "arm64-v8a",
        buildId = "AP4A.250105.001",
        fingerprint = "google/shiba/shiba:14/AP4A.250105.001/123:user/release-keys"
    )

    @Test
    fun `resolveMessage prefers explicit details`() {
        val capability = MetricCapability(
            metric = MetricType.STARTUP,
            supported = false,
            reason = AvailabilityReason.OS_TOO_OLD,
            details = "Requires API 29+"
        )

        assertEquals("Requires API 29+", capability.message)
    }

    @Test
    fun `capability serializes with snake_case metric names`() {
        val capability = MetricCapability(
            metric = MetricType.FRAME_TIMING,
            supported = false,
            reason = AvailabilityReason.NO_DATA_COLLECTED
        )

        val payload = json.encodeToString(capability)
        assertTrue(payload.contains("\"metric\":\"frame_timing\""))
        assertTrue(payload.contains("\"reason\":\"no_data_collected\""))

        val decoded = Json.decodeFromString<MetricCapability>(payload)
        assertEquals(capability, decoded)
    }

    @Test
    fun `resolveMessage falls back to friendly message when details missing`() {
        val capability = MetricCapability(
            metric = MetricType.FRAME_TIMING,
            supported = false,
            reason = AvailabilityReason.NO_DATA_COLLECTED,
            details = null
        )

        assertEquals(AvailabilityReason.NO_DATA_COLLECTED.friendlyMessage, capability.message)
    }

    @Test
    fun `resolveMessage falls back to friendly message when details blank`() {
        val capability = MetricCapability(
            metric = MetricType.STARTUP,
            supported = false,
            reason = AvailabilityReason.OS_TOO_OLD,
            details = "   "
        )

        assertEquals(AvailabilityReason.OS_TOO_OLD.friendlyMessage, capability.message)
    }

    @Test
    fun `supported capability serializes and reports friendly message`() {
        val capability = MetricCapability(
            metric = MetricType.POWER,
            supported = true,
            reason = AvailabilityReason.SUPPORTED
        )

        val payload = json.encodeToString(capability)
        assertTrue(payload.contains("\"metric\":\"power\""))
        assertTrue(payload.contains("\"reason\":\"supported\""))

        val decoded = Json.decodeFromString<MetricCapability>(payload)
        assertEquals(capability, decoded)
        assertEquals(AvailabilityReason.SUPPORTED.friendlyMessage, capability.message)
    }

    @Test
    fun `device info round trip retains identity`() {
        val payload = json.encodeToString(device)
        val decoded = Json.decodeFromString<DeviceInfo>(payload)
        assertEquals(device, decoded)
    }

    @Test
    fun `probe classifies supported metrics`() {
        val report = MetricCapabilityProbe().report(
            targetPackage = "dev.egarcia.andperf.compose",
            environment = ProbeEnvironment(deviceInfo = device),
            requirements = listOf(MetricRequirements.Startup, MetricRequirements.FrameTiming)
        )

        assertEquals(listOf(MetricType.STARTUP, MetricType.FRAME_TIMING), report.requested)
        assertEquals(listOf(MetricType.STARTUP, MetricType.FRAME_TIMING), report.available)
        assertTrue(report.skipped.isEmpty())
        assertTrue(report.humanReadableSummary().contains("skipped=none"))
    }

    @Test
    fun `probe classifies unsupported metrics when sdk is too old`() {
        val oldDevice = device.copy(sdkInt = 22)
        val report = MetricCapabilityProbe().report(
            targetPackage = "dev.egarcia.andperf.view",
            environment = ProbeEnvironment(deviceInfo = oldDevice),
            requirements = listOf(MetricRequirements.Startup, MetricRequirements.FrameTiming)
        )

        assertTrue(report.available.isEmpty())
        assertEquals(listOf(MetricType.STARTUP, MetricType.FRAME_TIMING), report.skipped.map { it.metric })
        assertEquals(AvailabilityReason.OS_TOO_OLD, report.skipped.first().reason)
        assertTrue(report.skipped.first().message.contains("Requires API 23+"))
    }

    @Test
    fun `probe preserves explicit unsupported probe result`() {
        val probe = MetricCapabilityProbe(
            probeOverrides = mapOf(
                MetricType.FRAME_TIMING to {
                    MetricCapability(
                        metric = MetricType.FRAME_TIMING,
                        supported = false,
                        reason = AvailabilityReason.NO_DATA_COLLECTED,
                        details = "Dry-run probe produced no frame samples."
                    )
                }
            )
        )

        val report = probe.report(
            targetPackage = "dev.egarcia.andperf.view",
            environment = ProbeEnvironment(deviceInfo = device),
            requirements = listOf(MetricRequirements.Startup, MetricRequirements.FrameTiming)
        )

        assertEquals(listOf(MetricType.STARTUP), report.available)
        assertEquals(MetricType.FRAME_TIMING, report.skipped.single().metric)
        assertEquals(AvailabilityReason.NO_DATA_COLLECTED, report.skipped.single().reason)
    }

    @Test
    fun `probe converts runtime errors to skipped capabilities`() {
        val probe = MetricCapabilityProbe(
            probeOverrides = mapOf(
                MetricType.FRAME_TIMING to { throw IllegalStateException("probe failed") }
            )
        )

        val report = probe.report(
            targetPackage = "dev.egarcia.andperf.view",
            environment = ProbeEnvironment(deviceInfo = device),
            requirements = listOf(MetricRequirements.FrameTiming)
        )

        assertTrue(report.available.isEmpty())
        assertEquals(AvailabilityReason.RUNTIME_ERROR, report.skipped.single().reason)
        assertEquals("probe failed", report.skipped.single().message)
    }

    @Test
    fun `capability report serializes requested and skipped metadata`() {
        val report = MetricCapabilityProbe().report(
            targetPackage = "dev.egarcia.andperf.compose",
            environment = ProbeEnvironment(deviceInfo = device.copy(sdkInt = 22)),
            requirements = listOf(MetricRequirements.Startup)
        )

        val payload = json.encodeToString(report)
        assertTrue(payload.contains("\"targetPackage\":\"dev.egarcia.andperf.compose\""))
        assertTrue(payload.contains("\"requested\":[\"startup\"]"))
        assertTrue(payload.contains("\"reason\":\"os_too_old\""))

        val decoded = Json.decodeFromString<MetricCapabilityReport>(payload)
        assertEquals(report, decoded)
    }
}
