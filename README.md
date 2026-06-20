# Compose vs Android View System Performance Benchmark

A quantitative, open research project comparing **Jetpack Compose** and the
**traditional Android View system** under controlled UI, dataset, and runtime
conditions.

The current checked-in baseline is intentionally small: it compares a
Compose `LazyColumn` against an Android View `RecyclerView`, each rendering the
same generated text-only dataset. The long-term research target is to evolve
that baseline into stricter visual parity, richer rows, and retained
device-verified benchmark artifacts before publishing numeric conclusions.

---

## 📖 Abstract

This study evaluates the rendering performance of Jetpack Compose compared to
the legacy Android View system when displaying equivalent user interfaces and
datasets. The source currently contains startup-focused AndroidX
Macrobenchmark tests for both app variants and includes `FrameTimingMetric()`
where supported by the runtime.

Planned benchmark coverage includes cold startup, first frame latency, jank
during fast scrolling, and frame time percentiles (p50, p90, p95, p99). Numeric
results should only be added after the raw JSON/HTML/perfetto outputs are
retained under documented device and run conditions.

---

## 🧩 Repository Structure

```
compose-vs-views/
 ├── app-compose/        → Jetpack Compose implementation
 ├── app-view/           → XML View + RecyclerView implementation
 ├── shared/             → Shared data models and fake repository
|benchmark/          → AndroidX Macrobenchmark tests
| ├── results/            → Tracked benchmark result policy and curated summaries
| ├── benchmark/          → AndroidX Macrobenchmark test APK sources
| ├── results/            → JSON / CSV benchmark outputs
 └── paper.md            → Research write-up (draft or published version)
```

---

## ⚙️ Methodology

| Aspect | Description |
|--------|--------------|
| **Frameworks compared** | Jetpack Compose (LazyColumn) vs Android Views (RecyclerView) |
| **Test tool** | AndroidX Macrobenchmark via the version catalog (`androidx.benchmark`) |
| **Current metrics** | Startup timing; frame timing when the device/runtime produces frame samples |
| **Planned metrics** | Cold startup, first frame latency, scroll jank, frame time percentiles, optional memory |
| **Devices** | _(List your test devices)_ |
| **Android versions** | _(e.g., Android 13, 14)_ |
| **Current iterations** | 3 per checked-in startup benchmark |
| **Target iterations** | 10 per app per compilation mode before publishing results |
| **Current build type** | Benchmark variants signed with debug signing; release-like measurement setup still needs validation |
| **Target build type** | Release or release-like builds, R8 minified where appropriate, identical ProGuard rules |
| **Compilation modes** | Planned: None, Partial |
| **Animation settings** | All animations disabled (Developer Options) |
| **Network** | Off (airplane mode) |
| **Thermal state** | Cooled device (25–35 °C) before each run |

### Benchmark source sets

The `benchmark` module uses the Android Gradle Plugin `com.android.test` plugin.
Its runnable Macrobenchmark classes live in the module's main source set:

- `benchmark/src/main/java/dev/egarcia/andperf/benchmark/ComposeViewBenchmarks.kt`
- `benchmark/src/main/java/dev/egarcia/andperf/benchmark/SmokeBenchmark.kt`

