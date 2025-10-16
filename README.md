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

1. Clean install both apps.
2. Run Macrobenchmark tests via Gradle:
   ```bash
   ./gradlew :benchmark:connectedBenchmarkAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.eg.benchmark.ComposeVsViewBenchmark
   ```
3. Extract JSON or CSV results:
   ```bash
   adb pull /sdcard/Android/data/com.eg.benchmark/files/metrics/ ./results/
   ```
4. Aggregate median and percentile data for analysis.

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
