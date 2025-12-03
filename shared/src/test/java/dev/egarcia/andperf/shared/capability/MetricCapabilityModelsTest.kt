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
        val info = DeviceInfo(
            manufacturer = "Google",
            model = "Pixel 8",
            device = "shiba",
            sdkInt = 34,
            abi = "arm64-v8a",
            buildId = "AP4A.250105.001",
            fingerprint = "google/shiba/shiba:14/AP4A.250105.001/123:user/release-keys"
        )

        val payload = json.encodeToString(info)
        val decoded = Json.decodeFromString<DeviceInfo>(payload)
        assertEquals(info, decoded)
    }
}