Do not add duplicate benchmark classes with the same package and class name under
`benchmark/src/androidTest/`; the benchmark APK is assembled from the module's
variant sources (for example `assembleBenchmark` / `assembleAndroidTest`), and
keeping one source-set owner avoids ambiguous documentation and duplicate test
definitions.

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
     -PbenchmarkTarget=:app-compose \
     -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose \
     -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_compose \
     --info --stacktrace
   
   # Run the corresponding View cold-start benchmark (explicit serial)
   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
     -PbenchmarkTarget=:app-view \
     -Pandroid.testInstrumentationRunnerArguments.serial=ABCD12BB3AB \
     -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view \
     -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_view \
     --info --stacktrace

   # Or run both sequentially in your shell (keeps outputs separate)
   ./gradlew :benchmark:connectedBenchmarkAndroidTest -PbenchmarkTarget=:app-compose -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.compose -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_compose --info --stacktrace && \
   ./gradlew :benchmark:connectedBenchmarkAndroidTest -PbenchmarkTarget=:app-view -Pandroid.testInstrumentationRunnerArguments.benchmarkTargetPackage=dev.egarcia.andperf.view -Pandroid.testInstrumentationRunnerArguments.class=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#coldStartup_view --info --stacktrace
   ```
   
   Note: passing the device serial is optional
   - Omit it when only one device/emulator is connected.
   - It becomes required when multiple devices are attached or when you need deterministic selection (CI).
   - Also note the `--info` and `--stacktrace` flags are optional diagnostic flags (use them for more logging or on failures).

   Note: this project also provides convenience tasks for target-specific benchmark runs:
   ```bash
   # Verify task wiring without requiring a device.
   bash ./gradlew runBenchmarkComposeClass --dry-run
   bash ./gradlew runBenchmarkViewClass --dry-run

   # Run one default cold-start method against each intended app package.
   bash ./gradlew runBenchmarkComposeClass --info --stacktrace
   bash ./gradlew runBenchmarkViewClass --info --stacktrace

   # Override the class/method while preserving the target app/package wiring.
   bash ./gradlew runBenchmarkViewClass \
     -PbenchmarkClass=dev.egarcia.andperf.benchmark.ComposeViewBenchmarks#scroll_view \
     --info --stacktrace
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

5. Preserve, aggregate, and analyze

   Keep generated build artifacts out of Git. For each verified physical-device run, copy only curated summaries/manifests into `results/` and record where the raw JSON, HTML, and perfetto artifacts were retained. See [`results/README.md`](results/README.md) for the artifact policy.

---

## 📊 Metrics Collected and Planned

| Metric | Description |
|---------|-------------|
| **Cold Startup (ms)** | Current startup benchmark target; time from process start until first frame rendered |
| **Frame timing samples** | Requested by current startup tests with `FrameTimingMetric()`; availability may vary by device/runtime |
| **First Frame Latency (ms)** | Planned reporting field derived from startup output where supported |
| **Frame Time Percentiles (p50–p99)** | Planned for continuous scroll benchmarks, not yet published as verified results |
| **Jank (%)** | Planned for continuous scroll benchmarks, not yet published as verified results |
| **Memory (MB)** | Optional future metric, not implemented in the current baseline |

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

### Current text-only baseline

The current checked-in apps share a deterministic, text-only dataset:
- Shared data model: `Item(id, title, subtitle)`.
- `FakeRepo.items()` generates 1,000 rows by default with titles and subtitles only; it does not load images or perform network requests.
- The Compose app renders the list with `LazyColumn` and a single `Text` containing both title and subtitle.
- The View app renders the list with `RecyclerView` and `item_row.xml`, currently a `56dp` row with `12dp` padding and separate title/subtitle `TextView`s (`16sp` and `14sp`).
- Release/build configuration parity should be verified before publishing measurements.

### Planned visual-parity target

Future benchmark-result runs should either keep this text-only baseline documented, or first update both implementations to an explicitly matched visual target. Planned parity items include matching row height, padding, typography, text structure, and any image-thumbnail/image-loader behavior before reporting Compose vs View frame/jank results.

### Current row parity checklist

The Compose `LazyColumn` row and View `RecyclerView` row are intentionally aligned before collecting benchmark results:

| Row attribute | Compose implementation | View implementation |
|---------------|------------------------|---------------------|
| Container width | `fillMaxWidth()` | `layout_width="match_parent"` |
| Container height | `56.dp` | `layout_height="56dp"` |
| Container padding | `12.dp` | `android:padding="12dp"` |
| Title text | Separate `Text`, `16.sp` | Separate `TextView`, `16sp` |
| Subtitle text | Separate `Text`, `14.sp` | Separate `TextView`, `14sp` |

Future image or thumbnail scenarios should be added to both implementations together, with matching decode size and loader behavior documented before publishing those measurements.

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
4. Export generated benchmark outputs from `benchmark/build/outputs/connected_android_test_additional_output/`, retain the raw artifacts, and add a curated summary or manifest under `results/` before comparing results.

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
