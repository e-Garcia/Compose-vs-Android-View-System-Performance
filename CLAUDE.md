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

**IMPORTANT:** Benchmark tests actually live in `benchmark/src/main/java/dev/egarcia/andperf/benchmark/` (this is a `com.android.test` module, so its "main" source set — not `androidTest` — is what gets compiled into the instrumentation APK). The runnable classes are `ComposeBenchmarks.kt`, `ViewBenchmarks.kt`, and `SmokeBenchmark.kt`.
There is a leftover `benchmark/src/androidTest/java/.../BenchmarkUtils.kt` file that is **not** part of the build for this module (dead code from an earlier metric-capability refactor — see Known Issues below). Do not edit it expecting it to affect test behavior; edit `benchmark/src/main/.../BenchmarkUtils.kt` instead.

> **Known broken tasks:** `runBenchmarkCompose`, `runBenchmarkView`, and `runAllBenchmarks` (below) currently fail immediately with `Task ... not found` — verified with `./gradlew runBenchmarkCompose --dry-run`. `runBenchmarkCompose`/`runAllBenchmarks` depend on `:benchmark:benchmarkComposeRun`, which is never defined anywhere in the build. `runBenchmarkView` depends on `:benchmark:benchmarkViewRun`, but that task is registered on the **root** project, not inside `:benchmark`. Until `build.gradle.kts` is fixed, use `runBenchmarkComposeClass` / `runBenchmarkViewClass` or the raw `connectedBenchmarkAndroidTest` invocations further below instead.

Run all benchmarks using convenience tasks (**currently broken, see note above**):
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
Benchmark tests are located in `benchmark/src/main/java/dev/egarcia/andperf/benchmark/` (see the source-set note under "Running Benchmarks" above):
- `ComposeBenchmarks.kt`: Compose cold startup and fast scroll benchmarks
- `ViewBenchmarks.kt`: View cold startup and fast scroll benchmarks
- `SmokeBenchmark.kt`: Quick smoke test for validation

Test methods:
1. `coldStartup_compose()`: requests StartupTimingMetric + FrameTimingMetric via `measureStartupWithCapabilityReport()`, 3 iterations, COLD startup — the metric-capability probe (`shared/.../capability/`) drops any metric the device can't actually surface before measuring
2. `coldStartup_view()`: same capability-aware helper and requested metrics as `coldStartup_compose()`, for the View app
3. `fastScroll_compose()`: FrameTimingMetric, 5 iterations, WARM startup, 8 scroll gestures
4. `fastScroll_view()`: Fast scroll gestures for the View app

### Test Features
- **Dynamic targeting**: Tests skip (JUnit Assume) when the expected target package is not installed on the device; prefer running the correct class directly.
- **Error recovery**: tests wrap `measureRepeated` in `catch (t: Throwable)` and convert any failure into a JUnit `Assume` skip. This is broader than intended — it also swallows real assertion failures/crashes/bugs as "skipped" instead of "failed." If you're debugging a benchmark that mysteriously reports as skipped, check for a real exception being caught here before assuming it's a metric-availability problem.
- **UiAutomator gestures**: Fast scroll tests automate swipe gestures with `device.swipe()`; the 150ms `Thread.sleep` between swipes and fixed 50ms swipe duration are hardcoded, not tied to device refresh rate.

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

