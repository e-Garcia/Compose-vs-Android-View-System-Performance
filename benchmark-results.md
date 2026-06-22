# Compose vs Android View — Benchmark Results

> **Emulator only.** Results from Android 16 emulator (Pixel 8, API 36, SwiftShader software rendering).
> Do **not** use for production comparisons — see disclaimer below.

## Cold Startup — timeToInitialDisplayMs

| Metric | Compose (cold) | View (cold) |
|--------|---------------|-------------|
| min    | 330.7 ms      | **255.0 ms**  |
| median | 333.1 ms      | **262.1 ms**  |
| max    | 433.1 ms      | **274.3 ms**  |

## View Bonus — Frame Render Metrics (Compose only)

| Metric | Value |
|--------|-------|
| frameCount (all iterations) | 1.0 (single frame) |
| frameDurationCpu — P50 | 154.1 ms |
| frameDurationCpu — P90 | 154.9 ms |
| frameDurationCpu — P95 | 155.0 ms |
| frameDurationCpu — P99 | 155.1 ms |
| frameOverrunMs — P50 | 222.0 ms |
| frameOverrunMs — P90 | 227.4 ms |
| frameOverrunMs — P95 | 228.1 ms |
| frameOverrunMs — P99 | 228.6 ms |

## Notes

- Device: `sdk_gphone64_x86_64` (Pixel 8 emulation), Android 16 (API 36)
- GPU: SwiftShader (software rendering) — **does not represent real hardware**
- Compose shows P50 frameDurationCpu of 154ms; View frame metrics not captured in separate run
- 3 iterations per benchmark; `perfetto-trace` files saved in `benchmark/build/outputs/connected_android_test_additional_output/`
- The `timeToInitialDisplay` metric measures the time from process start to the first frame being drawn to screen

## ⚠️ Disclaimer

> **These results are from an emulator with software rendering.**
> Real device benchmarks may show different relative performance between Compose and View.
> Software-rendered CPU costs on an emulator do **not** accurately reflect GPU-accelerated
> performance on a physical Pixel or any Android phone. Use with caution —
> the relative ranking (View ~22% faster cold-start on this emulator) is not guaranteed
> to hold on real hardware.

## Artifacts

Perfection trace files from each run:

| Benchmark | File |
|-----------|------|
| Compose iter 0 | `ComposeBenchmarks_coldStartup_compose_iter000_2026-06-22-18-19-06.perfetto-trace` |
| Compose iter 1 | `ComposeBenchmarks_coldStartup_compose_iter001_2026-06-22-18-19-08.perfetto-trace` |
| Compose iter 2 | `ComposeBenchmarks_coldStartup_compose_iter002_2026-06-22-18-19-10.perfetto-trace` |
| View iter 0 | `ViewBenchmarks_coldStartup_view_iter000_2026-06-22-18-21-00.perfetto-trace` |
| View iter 1 | `ViewBenchmarks_coldStartup_view_iter001_2026-06-22-18-21-02.perfetto-trace` |
| View iter 2 | `ViewBenchmarks_coldStartup_view_iter002_2026-06-22-18-21-03.perfetto-trace` |

All located under:
`benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/Reldoc_API36_1(AVD) - 16/`
