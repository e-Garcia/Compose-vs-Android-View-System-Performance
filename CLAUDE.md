# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a quantitative research project comparing **Jetpack Compose** and **traditional Android View system** performance under identical conditions. The project uses AndroidX Macrobenchmark to measure cold startup time, first frame latency, scroll jank, and frame time percentiles on physical devices.

## Repository Structure

- **app-compose/**: Jetpack Compose implementation using LazyColumn
- **app-view/**: XML View + RecyclerView implementation
- **shared/**: Shared data models (`Item`) and fake repository (`FakeRepo`), plus MetricCapability model
- **benchmark/**: AndroidX Macrobenchmark test module
- **results/**: JSON/CSV benchmark outputs (not yet populated)

## Key Architecture Points

### Module Dependencies
- Both `app-compose` and `app-view` depend on `shared` module
- `benchmark` module uses dynamic targeting via `-PbenchmarkTarget` property (defaults to `:app-compose`)
- All modules use Kotlin 2.2.21 with JVM toolchain 17

### Build Types
All app modules define three build types:
1. **release**: Minified with R8 and ProGuard optimization
2. **debug**: No minification
3. **benchmark**: Inherits from release, non-debuggable (`isDebuggable = false`) with minification disabled for consistent profiling

The benchmark module defines:
1. **debug**: `isDebuggable = true`, `matchingFallbacks = ["benchmark", "release"]` (for Android Studio recognition)
2. **benchmark**: `isDebuggable = true`, `isMinifyEnabled = false`, `matchingFallbacks = ["release"]`

### Shared Data Model
- `Item(id: Int, title: String, subtitle: String)` is defined in the `shared` module
- `FakeRepo.items(count: Int = 1000)` generates test data
- Both implementations use identical datasets to ensure fair comparison
- `MetricCapability` model supports device capability detection for future expansion (power, memory, thermal metrics)

### Version Catalogs
Project uses Gradle version catalogs (`gradle/libs.versions.toml`):
- AGP: 8.11.2
- Kotlin: 2.2.21
- compileSdk: 36
- minSdk: 24
- Compose BOM: 2025.11.00
- Benchmark: 1.4.1

## Common Commands

### Building

Build benchmark APKs for both apps:
```bash
./gradlew :app-compose:assembleBenchmark :app-view:assembleBenchmark
```

Build benchmark module:
```bash
./gradlew :benchmark:assembleBenchmark
```

Build all modules:
```bash
./gradlew build
```

Install benchmark builds (convenience tasks defined in root build.gradle.kts):
```bash
./gradlew assembleInstallCompose    # Install Compose app
./gradlew assembleInstallView       # Install View app
./gradlew benchInstallAll           # Install both apps
```

### Running Benchmarks

**IMPORTANT:** Benchmark tests are in `benchmark/src/androidTest/` (not `src/main/`). The main test classes are `ComposeBenchmarks.kt` and `ViewBenchmarks.kt` in the androidTest directory.

Run all benchmarks using convenience tasks (recommended):
```bash
./gradlew runBenchmarkCompose       # Compose app benchmarks (full suite)
./gradlew runBenchmarkView          # View app benchmarks (full suite)
./gradlew runAllBenchmarks          # Both sequentially
```

New shortcuts to run a single benchmark class (assemble/install + run, sets the instrumentation args):
```bash
./gradlew runBenchmarkComposeClass   # assembles & installs :app-compose and runs ComposeBenchmarks
./gradlew runBenchmarkViewClass      # assembles & installs :app-view and runs ViewBenchmarks
```

Specify device serial (when multiple devices connected):
```bash
./gradlew runBenchmarkCompose -PdeviceSerial=ABCD12BB3AB
```

Run benchmarks directly via connectedBenchmarkAndroidTest (class-targeted examples):
```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeBenchmarks#coldStartup_compose \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose
```

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ViewBenchmarks#coldStartup_view \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view
```

Run specific test:
```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeBenchmarks#coldStartup_compose
```

### Extracting Results

View HTML test report:
```bash
open benchmark/build/reports/androidTests/connected/benchmark/index.html
```

Additional outputs (Perfetto traces, JSON):
```
benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/<DEVICE_LABEL>/
```

Files include:
- `*.perfetto-trace` - System tracing for detailed frame analysis
- `dev.egarcia.andperf.benchmark-benchmarkData.json` - Benchmark metrics in JSON

Pull from device manually (if needed):
```bash
adb pull /sdcard/Android/media/dev.egarcia.andperf.benchmark/additional_test_output ./results/
```

### Development

Clean build:
```bash
./gradlew clean
```

Install debug builds:
```bash
./gradlew :app-compose:installDebug
./gradlew :app-view:installDebug
```

## Testing Guidelines

### Benchmark Test Structure
Benchmark tests are located in `benchmark/src/androidTest/java/dev/egarcia/andperf/benchmark/`:
- `ComposeBenchmarks.kt`: Compose cold startup and fast scroll benchmarks
- `ViewBenchmarks.kt`: View cold startup and fast scroll benchmarks
- `SmokeBenchmark.kt`: Quick smoke test for validation

Test methods:
1. `coldStartup_compose()`: StartupTimingMetric + FrameTimingMetric, 3 iterations, COLD startup
2. `coldStartup_view()`: StartupTimingMetric-focused (FrameTimingMetric omitted where devices don't surface frames)
3. `fastScroll_compose()`: FrameTimingMetric, 5 iterations, WARM startup, 8 scroll gestures
4. `fastScroll_view()`: Fast scroll gestures for the View app

### Test Features
- **Dynamic targeting**: Tests skip (JUnit Assume) when the expected target package is not installed on the device; prefer running the correct class directly.
- **Error recovery**: View tests include fallback logic for missing frame metrics; tests catch metric collection errors and mark them as skipped to avoid aborting full runs.
- **UiAutomator gestures**: Fast scroll tests automate swipe gestures with `device.swipe()`

### Metrics
- `StartupTimingMetric()`: Time to initial display, time to full display
- `FrameTimingMetric()`: Frame duration (CPU + total), overrun counts, percentiles (p50, p90, p95, p99)

### Package Names
- Compose app: `dev.egarcia.andperf.compose`
- View app: `dev.egarcia.andperf.view`
- Benchmark: `dev.egarcia.andperf.benchmark`

### Device Requirements
- Physical Android device (emulators not recommended for performance benchmarking)
- Android 7.0+ (API 24+)
- Developer options enabled with animations disabled
- Airplane mode recommended
- Device should be cooled between benchmark runs (25-35°C)

## Important Constraints

1. **Parity Required**: Both implementations must use identical data, layout dimensions, fonts, and visual styling for fair comparison
2. **Benchmark Build Type**: Always benchmark using the `benchmark` build variant, not `release` or `debug`
3. **Non-debuggable Apps**: For accurate macrobenchmark results, app build types must have `isDebuggable = false`
4. **No Network**: Tests use locally generated data from `FakeRepo`, no network calls
5. **Reproducibility**: All benchmark parameters (iterations, startup mode, metrics) should be documented
6. **Statistical Rigor**: Results should be analyzed with medians and percentiles (p50, p90, p95, p99)

## Research Methodology

This is an open research project. When adding new benchmarks or modifying implementations:
- Maintain equivalence between Compose and View implementations
- Document all changes that could affect performance
- Use appropriate statistical methods when analyzing results
- Consider thermal throttling and device state when running tests
- Run multiple iterations for statistical validity (10+ recommended for publication)

## Citation

If referencing this study, cite as:
> García García, Erick Josue Gabriel (2025). Compose vs Android View System Performance Benchmark.
> GitHub: https://github.com/e-Garcia/Compose-vs-Android-View-System-Performance