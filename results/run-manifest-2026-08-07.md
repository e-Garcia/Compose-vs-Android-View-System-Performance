# Run Manifest: 2026-08-07 (executed 2026-08-07, committed 2026-08-07)

## Run Summary

- **Operator:** Hermes Agent (cron executor)
- **Run date:** 2026-08-07
- **Run type:** Emulator (sdk_gphone64_x86_64, Android 16 / API 36)
- **Device serial:** emulator-5554 (emulator, not physical device)
- **Emulator image:** google/sdk_gphone64_x86_64:16/BE4B.251210.005/14574095:userdebug/dev-keys
- **CPU cores:** 4 (non-locked, max 2 GHz)
- **RAM:** 1.92 GB (2,061,959,168 bytes)
- **Compilation mode:** run-from-apk (non-debuggable release-like)
- **Animations:** All disabled (per README methodology)
- **Network:** Assumed off (airplane mode — not explicitly verified in cron environment)
- **Thermal:** Not instrumented (emulator default cooling)
- **Sustained performance mode:** Disabled

## Benchmark Commands

The benchmarks were executed via the convenience Gradle tasks (which assemble, install, and run):

```bash
# Compose cold-start benchmark
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeBenchmarks#coldStartup_compose \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose \
  --info --stacktrace

# Compose fast-scroll benchmark
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeBenchmarks#fastScroll_compose \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose \
  --info --stacktrace

# View cold-start benchmark
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ViewBenchmarks#coldStartup_view \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view \
  --info --stacktrace

# View fast-scroll benchmark
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ViewBenchmarks#fastScroll_view \
  -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view \
  --info --stacktrace
```

## Retained Artifacts (in `results/` directory)

| Artifact | Path | Size |
|----------|------|------|
| Compose benchmark JSON | `results/run-2026-08-07-compose/dev.egarcia.andperf.benchmark-benchmarkData.json` | 185 KB |
| Compose cold-start text | `results/run-2026-08-07-compose/additionaltestoutput.benchmark.message_dev.egarcia.andperf.benchmark.ComposeBenchmarks.coldStartup_compose.txt` | 1.5 KB |
| Compose fast-scroll text | `results/run-2026-08-07-compose/additionaltestoutput.benchmark.message_dev.egarcia.andperf.benchmark.ComposeBenchmarks.fastScroll_compose.txt` | 1.3 KB |
| View benchmark JSON | `results/run-2026-08-07-view.json` | 186 KB |
| View cold-start text | `results/additionaltestoutput.benchmark.message_dev.egarcia.andperf.benchmark.ViewBenchmarks.coldStartup_view.txt` | 907 B |
| View fast-scroll text | `results/additionaltestoutput.benchmark.message_dev.egarcia.andperf.benchmark.ViewBenchmarks.fastScroll_view.txt` | 1.2 KB |

## Curated Benchmark Results

All values extracted from the JSON (`benchmarkData.json`) and text outputs above.

### Cold Startup (timeToInitialDisplayMs — median)

| Metric | Compose | View | Δ (View − Compose) |
|--------|---------|------|---------------------|
| Cold startup (ms, median) | 353.9 | 300.7 | −53.2 ms (View faster) |
| Cold startup (ms, min) | 326.5 | 266.8 | −59.7 ms (View faster) |
| Cold startup (ms, max) | 360.1 | 341.6 | −18.5 ms (View faster) |
| Sample count | 3 iterations | 3 iterations | — |

**Interpretation:** The View implementation starts approximately 15% faster than Compose on this emulator build (300.7 ms vs 353.9 ms median). This is expected on Android 16 (API 36) where the View system has mature rendering pipelines, while Compose still pays composition overhead on first launch. On physical devices, the gap may narrow or reverse depending on the device's GPU and Compose compiler version.

### Fast Scroll — Frame Count (frames per run)

| Metric | Compose | View | Δ (View − Compose) |
|--------|---------|------|---------------------|
| Frames (median) | 472 | 478 | +6 frames (View keeps slightly more) |
| Frames (min) | 469 | 477 | +8 frames (View keeps more) |
| Frames (max) | 473 | 479 | +6 frames (View keeps more) |
| Coefficient of variation | 0.00349 | 0.00209 | View more consistent |
| Sample count | 5 runs | 5 runs | — |

**Interpretation:** Both implementations keep very high frame counts (469–479 out of ~480 frames expected for 8-second scrolls at 60fps). The View system edges out Compose by ~6 frames on median, with slightly lower coefficient of variation (0.21% vs 0.35%), indicating marginally more consistent scroll behavior on this emulator.

### Frame Time Percentiles (frameDurationCpuMs)

| Percentile | Compose fastScroll | View fastScroll | Δ (View − Compose) |
|------------|-------------------|-----------------|---------------------|
| P50 | 6.6 ms | 6.3 ms | −0.3 ms (View faster) |
| P90 | 7.7 ms | 7.7 ms | 0 ms (equal) |
| P95 | 8.1 ms | 8.0 ms | −0.1 ms (View faster) |
| P99 | 9.0 ms | 8.6 ms | −0.4 ms (View faster) |

**Interpretation:** The View system is marginally faster across all frame-time percentiles during fast scrolling, but the differences are negligible (sub-millisecond). Both stay well within the 16.67 ms budget for 60fps. The View system's P99 is 8.6 ms vs Compose's 9.0 ms — both indicate good headroom.

### Frame Overrun (FrameTimingMetric)

| Percentile | Compose fastScroll | View fastScroll |
|------------|-------------------|-----------------|
| P50 | −9.2 ms | −9.5 ms |
| P90 | −8.0 ms | −8.3 ms |
| P95 | −7.7 ms | −8.0 ms |
| P99 | −6.9 ms | −7.4 ms |

*Negative values indicate frames rendered ahead of schedule (headroom). Both implementations show substantial headroom.*

## Summary

This run produced verified benchmark results for four test scenarios (Compose cold-start, Compose fast-scroll, View cold-start, View fast-scroll) on an Android 16 emulator (sdk_gphone64_x86_64). The raw JSON artifacts (`run-2026-08-07-compose/`, `run-2026-08-07-view.json`) and text outputs are retained under `results/` and serve as the single source of truth for the values cited in the README results table above.

All raw JSON and text artifacts are under `results/`. No perfetto trace files are included in this commit (see `results/README.md` artifact policy for the trace retention rule).
