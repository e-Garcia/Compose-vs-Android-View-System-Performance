# Compose vs Android View System Performance Benchmark

A quantitative, open research project comparing **Jetpack Compose** and the **traditional Android View system** under identical UI, dataset, and runtime conditions.  
The goal is to measure and analyze real-device performance differences using reproducible, open benchmarks powered by **AndroidX Macrobenchmark**.

---

## 📖 Abstract

This study evaluates the rendering performance of Jetpack Compose compared to the legacy Android View system when displaying equivalent user interfaces and datasets.  
Benchmarks include cold startup time, first frame latency, jank during fast scrolling, and frame time percentiles (p50, p90, p95, p99).  
All measurements are executed on real physical devices using automated macrobenchmarks to ensure reproducibility and scientific rigor.

---

## 🧩 Repository Structure

```
compose-vs-views/
 ├── app-compose/        → Jetpack Compose implementation
 ├── app-view/           → XML View + RecyclerView implementation
 ├── shared/             → Shared data models and fake repository
 ├── benchmark/          → AndroidX Macrobenchmark tests
 ├── results/            → JSON / CSV benchmark outputs
 └── paper.md            → Research write-up (draft or published version)
```

---

## ⚙️ Methodology

| Aspect | Description |
|--------|--------------|
| **Frameworks compared** | Jetpack Compose (LazyColumn) vs Android Views (RecyclerView) |
| **Test tool** | AndroidX Macrobenchmark v1.2.4 |
| **Metrics** | Cold startup, First frame latency, Scroll jank, Frame time percentiles |
| **Devices** | _(List your test devices)_ |
| **Android versions** | _(e.g., Android 13, 14)_ |
| **Iterations per metric** | 10 per app per compilation mode |
| **Build type** | Release build, R8 minified, identical ProGuard rules |
| **Compilation modes** | None, Partial |
| **Animation settings** | All animations disabled (Developer Options) |
| **Network** | Off (airplane mode) |
| **Thermal state** | Cooled device (25–35 °C) before each run |

### Test Workflow

1. Build and (optionally) install the benchmark and tested apps. Prefer non-debuggable/release-like APKs for accurate results:

   ```bash
   # Assemble release and benchmark apks
   ./gradlew :app-compose:assembleRelease :app-view:assembleRelease :benchmark:assembleBenchmark

   # (Optional) Install benchmark/test APKs to the connected device
   ./gradlew :app-compose:installBenchmark :app-view:installBenchmark :benchmark:installBenchmark
   ```

2. Run Macrobenchmark tests via Gradle — recommended: run the benchmark instrumentation on the connected device serial and explicitly set the benchmark target package. Replace `RFCT71GG5XA` with your device serial (or omit to let Gradle pick a single connected device):

   ```bash
   # Simple run without specifying a device serial (works when only one device is connected)
   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose \
     -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_compose \
     --info --stacktrace
   
   # Run the corresponding View cold-start benchmark (explicit serial)
   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.serial=ABCD12BB3AB \
     -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view \
     -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_view \
     --info --stacktrace

   # Or run both sequentially in your shell (keeps outputs separate)
   ./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_compose --info --stacktrace && \
   ./gradlew :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_view --info --stacktrace
   ```
   
   Note: passing the device serial is optional
   - Omit it when only one device/emulator is connected.
   - It becomes required when multiple devices are attached or when you need deterministic selection (CI).
   - Also note the `--info` and `--stacktrace` flags are optional diagnostic flags (use them for more logging or on failures).

   Note: some projects provide a convenience task (for example `runAllBenchmarks`) — check your `build.gradle.kts` for such wrappers; if present you can run it like:
   ```bash
   ./gradlew runAllBenchmarks --info --stacktrace
   ```

