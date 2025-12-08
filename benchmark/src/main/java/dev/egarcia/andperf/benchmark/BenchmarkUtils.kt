package dev.egarcia.andperf.benchmark

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/** Shared helpers for benchmarks. Keep this class minimal and free of instrumentation-specific
 * side-effects so it can be used by multiple test classes. */
object BenchmarkUtils {

    val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            val context = InstrumentationRegistry.getInstrumentation().context
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }
}