1. **Parity Required**: Both implementations must use identical data, layout dimensions, fonts, and visual styling for fair comparison. Current gap: the row-parity checklist in README.md only covers size/height/padding — it does not pin `textColor`/font family, so Compose's Material3 `Text` (which inherits `onSurface` theming) and the View's plain `TextView` (no explicit `textAppearance`) are not guaranteed to render identically.
2. **Benchmark Build Type**: Always benchmark using the `benchmark` build variant, not `release` or `debug`
3. **Non-debuggable Apps**: For accurate macrobenchmark results, app build types must have `isDebuggable = false` (this is honored today — `isDebuggable = false` on all three app/benchmark modules — but `isMinifyEnabled = false` on the same build type means R8/ProGuard optimization is *not* applied, so results don't yet reflect a fully optimized release build)
4. **No Network**: Tests use locally generated data from `FakeRepo`, no network calls
5. **Reproducibility**: All benchmark parameters (iterations, startup mode, metrics) should be documented
6. **Statistical Rigor**: Results should be analyzed with medians and percentiles (p50, p90, p95, p99)

## Known Issues / Audit Findings (2026-07-23, updated 2026-07-23)

A full review of source, build config, and docs surfaced the following.

### Resolved

- **Broken root-level benchmark tasks** — fixed. `runBenchmarkCompose`/`runBenchmarkView`/`runAllBenchmarks` in `build.gradle.kts` referenced Gradle tasks that didn't exist where expected. Added the missing `benchmarkComposeRun` task and corrected the `:benchmark:benchmarkViewRun` references (the task is registered on the root project, not `:benchmark`). Verified with `./gradlew runBenchmarkCompose runBenchmarkView runAllBenchmarks --dry-run` — resolves cleanly now.
- **Orphaned metric-capability architecture** — wired in. `sanitizedMetricsForBenchmark`/`measureStartupWithCapabilityReport` moved from the dead, uncompiled `benchmark/src/androidTest/.../BenchmarkUtils.kt` into the real `benchmark/src/main/.../BenchmarkUtils.kt` (fixing a wrong `androidx.benchmark.macro.MacrobenchmarkRule` import along the way — should have been the `.junit4` package, which is likely why this file was never added to the build). `ComposeBenchmarks.coldStartup_compose()` and `ViewBenchmarks.coldStartup_view()` now call `measureStartupWithCapabilityReport()` instead of hardcoding a metrics list. `benchmark/build.gradle.kts` now depends on `libs.kotlinx.serialization.json` directly (`:shared` only exposed it as `implementation`, not transitive). The dead androidTest file (and its now-empty leftover subdirectories) was deleted. Fast-scroll tests and `SmokeBenchmark.kt` were left as-is (custom swipe `measureBlock`, doesn't fit the cold-start-shaped helper) — could be generalized later if wanted.
- **Results artifact policy vs. reality** — fixed. Added `results/run-*.json`, `results/run-*/`, and `results/*additionaltestoutput*.txt` to `.gitignore` so raw per-run dumps can't accidentally get committed, matching `results/README.md`'s curated-summaries-only policy.
- **Conflicting results documents / suspicious run provenance** — corrected. The "2026-08-07 emulator run" cited in `README.md` and `results/run-manifest-2026-08-07.md` was mislabeled: the raw perfetto-trace filenames and artifact mtimes under `results/` show the run actually happened **2026-07-06**. Renamed the manifest and raw artifact paths to `2026-07-06`, added a correction note in the manifest, and updated `README.md`'s references. The numbers themselves were kept (only the date was wrong). `README.md` and `benchmark-results.md` (2026-07-23 run) now cross-reference each other as separate dated runs instead of looking like unrelated, conflicting claims. The `results/run-manifest-*.md` operator field still says "Hermes Agent (cron executor)" — unverified, not re-investigated this pass.
- **Item counts in `benchmark-results.md`** (found in PR #25 review) — the Test Matrix claimed 56/500 scroll items; both apps call `FakeRepo.items()` with the unoverridden default of 1000. Corrected to 1000 across all rows.
- **`README.md`'s "Maintenance status (2026-08-07)" header** — same shape of problem as the run-date issue above, from the same source commit (`f582b68`, which changed the header from `2026-05-31` to `2026-08-07`, committed 2026-07-09). Corrected to `2026-07-09` (the commit date — the only real evidence of when the text was written). Also removed a stale claim that `local.properties` pointed at a nonexistent macOS SDK path: `local.properties` is gitignored/per-developer by design, so it shouldn't have been asserted as a persistent repo fact in the first place, and it's currently untrue here (points at a valid Linux SDK path, and every Gradle command run this session succeeded).
- **`app-view`/`app-compose` `MainActivity` base-class asymmetry** (tracked as TODO #6 in `benchmark-results.md`, also flagged in an untracked `REVIEW.md` in this checkout) — `app-view.MainActivity` extended `AppCompatActivity` while `app-compose.MainActivity` uses the lighter `ComponentActivity`, a real confound for a study whose core claim rests on a fair cold-start comparison. Switched `app-view` to `ComponentActivity`; verified `assembleBenchmark`/`assembleRelease` (incl. `lintVitalRelease`) still pass without the AppCompat delegate (no ActionBar/Toolbar usage, no vector-drawable back-compat needed at minSdk 24, so `Theme.MaterialComponents.DayNight.NoActionBar` still resolves). **Not yet re-benchmarked on a device** — this may shift the cold-start numbers in `benchmark-results.md`/`README.md`, which were measured against the old `AppCompatActivity` version.
- **Duplicated swipe-gesture code**: `fastScroll_compose()`/`fastScroll_view()` had byte-for-byte identical scroll logic inlined in each `measureBlock`. Extracted to `BenchmarkUtils.performFastScrollGestures()`; pure code motion, no behavior change.
- **An untracked `REVIEW.md` in this checkout contains at least one incorrect finding** — its P0 claim that `app-view/ItemAdapter` needs `DiffUtil`/`ListAdapter` because "1000 items rebind every change" doesn't hold: `MainActivity` constructs the adapter once with a fixed, immutable `List<Item>` and never calls `notifyDataSetChanged()` or any other update — there are no changes to rebind on. Don't implement that recommendation without re-verifying it first; treat `REVIEW.md`'s other claims (e.g. `Thread.sleep(150)` flakiness) as unverified until independently checked against source, same as any other undated/unattributed doc in this repo.

### Still open

- **Overly broad exception handling**: see "Test Features" above — `catch (t: Throwable)` in the benchmark tests can hide real bugs as skipped tests. Not addressed (behavior-changing, wants a device to verify against).
- **Nested Gradle invocation**: `runBenchmarkComposeClass`/`runBenchmarkViewClass` shell out to `bash ./gradlew ...` via `Exec`, which is the pattern the code comment on `assembleInstallCompose` explicitly says to avoid (nested Gradle daemons). Works today but is inconsistent with the rest of the file and fragile under CI.
- **Still emulator-only**: despite CLAUDE.md's own device requirement ("Physical Android device (emulators not recommended)"), all published results to date are from an Android 16 SwiftShader (software-rendered) emulator. Keep the disclaimers prominent until a physical-device run exists. This is the single biggest blocker to trusting any number in this repo, including the ones from this session's fixes.
- **Re-benchmark after the `ComponentActivity` change** — the published cold-start numbers no longer reflect what `app-view` actually does at `onCreate()`. Needs a real device/emulator run, not something this session could do.
- **`Thread.sleep(150)` in the (now-shared) scroll gesture helper** — fixed-duration sleeps are simple and reproducible but add ~1.2s of dead time per fast-scroll iteration and aren't tied to actual gesture completion. Worth a look if fast-scroll benchmark runtime or flakiness becomes a problem; not changed this pass since it's behavior-affecting test code and there's no device here to validate against.

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