3. Where to find results and traces

   - Autogenerated HTML test report (connected instrumentation report):
     `benchmark/build/reports/androidTests/connected/benchmark/index.html`
     You can open it locally with:

     ```bash
     open benchmark/build/reports/androidTests/connected/benchmark/index.html
     ```

   - Per-device HTML pages (examples):
     `benchmark/build/reports/androidTests/connected/benchmark/dev.egarcia.andperf.benchmark.html`
     `benchmark/build/reports/androidTests/connected/benchmark/dev.egarcia.andperf.benchmark.ComposeViewBenchmarks.html`

   - Additional test outputs and perfetto traces (perfetto traces, benchmark JSON):
     `benchmark/build/outputs/connected_android_test_additional_output/benchmark/connected/<DEVICE_LABEL>/`
     Example files you may find there: `*.perfetto-trace`, `dev.egarcia.andperf.benchmark-benchmarkData.json`.

4. Pull raw files from a device manually (if needed)

   ```bash
   adb pull /sdcard/Android/media/dev.egarcia.andperf.benchmark/additional_test_output ./benchmark/build/outputs/connected_android_test_additional_output/
   ```

5. Aggregate and analyze

   Collect the JSON/CSV outputs from the additional output directory or use the HTML report to inspect per-test timings, then aggregate medians/p90/p95 for final analysis.

---

## 📊 Metrics Collected

| Metric | Description |
|---------|-------------|
| **Cold Startup (ms)** | Time from process start until first frame rendered |
| **First Frame Latency (ms)** | Delay before first visible frame on launch |
| **Frame Time Percentiles (p50–p99)** | Frame render duration distribution during continuous scroll |
| **Jank (%)** | Percentage of frames exceeding 16.6 ms |
| **Memory (MB)** | Peak RSS during scroll (optional) |

---

## 📈 Example Results _(to fill after experiments)_

| Metric | Compose | Views | Δ Difference |
|--------|----------|--------|---------------|
| Cold startup (median) | — ms | — ms | — % |
| First frame latency | — ms | — ms | — % |
| Jank (avg) | — % | — % | — pp |
| Frame time p95 | — ms | — ms | — % |
| Memory peak | — MB | — MB | — MB |

**Preliminary Observation:** _(You’ll summarize trends once results exist — e.g., Compose shows higher p95 frame times but similar startup performance.)_

---

## 🔬 Implementation Details

Both implementations use:
- Identical data model (`Item(id, title, imageRes)`)
- Fixed item height and layout dimensions
- 1,000 locally cached image thumbnails (no network)
- Shared fonts, paddings, and typographic scales
- Same image loader and bitmap decode size
- Identical release build configurations

---

## 🧠 Research Goals

- Quantify real-world performance impact of Compose adoption
- Provide reproducible methodology for teams evaluating migration
- Contribute open, device-verified data to the Android developer community
- Establish a transparent baseline for future Compose performance optimizations

---

## 🗾 How to Reproduce

1. Clone this repository
   ```bash
   git clone https://github.com/e-Garcia/Compose-vs-Android-View-System-Performance.git
   ```
2. Build the release APKs
   ```bash
   ./gradlew :app-compose:assembleRelease :app-view:assembleRelease :benchmark:assembleBenchmark
   ```
3. Install and run benchmarks on connected physical device(s).
4. Export results from `/results/` and compare using your favorite data-analysis tool.

---

## 📚 Citation

If referencing this study, please cite as:

> **García García, Erick Josue Gabriel (2025). Compose vs Android View System Performance Benchmark.**  
> GitHub Repository: [https://github.com/e-Garcia/Compose-vs-Android-View-System-Performance](https://github.com/e-Garcia/Compose-vs-Android-View-System-Performance)  
> Open Research Project, Licensed under MIT.

---

## 🗳 License

This project is released under the **MIT License**.  
You are free to use, reproduce, and extend this research with attribution.

---

## 📣 Acknowledgments

Thanks to the AndroidX team for Macrobenchmark tooling and to the open-source community for continuous contributions to Android performance research.
